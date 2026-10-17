package app.smsexpense

import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

// Someone who says no first, turns SMS on in Settings and comes back must not be stuck on the permission screen (#151).
@RunWith(RobolectricTestRunner::class)
class PermissionReturnTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun grantingSmsInSettingsAndComingBackOpensTheHomeScreen() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Allow SMS access").assertExists()

            // Off to Settings: the app goes to the background and the permission is granted there.
            scenario.moveToState(Lifecycle.State.CREATED)
            shadowOf(app).grantPermissions(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            scenario.moveToState(Lifecycle.State.RESUMED)

            compose.waitForIdle()
            compose.onNodeWithText("Allow SMS access").assertDoesNotExist()
        }
    }
}
