package app.smsexpense.core

import java.math.BigDecimal

/** What the parser made of one message. */
sealed interface SmsReading {
    data class Transaction(
        val direction: Direction,
        val money: Money,
        /** The payee or payer exactly as the message wrote it, or null when the message does not say. */
        val merchant: String?,
        /** The last digits of the account or card the message names, or null. */
        val account: String?,
    ) : SmsReading

    data class Ignored(val reason: IgnoreReason) : SmsReading

    /** Looked like a transaction but the amount could not be read. Left for the on-device model (#14). */
    data object NotUnderstood : SmsReading
}

/**
 * Rule-based reader for bank, card, UPI and mobile money messages.
 * Every amount it returns appears in the message text; it never guesses one.
 */
object SmsParser {
    private fun r(p: String) = Regex(p, RegexOption.IGNORE_CASE)

    private const val NUMBER = """(\d{1,3}(?:,\d{2,3})+|\d+)(?:\.(\d{1,2}))?"""
    // Currency words can't follow a letter ("hrs 30" is not Rs 30, #31); symbols can ("Paid₹250", #154).
    private val currencyToken = """((?<![A-Za-z])(?:Rs\.?|INR|USD|US\$|AED|GBP|EUR|PKR|NGN|Ksh|KES)|₹|\$|£|€)"""
    private val moneyWithCurrency = r("""$currencyToken\s?$NUMBER""")
    private val bareAmountAfterVerb = r("""\b(?:debited|credited) by\s+$NUMBER""")
    private val notTheAmount = r("""(bal|balance|limit|lmt|due)[^\d]{0,12}$""")

    private val currencyCodes = mapOf(
        "rs" to "INR", "rs." to "INR", "inr" to "INR", "₹" to "INR",
        "usd" to "USD", "us$" to "USD", "$" to "USD", "aed" to "AED", "gbp" to "GBP", "£" to "GBP",
        "eur" to "EUR", "€" to "EUR", "pkr" to "PKR", "ngn" to "NGN", "ksh" to "KES", "kes" to "KES",
    )

    private val debitWords = r("""\b(debited|spent|paid|withdrawn|purchase|charged)\b|\bsent\b(?! you)|\bDr\.|\bDR\b|was used for|transaction was made|done at ATM""")
    private val creditWords = r("""\b(credited|deposited|received|refund|reversed)\b|\bsent you\b""")

    private val account = r("""\b(?:a/?c|acct|account|card)\b(?:\s*(?:no\.?|ending|:))?\s*[x*]*\s*(\d{3,5})\b""")

    // Merchant patterns, most specific first. Group 1 is the merchant.
    private val stop = """(?=\s+on\b|\s+from\b|\s+Ref\b|\s+\d|\s*\(|\.\s|\.$|\n|;|$)"""
    private val upiSlashTyped = r("""UPI/P2[AM]/\d+/([^\n/]+)""")
    private val upiSlashName = r("""UPI/([A-Za-z][^/\n]*)/\d+""")
    private val nach = r("""NACH-ACH-DR\s+(.+?)(?=\.\s|\.$|$)""")
    private val neft = r("""NEFT Cr-[A-Z0-9]+-(.+?)-""")
    private val vpa = r("""\b(?:to|from)\s+(?:VPA\s+)?([\w.\-]+@[a-z]+)""")
    private val semicolonCredited = r(""";\s*(.+?) credited""")
    private val trfTo = r("""trf to (.+?)\s+Ref""")
    private val desc = r("""Desc:\s*(?:POS PURCHASE\s+)?(.+)""")
    private val sentYou = r("""^(.+?) sent you""")
    private val at = r("""\bat\s+(.+?)(?=\s+on\b|\.\s|\.$|\n|$)""")
    private val to = r("""\bto\s+(.+?)$stop""")
    private val onName = r("""\bon ([A-Z][A-Z0-9&' ]+[A-Z0-9])\.""")
    private val from = r("""\bfrom\s+(.+?)$stop""")

    private val atm = r("""^ATM\b""")

    fun parse(source: Source, sender: String, text: String, defaultCurrency: String = "INR"): SmsReading {
        TransactionFilter.check(source, sender, text)?.let { return SmsReading.Ignored(it) }
        val money = readMoney(text, defaultCurrency) ?: return SmsReading.NotUnderstood
        val direction = readDirection(text) ?: return SmsReading.NotUnderstood
        return SmsReading.Transaction(direction, money, readMerchant(text, direction), account.find(text)?.groupValues?.get(1))
    }

    private fun readMoney(text: String, defaultCurrency: String): Money? {
        for (m in moneyWithCurrency.findAll(text)) {
            if (notTheAmount.containsMatchIn(text.substring(0, m.range.first))) continue
            val code = currencyCodes.getValue(m.groupValues[1].lowercase())
            return Money(toDecimal(m.groupValues[2], m.groupValues[3]), code)
        }
        val bare = bareAmountAfterVerb.find(text) ?: return null
        return Money(toDecimal(bare.groupValues[1], bare.groupValues[2]), defaultCurrency)
    }

    private fun toDecimal(whole: String, fraction: String) =
        BigDecimal(whole.replace(",", "") + if (fraction.isEmpty()) "" else ".$fraction")

    /** The earliest money verb decides: "debited ... SWIGGY credited" is a debit. */
    private fun readDirection(text: String): Direction? {
        val d = debitWords.find(text)?.range?.first
        val c = creditWords.find(text)?.range?.first
        return when {
            d == null && c == null -> null
            c == null -> Direction.DEBIT
            d == null -> Direction.CREDIT
            else -> if (d < c) Direction.DEBIT else Direction.CREDIT
        }
    }

    private fun readMerchant(text: String, direction: Direction): String? {
        val shared = listOf(upiSlashTyped, upiSlashName, nach, neft, vpa, semicolonCredited, trfTo, desc, sentYou)
        val ordered = shared + if (direction == Direction.DEBIT) listOf(at, to, onName) else listOf(from)
        for (pattern in ordered) {
            for (m in pattern.findAll(text)) {
                val candidate = m.groupValues[1].trim().trimEnd('.', ',')
                if (!looksLikeMerchant(candidate)) continue
                return if (atm.containsMatchIn(candidate)) "ATM" else candidate
            }
        }
        return null
    }

    private fun looksLikeMerchant(s: String): Boolean =
        s.length in 2..40 &&
            !s.first().isDigit() &&
            !s.startsWith("your", ignoreCase = true) &&
            !s.all { it.isDigit() || it == 'X' || it == 'x' } &&
            !Regex("""(?i)^(hdfc|icici|sbi|axis|kotak)?\s*bank\b""").containsMatchIn(s) &&
            !Regex("""(?i)^paytm balance""").containsMatchIn(s)
}
