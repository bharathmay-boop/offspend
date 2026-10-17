package app.smsexpense.core

/**
 * Decides whether a message describes money that actually moved.
 * Precision over recall: a missed transaction is better than a fake alert.
 */
object TransactionFilter {
    private fun r(p: String) = Regex(p, RegexOption.IGNORE_CASE)

    // A personal number, not a bank sender ID or a short code.
    private val personalSender = Regex("""^\+?\d{10,}$|^\+\d+X+$""")

    private val otp = r("""\bOTP\b|one[- ]time password|verification code""")
    // Money that already came back. "Refund will be processed" or "will be refunded to your" is only a promise (#152).
    private val reversedCredit = r("""(has been|have been) (reversed|refunded)|(?<!will be |shall be |would be )(reversed|refunded) to your""")
    private val failed = r("""\b(failed|declined|unsuccessful|could not be completed)\b""")
    private val upcoming = r("""will be (debited|charged|deducted)|autopay mandate|mandate .*\b(created|registered)\b""")
    private val request = r("""has requested|\brequested (rs|inr|₹)|collect request""")
    private val promotion = r("""pre-approved|apply now|\bT&C\b|\boffer\b|download now|congrat|\bget (rs|inr|₹).*cashback""")
    private val cardBillPayment = r("""payment of .* received towards|thank you for (your )?payment""")
    private val reminder = r("""statement is generated|total due|min(imum)? due|due by|expires on|recharge now""")
    private val balance = r("""\bavl\.? ?bal|available balance|\bbalance\b""")

    /** Words that only appear when money moved. */
    val movementVerb = r(
        """\b(debited|credited|deposited|spent|sent|paid|received|withdrawn|reversed|refund|refunded|purchase|charged)\b""" +
            """|\bDr\.|\bDR\b|was used for|transaction was made|done at ATM""",
    )

    fun check(source: Source, sender: String, text: String): IgnoreReason? {
        if (source == Source.SMS && personalSender.matches(sender.trim())) return IgnoreReason.NOT_FROM_BANK
        if (otp.containsMatchIn(text)) return IgnoreReason.OTP
        if (failed.containsMatchIn(text) && !reversedCredit.containsMatchIn(text)) return IgnoreReason.FAILED
        if (upcoming.containsMatchIn(text)) return IgnoreReason.UPCOMING
        if (request.containsMatchIn(text)) return IgnoreReason.REQUEST
        if (promotion.containsMatchIn(text)) return IgnoreReason.PROMOTION
        if (cardBillPayment.containsMatchIn(text)) return IgnoreReason.CARD_BILL_PAYMENT
        if (reminder.containsMatchIn(text)) return IgnoreReason.REMINDER
        if (!movementVerb.containsMatchIn(text)) {
            return if (balance.containsMatchIn(text)) IgnoreReason.BALANCE else IgnoreReason.NO_MONEY_MOVED
        }
        return null
    }
}
