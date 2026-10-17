package app.smsexpense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.CategoryEntity
import app.smsexpense.data.Ledger
import app.smsexpense.ui.AddResult
import app.smsexpense.ui.AddSheet
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.defaultCurrency
import app.smsexpense.ui.parseAmount
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.Motion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

// Cash, a card with no SMS, money paid back in person: all can be added by hand (#51).
@RunWith(RobolectricTestRunner::class)
class AddByHandTest {
    @get:Rule val compose = createComposeRule()
    private var saved: AddResult? = null
    private var saves = 0
    private val today = LocalDate.of(2026, 10, 13)
    private val categories = listOf(CategoryEntity(1, "Food", 0), CategoryEntity(2, "Groceries", 1))

    private fun sheet() = compose.setContent {
        ExpenseTheme {
            Box(Modifier.height(2000.dp)) {
                AddSheet(
                    categories = categories, currencies = listOf("INR", "USD"), defaultCurrency = "INR",
                    payees = listOf("Swiggy", "Sharma Kirana"), guess = { name, _ -> if (name == "Swiggy") 1L else null },
                    today = today, onSave = { saved = it; saves++ },
                )
            }
        }
    }

    private fun fileUnder(name: String) {
        compose.onNodeWithText(name).performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        compose.waitForIdle()
    }

    @Test fun theHomeScreenHasAnAddButton() {
        var added = 0
        compose.setContent {
            ExpenseTheme {
                HomeScreen(
                    month = YearMonth.of(2026, 10), summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                    importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                    onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = {}, onAdd = { added++ }, today = today,
                )
            }
        }
        compose.onNodeWithContentDescription("Add a payment").performClick()
        assertEquals(1, added)
    }

    @Test fun amountsMustBeAboveZeroWithAtMostTwoDecimals() {
        assertEquals(BigDecimal("50"), parseAmount("50"))
        assertEquals(BigDecimal("1250.5"), parseAmount("1,250.5"))
        assertNull(parseAmount("0"))
        assertNull(parseAmount("1.234"))
        assertNull(parseAmount(""))
        assertNull(parseAmount("abc"))
    }

    @Test fun theCurrencyUsedMostRecentlyIsTheDefault() {
        assertEquals("AED", defaultCurrency(listOf("AED", "AED", "INR"), Locale.forLanguageTag("en-IN")))
        assertEquals("INR", defaultCurrency(emptyList(), Locale.forLanguageTag("en-IN")))
    }

    @Test fun aCashPaymentIsFiledWithOneTapOnACategory() {
        sheet()
        compose.onNodeWithTag("amount").performTextInput("50")
        fileUnder("Food")
        assertEquals(AddResult(Direction.DEBIT, BigDecimal("50"), "INR", null, 1, today, ""), saved)
    }

    @Test fun moneyReceivedIsMarkedReceived() {
        sheet()
        compose.onNodeWithText("Received").performClick()
        compose.onNodeWithTag("amount").performTextInput("2000")
        compose.onNodeWithTag("payee").performTextInput("Ravi")
        fileUnder("Groceries")
        assertEquals(Direction.CREDIT, saved!!.direction)
        assertEquals("Ravi", saved!!.payee)
    }

    @Test fun withoutAnAmountACategoryTapSavesNothingAndSaysWhy() {
        sheet()
        fileUnder("Food")
        assertNull(saved)
        compose.onNodeWithText("Enter an amount above zero").assertExists()
    }

    // Graphite on #147: the amount can still be edited during the pause after a category tap.
    @Test fun clearingTheAmountDuringThePauseSavesNothingAndSaysWhy() {
        sheet()
        compose.onNodeWithTag("amount").performTextInput("50")
        compose.onNodeWithText("Food").performScrollTo().performClick()
        compose.onNodeWithTag("amount").performTextClearance()
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        compose.waitForIdle()
        assertNull(saved)
        compose.onNodeWithText("Enter an amount above zero").assertExists()
    }

    @Test fun tappingSaveTwiceSavesOnce() {
        sheet()
        compose.onNodeWithTag("amount").performTextInput("50")
        compose.onNodeWithText("Add note").performScrollTo().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(1, saves)
    }

    @Test fun aKnownPayeeIsSuggestedAndPicksItsCategory() {
        sheet()
        compose.onNodeWithTag("payee").performTextInput("Swi")
        compose.onNodeWithText("Swiggy").performClick()
        compose.onNodeWithText("Suggested", useUnmergedTree = true).assertExists()
    }

    @Test fun theLedgerStoresAHandAddedEntryAsConfirmedAndLearnsThePayee() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val ledger = Ledger(db)
        ledger.seedDefaultCategories()
        val food = db.categories().byName("Food")!!
        ledger.addByHand(AddResult(Direction.DEBIT, BigDecimal("180"), "INR", "Sharma Kirana", food.id, today, "Milk"))
        val tx = db.transactions().observeAll().first().single()
        assertEquals("MANUAL", tx.source)
        assertTrue(tx.confirmed)
        assertEquals("Milk", tx.note)
        assertEquals(food.id, ledger.guessCategory("Sharma Kirana", Direction.DEBIT)!!.id)
        db.close()
    }
}
