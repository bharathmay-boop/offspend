package app.smsexpense

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.NotificationActionReceiver
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.ui.AddResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

// Added by hand, then the bank's SMS arrives: one payment, not two, and the person is asked (#53).
@RunWith(RobolectricTestRunner::class)
class MatchHandAddedTest {
    private lateinit var app: ExpenseApp
    private val zone = ZoneId.systemDefault()
    private val sms = "Sent Rs.420.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 13/10/26\nRef 412398765099"
    private val t0 = Instant.parse("2026-10-13T08:00:00Z").toEpochMilli()
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications
        .filter { it.flags and Notification.FLAG_GROUP_SUMMARY == 0 }

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.ledger.seedDefaultCategories()
    }

    private suspend fun addByHand(at: Long, amount: String = "420") = app.ledger.addByHand(
        AddResult(Direction.DEBIT, BigDecimal(amount), "INR", "Swiggy", null, Instant.ofEpochMilli(at).atZone(zone).toLocalDate(), ""),
        now = at,
    )

    private suspend fun visible() = app.db.transactions().observeAll().first()

    @Test fun theBankSmsForAHandAddedPaymentIsNotCountedTwice() = runTest {
        val manual = addByHand(t0)
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)
        assertEquals(listOf(manual), visible().map { it.id })
        val n = notifications.single()
        assertEquals("You added this by hand. Same payment?", n.extras.getString("android.text"))
        assertEquals(listOf("Yes, same", "No, add it"), n.actions.map { it.title.toString() })
    }

    @Test fun noAddItFilesTheBankPaymentAsWell() = runTest {
        addByHand(t0)
        val bank = app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)!!
        NotificationActionReceiver.handle(app, Intent(TransactionNotifier.ACTION_SEPARATE), bank)
        assertEquals(2, visible().size)
    }

    // #171: the bank's text confirms the hand-added payment, and goes with the bank's copy on "No, add it".
    @Test fun theBankTextConfirmsTheHandAddedPayment() = runTest {
        val manual = addByHand(t0)
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)
        assertEquals(listOf("MANUAL", "SMS"), app.db.sightings().forTransaction(manual).map { it.source })
    }

    @Test fun noAddItTakesTheBankTextsSightingWithIt() = runTest {
        val manual = addByHand(t0)
        val bank = app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)!!
        NotificationActionReceiver.handle(app, Intent(TransactionNotifier.ACTION_SEPARATE), bank)
        assertEquals(listOf("MANUAL"), app.db.sightings().forTransaction(manual).map { it.source })
        assertEquals(listOf("SMS"), app.db.sightings().forTransaction(bank).map { it.source })
    }

    @Test fun aHandAddedPaymentFromMoreThanADayBeforeIsNotMatched() = runTest {
        addByHand(t0)
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 30 * 60 * 60 * 1000L)
        assertEquals(2, visible().size)
    }

    @Test fun aHandAddedPaymentIsMatchedOnlyOnce() = runTest {
        addByHand(t0)
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)
        app.onMessage(Source.SMS, "AD-HDFCBK", sms.replace("412398765099", "412398765100"), t0 + 120_000)
        assertEquals(2, visible().size)
    }

    @Test fun aDifferentAmountIsNotMatched() = runTest {
        addByHand(t0, amount = "400")
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, t0 + 60_000)
        assertEquals(2, visible().size)
    }
}
