package app.smsexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class DedupeTest {
    private lateinit var db: AppDatabase
    private lateinit var ledger: Ledger
    private val minute = 60_000L
    private val gpay = "com.google.android.apps.nbu.paisa.user"

    // PNB names no payee; Google Pay does.
    private val bankSms = "Ac XXXXXXXX3210 Debited with Rs.320.00 , 08-10-2026 13:15:31 thru UPI:428877776666. Bal Rs.12,000.00 CR. Helpline 18001800-PNB"
    private val gpayNotification = "Paid ₹320 to California Burrito"

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        ledger = Ledger(db).also { it.seedDefaultCategories() }
    }

    @After fun tearDown() = db.close()

    private suspend fun all() = db.transactions().observeAll().first()

    @Test fun theSamePaymentFromSmsAndANotificationIsFiledOnce() = runTest {
        assertNotNull(ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0))
        assertNull(ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute))
        assertEquals(1, all().size)
    }

    @Test fun theNotificationFillsInThePayeeTheSmsDidNotName() = runTest {
        ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, minute)
        assertEquals("California Burrito", all().single().merchant)
    }

    @Test fun itWorksWhicheverArrivesFirst() = runTest {
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 0)
        assertNull(ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 3 * minute))
        assertEquals("California Burrito", all().single().merchant)
    }

    // #170: the payment app usually names the payee better than the bank.
    private val hdfcVpa = "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo zomatoorder.payu@hdfcbank\nOn 08/10/26\nRef 412398765012"
    private val gpayZomato = "Paid ₹320 to Zomato"

    @Test fun theAppsNameReplacesTheBanksWhenTheBankCameFirst() = runTest {
        ledger.record(Source.SMS, "AD-HDFCBK", hdfcVpa, 0)
        ledger.record(Source.NOTIFICATION, gpay, gpayZomato, minute)
        assertEquals("Zomato", all().single().merchant)
    }

    @Test fun theAppsNameStaysWhenTheBankComesSecond() = runTest {
        ledger.record(Source.NOTIFICATION, gpay, gpayZomato, 0)
        ledger.record(Source.SMS, "AD-HDFCBK", hdfcVpa, minute)
        assertEquals("Zomato", all().single().merchant)
    }

    @Test fun aNameThePersonTypedAlwaysWins() = runTest {
        val id = ledger.record(Source.SMS, "AD-HDFCBK", hdfcVpa, 0)!!
        db.transactions().update(db.transactions().byId(id)!!.copy(merchant = "Office lunch"))
        ledger.record(Source.NOTIFICATION, gpay, gpayZomato, minute)
        assertEquals("Office lunch", all().single().merchant)
    }

    @Test fun aBetterNameGetsAGuessWhenThePersonHasNotPickedOne() = runTest {
        ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)
        assertNull(all().single().categoryId)
        ledger.record(Source.NOTIFICATION, gpay, gpayNotification, minute)
        assertEquals(db.categories().byName("Food")!!.id, all().single().categoryId)
        assertFalse(all().single().confirmed)
    }

    @Test fun aCategoryThePersonPickedIsKeptWhenTheNameImproves() = runTest {
        val id = ledger.record(Source.SMS, "AD-HDFCBK", hdfcVpa, 0)!!
        val travel = db.categories().byName("Travel")!!.id
        ledger.setCategory(id, travel)
        ledger.record(Source.NOTIFICATION, gpay, gpayZomato, minute)
        assertEquals(travel, all().single().categoryId)
    }

    @Test fun paymentsFarApartInTimeAreKeptSeparately() = runTest {
        ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)
        assertNotNull(ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 30 * minute))
    }

    @Test fun differentAmountsAreKeptSeparately() = runTest {
        ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)
        assertNotNull(ledger.record(Source.NOTIFICATION, gpay, "Paid ₹321 to California Burrito", minute))
    }

    @Test fun twoEqualSmsPaymentsAreBothKept() = runTest {
        // Two coffees at the same price, both reported by SMS: these are two payments.
        ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)
        assertNotNull(ledger.record(Source.SMS, "VM-PNBSMS", bankSms.replace("428877776666", "428877776667"), minute))
        assertEquals(2, all().size)
    }

    // #187: deleting one report must not hide a later real payment the other source reports.
    @Test fun aDeletedPaymentDoesNotSwallowTheOtherSourcesReport() = runTest {
        val deleted = ledger.record(Source.SMS, "VM-PNBSMS", bankSms, 0)!!
        ledger.delete(deleted)
        assertNotNull(ledger.record(Source.NOTIFICATION, gpay, gpayNotification, 2 * minute))
        assertEquals("California Burrito", all().single().merchant)
    }
}
