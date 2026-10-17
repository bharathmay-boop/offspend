package app.smsexpense

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.smsexpense.ui.Bullet
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BulletTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wrappedTextStartsAfterTheBulletNotUnderIt() {
        val point = "Everything stays on this phone. The app has no account and uploads nothing."
        compose.setContent { ExpenseTheme { androidx.compose.foundation.layout.Box(Modifier.width(200.dp)) { Bullet(point) } } }
        val bullet = compose.onNodeWithText("•", useUnmergedTree = true).getBoundsInRoot()
        val text = compose.onNodeWithText(point, useUnmergedTree = true).getBoundsInRoot()
        // The whole text block, including its wrapped lines, sits to the right of the bullet.
        assertTrue("text starts at ${text.left}, bullet ends at ${bullet.right}", text.left >= bullet.right)
    }
}
