package app.smsexpense

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import androidx.core.app.Person
import androidx.core.app.NotificationCompat
import android.service.notification.StatusBarNotification
import android.os.Process
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
import org.robolectric.Robolectric
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

    // #190: a contact can type a payment-shaped chat message in Google Pay or PhonePe; it isn't the owner's payment.
    @Test fun aChatMessageInAPaymentAppIsNotFiled() {
        val chat = NotificationCompat.Builder(app, "chat")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(
                NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
                    .addMessage("Paid Rs 5000 to Amazon", 3_000L, Person.Builder().setName("Dummy Contact").build()),
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        val alert = NotificationCompat.Builder(app, "alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Google Pay")
            .setContentText("Paid ₹320 to California Burrito")
            .build()
        // The chat first, then the app's own alert: once the alert is filed, the chat would have been too.
        post(chat)
        post(alert)
        val deadline = System.currentTimeMillis() + 5_000
        while (filed().isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        Thread.sleep(300)
        assertEquals(listOf("California Burrito"), filed().map { it.merchant })
    }

    private fun filed() = runBlocking { app.db.transactions().observeAll().first() }

    @Test fun messagingStyleAndTheMessageCategoryBothCountAsAConversation() {
        val styled = NotificationCompat.Builder(app, "c").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(NotificationCompat.MessagingStyle(Person.Builder().setName("You").build()).addMessage("hi", 1L, Person.Builder().setName("A").build()))
            .build()
        val categorised = NotificationCompat.Builder(app, "c").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).setContentText("Paid Rs 5000 to Amazon").build()
        val alert = NotificationCompat.Builder(app, "c").setSmallIcon(android.R.drawable.ic_dialog_info).setContentText("Paid ₹320").build()
        assertTrue(PaymentNotificationListener.isConversation(styled))
        assertTrue(PaymentNotificationListener.isConversation(categorised))
        assertFalse(PaymentNotificationListener.isConversation(alert))
    }

    @Suppress("DEPRECATION") // the public constructor is the only way to build one outside the system
    private fun post(n: android.app.Notification) {
        val sbn = StatusBarNotification(gpay, gpay, 1, null, 0, 0, 0, n, Process.myUserHandle(), System.currentTimeMillis())
        Robolectric.buildService(PaymentNotificationListener::class.java).create().get().onNotificationPosted(sbn)
    }
}
