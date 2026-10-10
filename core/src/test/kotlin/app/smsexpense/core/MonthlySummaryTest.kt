package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthlySummaryTest {
    private val ist = ZoneId.of("Asia/Kolkata")
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0).atZone(ist).toInstant().toEpochMilli()
    private fun e(dir: Direction, amount: String, cur: String, cat: String?, time: Long) = Entry(dir, BigDecimal(amount), cur, cat, time)

    private val october = YearMonth.of(2026, 10)
    private val entries = listOf(
        e(Direction.DEBIT, "320.00", "INR", "Food", at(2026, 10, 8)),
        e(Direction.DEBIT, "180.50", "INR", "Food", at(2026, 10, 9)),
        e(Direction.DEBIT, "1249", "INR", "Shopping", at(2026, 10, 7)),
        e(Direction.DEBIT, "400", "INR", null, at(2026, 10, 6)),
        e(Direction.CREDIT, "125000", "INR", "Salary", at(2026, 10, 1)),
        e(Direction.DEBIT, "45.50", "AED", "Groceries", at(2026, 10, 6)),
        e(Direction.DEBIT, "999", "INR", "Food", at(2026, 9, 30)),
    )

    @Test fun totalsSpentAndReceivedPerCurrencyWithoutMixingThem() {
        val months = MonthlySummary.of(entries, october, ist)
        assertEquals(listOf("INR", "AED"), months.map { it.currency })
        val inr = months[0]
        assertEquals(0, BigDecimal("2149.50").compareTo(inr.spent))
        assertEquals(0, BigDecimal("125000").compareTo(inr.received))
        assertEquals(0, BigDecimal("45.50").compareTo(months[1].spent))
    }

    @Test fun categoriesAreLargestFirstAndUncategorisedIsKept() {
        val inr = MonthlySummary.of(entries, october, ist)[0]
        assertEquals(listOf("Shopping", "Food", null), inr.spentByCategory.map { it.category })
        assertEquals(0, BigDecimal("500.50").compareTo(inr.spentByCategory[1].amount))
    }

    @Test fun monthBoundaryUsesThePhonesTimeZone() {
        // 30 Sep 23:30 in India is still September there, even though it is 18:00 UTC.
        val late = e(Direction.DEBIT, "10", "INR", "Food", at(2026, 9, 30, 23) + 30 * 60_000)
        assertEquals(YearMonth.of(2026, 9), MonthlySummary.monthOf(late.occurredAt, ist))
    }

    @Test fun anEmptyMonthHasNoBlocks() {
        assertEquals(emptyList<CurrencyMonth>(), MonthlySummary.of(entries, YearMonth.of(2026, 11), ist))
    }
}
