package app.smsexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.smsexpense.ui.AppNotificationsOffer
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

// Messages on the home screen are short, and go away once they have been read (#60).
@RunWith(RobolectricTestRunner::class)
class HomeMessagesTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 13)
    private var noticesShown = 0

    private fun show(
        month: YearMonth = YearMonth.of(2026, 10),
        importing: Boolean = false,
        notice: String? = null,
        offer: AppNotificationsOffer? = null,
    ) = compose.setContent {
        ExpenseTheme {
            HomeScreen(
                month = month, summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                importing = importing, notice = notice, onNoticeShown = { noticesShown++ },
                appNotificationsOffer = offer, onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = {}, onAdd = {}, today = today,
            )
        }
    }

    @Test fun anEmptyPastMonthDoesNotSayYet() {
        show(month = YearMonth.of(2026, 9))
        compose.onNodeWithText("No payments in September").assertExists()
        compose.onNodeWithText("yet", substring = true).assertDoesNotExist()
    }

    @Test fun anEmptyCurrentMonthStillSaysYet() {
        show()
        compose.onNodeWithText("No payments this month yet").assertExists()
    }

    @Test fun whileTheFirstImportRunsThereArePlaceholdersNotTheEmptyState() {
        show(importing = true)
        compose.onNodeWithTag("placeholder").assertExists()
        compose.onNodeWithText("No payments", substring = true).assertDoesNotExist()
    }

    // The app clears the notice once it's marked shown; that must not cancel the snackbar on screen.
    @Test fun theNoticeStaysOnScreenWhenTheAppMarksItShown() {
        var notice by mutableStateOf<String?>("Filed 9 payments from the last 3 months.")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ExpenseTheme {
                HomeScreen(
                    month = YearMonth.of(2026, 10), summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                    importing = false, notice = notice, onNoticeShown = { notice = null },
                    appNotificationsOffer = null, onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = {}, onAdd = {}, today = today,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Filed 9 payments from the last 3 months.").assertExists()
    }

    @Test fun theImportResultShowsOnceAsASnackbar() {
        show(notice = "Filed 9 payments from the last 3 months.")
        compose.onNodeWithText("Filed 9 payments from the last 3 months.").assertExists()
        // Marked shown once the snackbar has had its time on screen.
        compose.mainClock.advanceTimeBy(6_000)
        compose.waitForIdle()
        assertEquals(1, noticesShown)
    }

    @Test fun theUpiOfferIsOneLine() {
        show(offer = AppNotificationsOffer(onTurnOn = {}, onNotNow = {}))
        compose.onNodeWithText("Small UPI payments often skip the SMS.").assertExists()
        compose.onNodeWithText("Google Pay", substring = true).assertDoesNotExist()
    }
}
