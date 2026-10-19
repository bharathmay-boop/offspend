package app.smsexpense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.SheetResult
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class CategorySheetTest {
    @get:Rule val compose = createComposeRule()

    private val categories = listOf(
        CategoryEntity(1, "Food", 0), CategoryEntity(2, "Groceries", 1), CategoryEntity(3, "Transport", 2), CategoryEntity(4, "Shopping", 3),
    )
    private var saved: SheetResult? = null

    private var skipped = 0

    private fun show(initialCategory: Long?, maxHeight: Int = 2000, initialNote: String? = null, canSkip: Boolean = false, knownPayee: Boolean = false, mostUsed: List<Long> = emptyList()) = compose.setContent {
        ExpenseTheme {
            Box(Modifier.height(maxHeight.dp)) {
            CategorySheet(
                amount = BigDecimal("320.00"), currency = "INR", direction = Direction.DEBIT,
                merchant = "CALIFORNIA BURRITO", detail = "8 Oct · Account ••1234",
                categories = categories, initialCategoryId = initialCategory, initialNote = initialNote,
                onSave = { saved = it }, onSkip = if (canSkip) ({ skipped++ }) else null, knownPayee = knownPayee, mostUsed = mostUsed,
            )
            }
        }
    }

    // With the keyboard open the sheet gets only a strip of screen (#47); everything must still be reachable.
    @Test fun aShortSheetScrollsSoTheFieldsAndSaveStayReachable() {
        show(null, maxHeight = 300)
        compose.onNodeWithText("All categories").performScrollTo().performClick()
        compose.onNodeWithText("+ New category").performScrollTo().performClick()
        compose.onNodeWithTag("newCategory").performScrollTo().performTextInput("Office")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(SheetResult(null, "Office", ""), saved)
    }

    @Test fun showsTheAmountAndPayee() {
        show(1)
        compose.onNodeWithText("California Burrito").assertExists()
        // TalkBack reads the whole amount with its direction, not just the symbols.
        compose.onNodeWithContentDescription("Paid ₹320.00").assertExists()
    }

    // One tap files it (#58): no Save to find in the common case.
    @Test fun tappingACategoryFilesThePaymentAtOnce() {
        show(1)
        compose.onNodeWithText("Tap to change").performClick()
        compose.onNodeWithText("Groceries").performClick()
        // The check mark shows for a moment before the sheet files and closes.
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        compose.waitForIdle()
        assertEquals(SheetResult(2, null, ""), saved)
        compose.onNodeWithText("Save").assertDoesNotExist()
    }

    // #168: for a payee the app knows, a pick changes this payment only, unless the switch is on.
    @Test fun aKnownPayeeOffersToUseThePickEveryTime() {
        show(initialCategory = 1, knownPayee = true)
        compose.onNodeWithText("Tap to change").performClick()
        compose.onNodeWithText("Use my pick for every California Burrito payment").performClick()
        compose.onNodeWithText("Groceries").performClick()
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        compose.waitForIdle()
        assertEquals(SheetResult(2, null, "", alwaysUse = true), saved)
    }

    @Test fun aNewPayeeHasNoSwitchBecauseThePickIsRememberedAnyway() {
        show(initialCategory = 1)
        compose.onNodeWithText("Tap to change").performClick()
        compose.onNodeWithText("Use my pick for every California Burrito payment").assertDoesNotExist()
    }

    // #166: a guessed payment shows its category as one row, not every chip.
    @Test fun aGuessedPaymentShowsOneCategoryRow() {
        show(1)
        compose.onNodeWithContentDescription("Category, Food").assertExists()
        compose.onNodeWithText("Tap to change").assertExists()
        compose.onNodeWithText("Groceries").assertDoesNotExist()
        compose.onNodeWithText("+ New category").assertDoesNotExist()
    }

    @Test fun withNoGuessTheThreeMostUsedAreOffered() {
        show(null, mostUsed = listOf(4, 3, 2))
        compose.onNodeWithText("Shopping").assertExists()
        compose.onNodeWithText("Transport").assertExists()
        compose.onNodeWithText("Groceries").assertExists()
        compose.onNodeWithText("Food").assertDoesNotExist()
        compose.onNodeWithText("All categories").performClick()
        compose.onNodeWithText("Food").assertExists()
    }

    @Test fun aNewInstallOffersTheFirstThreeCategories() {
        show(null)
        compose.onNodeWithText("Food").assertExists()
        compose.onNodeWithText("Transport").assertExists()
        compose.onNodeWithText("Shopping").assertDoesNotExist()
    }

    @Test fun aQuickPickFilesAtOnce() {
        show(null, mostUsed = listOf(3))
        compose.onNodeWithText("Transport").performClick()
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        compose.waitForIdle()
        assertEquals(SheetResult(3, null, ""), saved)
    }

    @Test fun theGuessComesFirstAndIsLabelled() {
        show(2)
        compose.onNodeWithText("Tap to change").performClick()
        compose.onNodeWithText("Suggested", useUnmergedTree = true).assertExists()
        val guess = compose.onNodeWithText("Groceries").getUnclippedBoundsInRoot().left
        val other = compose.onNodeWithText("Food").getUnclippedBoundsInRoot().left
        assertTrue("the guess should come before the other chips", guess < other)
    }

    @Test fun withANoteOpenPickingACategoryWaitsForSave() {
        show(1)
        compose.onNodeWithText("Add note").performClick()
        compose.onNodeWithTag("note").performTextInput("Weekly shop")
        compose.onNodeWithText("Tap to change").performClick()
        compose.onNodeWithText("Groceries").performClick()
        compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 100)
        assertNull(saved)
        compose.onNodeWithText("Save").performClick()
        assertEquals(SheetResult(2, null, "Weekly shop"), saved)
    }

    @Test fun anExistingNoteIsShownAndKept() {
        show(1, initialNote = "Team lunch")
        compose.onNodeWithTag("note").assertTextContains("Team lunch")
        compose.onNodeWithText("Save").performClick()
        assertEquals(SheetResult(1, null, "Team lunch"), saved)
    }

    @Test fun skipShowsOnlyInAReviewRunAndFilesNothing() {
        show(1)
        compose.onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test fun skipCallsBackWithoutSaving() {
        show(1, canSkip = true)
        compose.onNodeWithText("Skip").performScrollTo().performClick()
        assertEquals(1, skipped)
        assertNull(saved)
    }

    @Test fun aNewCategoryIsFiledFromTheKeyboardDoneKey() {
        show(null)
        compose.onNodeWithText("All categories").performClick()
        compose.onNodeWithText("+ New category").performClick()
        compose.onNodeWithTag("newCategory").performTextInput("Pets")
        compose.onNodeWithTag("newCategory").performImeAction()
        assertEquals(SheetResult(null, "Pets", ""), saved)
    }
}
