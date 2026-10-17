package app.smsexpense.data

import app.smsexpense.core.Categorizer
import app.smsexpense.core.DefaultCategories
import app.smsexpense.core.Direction

/** The app's operations on stored money, in one place so screens and receivers share them. */
class Ledger(private val db: AppDatabase) {

    suspend fun seedDefaultCategories() {
        if (db.categories().all().isNotEmpty()) return
        DefaultCategories.forEachIndexed { slot, name -> db.categories().insert(CategoryEntity(name = name, slot = slot)) }
    }

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
