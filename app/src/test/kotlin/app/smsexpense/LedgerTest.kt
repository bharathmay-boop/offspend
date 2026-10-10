package app.smsexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import app.smsexpense.data.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class LedgerTest {
    private lateinit var db: AppDatabase
    private lateinit var ledger: Ledger

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        ledger = Ledger(db)
        ledger.seedDefaultCategories()
    }

    @After fun tearDown() = db.close()

    private suspend fun save(merchant: String, fingerprint: String) = db.transactions().insert(
        TransactionEntity(
            direction = Direction.DEBIT, amount = BigDecimal("450"), currency = "INR", merchant = merchant,
            accountLast4 = null, occurredAt = 0, categoryId = null, note = null, source = "SMS", fingerprint = fingerprint,
        ),
    )

    @Test fun seedsDefaultCategoriesOnceWithFixedColorSlots() = runTest {
        ledger.seedDefaultCategories()
        val all = db.categories().all()
        assertEquals(12, all.size)
        assertEquals((0 until 12).toList(), all.map { it.slot })
    }

    @Test fun aCorrectionIsRememberedForTheSameMerchantNextTime() = runTest {
        assertEquals("Food", ledger.guessCategory("SWIGGY", Direction.DEBIT)?.name)

        val office = ledger.categoryNamed("Office lunch")
        ledger.setCategory(save("SWIGGY", "one"), office.id)

        assertEquals("Office lunch", ledger.guessCategory("swiggy@icici", Direction.DEBIT)?.name)
    }

    @Test fun settingACategoryConfirmsTheTransaction() = runTest {
        val id = save("RAHUL S", "two")
        ledger.setCategory(id, ledger.categoryNamed("Transfers").id)
        assertTrue(db.transactions().byId(id)!!.confirmed)
    }

    @Test fun newCategoriesTakeTheNextColorSlotAndNamesAreNotDuplicated() = runTest {
        val a = ledger.categoryNamed("Pets")
        val b = ledger.categoryNamed("  pets ")
        assertEquals(a.id, b.id)
        assertEquals(12, a.slot)
    }

    @Test fun blankNotesAreStoredAsNoNote() = runTest {
        val id = save("ZEPTO", "three")
        ledger.setNote(id, "   ")
        assertNull(db.transactions().byId(id)!!.note)
    }
}
