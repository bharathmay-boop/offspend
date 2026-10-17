package app.smsexpense

import android.Manifest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = { reviewed = it }, today = today,
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
            compose.onNodeWithText("1 of 2").assertExists()
            compose.onNodeWithText("Food").performClick()
            compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 500)
            compose.waitForIdle()
            compose.onNodeWithText("2 of 2").assertExists()
            compose.onNodeWithText("Groceries").performClick()
            compose.mainClock.advanceTimeBy(Motion.CONFIRM_HOLD_MS + 500)
            compose.waitForIdle()
        }
        runBlocking {
            assertNotNull(app.db.transactions().byId(a)!!.categoryId)
            assertNotNull(app.db.transactions().byId(b)!!.categoryId)
        }
    }
}
