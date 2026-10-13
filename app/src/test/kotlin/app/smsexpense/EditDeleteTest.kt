package app.smsexpense

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import app.smsexpense.ui.AddResult
import app.smsexpense.ui.AddSheet
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

// Mistakes get fixed: any entry can be edited or deleted, with undo (#52).
@RunWith(RobolectricTestRunner::class)
class EditDeleteTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var ledger: Ledger
    private val day = LocalDate.of(2026, 10, 13)
    private val sms = "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo CALIFORNIA BURRITO\nOn 08/10/26\nRef 412398765012"

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        ledger = Ledger(db)
        ledger.seedDefaultCategories()
    }

    @After fun tearDown() = db.close()

    private suspend fun visible() = db.transactions().observeAll().first()

    @Test fun aHandAddedEntryCanBeChangedInFull() = runBlocking {
        val id = ledger.addByHand(AddResult(Direction.DEBIT, BigDecimal("5000"), "INR", "Kirana", null, day, ""))
        ledger.edit(id, AddResult(Direction.CREDIT, BigDecimal("500"), "AED", "Ravi", null, day.minusDays(2), "paid back"))
        val tx = db.transactions().byId(id)!!
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(0, BigDecimal("500").compareTo(tx.amount))
        assertEquals("AED", tx.currency)
        assertEquals("Ravi", tx.merchant)
        assertEquals("paid back", tx.note)
    }

    @Test fun anEntryReadFromTheBankKeepsWhatTheBankSaid() = runBlocking {
        val id = ledger.record(Source.SMS, "AD-HDFCBK", sms, 1_000L)!!
        ledger.edit(id, AddResult(Direction.CREDIT, BigDecimal("1"), "USD", "Burrito place", null, day, "lunch"))
        val tx = db.transactions().byId(id)!!
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(0, BigDecimal("320").compareTo(tx.amount))
        assertEquals("INR", tx.currency)
        assertEquals("Burrito place", tx.merchant)
        assertEquals("lunch", tx.note)
    }

    @Test fun deleteHidesTheEntryAndUndoBringsItBack() = runBlocking {
        val id = ledger.addByHand(AddResult(Direction.DEBIT, BigDecimal("50"), "INR", null, null, day, ""))
        ledger.delete(id)
        assertTrue(visible().isEmpty())
        ledger.restore(id)
        assertEquals(listOf(id), visible().map { it.id })
    }

    @Test fun aDeletedSmsEntryDoesNotComeBackOnTheNextCatchUp() = runBlocking {
        val id = ledger.record(Source.SMS, "AD-HDFCBK", sms, 2_000L)!!
        ledger.delete(id)
        assertNull(ledger.record(Source.SMS, "AD-HDFCBK", sms, 2_000L))
        assertTrue(visible().isEmpty())
    }

    @Test fun theHomeScreenOffersUndoAfterADelete() {
        var undone: Long? = null
        // The app clears this once it's marked shown, as MainActivity does; Undo must still be there.
        var deleted by mutableStateOf<Long?>(5L)
        compose.setContent {
            ExpenseTheme {
                HomeScreen(
                    month = YearMonth.of(2026, 10), summaries = emptyList(), transactions = emptyList(), categories = emptyMap(),
                    importing = false, notice = null, onNoticeShown = {}, appNotificationsOffer = null,
                    onPreviousMonth = {}, onNextMonth = null, onOpen = {}, onReview = {}, onAdd = {},
                    justDeleted = deleted, onUndoDelete = { undone = it }, onDeletedShown = { deleted = null }, today = day,
                )
            }
        }
        compose.onNodeWithText("Payment deleted").assertExists()
        compose.onNodeWithText("Undo").performClick()
        compose.waitForIdle()
        assertEquals(5L, undone)
    }

    @Test fun theSheetMenuOffersEditAndDelete() {
        var deleted = 0
        var edited = 0
        compose.setContent {
            ExpenseTheme {
                Box(Modifier.height(2000.dp)) {
                    CategorySheet(
                        amount = BigDecimal("320.00"), currency = "INR", direction = Direction.DEBIT, merchant = "CALIFORNIA BURRITO",
                        detail = "", categories = emptyList(), initialCategoryId = null, initialNote = null, onSave = {},
                        onEdit = { edited++ }, onDelete = { deleted++ },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(1, edited)
        assertEquals(1, deleted)
    }

    @Test fun editingABankEntryLocksTheAmount() {
        compose.setContent {
            ExpenseTheme {
                AddSheet(
                    categories = emptyList(), currencies = listOf("INR"), defaultCurrency = "INR", payees = emptyList(),
                    guess = { _, _ -> null }, today = day, onSave = {},
                    initial = AddResult(Direction.DEBIT, BigDecimal("320"), "INR", "California Burrito", null, day, ""),
                    moneyLocked = true,
                )
            }
        }
        compose.onNodeWithTag("amount").assertIsNotEnabled()
        compose.onNodeWithText("From your bank's message", substring = true).assertExists()
    }

    @Test fun anExistingDatabaseGainsTheDeletedColumnWithItsDataKept() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration-v2-test.db").apply { parentFile!!.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v2 ->
            v2.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `direction` TEXT NOT NULL, `amount` TEXT NOT NULL, `currency` TEXT NOT NULL, `merchant` TEXT, `accountLast4` TEXT, `occurredAt` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT, `source` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `confirmed` INTEGER NOT NULL)")
            v2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_fingerprint` ON `transactions` (`fingerprint`)")
            v2.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `slot` INTEGER NOT NULL, `icon` TEXT)")
            v2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            v2.execSQL("CREATE TABLE IF NOT EXISTS `merchant_rules` (`merchantKey` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`merchantKey`))")
            v2.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            v2.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '52a2df6d68f95de71e3338cf31639fc4')")
            v2.execSQL("INSERT INTO transactions (direction, amount, currency, occurredAt, source, fingerprint, confirmed) VALUES ('DEBIT', '99', 'INR', 5, 'SMS', 'x', 0)")
            v2.version = 2
        }
        val upgraded = AppDatabase.create(context, "migration-v2-test.db")
        assertEquals(1, upgraded.transactions().observeAll().first().size)
        upgraded.close()
    }
}
