package app.smsexpense

import app.smsexpense.ui.components.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class FormatMoneyTest {
    // #188: a row saved with an unknown code before the fix still shows instead of crashing Home.
    @Test fun anUnknownCurrencyCodeIsShownAsItIs() {
        assertEquals("QQQ 1240.50", formatMoney(BigDecimal("1240.50"), "QQQ", Locale.US))
    }
}
