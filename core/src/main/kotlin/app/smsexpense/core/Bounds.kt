package app.smsexpense.core

import java.math.BigDecimal

/** Limits on what a message can put in the ledger, applied before anything is stored (#217). */
object Bounds {
    /** No real payment is this large, even in currencies counted in millions; a longer number is not a payment. */
    val MAX_AMOUNT = BigDecimal("999999999999.99")

    /** Room for long legal names ("Amazon Pay India Private Limited") without filling the notification. */
    const val MERCHANT_MAX = 60

    /**
     * The reading with its payee cleaned up, or null when the amount is zero or out of range. (A negative amount
     * can't get this far: [Money] refuses one.)
     * Invisible format characters (Unicode Cf, which includes the direction overrides like U+202E) and control
     * characters are removed, so a payee can't be made to display as something else; spacing is collapsed.
     */
    fun check(reading: SmsReading.Transaction): SmsReading.Transaction? {
        if (reading.money.amount.signum() == 0 || reading.money.amount > MAX_AMOUNT) return null
        return reading.copy(merchant = reading.merchant?.let(::cleanMerchant))
    }

    fun cleanMerchant(raw: String): String? =
        raw.filterNot { Character.getType(it) == Character.FORMAT.toInt() || Character.isISOControl(it) && !it.isWhitespace() }
            .replace(Regex("""\s+"""), " ")
            .trim()
            .let { if (it.length <= MERCHANT_MAX) it else it.take(MERCHANT_MAX).let { cut -> if (cut.last().isHighSurrogate()) cut.dropLast(1) else cut } }
            .trim()
            .takeIf { it.isNotEmpty() }
}
