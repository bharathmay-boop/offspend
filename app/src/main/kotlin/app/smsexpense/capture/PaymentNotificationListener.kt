package app.smsexpense.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import app.smsexpense.ExpenseApp
import app.smsexpense.core.SmsParser
import app.smsexpense.core.SmsReading
import app.smsexpense.core.Source
import kotlinx.coroutines.launch

/**
 * Reads payment notifications from UPI and bank apps, for payments that never send an SMS
 * (small UPI payments, app-first banks). Only the apps in [PaymentApps] are read; every other
 * notification is ignored without being looked at.
 */
class PaymentNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!PaymentApps.isPaymentApp(sbn.packageName)) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val at = timeOf(sbn.notification.`when`, sbn.postTime)
        val app = applicationContext as ExpenseApp
        app.scope.launch { capture(app, sbn.packageName, title, text, at) }
    }

    companion object {
        private const val MAX_SKEW_MS = 10 * 60 * 1000L

        /**
         * The app's own timestamp stays the same when it updates a notification, so it is used when it is close to
         * when the phone actually showed it. The app sets it freely, so one far off falls back to the phone's time (#191).
         */
        fun timeOf(whenMs: Long, postTime: Long): Long =
            whenMs.takeIf { it > 0 && kotlin.math.abs(it - postTime) <= MAX_SKEW_MS } ?: postTime

        /**
         * Picks the first readable version (the text alone, then title and text together) and saves only that one,
         * so a repeat of the same notification is recognised as a duplicate instead of retried in another form.
         */
        suspend fun capture(app: ExpenseApp, packageName: String, title: String, text: String, postedAt: Long): Long? {
            if (!PaymentApps.isPaymentApp(packageName)) return null
            val body = listOf(text, "$title $text").map { it.trim() }.distinct().filter { it.isNotEmpty() }
                .firstOrNull { SmsParser.parse(Source.NOTIFICATION, packageName, it) is SmsReading.Transaction }
                ?: return null
            return app.onMessage(Source.NOTIFICATION, packageName, body, postedAt)
        }

        fun isEnabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun component(context: Context) = ComponentName(context, PaymentNotificationListener::class.java)
    }
}

/** The payment and banking apps whose notifications are read. Everything else is ignored. */
object PaymentApps {
    private val packages = setOf(
        // UPI apps (India)
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp", // BHIM
        "com.dreamplug.androidapp", // CRED
        "in.amazon.mShop.android.shopping", // Amazon Pay lives in the Amazon app
        // Bank apps (India)
        "com.snapwork.hdfc",
        "com.csam.icici.bank.imobile",
        "com.sbi.lotusintouch", // YONO SBI
        "com.axis.mobile",
        "com.msf.kbank.mobile", // Kotak
        // App-first banks elsewhere, where payment alerts come as notifications rather than SMS
        "com.revolut.revolut",
        "co.uk.getmondo", // Monzo
        "com.starlingbank.android",
        "com.chase.sig.android",
        "com.wf.wellsfargomobile",
    )

    fun isPaymentApp(packageName: String) = packageName in packages
}
