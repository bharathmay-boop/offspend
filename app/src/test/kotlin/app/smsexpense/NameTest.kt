package app.smsexpense

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The product is called Offspend (#206): everywhere the name shows, it is that one.
@RunWith(RobolectricTestRunner::class)
class NameTest {
    private val app get() = ApplicationProvider.getApplicationContext<ExpenseApp>()

    @Test fun theAppIsCalledOffspend() {
        assertEquals("Offspend", app.getString(R.string.app_name))
    }

    @Test fun thePrivacyPolicyInTheAppNamesIt() {
        val policy = app.assets.open("privacy-policy.md").bufferedReader().use { it.readText() }
        assertTrue(policy.contains("Offspend helps you track what you spend"))
    }

    @Test fun theProblemReportNamesIt() = runTest {
        assertTrue(ProblemReport.build(app).startsWith("Offspend problem report"))
    }
}
