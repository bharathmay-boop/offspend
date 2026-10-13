package app.smsexpense

import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.swipeStep
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.YearMonth

// Changing month should be noticed and reachable by thumb (#61).
@RunWith(RobolectricTestRunner::class)
class MonthMotionTest {
    @get:Rule val compose = createComposeRule()

    private var month by mutableStateOf(YearMonth.of(2026, 9))
    private val current = YearMonth.of(2026, 10)

    private fun show() = compose.setContent {
        ExpenseTheme {
            HomeScreen(
                month = month, summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                onPreviousMonth = { month = month.minusMonths(1) },
                onNextMonth = if (month < current) ({ month = month.plusMonths(1) }) else null,
                onOpen = {}, onReview = {}, onAdd = {},
            )
        }
    }

    @Test fun aLongSwipeChangesMonthAShortOneDoesNot() {
        assertEquals(-1, swipeStep(200f, threshold = 100f))
        assertEquals(1, swipeStep(-200f, threshold = 100f))
        assertEquals(0, swipeStep(60f, threshold = 100f))
    }

    @Test fun swipingRightGoesBackAndLeftGoesForward() {
        show()
        compose.onNodeWithTag("month").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(YearMonth.of(2026, 8), month)
        compose.onNodeWithTag("month").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(YearMonth.of(2026, 9), month)
    }

    @Test fun theArrowsHaveNamesForTalkBack() {
        show()
        compose.onNodeWithContentDescription("Next month").performClick()
        assertEquals(current, month)
        compose.onNodeWithContentDescription("Previous month").performClick()
        assertEquals(YearMonth.of(2026, 9), month)
    }

    @Test fun withAnimationsOffTheNewMonthShowsAtOnce() {
        Settings.Global.putFloat(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        show()
        compose.onNodeWithText("September 2026").assertExists()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            month = YearMonth.of(2026, 8)
            Snapshot.sendApplyNotifications()
        }
        // A few frames, far less than the slide would take (the spring settles in about 300ms).
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("August 2026").assertExists()
        compose.onNodeWithText("September 2026").assertDoesNotExist()
    }
}
