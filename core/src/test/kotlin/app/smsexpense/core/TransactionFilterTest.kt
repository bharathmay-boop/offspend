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

    // #152: a promise of a refund is not money back.
    @Test fun aFailedPaymentThatOnlyPromisesARefundIsSkipped() {
        assertEquals(
            IgnoreReason.FAILED,
            TransactionFilter.check(Source.SMS, "AD-HDFCBK", "Your payment of Rs 500 failed. Refund, if debited, will be processed in 5 days"),
        )
        assertEquals(
            IgnoreReason.FAILED,
            TransactionFilter.check(Source.SMS, "AD-HDFCBK", "Your payment of Rs 500 failed. Any amount debited will be refunded in 5 days"),
        )
    }

    @Test fun aRefundCreditedAfterAFailedPaymentIsStillMoney() {
        assertNull(TransactionFilter.check(Source.SMS, "AD-HDFCBK", "Rs 500 refunded to your A/c XX1234 for failed UPI txn"))
    }
}
