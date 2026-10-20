package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ModelFallbackTest {
    // Passes the filter (it says "debited") but has no currency next to the amount, so the rules cannot read it.
    private val message = "Txn alert: your a/c ending 4455 is debited for 1,240.50 towards ZEPTO MARKETPLACE. Ref 9988"

    private fun answer(amount: String = "1240.50", merchant: String? = "ZEPTO MARKETPLACE", account: String? = "4455", direction: String = "DEBIT") =
        """{"is_transaction": true, "direction": "$direction", "amount": "$amount", "currency": "INR", """ +
            """"merchant": ${merchant?.let { "\"$it\"" } ?: "null"}, "account": ${account?.let { "\"$it\"" } ?: "null"}}"""

    @Test fun theRulesReallyCannotReadThisMessage() {
        assertEquals(SmsReading.NotUnderstood, SmsParser.parse(Source.SMS, "VM-FEDBNK", message))
    }

    @Test fun acceptsAnAnswerThatMatchesTheMessage() {
        val tx = ModelFallback.accept(message, answer())!!
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(0, BigDecimal("1240.50").compareTo(tx.money.amount))
        assertEquals("ZEPTO MARKETPLACE", tx.merchant)
        assertEquals("4455", tx.account)
    }

    @Test fun rejectsAnAmountThatIsNotInTheMessage() {
        assertNull(ModelFallback.accept(message, answer(amount = "1340.50")))
    }

    @Test fun dropsAMerchantOrAccountTheModelMadeUp() {
        val tx = ModelFallback.accept(message, answer(merchant = "BLINKIT", account = "9999"))!!
        assertNull(tx.merchant)
        assertNull(tx.account)
    }

    @Test fun readsJsonWrappedInProseOrCodeFences() {
        assertTrue(ModelFallback.accept(message, "Sure! ```json\n${answer()}\n```") != null)
    }

    @Test fun rejectsGarbageNonTransactionsAndBadDirections() {
        assertNull(ModelFallback.accept(message, "I can't help with that"))
        assertNull(ModelFallback.accept(message, null))
        assertNull(ModelFallback.accept(message, """{"is_transaction": false}"""))
        assertNull(ModelFallback.accept(message, answer(direction = "SIDEWAYS")))
    }

    @Test fun thePromptCarriesTheMessage() {
        assertTrue(ModelFallback.prompt(message).endsWith(message))
    }

    // #188: a made-up code would crash every screen the payment shows on.
    @Test fun anUnknownCurrencyCodeFallsBackToTheDefault() {
        val qqq = answer().replace("\"INR\"", "\"QQQ\"")
        assertEquals("INR", ModelFallback.accept(message, qqq)!!.money.currency)
        assertEquals("AED", ModelFallback.accept(message, answer().replace("\"INR\"", "\"aed\""))!!.money.currency)
    }
}
