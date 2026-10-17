package app.smsexpense.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal

/**
 * Asks an on-device model to read a message the rules could not, and checks its answer before trusting it.
 * A model can make things up, so every field it returns must be found in the message itself.
 */
object ModelFallback {
    fun prompt(message: String): String = """
        You read bank and payment messages. Reply with one JSON object and nothing else, in this shape:
        {"is_transaction": true, "direction": "DEBIT", "amount": "320.00", "currency": "INR", "merchant": "SWIGGY", "account": "1234"}
        Rules:
        - is_transaction is false for OTPs, offers, reminders, failed or upcoming payments. Then leave the other fields null.
        - direction is DEBIT when money left the account and CREDIT when it came in.
        - amount is the money that moved, copied from the message, not a balance or limit.
        - currency is the 3-letter ISO code.
        - merchant is who was paid or who paid, copied exactly from the message, or null if it is not named.
        - account is the account or card digits shown in the message, or null.
        Message:
        $message
    """.trimIndent()

    @Serializable
    private data class Answer(
        val is_transaction: Boolean = false,
        val direction: String? = null,
        val amount: String? = null,
        val currency: String? = null,
        val merchant: String? = null,
        val account: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val numberInText = Regex("""\d{1,3}(?:,\d{2,3})+(?:\.\d+)?|\d+(?:\.\d+)?""")

    /** @return the transaction if the model's answer holds up against the message, otherwise null. */
    fun accept(message: String, modelOutput: String?, defaultCurrency: String = "INR"): SmsReading.Transaction? {
        val answer = modelOutput?.let(::firstJsonObject)?.let { runCatching { json.decodeFromString<Answer>(it) }.getOrNull() }
            ?: return null
        if (!answer.is_transaction) return null
        val direction = runCatching { Direction.valueOf(answer.direction.orEmpty().uppercase()) }.getOrNull() ?: return null
        val amount = answer.amount?.replace(",", "")?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
        // The amount must be one of the numbers actually written in the message.
        val written = numberInText.findAll(message).map { BigDecimal(it.value.replace(",", "")) }
        if (written.none { it.compareTo(amount) == 0 }) return null
        val currency = answer.currency?.uppercase()?.takeIf { it.matches(Regex("[A-Z]{3}")) } ?: defaultCurrency
        val merchant = answer.merchant?.trim()?.takeIf { it.isNotEmpty() && message.contains(it, ignoreCase = true) }
        val account = answer.account?.filter { it.isDigit() }?.takeIf { it.length in 3..5 && message.contains(it) }
        return SmsReading.Transaction(direction, Money(amount, currency), merchant, account)
    }

    /** Models often wrap JSON in prose or code fences; take the first {...} block. */
    private fun firstJsonObject(s: String): String? {
        val start = s.indexOf('{').takeIf { it >= 0 } ?: return null
        val end = s.lastIndexOf('}').takeIf { it > start } ?: return null
        return s.substring(start, end + 1)
    }
}
