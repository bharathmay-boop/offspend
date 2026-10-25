package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.smsexpense.capture.PaymentNotificationListener
import app.smsexpense.ui.SettingsRow
import app.smsexpense.ui.SettingsScreen
import app.smsexpense.ui.SettingsSection
import app.smsexpense.ui.ThemeDialog
import app.smsexpense.ui.theme.ExpenseTheme

/** Settings, opened from the round button at the top right of the home screen (#77). */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) {
                    var alertsOn by remember { mutableStateOf(PaymentNotificationListener.isEnabled(this)) }
                    // Turning the alerts on or off happens in the system's settings, so check again on return.
                    LifecycleResumeEffect(Unit) {
                        alertsOn = PaymentNotificationListener.isEnabled(this@SettingsActivity)
                        onPauseOrDispose {}
                    }
                    val theme by app.theme.collectAsState()
                    var choosingTheme by remember { mutableStateOf(false) }
                    // Sections in the epic's order (#76). Each fills in as its feature lands; empty ones don't show.
                    SettingsScreen(
                        sections = listOf(
                            SettingsSection("You", emptyList()),
                            SettingsSection("Categories", emptyList()),
                            SettingsSection("Money", emptyList()),
                            SettingsSection(
                                "Notifications",
                                listOf(
                                    SettingsRow(R.drawable.ic_notifications, "Payment app alerts", if (alertsOn) "On" else "Off") {
                                        startActivity(PaymentNotificationListener.settingsIntent(this@SettingsActivity))
                                    },
                                ),
                            ),
                            SettingsSection("Privacy and data", emptyList()),
                            SettingsSection("Look", listOf(SettingsRow(R.drawable.ic_palette, "Theme", theme.label) { choosingTheme = true })),
                            SettingsSection("Plus", emptyList()),
                            SettingsSection(
                                "Support",
                                listOf(
                                    SettingsRow(R.drawable.ic_edit_note, "Report a problem", null) {
                                        startActivity(ReportActivity.intent(this@SettingsActivity))
                                    },
                                ),
                            ),
                            SettingsSection(
                                "About",
                                listOf(
                                    SettingsRow(R.drawable.ic_info, "Version", versionLabel(), onClick = null),
                                    SettingsRow(R.drawable.ic_lock, "Privacy policy", null) {
                                        startActivity(DocumentActivity.privacyPolicy(this@SettingsActivity))
                                    },
                                    SettingsRow(R.drawable.ic_description, "Open source licences", "Bricolage Grotesque, Inter") {
                                        startActivity(DocumentActivity.licences(this@SettingsActivity))
                                    },
                                ),
                            ),
                        ),
                        onBack = ::finish,
                    )
                    if (choosingTheme) {
                        ThemeDialog(theme, onPick = { app.setTheme(it); choosingTheme = false }, onDismiss = { choosingTheme = false })
                    }
                }
            }
        }
    }

    /** "0.1.1 (2)": the version name people see, and the build number Play counts. */
    private fun versionLabel(): String = ProblemReport.version(this)

    companion object {
        fun intent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
