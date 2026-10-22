package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

// #217: what a message can put in the ledger.
class BoundsTest {
    private fun reading(amount: String, merchant: String? = "Swiggy") =
        SmsReading.Transaction(Direction.DEBIT, Money(BigDecimal(amount), "INR"), merchant, "1234")

    @Test fun anAmountWithHundredsOfDigitsIsNotAPayment() {
        assertNull(Bounds.check(reading("9".repeat(300))))
        assertNull(Bounds.check(reading("1000000000000")))
    }

    @Test fun nothingMovedIsNotAPayment() {
        assertNull(Bounds.check(reading("0.00")))
    }

    @Test fun largeRealAmountsStillCount() {
        // 50 crore rupees, or a few thousand dollars counted in rupiah.
        assertEquals(BigDecimal("500000000.00"), Bounds.check(reading("500000000.00"))!!.money.amount)
        assertEquals(BigDecimal("999999999999.99"), Bounds.check(reading("999999999999.99"))!!.money.amount)
    }

    @Test fun directionOverridesAndOtherInvisibleCharactersAreRemoved() {
        // U+202E shows everything after it reversed; U+200B is a zero-width space; U+2066 starts an isolate.
        assertEquals("Amazonnozama", Bounds.cleanMerchant("Amazon\u202Enozama"))
        assertEquals("Swiggy", Bounds.cleanMerchant("Swi\u200Bggy\u2066"))
        assertEquals("Chai Point", Bounds.cleanMerchant("Chai\u0007 \n  Point"))
    }

    @Test fun aLongPayeeIsCutWithoutSplittingAnEmoji() {
        assertEquals(Bounds.MERCHANT_MAX, Bounds.cleanMerchant("A".repeat(500))!!.length)
        val name = "B".repeat(Bounds.MERCHANT_MAX - 1) + "\uD83D\uDE00"
        assertEquals("B".repeat(Bounds.MERCHANT_MAX - 1), Bounds.cleanMerchant(name))
    }

    @Test fun aPayeeOfOnlyInvisibleCharactersBecomesNoPayee() {
        assertNull(Bounds.check(reading("320", merchant = "\u202E\u200B "))!!.merchant)
    }
}
