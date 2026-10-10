package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Checks the test set itself, so a typo in an expected value cannot hide a parser bug. */
class FixturesTest {
    private val numberInText = Regex("""\d{1,3}(?:,\d{2,3})+(?:\.\d+)?|\d+(?:\.\d+)?""")

    @Test fun hasAtLeastFiftySamples() = assertTrue(Fixtures.all.size >= 50)

    @Test fun idsAreUnique() = assertEquals(Fixtures.all.size, Fixtures.all.map { it.id }.toSet().size)

    @Test fun everyExpectedAmountAppearsInItsMessage() {
        for (f in Fixtures.all.filter { it.expected.is_transaction }) {
            val want = BigDecimal(f.expected.amount!!)
            val found = numberInText.findAll(f.text).map { BigDecimal(it.value.replace(",", "")) }
            assertTrue("${f.id}: ${f.expected.amount} not in text", found.any { it.compareTo(want) == 0 })
        }
    }

    @Test fun transactionsAreComplete() {
        for (f in Fixtures.all.filter { it.expected.is_transaction }) {
            assertTrue(f.id, f.expected.direction in setOf("DEBIT", "CREDIT"))
            assertTrue(f.id, f.expected.currency!!.matches(Regex("[A-Z]{3}")))
        }
    }

    @Test fun ignoredMessagesNameAKnownReason() {
        val reasons = IgnoreReason.entries.map { it.name }.toSet()
        for (f in Fixtures.all.filter { !it.expected.is_transaction }) assertTrue(f.id, f.expected.reason in reasons)
    }

    @Test fun merchantsAndAccountsAppearInTheirMessage() {
        for (f in Fixtures.all.filter { it.expected.is_transaction }) {
            f.expected.merchant?.takeIf { it != "ATM" }?.let { assertTrue(f.id, f.text.contains(it)) }
            f.expected.account?.let { assertTrue(f.id, f.text.contains(it)) }
        }
    }
}
