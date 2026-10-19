package app.smsexpense

import android.graphics.Color
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The sheet sits over whatever is behind it, so the status bar is see-through, not a black strip (#73).
@RunWith(RobolectricTestRunner::class)
class SheetStatusBarTest {
    @Test fun theSheetsWindowIsTranslucentWithATransparentStatusBar() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.intent(app, 1)).onActivity { activity ->
            val window = activity.window
            assertTrue(activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowIsTranslucent)).getBoolean(0, false))
            // Without this flag Android paints its own black status bar, whatever colour is set.
            assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS != 0)
            @Suppress("DEPRECATION")
            assertEquals(Color.TRANSPARENT, window.statusBarColor)
        }
    }
}
