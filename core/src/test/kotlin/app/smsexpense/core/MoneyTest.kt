package app.smsexpense.core

import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    @Test fun rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException::class.java) { Money(BigDecimal("-1"), "INR") }
    }

    @Test fun rejectsNonIsoCurrency() {
        assertThrows(IllegalArgumentException::class.java) { Money(BigDecimal("1"), "Rs") }
    }
}
