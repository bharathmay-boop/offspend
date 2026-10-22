package app.smsexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelFallbackLedgerTest {
    private lateinit var db: AppDatabase
    private val unreadable = "Txn alert: your a/c ending 4455 is debited for 1,240.50 towards ZEPTO MARKETPLACE. Ref 9988"
    private val goodAnswer = """{"is_transaction": true, "direction": "DEBIT", "amount": "1240.50", "currency": "INR", "merchant": "ZEPTO MARKETPLACE", "account": "4455"}"""
    private val prompts = mutableListOf<String>()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun ledger(answer: String?) = Ledger(db) { prompt -> prompts += prompt; answer }.also { runTest { it.seedDefaultCategories() } }

    @Test fun aMessageTheRulesCannotReadIsSavedFromTheModelsCheckedAnswer() = runTest {
        val id = ledger(goodAnswer).record(Source.SMS, "VM-FEDBNK", unreadable, 0)
        assertEquals("ZEPTO MARKETPLACE", db.transactions().byId(id!!)!!.merchant)
    }

    @Test fun aMadeUpAmountIsNotSaved() = runTest {
        assertNull(ledger(goodAnswer.replace("1240.50", "9240.50")).record(Source.SMS, "VM-FEDBNK", unreadable, 0))
    }

    @Test fun withoutAModelNothingChanges() = runTest {
        assertNull(Ledger(db).record(Source.SMS, "VM-FEDBNK", unreadable, 0))
    }

    @Test fun otpsAndMessagesTheRulesCanReadNeverReachTheModel() = runTest {
        val l = ledger(goodAnswer)
        l.record(Source.SMS, "VM-HDFCBK", "123456 is the OTP for your transaction of Rs.2,399.00. Do not share.", 0)
        l.record(Source.SMS, "AD-HDFCBK", "Sent Rs.320.00\nFrom HDFC Bank A/C *1234\nTo CALIFORNIA BURRITO", 0)
        assertEquals(emptyList<String>(), prompts)
    }

    // #218: the SMS receiver's deadline is about 10 seconds; a model that hangs must not hold it.
    @Test fun aSlowModelIsGivenUpOnInsideTheReceiversDeadline() = runTest {
        val slow = Ledger(db) { delay(60_000); goodAnswer }
        assertNull(slow.record(Source.SMS, "VM-FEDBNK", unreadable, 0))
        assertEquals(Ledger.MODEL_TIMEOUT_MS, testScheduler.currentTime)
    }

    // #218: catch-up reads the inbox again with a minute of overlap; a message already filed isn't sent to the model again.
    @Test fun aMessageAlreadyFiledIsNotSentToTheModelAgain() = runTest {
        val l = ledger(goodAnswer)
        l.record(Source.SMS, "VM-FEDBNK", unreadable, 5_000)
        l.record(Source.SMS, "VM-FEDBNK", unreadable, 5_000)
        assertEquals(1, prompts.size)
    }
}
