package app.smsexpense

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

    private fun show(initialCategory: Long?) = compose.setContent {
        ExpenseTheme {
            CategorySheet(
                amount = BigDecimal("320.00"), currency = "INR", direction = Direction.DEBIT,
                merchant = "CALIFORNIA BURRITO", detail = "8 Oct · Account ··1234",
                categories = categories, initialCategoryId = initialCategory, initialNote = null,
                onSave = { saved = it }, onCancel = {},
            )
        }
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
