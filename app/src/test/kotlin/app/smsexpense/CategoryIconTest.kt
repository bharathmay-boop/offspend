package app.smsexpense

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.smsexpense.core.DefaultCategories
import app.smsexpense.ui.components.CategoryIcon
import app.smsexpense.ui.components.categoryIconRes
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryIconTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyBuiltInCategoryHasAnIcon() {
        DefaultCategories.forEach { assertNotNull("no icon for $it", categoryIconRes(it)) }
    }

    @Test fun aCategoryThePersonMadeShowsItsFirstLetter() {
        assertNull(categoryIconRes("office"))
        compose.setContent { ExpenseTheme { CategoryIcon("office", slot = 3) } }
        compose.onNodeWithText("O", useUnmergedTree = true).assertExists()
    }

    @Test fun aPaymentWithNoCategoryShowsAQuestionMark() {
        compose.setContent { ExpenseTheme { CategoryIcon(null, slot = -1) } }
        compose.onNodeWithText("?", useUnmergedTree = true).assertExists()
    }
}
