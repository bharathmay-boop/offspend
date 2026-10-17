package app.smsexpense.core

/** Why a message was not treated as a transaction. */
enum class IgnoreReason {
    NOT_FROM_BANK, OTP, FAILED, UPCOMING, REQUEST, PROMOTION, CARD_BILL_PAYMENT, REMINDER, BALANCE, NO_MONEY_MOVED,
}

enum class Source { SMS, NOTIFICATION }
