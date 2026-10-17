package app.smsexpense

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.NotificationActionReceiver
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.core.Categorizer
import app.smsexpense.core.Source
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationTest {
    private lateinit var app: ExpenseApp
    private val payment = "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo CALIFORNIA BURRITO\nOn 08/10/26\nRef 412398765012"
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.ledger.seedDefaultCategories()
    }

    @Test fun aNewPaymentShowsTheAmountGuessAndOneTapActions() = runTest {
        app.onMessage(Source.SMS, "AD-HDFCBK", payment, 1_000L)
        val n = notifications.single()
        assertEquals("−₹320.00 · California Burrito", n.extras.getString("android.title"))
        // #167: the guess is already filed, so it says so; tapping the notification is how to change it.
        assertEquals("Food · Tap to change", n.extras.getString("android.text"))
        assertEquals(listOf("Yes, Food", "Add note"), n.actions.map { it.title.toString() })
    }

    @Test fun aPayeeFiledUnderThisCategoryBeforeJustSaysOkay() = runTest {
        val first = app.onMessage(Source.SMS, "AD-HDFCBK", payment, 1_000L)!!
        NotificationActionReceiver.handle(
            app, Intent(TransactionNotifier.ACTION_CONFIRM).putExtra(TransactionNotifier.EXTRA_CATEGORY, app.db.categories().byName("Food")!!.id), first,
        )
        app.onMessage(Source.SMS, "AD-HDFCBK", payment.replace("320.00", "280.00"), 9_000L)
        assertEquals(listOf("Okay", "Add note"), notifications.single().actions.map { it.title.toString() })
    }

    @Test fun aLongCategoryNameIsCutSoTheButtonFits() {
        assertEquals("Yes, Office supplie…", TransactionNotifier.confirmLabel("Office supplies and printing", filedBefore = false))
        assertEquals("Yes, EMI & loans", TransactionNotifier.confirmLabel("EMI & loans", filedBefore = false))
    }

    @Test fun anUnknownPayeeAsksForACategoryInstead() = runTest {
        app.onMessage(Source.SMS, "VM-CANBNK", "An amount of INR 1,050.00 has been DEBITED to your account XXX778 on 06/10/2026. - Canara Bank", 2_000L)
        val n = notifications.single()
        assertEquals("Needs a category", n.extras.getString("android.text"))
        assertEquals(listOf("Add note"), n.actions.map { it.title.toString() })
    }

    @Test fun historyImportsDoNotNotify() = runTest {
        app.onMessage(Source.SMS, "AD-HDFCBK", payment, 3_000L, notify = false)
        assertTrue(notifications.isEmpty())
    }

    @Test fun yesConfirmsLearnsAndDismisses() = runTest {
        val id = app.onMessage(Source.SMS, "AD-HDFCBK", payment, 4_000L)!!
        val food = app.db.categories().byName("Food")!!
        NotificationActionReceiver.handle(
            app,
            Intent(TransactionNotifier.ACTION_CONFIRM).putExtra(TransactionNotifier.EXTRA_CATEGORY, food.id),
            id,
        )
        assertTrue(app.db.transactions().byId(id)!!.confirmed)
        assertTrue(app.db.merchantRules().all().any { it.merchantKey == Categorizer.merchantKey("CALIFORNIA BURRITO") })
        assertTrue(notifications.isEmpty())
    }

    @Test fun aBlankReplyLeavesAnExistingNoteAlone() = runTest {
        val id = app.onMessage(Source.SMS, "AD-HDFCBK", payment, 6_000L)!!
        app.ledger.setNote(id, "Team lunch")
        val intent = Intent(TransactionNotifier.ACTION_NOTE)
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder(TransactionNotifier.KEY_NOTE).build()), intent,
            Bundle().apply { putCharSequence(TransactionNotifier.KEY_NOTE, "   ") },
        )
        NotificationActionReceiver.handle(app, intent, id)
        assertEquals("Team lunch", app.db.transactions().byId(id)!!.note)
    }

    @Test fun aReplyFromTheNotificationIsSavedAsTheNote() = runTest {
        val id = app.onMessage(Source.SMS, "AD-HDFCBK", payment, 5_000L)!!
        val intent = Intent(TransactionNotifier.ACTION_NOTE)
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder(TransactionNotifier.KEY_NOTE).build()), intent,
            Bundle().apply { putCharSequence(TransactionNotifier.KEY_NOTE, "Team lunch") },
        )
        NotificationActionReceiver.handle(app, intent, id)
        assertEquals("Team lunch", app.db.transactions().byId(id)!!.note)
        // The re-posted notification shows the note, so the person can see it was saved (#48).
        assertEquals("Note: Team lunch · Food · Tap to change", notifications.single().extras.getString("android.text"))
    }

    // Android 16 groups an app's loose notifications from the second one on and marks them all silent (#71).
    // Our own group, with a summary that leaves alerting to each payment, keeps every new payment popping up.
    @Test fun eachPaymentStillPopsUpWhenSeveralAreWaiting() = runTest {
        app.onMessage(Source.SMS, "AD-HDFCBK", payment, 10_000L)
        app.onMessage(Source.SMS, "AD-HDFCBK", payment.replace("320.00", "450.00"), 20_000L)
        val (summary, payments) = notifications.partition { it.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
        assertEquals(2, payments.size)
        assertEquals(1, summary.size)
        (payments + summary).forEach {
            assertEquals(TransactionNotifier.GROUP, it.group)
            assertEquals(Notification.GROUP_ALERT_CHILDREN, it.groupAlertBehavior)
        }
    }

    @Test fun theSummaryGoesWithTheLastPayment() = runTest {
        val a = app.onMessage(Source.SMS, "AD-HDFCBK", payment, 30_000L)!!
        val b = app.onMessage(Source.SMS, "AD-HDFCBK", payment.replace("320.00", "450.00"), 40_000L)!!
        TransactionNotifier.dismiss(app, a)
        TransactionNotifier.dismiss(app, b)
        assertTrue(notifications.isEmpty())
    }

    // Graphite on #144: Android posts and cancels in the background, so the tray can lag behind the call just made.
    @Test fun theCountIncludesThePaymentJustPostedAndNotTheOneJustRemoved() {
        assertEquals(2, TransactionNotifier.waiting(shown = setOf(1), posted = 2))
        assertEquals(1, TransactionNotifier.waiting(shown = setOf(1, 2), removed = 2))
        assertEquals(2, TransactionNotifier.waiting(shown = setOf(1, 2), posted = 2))
    }
}
