package app.smsexpense

import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CompletableFuture
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.InboxMessage
import app.smsexpense.core.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatchUpTest {
    private lateinit var app: ExpenseApp
    private val inbox = mutableListOf<InboxMessage>()
    private val reader: (Long) -> List<InboxMessage> = { since -> inbox.filter { it.receivedAt >= since } }

    private fun payment(merchant: String, at: Long) =
        InboxMessage("AD-HDFCBK", "Sent Rs.100.00\nFrom HDFC Bank A/C *1234\nTo $merchant\nOn 08/10/26", at)

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        app.ledger.seedDefaultCategories()
        app.markHistoryDone(at = 1_000_000L)
    }

    @Test fun filesAPaymentThatArrivedWhileTheAppWasStopped() = runTest {
        inbox += payment("ZEPTO", 2_000_000L)
        assertEquals(1, app.catchUp(reader))
        assertEquals("ZEPTO", app.db.transactions().observeAll().first().single().merchant)
    }

    @Test fun aPaymentTheReceiverAlreadyFiledIsNotFiledAgain() = runTest {
        val seen = payment("BLINKIT", 2_000_000L)
        inbox += seen
        app.onMessage(Source.SMS, seen.sender, seen.body, seen.receivedAt)
        assertEquals(0, app.catchUp(reader))
        assertEquals(1, app.db.transactions().observeAll().first().size)
    }

    @Test fun aLivePaymentIsNotFiledAgainWhenTheInboxStampsItWithALaterReceivedTime() = runTest {
        // Live, the receiver sees the message centre time. In the inbox the same SMS has a
        // received time a few seconds later, and the message centre time in sentAt.
        val body = payment("BLINKIT", 0).body
        app.onMessage(Source.SMS, "AD-HDFCBK", body, 2_000_000L)
        inbox += InboxMessage("AD-HDFCBK", body, receivedAt = 2_004_000L, sentAt = 2_000_000L)
        assertEquals(0, app.catchUp(reader))
        assertEquals(1, app.db.transactions().observeAll().first().size)
    }

    @Test fun eachCatchUpOnlyLooksAfterTheLastMessageSeen() = runTest {
        inbox += payment("ZEPTO", 2_000_000L)
        app.catchUp(reader)
        inbox += payment("SWIGGY", 3_000_000L)
        assertEquals(1, app.catchUp(reader))
    }

    @Test fun aMissedPaymentIsStillFiledWhenALaterOneWasCaughtLive() = runTest {
        val missed = payment("ZEPTO", 2_000_000L)
        val live = payment("SWIGGY", 2_500_000L)
        inbox += listOf(missed, live)
        app.onMessage(Source.SMS, live.sender, live.body, live.receivedAt)
        assertEquals(1, app.catchUp(reader))
        assertEquals(setOf("ZEPTO", "SWIGGY"), app.db.transactions().observeAll().first().map { it.merchant }.toSet())
    }

    @Test fun messagesFromBeforeSetupAreLeftToTheHistoryImport() = runTest {
        inbox += payment("OLD SHOP", 10_000L)
        assertEquals(0, app.catchUp(reader))
    }

    // #218: every app start launches a catch-up; a second one while the first is still reading does nothing.
    @Test fun aSecondCatchUpWhileOneRunsDoesNothing() = runTest {
        val reading = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = CompletableFuture.supplyAsync { runBlocking { app.catchUp { reading.countDown(); release.await(); emptyList() } } }
        reading.await(5, TimeUnit.SECONDS)
        var secondRead = false
        assertEquals(0, app.catchUp { secondRead = true; emptyList() })
        release.countDown()
        first.get(5, TimeUnit.SECONDS)
        assertEquals(false, secondRead)
        // Once the first is done, the next one runs as normal.
        app.catchUp { secondRead = true; emptyList() }
        assertEquals(true, secondRead)
    }
}
