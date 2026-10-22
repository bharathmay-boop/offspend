package app.smsexpense

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.SettingsRow
import app.smsexpense.ui.SettingsScreen
import app.smsexpense.ui.SettingsSection
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

// Settings, and the way in from the home screen (#77).
@RunWith(RobolectricTestRunner::class)
class SettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun theRoundButtonOnHomeOpensSettings() {
        var opened = 0
        compose.setContent {
            ExpenseTheme {
                HomeScreen(
                    month = YearMonth.of(2026, 10), summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                    importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                    onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = {}, onAdd = {},
                    onSettings = { opened++ }, today = LocalDate.of(2026, 10, 22),
                )
            }
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, opened)
    }

    @Test fun aSectionWithNoRowsYetIsLeftOut() {
        var tapped = 0
        compose.setContent {
            ExpenseTheme {
                SettingsScreen(
                    sections = listOf(
                        SettingsSection("You", emptyList()),
                        SettingsSection("Notifications", listOf(SettingsRow(R.drawable.ic_notifications, "Payment app alerts", "Off") { tapped++ })),
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("You").assertDoesNotExist()
        compose.onNodeWithText("Notifications").assertExists()
        compose.onNodeWithText("Off").assertExists()
        compose.onNodeWithText("Payment app alerts").performClick()
        assertEquals(1, tapped)
    }

    @Test fun backClosesSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            compose.onNodeWithText("Payment app alerts").assertExists()
            compose.onNodeWithContentDescription("Back").performClick()
            scenario.onActivity { assertEquals(true, it.isFinishing) }
        }
    }

    @Test fun thePaymentAppAlertsRowOpensTheSystemSwitch() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            compose.onNodeWithText("Payment app alerts").performClick()
            val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
            val started = shadowOf(app).nextStartedActivity
            assertEquals(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS, started?.action)
        }
    }

    // The per-app screen only exists from Android 11; asking for it on older phones would crash.
    @Test @Config(sdk = [29]) fun onAndroid10TheRowOpensTheListOfListeners() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.onNodeWithText("Payment app alerts").performClick()
            val started = shadowOf(ApplicationProvider.getApplicationContext<ExpenseApp>()).nextStartedActivity
            assertEquals(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, started?.action)
        }
    }
}
