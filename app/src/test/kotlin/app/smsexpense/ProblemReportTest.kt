package app.smsexpense

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.ui.AddResult
import app.smsexpense.ui.ReportScreen
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.math.BigDecimal
import java.time.ZonedDateTime

// "Report a problem" (#100): counts and states, never the person's money.
@RunWith(RobolectricTestRunner::class)
class ProblemReportTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: ExpenseApp

    @Before fun setUp() = runTest {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.ledger.seedDefaultCategories()
    }

    @Test fun theReportHoldsCountsAndNothingFromThePayments() = runTest {
        val now = ZonedDateTime.now()
        val nl = 10.toChar()
        val sms = "Sent Rs.98765.43${nl}From HDFC Bank A/C *4321${nl}To SECRETSHOP LTD${nl}On 25/10/26${nl}Ref 412398765012"
        app.onMessage(Source.SMS, "AD-HDFCBK", sms, now.toInstant().toEpochMilli())
        app.ledger.addByHand(
            AddResult(Direction.DEBIT, BigDecimal("77.12"), "INR", "HIDDENPAYEE", null, now.toLocalDate(), "private note"),
            now = now.toInstant().toEpochMilli(),
        )

        val report = ProblemReport.build(app, now)

        assertTrue(report, report.contains("Payments this month: 2"))
        assertTrue(report, report.contains("Added by hand, all time: 1"))
        assertTrue(report, report.contains("App: ${ProblemReport.version(app)}"))
        for (private in listOf("98765", "SECRETSHOP", "4321", "77.12", "HIDDENPAYEE", "private note", "412398765012")) {
            assertFalse("$private is in the report", report.contains(private, ignoreCase = true))
        }
    }

    @Test fun thePermissionStatesAreReadFromThePhone() = runTest {
        assertTrue(ProblemReport.build(app).contains("Read SMS: NOT allowed"))
        shadowOf(app).grantPermissions(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        val report = ProblemReport.build(app)
        assertTrue(report, report.contains("Read SMS: allowed"))
        assertTrue(report, report.contains("Receive SMS: allowed"))
    }

    // Review: if the report cannot be built, the screen must still end with something to share.
    @Test fun aReportThatCannotBeBuiltSaysSoInsteadOfWaitingForever() = runTest {
        val text = ProblemReport.buildOrExplain(app) { throw IllegalStateException("secret detail") }
        assertTrue(text, text.contains("could not be built (IllegalStateException)"))
        assertTrue(text, text.contains(ProblemReport.version(app)))
        assertFalse(text, text.contains("secret detail"))
    }

    @Test fun theShareRequestCarriesTheReportAndOnlyAddsAnAddressWhenThereIsOne() {
        val plain = ProblemReport.shareIntent("the report", "0.1.1 (2)", email = "")
        assertEquals(Intent.ACTION_SEND, plain.action)
        assertEquals("text/plain", plain.type)
        assertEquals("the report", plain.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("Problem report, version 0.1.1 (2)", plain.getStringExtra(Intent.EXTRA_SUBJECT))
        assertNull(plain.getStringArrayExtra(Intent.EXTRA_EMAIL))

        val addressed = ProblemReport.shareIntent("the report", "0.1.1 (2)", email = "help@example.com")
        assertEquals(listOf("help@example.com"), addressed.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList())
    }

    @Test fun theScreenShowsTheWholeReportAndSharesIt() {
        var shared = 0
        compose.setContent { ExpenseTheme { ReportScreen(report = "Payments this month: 3", onShare = { shared++ }, onBack = {}) } }
        compose.onNodeWithText("Payments this month: 3").assertExists()
        compose.onNodeWithText("Share report").performClick()
        assertEquals(1, shared)
    }

    @Test fun theShareButtonWaitsUntilTheReportIsBuilt() {
        compose.setContent { ExpenseTheme { ReportScreen(report = null, onShare = {}, onBack = {}) } }
        compose.onNodeWithText("Share report").assertIsNotEnabled()
    }

    @Test fun reportAProblemInSettingsOpensTheReportScreen() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.onNodeWithText("Report a problem").performClick()
            assertEquals(ReportActivity::class.java.name, shadowOf(app).nextStartedActivity?.component?.className)
        }
    }
}
