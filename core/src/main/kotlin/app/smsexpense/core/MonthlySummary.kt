package app.smsexpense.core

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** The fields of a stored transaction that the monthly view needs. */
data class Entry(
    val direction: Direction,
    val amount: BigDecimal,
    val currency: String,
    /** Null when the person has not picked one and there was no guess. */
    val category: String?,
    val occurredAt: Long,
)

data class CategoryTotal(val category: String?, val amount: BigDecimal)

/** One currency's month. Currencies are never added together or converted. */
data class CurrencyMonth(
    val currency: String,
    val spent: BigDecimal,
    val received: BigDecimal,
    /** Spending only, largest first. A null category means "needs a category". */
    val spentByCategory: List<CategoryTotal>,
)

object MonthlySummary {
    fun monthOf(epochMillis: Long, zone: ZoneId): YearMonth = YearMonth.from(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun inMonth(entries: List<Entry>, month: YearMonth, zone: ZoneId): List<Entry> =
        entries.filter { monthOf(it.occurredAt, zone) == month }

    /** Totals for the month, one block per currency, the currency with the most activity first. */
    fun of(entries: List<Entry>, month: YearMonth, zone: ZoneId): List<CurrencyMonth> {
        val byCurrency = inMonth(entries, month, zone).groupBy { it.currency }
        return byCurrency
            .map { (currency, list) ->
                val debits = list.filter { it.direction == Direction.DEBIT }
                CurrencyMonth(
                    currency = currency,
                    spent = debits.sumOf { it.amount },
                    received = list.filter { it.direction == Direction.CREDIT }.sumOf { it.amount },
                    spentByCategory = debits.groupBy { it.category }
                        .map { (category, items) -> CategoryTotal(category, items.sumOf { it.amount }) }
                        .sortedByDescending { it.amount },
                )
            }
            .sortedByDescending { byCurrency.getValue(it.currency).size }
    }
}
