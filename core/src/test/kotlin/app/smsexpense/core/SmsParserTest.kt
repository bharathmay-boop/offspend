package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class SmsParserTest {
    private fun read(f: Fixture) =
        SmsParser.parse(if (f.source == "notification") Source.NOTIFICATION else Source.SMS, f.sender, f.text)

    @Test fun readsEveryTransactionInTheTestSet() {
        val failures = Fixtures.all.filter { it.expected.is_transaction }.mapNotNull { f ->
            val got = read(f) as? SmsReading.Transaction ?: return@mapNotNull "${f.id}: got ${read(f)}"
            val e = f.expected
            val problems = buildList {
                if (got.direction.name != e.direction) add("direction ${got.direction}")
                if (got.money.amount.compareTo(BigDecimal(e.amount)) != 0) add("amount ${got.money.amount}")
                if (got.money.currency != e.currency) add("currency ${got.money.currency}")
                if (got.merchant != e.merchant) add("merchant '${got.merchant}'")
                if (got.account != e.account) add("account ${got.account}")
            }
            if (problems.isEmpty()) null else "${f.id}: ${problems.joinToString()}"
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    @Test fun parserReturnsTheFilterReasonForIgnoredMessages() {
        for (f in Fixtures.all.filter { !it.expected.is_transaction }) {
            assertEquals(f.id, SmsReading.Ignored(IgnoreReason.valueOf(f.expected.reason!!)), read(f))
        }
    }

    @Test fun messageWithAVerbButNoAmountIsNotUnderstood() {
        assertEquals(SmsReading.NotUnderstood, SmsParser.parse(Source.SMS, "VM-HDFCBK", "Your account was debited. -HDFC Bank"))
    }

    @Test fun aCurrencyLetterTokenInsideAWordIsNotTheAmount() {
        for (word in listOf("hrs", "Mrs", "yrs")) {
            val got = SmsParser.parse(Source.SMS, "AD-HDFCBK", "Dear Customer, 4 $word 30 min ago Rs 250 was debited from a/c XX1234 at AMAZON")
                as SmsReading.Transaction
            assertEquals(word, 0, BigDecimal(250).compareTo(got.money.amount))
        }
    }
}
