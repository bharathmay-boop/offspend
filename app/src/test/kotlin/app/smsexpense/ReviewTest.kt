package app.smsexpense

import android.Manifest
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.Motion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// Payments the app could not categorise are filed in one run, not hunted down one by one (#62).
@RunWith(RobolectricTestRunner::class)
class ReviewTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 13)
    private var reviewed: List<Long>? = null

    private fun tx(id: Long, categoryId: Long?) = TransactionEntity(
        id = id, direction = Direction.DEBIT, amount = BigDecimal("100.00"), currency = "INR", merchant = "SHOP $id",
        accountLast4 = null, occurredAt = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - id,
        categoryId = categoryId, note = null, source = "SMS", fingerprint = "f$id",
    )

    private fun show(transactions: List<TransactionEntity>) = compose.setContent {
        ExpenseTheme {
            HomeScreen(
                month = YearMonth.of(2026, 10), summaries = emptyList(), transactions = transactions, categories = emptyMap(),
                importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = { reviewed = it }, onAdd = {}, today = today,
            )
        }
    }

    @Test fun theCountOfPaymentsNeedingACategoryLeadsToReview() {
        show(listOf(tx(1, null), tx(2, 5), tx(3, null), tx(4, null)))
        compose.onNodeWithText("3 payments need a category").assertExists()
        compose.onNodeWithText("Review").performClick()
        assertEquals(listOf(1L, 3L, 4L), reviewed)
    }

    @Test fun oneIsSingular() {
        show(listOf(tx(1, null), tx(2, 5)))
        compose.onNodeWithText("1 payment needs a category").assertExists()
    }

    @Test fun nothingToReviewShowsNothing() {
        show(listOf(tx(2, 5)))
        compose.onNodeWithText("need a category", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Review").assertDoesNotExist()
    }
}

@RunWith(RobolectricTestRunner::class)
class ReviewSheetTest {
    @get:Rule val compose = createEmptyComposeRule()

    // The sheet appears once its payment has loaded from the database, off the main thread, so wait for it.
    private fun waitFor(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun filingOneMovesStraightToTheNext() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val canara = "An amount of INR %s has been DEBITED to your account XXX778 on 06/10/2026. - Canara Bank"
        val (a, b) = runBlocking {
            app.ledger.seedDefaultCategories()
            app.onMessage(Source.SMS, "VM-CANBNK", canara.format("1,050.00"), 1_000L)!! to
                app.onMessage(Source.SMS, "VM-CANBNK", canara.format("2,000.00"), 2_000L)!!
        }
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.reviewIntent(app, longArrayOf(a, b))).use {
            waitFor("1 of 2")
            compose.onNodeWithText("1 of 2", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Food").performClick()
            compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 500)
            compose.waitForIdle()
            waitFor("2 of 2")
            compose.onNodeWithText("2 of 2", useUnmergedTree = true).assertExists()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Groceries").performClick()
            compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 500)
            compose.waitForIdle()
            // The sheet saves on its own scope, which ends when the activity closes at the end of this block.
            // So wait here for the last save, while the main thread keeps running (#230).
            compose.waitUntil(5_000) { runBlocking { app.db.transactions().byId(b)!!.categoryId != null } }
        }
        runBlocking {
            assertNotNull(app.db.transactions().byId(a)!!.categoryId)
            assertNotNull(app.db.transactions().byId(b)!!.categoryId)
        }
    }

    // Some payments can't be filed yet; Skip leaves them as they are and moves on (#74).
    @Test fun skipLeavesThePaymentWithoutACategoryAndMovesOn() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val canara = "An amount of INR %s has been DEBITED to your account XXX778 on 06/10/2026. - Canara Bank"
        val (a, b) = runBlocking {
            app.ledger.seedDefaultCategories()
            app.onMessage(Source.SMS, "VM-CANBNK", canara.format("3,050.00"), 5_000L)!! to
                app.onMessage(Source.SMS, "VM-CANBNK", canara.format("4,000.00"), 6_000L)!!
        }
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.reviewIntent(app, longArrayOf(a, b))).use { scenario ->
            waitFor("Skip")
            compose.onNodeWithText("Skip").performScrollTo().performClick()
            compose.waitForIdle()
            waitFor("2 of 2")
            compose.onNodeWithText("2 of 2", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Skip").performScrollTo().performClick()
            compose.mainClock.advanceTimeBy(2_000)
            compose.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
            var finishing = false
            if (scenario.state != Lifecycle.State.DESTROYED) scenario.onActivity { finishing = it.isFinishing }
            assertTrue("the sheet should close after the last payment", scenario.state == Lifecycle.State.DESTROYED || finishing)
        }
        runBlocking {
            assertNull(app.db.transactions().byId(a)!!.categoryId)
            assertNull(app.db.transactions().byId(b)!!.categoryId)
        }
    }

    // Even a review of one payment can skip it (#74).
    @Test fun aReviewOfOnePaymentCanStillSkip() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val a = runBlocking {
            app.ledger.seedDefaultCategories()
            app.onMessage(Source.SMS, "VM-CANBNK", "An amount of INR 5,050.00 has been DEBITED to your account XXX778 on 06/10/2026. - Canara Bank", 7_000L)!!
        }
        ActivityScenario.launch<CategorySheetActivity>(CategorySheetActivity.reviewIntent(app, longArrayOf(a))).use {
            waitFor("Skip")
            compose.onNodeWithText("Skip").performScrollTo().assertExists()
        }
    }
}
