package app.smsexpense

import app.smsexpense.data.SightingEntity
import app.smsexpense.ui.Provenance
import app.smsexpense.ui.SourceLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

// "Where this came from" reads like a person would say it, and never shows the message (#171).
class ProvenanceTest {
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val at = 8 * 3_600_000L + 44 * 60_000L // 8:44 AM UTC, 2:14 PM in India

    private fun sighting(source: String, sender: String?, merchant: String?, account: String? = null, seenAt: Long = at) =
        SightingEntity(transactionId = 1, source = source, sender = sender, merchant = merchant, accountLast4 = account, seenAt = seenAt, fingerprint = null)

    // Newer Java puts a narrow no-break space before AM/PM; compare with a plain one.
    private fun lines(paymentSource: String, vararg seen: SightingEntity, appName: (String) -> String? = { null }) =
        Provenance.lines(paymentSource, seen.toList(), appName, ZoneOffset.ofHoursMinutes(5, 30), Locale.US)
            .map { it.copy(text = it.text.replace('\u202F', ' ')) }

    @Test fun aBankTextNamesTheBankTheAccountAndTheTime() {
        assertEquals(
            listOf(SourceLine("HDFC Bank text · a/c ••1234 · 2:14 PM", null)),
            lines("SMS", sighting("SMS", "VM-HDFCBK", "SWIGGY", "1234")),
        )
    }

    @Test fun aPaymentSeenTwiceShowsBothAndWhatEachCalledThePayee() {
        val got = lines("SMS", sighting("SMS", "AD-HDFCBK", "zomatoorder.payu@hdfcbank", "1234"), sighting("NOTIFICATION", gpay, "Zomato"))
        assertEquals(
            listOf(
                SourceLine("HDFC Bank text · a/c ••1234 · 2:14 PM", "zomatoorder.payu@hdfcbank"),
                SourceLine("Google Pay alert · 2:14 PM", "Zomato"),
            ),
            got,
        )
        assertEquals("Seen twice, counted once", Provenance.footer(2))
        assertEquals("Seen 3 times, counted once", Provenance.footer(3))
        assertNull(Provenance.footer(1))
    }

    @Test fun theInstalledAppsOwnNameWins() {
        assertEquals("PhonePe for Business alert · 2:14 PM", lines("NOTIFICATION", sighting("NOTIFICATION", "com.phonepe.app", "Ravi")) { "PhonePe for Business" }.single().text)
    }

    @Test fun aHandAddedPaymentIsMatchedWithTheBankText() {
        val got = lines("MANUAL", sighting("MANUAL", null, "Chai stall", seenAt = at - 4 * 60_000L), sighting("SMS", "JD-ICICIT", null, "567"))
        assertEquals(listOf("Added by you · 2:10 PM", "Matched with ICICI Bank text · a/c ••567 · 2:14 PM"), got.map { it.text })
    }

    @Test fun unknownSendersAndOlderPaymentsStillReadSensibly() {
        assertEquals("XYZBNK", Provenance.bankName("VM-XYZBNK"))
        assertEquals("Bank text · 2:14 PM", lines("SMS", sighting("SMS", null, "SWIGGY")).single().text)
        assertEquals("Payment app alert · 2:14 PM", lines("NOTIFICATION", sighting("NOTIFICATION", "com.example.pay", "X")).single().text)
    }

    // #194: anyone can send from "HDFC" or "HDFCBK"; only the registered header shape is named as a bank.
    @Test fun aSenderOutsideTheBankHeaderShapeIsNotNamedAsABank() {
        assertNull(Provenance.bankName("HDFC"))
        assertNull(Provenance.bankName("HDFCBK"))
        assertNull(Provenance.bankName("XX-HDFCBANK"))
        assertEquals("HDFC Bank", Provenance.bankName("VM-HDFCBK-S"))
        assertEquals("Text from HDFCBK · 2:14 PM", lines("SMS", sighting("SMS", "HDFCBK", "SWIGGY")).single().text)
        assertEquals("Matched with a text from HDFC · 2:14 PM", lines("MANUAL", sighting("SMS", "HDFC", null)).single().text)
    }
}
