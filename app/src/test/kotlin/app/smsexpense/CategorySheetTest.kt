package app.smsexpense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.SheetResult
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class CategorySheetTest {
    @get:Rule val compose = createComposeRule()

    private val categories = listOf(CategoryEntity(1, "Food", 0), CategoryEntity(2, "Groceries", 1))
    private var saved: SheetResult? = null

    private fun show(initialCategory: Long?, maxHeight: Int = 2000) = compose.setContent {
        ExpenseTheme {
            Box(Modifier.height(maxHeight.dp)) {
            CategorySheet(
                amount = BigDecimal("320.00"), currency = "INR", direction = Direction.DEBIT,
                merchant = "CALIFORNIA BURRITO", detail = "8 Oct · Account ··1234",
                categories = categories, initialCategoryId = initialCategory, initialNote = null,
                onSave = { saved = it }, onCancel = {},
            )
            }
        }
    }

    // With the keyboard open the sheet gets only a strip of screen (#47); everything must still be reachable.
    @Test fun aShortSheetScrollsSoTheFieldsAndSaveStayReachable() {
        show(null, maxHeight = 300)
        compose.onNodeWithText("+ New category").performScrollTo().performClick()
        compose.onNodeWithTag("newCategory").performScrollTo().performTextInput("Office")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(SheetResult(null, "Office", ""), saved)
    }

    @Test fun showsTheAmountAndPayee() {
        show(1)
        compose.onNodeWithText("CALIFORNIA BURRITO").assertExists()
        // TalkBack reads the whole amount with its direction, not just the symbols.
        compose.onNodeWithContentDescription("Paid ₹320.00").assertExists()
    }

    @Test fun pickingAnotherCategoryAndANoteSavesBoth() {
        show(1)
        compose.onNodeWithText("Groceries").performClick()
        compose.onNodeWithTag("note").performTextInput("Weekly shop")
        compose.onNodeWithText("Save").performClick()
        assertEquals(SheetResult(2, null, "Weekly shop"), saved)
    }

    @Test fun aNewCategoryCanBeCreatedFromTheSheet() {
        show(null)
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("+ New category").performClick()
        compose.onNodeWithTag("newCategory").performTextInput("Pets")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals(SheetResult(null, "Pets", ""), saved)
    }
}
