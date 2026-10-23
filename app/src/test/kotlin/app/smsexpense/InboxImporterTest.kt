package app.smsexpense

import android.Manifest
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.InboxImporter
import app.smsexpense.capture.InboxMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InboxImporterTest {
    private lateinit var app: ExpenseApp

    private val inbox = listOf(
        InboxMessage("AD-HDFCBK", "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo CALIFORNIA BURRITO\nOn 08/10/26", 1_000L),
        InboxMessage("JD-ICICIT", "Your OTP for UPI PIN reset is 482913. Do not share it with anyone.", 2_000L),
        InboxMessage("+919800000000", "Dinner at 8?", 3_000L),
        InboxMessage("AX-AXISBK", "INR 899.00 debited\nA/c no. XX2222\nUPI/P2M/428855556666/NETFLIX\nAxis Bank", 4_000L),
    )

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.ledger.seedDefaultCategories()
    }

    private fun importer(messages: List<InboxMessage>) =
        InboxImporter({ since -> messages.filter { it.receivedAt >= since } }, { s, from, body, at -> app.onMessage(s, from, body, at, notify = false) })

    @Test fun keepsOnlyTheTransactionsFromThePastInbox() = runTest {
        assertEquals(2, importer(inbox).importSince(0))
        assertEquals(listOf("NETFLIX", "CALIFORNIA BURRITO"), app.db.transactions().observeAll().first().map { it.merchant })
    }

    @Test fun pastMessagesDoNotSendNotifications() = runTest {
        importer(inbox).importSince(0)
        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
    }

    @Test fun importingTwiceDoesNotDuplicate() = runTest {
        importer(inbox).importSince(0)
        assertEquals(0, importer(inbox).importSince(0))
        assertEquals(2, app.db.transactions().observeAll().first().size)
    }

    @Test fun onlyReadsFromTheStartTime() = runTest {
        assertEquals(1, importer(inbox).importSince(3_500L))
    }

    // #236: one message that fails must not stop the import or hide the ones after it.
    @Test fun aMessageThatThrowsIsSkippedAndTheRestAreStillFiled() = runTest {
        val poison = InboxMessage("AD-SPOOF", "poison", 500L)
        val messages = listOf(poison) + inbox
        val importer = InboxImporter(
            { since -> messages.filter { it.receivedAt >= since } },
            { s, from, body, at -> if (body == "poison") error("boom") else app.onMessage(s, from, body, at, notify = false) },
        )
        assertEquals(2, importer.importSince(0))
        assertEquals(2, app.db.transactions().observeAll().first().size)
    }
}
