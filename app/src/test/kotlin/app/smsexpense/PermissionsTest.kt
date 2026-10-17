package app.smsexpense

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The promise is that nothing leaves the phone. An app without network permissions cannot break it by accident. */
@RunWith(RobolectricTestRunner::class)
class PermissionsTest {
    @Test fun theAppCannotGoOnline() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        val requested = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("INTERNET must not be requested: ${requested.toList()}", "android.permission.INTERNET" in requested)
        assertFalse("ACCESS_NETWORK_STATE must not be requested", "android.permission.ACCESS_NETWORK_STATE" in requested)
    }
}
