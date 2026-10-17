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
import org.junit.Assert.assertFalse
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

    // #168: a one-off (a gift bought on Amazon) must not change every Amazon payment after it.
    @Test fun changingAKnownPayeeChangesThisPaymentOnly() = runTest {
        ledger.setCategory(save("AMAZON", "a1"), ledger.categoryNamed("Shopping").id)
        assertTrue(ledger.knowsPayee("AMAZON"))
        val gift = save("AMAZON", "a2")
        val gifts = ledger.categoryNamed("Gifts")
        ledger.setCategory(gift, gifts.id)
        assertEquals(gifts.id, db.transactions().byId(gift)!!.categoryId)
        assertEquals("Shopping", ledger.guessCategory("AMAZON", Direction.DEBIT)?.name)
    }

    @Test fun alwaysUseReplacesWhatTheAppRemembers() = runTest {
        ledger.setCategory(save("AMAZON", "a1"), ledger.categoryNamed("Shopping").id)
        ledger.file(save("AMAZON", "a2"), ledger.categoryNamed("Gifts").id, null, null, alwaysUse = true)
        assertEquals("Gifts", ledger.guessCategory("AMAZON", Direction.DEBIT)?.name)
    }

    @Test fun aPaymentWithNoPayeeTeachesNothing() = runTest {
        val id = db.transactions().insert(
            TransactionEntity(
                direction = Direction.DEBIT, amount = BigDecimal("450"), currency = "INR", merchant = null,
                accountLast4 = null, occurredAt = 0, categoryId = null, note = null, source = "SMS", fingerprint = "none",
            ),
        )
        ledger.setCategory(id, ledger.categoryNamed("Cash").id, alwaysUse = true)
        assertTrue(db.merchantRules().all().isEmpty())
        assertFalse(ledger.knowsPayee(null))
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

    @Test fun fileCreatesATypedCategoryUsesItAndSavesTheNote() = runTest {
        val id = save("PETCO", "four")
        ledger.file(id, categoryId = null, newCategoryName = "Pets", note = "Dog food")
        val tx = db.transactions().byId(id)!!
        assertEquals("Pets", db.categories().all().first { it.id == tx.categoryId }.name)
        assertEquals("Dog food", tx.note)
        assertEquals("Pets", ledger.guessCategory("PETCO", Direction.DEBIT)?.name)
    }

    @Test fun blankNotesAreStoredAsNoNote() = runTest {
        val id = save("ZEPTO", "three")
        ledger.setNote(id, "   ")
        assertNull(db.transactions().byId(id)!!.note)
    }
}
