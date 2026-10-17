package app.smsexpense

import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.CategoryTotal
import app.smsexpense.core.CurrencyMonth
import app.smsexpense.ui.MonthCard
import app.smsexpense.ui.glance
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

// The month card is one glance (#57): the total, one bar, the top categories. The full list is one tap away.
@RunWith(RobolectricTestRunner::class)
class MonthGlanceTest {
    @get:Rule val compose = createComposeRule()

    private fun t(name: String?, amount: String) = CategoryTotal(name, BigDecimal(amount))
    private val six = listOf(t("Shopping", "500"), t("Food", "400"), t("Groceries", "300"), t(null, "200"), t("Transport", "100"), t("Bills", "50"))

    @Test fun theTopFourShowAndTheRestAreOther() {
        val g = glance(six, top = 4)
        assertEquals(listOf("Shopping", "Food", "Groceries", null), g.shown.map { it.category })
        assertEquals(BigDecimal("150"), g.other)
    }

    @Test fun nothingIsGroupedWhenThereAreFewCategories() {
        val g = glance(six.take(5), top = 4)
        // Grouping a single category into "Other" hides it for no gain, so five show as five.
        assertEquals(5, g.shown.size)
        assertEquals(BigDecimal.ZERO, g.other)
    }

    private fun show() = compose.setContent {
        ExpenseTheme {
            MonthCard(CurrencyMonth("INR", spent = BigDecimal("1550"), received = BigDecimal.ZERO, spentByCategory = six), slotOf = emptyMap())
        }
    }

    @Test fun theFullListIsOneTapAway() {
        show()
        compose.onNodeWithText("Bills").assertDoesNotExist()
        compose.onNodeWithText("Other", substring = true).performClick()
        compose.onNodeWithText("Bills").assertExists()
    }

    @Test fun withAnimationsOffTheTotalShowsAtOnce() {
        Settings.Global.putFloat(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Spent ₹1,550.00 this month").assertExists()
        compose.onNodeWithText("₹1,550.00").assertExists()
    }

    @Test fun theTotalCountsUpToItsValue() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("₹1,550.00").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("₹1,550.00").assertExists()
    }
}
