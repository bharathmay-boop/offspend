package app.smsexpense.core

/** The categories a new install starts with, in the order their colors are assigned. */
val DefaultCategories = listOf(
    "Food", "Groceries", "Transport", "Shopping", "Bills", "Entertainment",
    "Health", "Travel", "EMI & loans", "Cash", "Salary", "Transfers",
)

/**
 * Guesses a category for a merchant. What the person taught it always wins over the built-in keywords.
 */
object Categorizer {
    /** "swiggy.instamart@icici" and "SWIGGY INSTAMART" both become "swiggy instamart". */
    fun merchantKey(merchant: String): String =
        plain(merchant)
            .replace(Regex("\\b(pvt|ltd|private|limited|india|inc|llp)\\b"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    // Keywords match before the suffixes are stripped, or "Air India" would become "air" (#153).
    private fun plain(merchant: String): String =
        merchant.substringBefore('@').lowercase().replace(Regex("[^a-z0-9]+"), " ")

    // ponytail: keyword list, swap for the on-device model (#14) when this stops being enough
    private val keywords: List<Pair<Regex, String>> = listOf(
        "swiggy|zomato|burrito|chai|cafe|coffee|starbucks|restaurant|pizza|domino|kfc|mcdonald|pret|eatsure" to "Food",
        "blinkit|zepto|bigbasket|instamart|dmart|grocer|supermarket|hypermarket|naivas|carrefour|lulu|whole foods|shoprite|imtiaz" to "Groceries",
        "uber|ola|rapido|metro|irctc|fuel|petrol|indian oil|hpcl|bpcl|parking|fastag" to "Transport",
        "amazon|flipkart|myntra|ajio|meesho|decathlon|nykaa|ikea|croma" to "Shopping",
        "airtel|jio|vodafone|vi |electricity|bescom|tata power|water|gas|broadband|recharge|insurance" to "Bills",
        "netflix|spotify|hotstar|prime video|youtube|bookmyshow|pvr|inox|steam" to "Entertainment",
        "pharmacy|apollo|medplus|1mg|pharmeasy|hospital|clinic|lab" to "Health",
        "makemytrip|goibibo|indigo|air india|vistara|emirates|hotel|taj|oyo|airbnb|booking" to "Travel",
        "bajaj finance|emi|loan|nach" to "EMI & loans",
        "^atm$" to "Cash",
        "salary|payroll|technologies|solutions" to "Salary",
    ).map { (pattern, category) -> Regex("(?:^|\\b)(?:$pattern)") to category }

    /**
     * @param learned merchant keys the person has categorised before, mapped to their category.
     * @return the category name, or null when there is no good guess.
     */
    fun guess(merchant: String?, direction: Direction, learned: Map<String, String>): String? {
        if (merchant == null) return null
        val key = merchantKey(merchant)
        learned[key]?.let { return it }
        val hit = keywords.firstOrNull { (regex, _) -> regex.containsMatchIn(plain(merchant)) }?.second ?: return null
        // A keyword like "salary" only makes sense for money coming in, and spending ones only for money going out.
        val creditOnly = hit == "Salary"
        return if (creditOnly == (direction == Direction.CREDIT)) hit else null
    }
}
