package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
import app.smsexpense.ui.theme.ExpenseTheme

/** Settings, opened from the round button at the top right of the home screen (#77). */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) {
                    var alertsOn by remember { mutableStateOf(PaymentNotificationListener.isEnabled(this)) }
                    // Turning the alerts on or off happens in the system's settings, so check again on return.
                    LifecycleResumeEffect(Unit) {
                        alertsOn = PaymentNotificationListener.isEnabled(this@SettingsActivity)
                        onPauseOrDispose {}
                    }
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
                            SettingsSection("Look", emptyList()),
                            SettingsSection("Plus", emptyList()),
                            SettingsSection("About", emptyList()),
                        ),
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
