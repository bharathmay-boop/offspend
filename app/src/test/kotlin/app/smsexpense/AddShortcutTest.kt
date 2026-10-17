package app.smsexpense

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.xmlpull.v1.XmlPullParser

// A long press on the app icon offers "Add payment", which opens the add sheet (#54).
@RunWith(RobolectricTestRunner::class)
class AddShortcutTest {
    @Test fun theShortcutPointsAtTheAddAction() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        val xml = app.resources.getXml(R.xml.shortcuts)
        val actions = mutableListOf<String>()
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType == XmlPullParser.START_TAG && xml.name == "intent") {
                actions += xml.getAttributeValue("http://schemas.android.com/apk/res/android", "action")
            }
        }
        assertEquals(listOf(MainActivity.ACTION_ADD), actions)
    }

    @Test fun theAddActionOpensTheAddSheet() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java).setAction(MainActivity.ACTION_ADD)).setup()
        val next = shadowOf(app).nextStartedActivity
        assertTrue("expected the add sheet, got $next", next?.component?.className == AddPaymentActivity::class.java.name)
    }
}
