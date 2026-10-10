package app.smsexpense.core

import java.math.BigDecimal

enum class Direction { DEBIT, CREDIT }

/** An amount in a currency. Kept as BigDecimal so sums never drift the way doubles do. */
data class Money(val amount: BigDecimal, val currency: String) {
    init {
        require(amount.signum() >= 0) { "amount must not be negative; direction carries the sign" }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) { "currency must be an ISO 4217 code" }
    }
}
