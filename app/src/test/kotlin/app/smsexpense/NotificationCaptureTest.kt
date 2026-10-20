package app.smsexpense

import androidx.test.core.app.ApplicationProvider
import app.smsexpense.capture.PaymentNotificationListener
import app.smsexpense.core.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class NotificationCaptureTest {
    private lateinit var app: ExpenseApp
    private val gpay = "com.google.android.apps.nbu.paisa.user"

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        app.ledger.seedDefaultCategories()
    }

    @Test fun aGooglePayPaymentIsSavedAsANotificationTransaction() = runTest {
        val id = PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 1_000L)
        val tx = app.db.transactions().byId(id!!)!!
        assertEquals(Source.NOTIFICATION.name, tx.source)
        assertEquals(0, BigDecimal("320").compareTo(tx.amount))
        assertEquals("California Burrito", tx.merchant)
        assertEquals("Food", app.db.categories().all().first { it.id == tx.categoryId }.name)
    }

    @Test fun theAmountCanBeInTheTitleAndThePayeeInTheText() = runTest {
        assertNotNull(PaymentNotificationListener.capture(app, "com.phonepe.app", "₹450 sent to", "Rahul S", 2_000L))
    }

    @Test fun otherAppsAreIgnoredEvenIfTheyMentionMoney() = runTest {
        assertNull(PaymentNotificationListener.capture(app, "com.whatsapp", "Mom", "Paid ₹320 to the plumber", 3_000L))
        assertEquals(0, app.db.transactions().observeAll().first().size)
    }

    @Test fun offersFromPaymentAppsAreIgnored() = runTest {
        assertNull(PaymentNotificationListener.capture(app, gpay, "Cashback!", "Get Rs.500 cashback on your next 3 payments. T&C apply", 4_000L))
    }

    @Test fun anUpdatedNotificationWithTheSameTimeIsNotSavedTwice() = runTest {
        PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L)
        assertNull(PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L))
    }

    // #191: the app sets the alert's time itself, so a repost with a new time is still the same payment.
    @Test fun theSameAlertPostedAgainWithANewTimeIsNotSavedTwice() = runTest {
        assertNotNull(PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L))
        assertNull(PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L + 4 * 60_000))
        assertEquals(1, app.db.transactions().observeAll().first().size)
    }

    @Test fun aRepostDoesNotBringBackAPaymentThePersonDeleted() = runTest {
        val id = PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L)!!
        app.ledger.delete(id)
        assertNull(PaymentNotificationListener.capture(app, gpay, "Google Pay", "Paid ₹320 to California Burrito", 5_000L + 60_000))
        assertEquals(0, app.db.transactions().observeAll().first().size)
    }

    @Test fun anAlertsOwnTimeIsUsedOnlyWhenItIsCloseToWhenThePhoneShowedIt() {
        val shown = 1_760_000_000_000L
        assertEquals(shown - 60_000, PaymentNotificationListener.timeOf(shown - 60_000, shown))
        assertEquals(shown, PaymentNotificationListener.timeOf(1_577_836_800_000L, shown)) // 2020-01-01
        assertEquals(shown, PaymentNotificationListener.timeOf(shown + 86_400_000L, shown))
        assertEquals(shown, PaymentNotificationListener.timeOf(0, shown))
    }
}
