package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionFilterTest {
    private fun check(f: Fixture) =
        TransactionFilter.check(if (f.source == "notification") Source.NOTIFICATION else Source.SMS, f.sender, f.text)

    @Test fun rejectsEveryNonTransactionWithTheRightReason() {
        for (f in Fixtures.all.filter { !it.expected.is_transaction }) {
            assertEquals(f.id, IgnoreReason.valueOf(f.expected.reason!!), check(f))
        }
    }

    @Test fun letsEveryRealTransactionThrough() {
        for (f in Fixtures.all.filter { it.expected.is_transaction }) assertNull(f.id, check(f))
    }

    @Test fun aReversalAfterAFailedPaymentIsStillMoney() {
        assertNull(TransactionFilter.check(Source.SMS, "JD-ICICIT", "Rs.450.00 has been reversed to your A/c XX567 for failed UPI txn"))
    }
}
