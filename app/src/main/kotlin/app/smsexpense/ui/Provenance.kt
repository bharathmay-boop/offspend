package app.smsexpense.ui

import app.smsexpense.data.SightingEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** One line of "Where this came from" on the category sheet (#171). [payee] shows only when the sources disagree. */
data class SourceLine(val text: String, val payee: String?)

/**
 * Turns a payment's sightings into lines a person can read: "HDFC Bank text · a/c ••1234 · 2:14 PM",
 * "Google Pay alert · 2:14 PM", "Added by you · 2:10 PM". The message itself is never shown.
 */
object Provenance {
    /**
     * @param paymentSource the payment's own source. A hand-added payment that a bank later reported says
     *   "Matched with HDFC Bank text"; the sender isn't verified, so it never says the bank confirmed it (#194).
     * @param appName the installed app's name for a package, or null.
     */
    fun lines(
        paymentSource: String,
        seen: List<SightingEntity>,
        appName: (String) -> String?,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<SourceLine> {
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone)
        val namesDiffer = seen.mapNotNull { it.merchant }.distinct().size > 1
        return seen.map { s ->
            val who = when (s.source) {
                "MANUAL" -> "Added by you"
                "NOTIFICATION" -> "${s.sender?.let { appName(it) ?: KNOWN_APPS[it] } ?: "Payment app"} alert"
                else -> s.sender?.let { id -> bankName(id)?.let { "$it text" } ?: "Text from $id" } ?: "Bank text"
            }
            val confirmed = paymentSource == "MANUAL" && s.source != "MANUAL"
            val parts = listOfNotNull(
                if (confirmed) "Matched with ${who.replace("Text from", "a text from")}" else who,
                s.accountLast4?.let { "a/c ••$it" },
                time.format(Instant.ofEpochMilli(s.seenAt)),
            )
            SourceLine(parts.joinToString(" · "), s.merchant.takeIf { namesDiffer })
        }
    }

    /** "Seen twice, counted once" when more than one source reported the payment. */
    fun footer(sightings: Int): String? = when {
        sightings < 2 -> null
        sightings == 2 -> "Seen twice, counted once"
        else -> "Seen $sightings times, counted once"
    }

    /**
     * "VM-HDFCBK" becomes "HDFC Bank". An ID we don't know is shown without its prefix, like "XYZBNK".
     * Only the registered header shape (operator, '-', six-character header, optional '-S' style suffix) gets a
     * name; anyone can send as "HDFC" or "HDFCBK", so those get null and are shown as they are (#194).
     */
    // ponytail: the shape narrows spoofing, it doesn't authenticate; the label stays "HDFC Bank text", not "from HDFC Bank"
    fun bankName(sender: String): String? {
        val id = HEADER.matchEntire(sender.uppercase())?.groupValues?.get(1) ?: return null
        return BANKS.entries.firstOrNull { (prefix, _) -> id.startsWith(prefix) }?.value ?: id
    }

    private val HEADER = Regex("[A-Z]{2}-([A-Z0-9]{6})(-[A-Z])?")

    // ponytail: the common Indian bank sender IDs; unknown ones fall back to the ID itself
    private val BANKS = linkedMapOf(
        "HDFC" to "HDFC Bank", "ICICI" to "ICICI Bank", "SBI" to "SBI", "AXIS" to "Axis Bank", "KOTAK" to "Kotak",
        "PNB" to "PNB", "CANBNK" to "Canara Bank", "BOB" to "Bank of Baroda", "YESBNK" to "Yes Bank",
        "IDFC" to "IDFC First Bank", "INDUS" to "IndusInd Bank", "UNION" to "Union Bank", "FEDBNK" to "Federal Bank",
    )

    private val KNOWN_APPS = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "in.amazon.mShop.android.shopping" to "Amazon",
    )
}
