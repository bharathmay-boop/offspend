package app.smsexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class RecordTest {
    private lateinit var db: AppDatabase
    private lateinit var ledger: Ledger

    private val payment = "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo CALIFORNIA BURRITO\nOn 08/10/26\nRef 412398765012"

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        ledger = Ledger(db).also { it.seedDefaultCategories() }
    }

    @After fun tearDown() = db.close()

    @Test fun aBankPaymentIsSavedWithASuggestedCategoryButNotConfirmed() = runTest {
        val id = ledger.record(Source.SMS, "AD-HDFCBK", payment, 1_000L)
        val tx = db.transactions().byId(id!!)!!
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(0, BigDecimal("320").compareTo(tx.amount))
        assertEquals("CALIFORNIA BURRITO", tx.merchant)
        assertEquals("Food", db.categories().all().first { it.id == tx.categoryId }.name)
        assertFalse(tx.confirmed)
    }

    @Test fun anOtpIsNotSaved() = runTest {
        assertNull(ledger.record(Source.SMS, "VM-HDFCBK", "123456 is the OTP for your transaction of Rs.2,399.00", 1_000L))
        assertEquals(0, db.transactions().observeAll().first().size)
    }

    @Test fun theSameMessageTwiceIsSavedOnce() = runTest {
        assertNotNull(ledger.record(Source.SMS, "AD-HDFCBK", payment, 1_000L))
        assertNull(ledger.record(Source.SMS, "AD-HDFCBK", payment, 1_000L))
        assertEquals(1, db.transactions().observeAll().first().size)
    }
}
