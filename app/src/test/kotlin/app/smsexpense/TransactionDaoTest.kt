package app.smsexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun tx(fingerprint: String) = TransactionEntity(
        direction = Direction.DEBIT, amount = BigDecimal("320.00"), currency = "INR",
        merchant = "California Burrito", accountLast4 = "1234", occurredAt = 1_760_000_000_000,
        categoryId = null, note = null, source = "SMS", fingerprint = fingerprint,
    )

    @Test fun savesAndReadsATransaction() = runTest {
        val id = db.transactions().insert(tx("a"))
        val saved = db.transactions().byId(id)!!
        assertEquals(BigDecimal("320.00"), saved.amount)
        assertEquals("California Burrito", saved.merchant)
    }

    @Test fun sameFingerprintIsStoredOnce() = runTest {
        db.transactions().insert(tx("same"))
        assertEquals(-1L, db.transactions().insert(tx("same")))
        assertEquals(1, db.transactions().observeAll().first().size)
    }
}
