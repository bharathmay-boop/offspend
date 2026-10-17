package app.smsexpense.data

import app.smsexpense.core.Categorizer
import app.smsexpense.core.DefaultCategories
import app.smsexpense.core.Direction
import app.smsexpense.core.SmsParser
import app.smsexpense.core.SmsReading
import app.smsexpense.core.Source
import java.security.MessageDigest

/** The app's operations on stored money, in one place so screens and receivers share them. */
class Ledger(private val db: AppDatabase) {

    suspend fun seedDefaultCategories() {
        if (db.categories().all().isNotEmpty()) return
        DefaultCategories.forEachIndexed { slot, name -> db.categories().insert(CategoryEntity(name = name, slot = slot)) }
    }

    /**
     * Reads one message and, if it is a transaction, saves it with a suggested category.
     * @return the new transaction's id, or null when the message was ignored or already saved.
     */
    suspend fun record(source: Source, sender: String, body: String, receivedAt: Long): Long? {
        val reading = SmsParser.parse(source, sender, body) as? SmsReading.Transaction ?: return null
        val guess = guessCategory(reading.merchant, reading.direction)
        val id = db.transactions().insert(
            TransactionEntity(
                direction = reading.direction,
                amount = reading.money.amount,
                currency = reading.money.currency,
                merchant = reading.merchant,
                accountLast4 = reading.account,
                occurredAt = receivedAt,
                categoryId = guess?.id,
                note = null,
                source = source.name,
                fingerprint = fingerprint(source, sender, body, receivedAt),
            ),
        )
        return id.takeIf { it != -1L }
    }

    private fun fingerprint(source: Source, sender: String, body: String, receivedAt: Long): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$source|$sender|$receivedAt|$body".toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** The category the app would suggest for this merchant, or null. */
    suspend fun guessCategory(merchant: String?, direction: Direction): CategoryEntity? {
        val byId = db.categories().all().associateBy { it.id }
        val learned = db.merchantRules().all().mapNotNull { rule -> byId[rule.categoryId]?.let { rule.merchantKey to it.name } }.toMap()
        val name = Categorizer.guess(merchant, direction, learned) ?: return null
        return db.categories().byName(name)
    }

    /** Creates a category if the name is new, and returns it either way. */
    suspend fun categoryNamed(name: String): CategoryEntity {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "category name must not be empty" }
        db.categories().byName(clean)?.let { return it }
        val id = db.categories().insert(CategoryEntity(name = clean, slot = db.categories().nextSlot()))
        return db.categories().all().first { it.id == id }
    }

    /** Files a transaction under a category, and remembers that choice for the merchant next time. */
    suspend fun setCategory(transactionId: Long, categoryId: Long) {
        val tx = db.transactions().byId(transactionId) ?: return
        db.transactions().setCategory(transactionId, categoryId)
        tx.merchant?.let { db.merchantRules().upsert(MerchantRuleEntity(Categorizer.merchantKey(it), categoryId)) }
    }

    suspend fun setNote(transactionId: Long, note: String?) =
        db.transactions().setNote(transactionId, note?.trim()?.takeIf { it.isNotEmpty() })
}
