package app.smsexpense

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.SheetResult
import app.smsexpense.ui.components.CategoryIcon
import app.smsexpense.ui.components.PickableIcons
import app.smsexpense.ui.components.pickableIconRes
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

// A category the person makes can have an icon from the same family as the built-in ones (#75).
@RunWith(RobolectricTestRunner::class)
class CategoryIconPickerTest {
    @get:Rule val compose = createComposeRule()
    private var saved: SheetResult? = null

    private fun sheet() = compose.setContent {
        ExpenseTheme {
            Box(Modifier.height(2000.dp)) {
                CategorySheet(
                    amount = BigDecimal("1899.00"), currency = "INR", direction = Direction.DEBIT, merchant = "DECATHLON",
                    detail = "", categories = emptyList(), initialCategoryId = null, initialNote = null, onSave = { saved = it },
                )
            }
        }
    }

    @Test fun everyPickableIconResolves() {
        PickableIcons.forEach { (key, _) -> assertNotNull("no drawable for $key", pickableIconRes(key)) }
    }

    @Test fun aNewCategoryCanBeSavedWithAnIcon() {
        sheet()
        compose.onNodeWithText("All categories").performClick()
        compose.onNodeWithText("+ New category").performClick()
        compose.onNodeWithTag("newCategory").performTextInput("Fitness")
        compose.onNodeWithContentDescription("Fitness icon").performScrollTo().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(SheetResult(null, "Fitness", "", "fitness_center"), saved)
    }

    @Test fun tappingThePickedIconAgainClearsIt() {
        sheet()
        compose.onNodeWithText("All categories").performClick()
        compose.onNodeWithText("+ New category").performClick()
        compose.onNodeWithTag("newCategory").performTextInput("Pets")
        compose.onNodeWithContentDescription("Pets icon").performScrollTo().performClick().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(SheetResult(null, "Pets", "", null), saved)
    }

    @Test fun aPickedIconReplacesTheLetter() {
        compose.setContent { ExpenseTheme { CategoryIcon("Office", slot = 9, icon = "work") } }
        compose.onNodeWithText("O", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun theLedgerStoresTheIconWithTheNewCategory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val ledger = Ledger(db)
        val id = db.transactions().insert(
            TransactionEntity(
                direction = Direction.DEBIT, amount = BigDecimal("1899"), currency = "INR", merchant = "DECATHLON",
                accountLast4 = null, occurredAt = 0, categoryId = null, note = null, source = "SMS", fingerprint = "f1",
            ),
        )
        ledger.file(id, null, "Fitness", "", newCategoryIcon = "fitness_center")
        assertEquals("fitness_center", db.categories().byName("Fitness")!!.icon)
        db.close()
    }

    @Test fun anExistingDatabaseKeepsItsDataWhenTheIconColumnArrives() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration-test.db").apply { parentFile!!.mkdirs(); delete() }
        // The version 1 schema exactly as Room created it before #75.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            v1.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `direction` TEXT NOT NULL, `amount` TEXT NOT NULL, `currency` TEXT NOT NULL, `merchant` TEXT, `accountLast4` TEXT, `occurredAt` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT, `source` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `confirmed` INTEGER NOT NULL)")
            v1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_fingerprint` ON `transactions` (`fingerprint`)")
            v1.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `slot` INTEGER NOT NULL)")
            v1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            v1.execSQL("CREATE TABLE IF NOT EXISTS `merchant_rules` (`merchantKey` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`merchantKey`))")
            v1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            v1.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6d5a1964be2c6aa3ded3b55f42ed5084')")
            v1.execSQL("INSERT INTO categories (name, slot) VALUES ('Office', 12)")
            v1.version = 1
        }
        val db = AppDatabase.create(context, "migration-test.db")
        val office = db.categories().byName("Office")!!
        assertEquals(12, office.slot)
        assertEquals(null, office.icon)
        db.close()
    }
}
