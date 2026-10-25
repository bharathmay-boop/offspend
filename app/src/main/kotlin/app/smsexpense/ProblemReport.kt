package app.smsexpense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import app.smsexpense.capture.PaymentNotificationListener
import app.smsexpense.data.Ledger
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The text a tester sends when something goes wrong (#100, #108). It says which build and phone it is and whether the
 * things the app needs are switched on, so "it didn't catch my payment" can be answered without questions.
 * It holds counts only. Never a message, an amount, a payee, a note or an account number.
 */
object ProblemReport {
    /** Where reports are addressed. Empty until the contact email is chosen (#104); the share sheet works without one. */
    const val CONTACT_EMAIL = ""

    suspend fun build(
        app: ExpenseApp,
        now: ZonedDateTime = ZonedDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val month = YearMonth.from(now)
        val live = app.db.transactions().observeAll().first().filter { !it.deleted }
        val thisMonth = live.filter { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month }
        return """
            Expense Tracker problem report
            Made ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx"))}

            App: ${version(app)}
            Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Language: ${Locale.getDefault().toLanguageTag()}

            What the app needs
            - Read SMS: ${yes(granted(app, Manifest.permission.READ_SMS), "allowed", "NOT allowed")}
            - Receive SMS: ${yes(granted(app, Manifest.permission.RECEIVE_SMS), "allowed", "NOT allowed")}
            - Notifications: ${yes(NotificationManagerCompat.from(app).areNotificationsEnabled(), "on", "OFF")}
            - Payment app alerts: ${yes(PaymentNotificationListener.isEnabled(app), "on", "off")}
            - Battery saver limits the app: ${yes(!ignoresBatteryLimits(app), "yes", "no")}

            Counts (no payments, payees or notes are in this report)
            - Payments this month: ${thisMonth.size}
            - Without a category: ${thisMonth.count { it.categoryId == null }}
            - Added by hand, all time: ${live.count { it.source == Ledger.MANUAL }}
        """.trimIndent()
    }

    /** "0.1.1 (2)": the version name people see and the build number Play counts. */
    fun version(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
    }

    /** The share sheet request: the report as text, so the person picks email, WhatsApp or anything else. The app sends nothing itself. */
    fun shareIntent(report: String, version: String, email: String = CONTACT_EMAIL): Intent =
        Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Problem report, version $version")
            .putExtra(Intent.EXTRA_TEXT, report)
            .apply { if (email.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email)) }

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun ignoresBatteryLimits(context: Context) =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    private fun yes(value: Boolean, whenTrue: String, whenFalse: String) = if (value) whenTrue else whenFalse
}
