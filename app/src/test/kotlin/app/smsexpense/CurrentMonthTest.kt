package app.smsexpense

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class CurrentMonthTest {
    private val sep = YearMonth.of(2026, 9)
    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11)

    @Test fun lookingAtThisMonthMovesToTheNewMonth() = assertEquals(nov, followCurrentMonth(oct, oct, nov))

    @Test fun lookingAtAnOlderMonthStaysThere() = assertEquals(sep, followCurrentMonth(sep, oct, nov))

    @Test fun nothingChangesWithinTheSameMonth() = assertEquals(oct, followCurrentMonth(oct, oct, oct))
}
