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
import kotlinx.coroutines.CancellationException
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
            Offspend problem report
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

    /**
     * The report, or when it cannot be built (a broken database, say) a short note saying so, so the screen never waits
     * forever and the person can still share something that names the build.
     */
    suspend fun buildOrExplain(app: ExpenseApp, make: suspend () -> String = { build(app) }): String =
        try {
            make()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            buildString {
                appendLine("Offspend problem report")
                appendLine("App: ${runCatching { version(app) }.getOrDefault("unknown")}")
                append("The report could not be built (${e.javaClass.simpleName}). Please say so when you send this.")
            }
        }

    /** "0.1.3 (4)": the version name people see and the build number Play counts. */
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
