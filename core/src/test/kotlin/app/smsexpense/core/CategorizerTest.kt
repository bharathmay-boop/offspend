package app.smsexpense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorizerTest {
    @Test fun merchantKeyIgnoresUpiHandleCaseAndCompanySuffixes() {
        assertEquals("swiggy instamart", Categorizer.merchantKey("swiggy.instamart@icici"))
        assertEquals("swiggy instamart", Categorizer.merchantKey("SWIGGY INSTAMART"))
        assertEquals("acme technologies", Categorizer.merchantKey("ACME TECHNOLOGIES PVT LTD"))
    }

    @Test fun guessesFromKeywords() {
        assertEquals("Food", Categorizer.guess("CALIFORNIA BURRITO", Direction.DEBIT, emptyMap()))
        assertEquals("Groceries", Categorizer.guess("blinkit@icici", Direction.DEBIT, emptyMap()))
        assertEquals("Cash", Categorizer.guess("ATM", Direction.DEBIT, emptyMap()))
        assertEquals("Salary", Categorizer.guess("ACME TECHNOLOGIES PVT LTD", Direction.CREDIT, emptyMap()))
    }

    @Test fun whatThePersonTaughtWinsOverKeywords() {
        val learned = mapOf("swiggy" to "Office lunch")
        assertEquals("Office lunch", Categorizer.guess("SWIGGY", Direction.DEBIT, learned))
        assertEquals("Office lunch", Categorizer.guess("swiggy@icici", Direction.DEBIT, learned))
    }

    @Test fun noGuessWhenUnsure() {
        assertNull(Categorizer.guess(null, Direction.DEBIT, emptyMap()))
        assertNull(Categorizer.guess("RAHUL S", Direction.CREDIT, emptyMap()))
        // A salary keyword on money going out is not salary.
        assertNull(Categorizer.guess("ACME TECHNOLOGIES PVT LTD", Direction.DEBIT, emptyMap()))
    }

    @Test fun guessesForTheTestSetMerchants() {
        val guessed = Fixtures.all.filter { it.expected.is_transaction }.count {
            Categorizer.guess(it.expected.merchant, Direction.valueOf(it.expected.direction!!), emptyMap()) != null
        }
        // Most named merchants should get a first guess; people and unknown shops should not.
        assert(guessed >= 20) { "only $guessed guessed" }
    }
}
