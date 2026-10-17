package app.smsexpense

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Direction
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import app.smsexpense.ui.AddResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate

// Every source that reported a payment is kept, so the sheet can show where it came from (#169).
@RunWith(RobolectricTestRunner::class)
class SightingsTest {
    private lateinit var db: AppDatabase
    private lateinit var ledger: Ledger
    private val minute = 60_000L
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val bankSms = "Ac XXXXXXXX3210 Debited with Rs.320.00 , 08-10-2026 13:15:31 thru UPI:428877776666. Bal Rs.12,000.00 CR. Helpline 18001800-PNB"
    private val gpayNotification = "Paid ₹320 to California Burrito"

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        ledger = Ledger(db).also { it.seedDefaultCategories() }
    }

    @After fun tearDown() = db.close()

    @Test fun aBankTextThenAnAppAlertIsOnePaymentSeenTwice() = runTest {
        val id = ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)!!
        assertNull(ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute))

        val seen = db.sightings().forTransaction(id)
        assertEquals(listOf("SMS", "NOTIFICATION"), seen.map { it.source })
        assertEquals(listOf("VM-PNBSMS", gpay), seen.map { it.sender })
        assertEquals(listOf(null, "California Burrito"), seen.map { it.merchant })
        assertEquals("3210", seen[0].accountLast4)
    }

    @Test fun theSameAlertTwiceIsOneSighting() = runTest {
        val id = ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)!!
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute)
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute)
        assertEquals(2, db.sightings().forTransaction(id).size)
    }

    @Test fun aPaymentAddedByHandIsSeenByThePerson() = runTest {
        val id = ledger.addByHand(AddResult(Direction.DEBIT, BigDecimal("50"), "INR", "Chai stall", 1, LocalDate.of(2026, 10, 13), ""))
        val seen = db.sightings().forTransaction(id).single()
        assertEquals("MANUAL", seen.source)
        assertNull(seen.sender)
    }

    @Test fun noSightingHoldsTheMessageText() = runTest {
        val id = ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)!!
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute)
        for (s in db.sightings().forTransaction(id)) {
            assertFalse(s.toString().contains("Debited with"))
            assertFalse(s.toString().contains("Paid ₹320"))
        }
    }

    @Test fun anExistingDatabaseGetsOneSightingPerPayment() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("v3-test.db").apply { parentFile!!.mkdirs(); delete() }
        // The version 3 schema exactly as Room created it before #169.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v3 ->
            v3.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `direction` TEXT NOT NULL, `amount` TEXT NOT NULL, `currency` TEXT NOT NULL, `merchant` TEXT, `accountLast4` TEXT, `occurredAt` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT, `source` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `confirmed` INTEGER NOT NULL, `deleted` INTEGER NOT NULL DEFAULT 0)")
            v3.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_fingerprint` ON `transactions` (`fingerprint`)")
            v3.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `slot` INTEGER NOT NULL, `icon` TEXT)")
            v3.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            v3.execSQL("CREATE TABLE IF NOT EXISTS `merchant_rules` (`merchantKey` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`merchantKey`))")
            v3.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            v3.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'v3')")
            v3.execSQL("INSERT INTO transactions (direction, amount, currency, merchant, accountLast4, occurredAt, categoryId, note, source, fingerprint, confirmed) VALUES ('DEBIT', '320.00', 'INR', 'SWIGGY', '1234', 1000, NULL, 'lunch', 'SMS', 'f1', 1)")
            v3.execSQL("INSERT INTO transactions (direction, amount, currency, merchant, accountLast4, occurredAt, categoryId, note, source, fingerprint, confirmed) VALUES ('DEBIT', '50.00', 'INR', 'Chai', NULL, 2000, NULL, NULL, 'MANUAL', 'manual-x', 1)")
            v3.version = 3
        }
        val db = AppDatabase.create(context, "v3-test.db")
        val all = db.transactions().observeAll().first()
        assertEquals(2, all.size)
        assertEquals("lunch", all.first { it.merchant == "SWIGGY" }.note)
        for (tx in all) {
            val seen = db.sightings().forTransaction(tx.id).single()
            assertEquals(tx.source, seen.source)
            assertEquals(tx.occurredAt, seen.seenAt)
            assertNull(seen.sender)
        }
        db.close()
    }
}
