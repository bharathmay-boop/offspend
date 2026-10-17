package app.smsexpense

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.smsexpense.ui.PermissionScreen
import app.smsexpense.ui.theme.ExpenseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The first screen asks for trust, so it shows only what is needed to decide (#59). Details are one tap away.
@RunWith(RobolectricTestRunner::class)
class PermissionScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(denied: Boolean = false) = compose.setContent {
        ExpenseTheme { PermissionScreen(deniedBefore = denied, onAllow = {}, onOpenSettings = {}) }
    }

    @Test fun threeShortStepsExplainTheApp() {
        show()
        listOf("Your bank texts you", "The app files the payment", "You confirm with one tap").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Read on this phone. Nothing is uploaded.").assertExists()
        // Play's prominent disclosure has to be visible without a tap.
        compose.onNodeWithText("The app reads your SMS to find bank payments", substring = true).assertExists()
        compose.onNodeWithText("Allow SMS access").assertExists()
    }

    @Test fun whatTheAppReadsIsOneTapAway() {
        show()
        compose.onNodeWithText("never stored", substring = true).assertDoesNotExist()
        compose.onNodeWithText("What does it read?").performScrollTo().performClick()
        compose.onNodeWithText("never stored", substring = true).assertExists()
        compose.onNodeWithText("last 3 months", substring = true).assertExists()
    }

    @Test fun afterADenialItPointsToSettings() {
        show(denied = true)
        compose.onNodeWithText("Open Settings").assertExists()
        compose.onNodeWithText("Ask again").assertExists()
    }
}
