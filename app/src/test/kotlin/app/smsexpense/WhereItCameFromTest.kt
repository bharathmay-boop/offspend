package app.smsexpense

import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.data.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.math.BigDecimal

// The sheet shows the work the app did: which sources reported the payment, and that it was counted once (#171).
@RunWith(RobolectricTestRunner::class)
class WhereItCameFromTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val bankSms = "Ac XXXXXXXX3210 Debited with Rs.320.00 , 08-10-2026 13:15:31 thru UPI:428877776666. Bal Rs.12,000.00 CR. Helpline 18001800-PNB"

    @Test fun aPaymentFromATextAndAnAlertSaysWhereItCameFrom() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val id = runBlocking {
            app.ledger.seedDefaultCategories()
            val id = app.onMessage(Source.SMS, "VM-PNBSMS", bankSms, 1_000L, notify = false)!!
            app.onMessage(Source.NOTIFICATION, gpay, "Paid ₹320 to California Burrito", 61_000L, notify = false)
            id
        }
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.intent(app, id)).use {
            // The sheet appears once its payment has loaded from the database, off the main thread.
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Where this came from").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("PNB text · a/c ••3210", substring = true).assertExists()
            compose.onNodeWithText("Google Pay alert", substring = true).assertExists()
            compose.onNodeWithText("Seen twice, counted once").assertExists()
            // The account digits show once, in the source line, not again under the payee (#180).
            compose.onAllNodesWithText("3210", substring = true).assertCountEquals(1)
            // The message itself is never shown.
            compose.onNodeWithText("Helpline", substring = true).assertDoesNotExist()

            // While choosing a category the list takes the space; the section comes back after.
            compose.onNodeWithText("Tap to change").performClick()
            compose.onNodeWithText("Where this came from").assertDoesNotExist()
        }
    }

    // A payment from before sightings existed has no source lines, so the date line keeps the account (#180).
    @Test fun aPaymentWithNoSourcesKeepsTheAccountUnderThePayee() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        val id = runBlocking {
            app.ledger.seedDefaultCategories()
            app.db.transactions().insert(
                TransactionEntity(
                    direction = Direction.DEBIT, amount = BigDecimal("320.00"), currency = "INR", merchant = "SWIGGY", accountLast4 = "3210",
                    occurredAt = 1_000L, categoryId = null, note = null, source = "SMS", fingerprint = "old",
                ),
            )
        }
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.intent(app, id)).use {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Swiggy").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Account ••3210", substring = true).assertExists()
            compose.onNodeWithText("Where this came from").assertDoesNotExist()
        }
    }
}
