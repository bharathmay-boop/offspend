package app.smsexpense

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.smsexpense.core.Direction
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.components.dayLabel
import app.smsexpense.ui.components.displayMerchant
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// The payment list is scanned, not read (#56): day headers instead of a date on every row, names as people write them.
@RunWith(RobolectricTestRunner::class)
class PaymentListTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 13)

    @Test fun daysAreLabelledTodayYesterdayThenByDate() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("Tue, 6 Oct", dayLabel(LocalDate.of(2026, 10, 6), today))
    }

    @Test fun bankCapitalsBecomeTitleCaseButShortFormsStay() {
        assertEquals("Swiggy", displayMerchant("SWIGGY"))
        assertEquals("Blue Tokai", displayMerchant("BLUE TOKAI"))
        assertEquals("IRCTC Tickets", displayMerchant("IRCTC TICKETS"))
        assertEquals("KFC", displayMerchant("KFC"))
        assertEquals("The Belgian Waffle", displayMerchant("THE BELGIAN WAFFLE"))
        // Already mixed case: the bank or the person wrote it that way, so leave it.
        assertEquals("McDonald's", displayMerchant("McDonald's"))
        assertEquals("swiggy@icici", displayMerchant("swiggy@icici"))
    }

    @Test fun paymentsAreGroupedUnderOneHeaderPerDay() {
        val zone = ZoneId.systemDefault()
        fun at(day: LocalDate, id: Long, merchant: String) = TransactionEntity(
            id = id, direction = Direction.DEBIT, amount = BigDecimal("100.00"), currency = "INR", merchant = merchant,
            accountLast4 = null, occurredAt = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            categoryId = null, note = null, source = "SMS", fingerprint = "f$id",
        )
        compose.setContent {
            ExpenseTheme {
                HomeScreen(
                    month = YearMonth.of(2026, 10), summaries = emptyList(),
                    transactions = listOf(at(today, 1, "SWIGGY"), at(today, 2, "ZOMATO"), at(today.minusDays(1), 3, "UBER")),
                    categories = emptyMap(), importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                    onPreviousMonth = {}, onNextMonth = null, onOpen = {}, today = today,
                )
            }
        }
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Yesterday").assertExists()
        compose.onNodeWithText("Swiggy").assertExists()
        compose.onNodeWithText("Oct 13, 2026", substring = true).assertDoesNotExist()
    }
}
