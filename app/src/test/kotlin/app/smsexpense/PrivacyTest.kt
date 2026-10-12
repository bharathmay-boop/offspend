package app.smsexpense

import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrivacyTest {
    @Test fun theMoneyDatabaseIsNotIncludedInAndroidBackups() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        val flags = app.packageManager.getApplicationInfo(app.packageName, 0).flags
        assertEquals("allowBackup must be off: the app promises nothing leaves the phone", 0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
