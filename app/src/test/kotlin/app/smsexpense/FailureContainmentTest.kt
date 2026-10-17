package app.smsexpense

import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.InboxMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FailureContainmentTest {
    private lateinit var app: ExpenseApp

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        app.ledger.seedDefaultCategories()
    }

    @Test fun aHistoryImportThatFailsEndsInAVisibleStateAndIsRetriedLater() = runTest {
        app.importHistory { throw SecurityException("READ_SMS revoked") }
        assertEquals(ExpenseApp.History.Failed, app.history.value)

        // Not marked done, so a later launch reads the inbox again.
        app.importHistory { emptyList() }
        assertEquals(ExpenseApp.History.Done(0), app.history.value)
    }

    // Graphite on #129: the permission callback and the home screen both start the import, so the claim must be
    // taken before the call returns, or both run and the second can replace "Filed 12 payments" with 0.
    @Test fun startingTheImportTwiceReadsTheInboxOnce() {
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val release = java.util.concurrent.CountDownLatch(1)
        val inbox: (Long) -> List<InboxMessage> = { reads.incrementAndGet(); release.await(); emptyList() }
        app.importHistoryOnce(inbox)
        assertEquals(ExpenseApp.History.Reading, app.history.value)
        app.importHistoryOnce(inbox)
        release.countDown()
        Thread.sleep(500)
        assertEquals(1, reads.get())
    }

    @Test fun oneMessageThatFailsDoesNotStopTheOthers() = runTest {
        val filed = mutableListOf<Int>()
        eachContained(listOf(1, 2, 3)) { if (it == 2) error("disk full") else filed += it }
        assertEquals(listOf(1, 3), filed)
    }
}
