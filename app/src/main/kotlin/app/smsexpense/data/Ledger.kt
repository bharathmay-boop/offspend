package app.smsexpense.data

import app.smsexpense.core.Categorizer
import app.smsexpense.core.DefaultCategories
import app.smsexpense.core.Direction
import app.smsexpense.core.ModelFallback
import app.smsexpense.core.SmsParser
import app.smsexpense.core.SmsReading
import app.smsexpense.core.Source
import app.smsexpense.ui.AddResult
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** The app's operations on stored money, in one place so screens and receivers share them. */
class Ledger(
    private val db: AppDatabase,
    /** Reads a prompt with an on-device model, or returns null when there is none. Never a network call. */
    private val model: (suspend (String) -> String?)? = null,
) {

    suspend fun seedDefaultCategories() {
        if (db.categories().all().isNotEmpty()) return
        DefaultCategories.forEachIndexed { slot, name -> db.categories().insert(CategoryEntity(name = name, slot = slot)) }
    }

    /**
     * Reads one message and, if it is a transaction, saves it with a suggested category.
     * @return the new transaction's id, or null when the message was ignored or already saved.
     */
    suspend fun record(source: Source, sender: String, body: String, receivedAt: Long): Long? {
        val reading = when (val r = SmsParser.parse(source, sender, body)) {
            is SmsReading.Transaction -> r
            // The rules could not read the amount; the on-device model gets one try, and its answer is checked.
            SmsReading.NotUnderstood -> model?.let { ModelFallback.accept(body, it(ModelFallback.prompt(body))) } ?: return null
            is SmsReading.Ignored -> return null
        }
        sameTransactionFromOtherSource(reading, source, receivedAt)?.let { twin ->
            // Keep the first one; if it had no payee and this one names it, fill it in.
            reading.merchant?.let { db.transactions().fillMerchant(twin.id, it) }
            return null
        }
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

    /**
     * The bank SMS and the payment app's notification often both report one payment.
     * Same direction, amount and currency from the other source within ten minutes counts as the same payment.
     */
    // ponytail: two genuine equal payments minutes apart, one seen by SMS and one by notification, would merge; pair rows explicitly if that shows up
    private suspend fun sameTransactionFromOtherSource(reading: SmsReading.Transaction, source: Source, at: Long): TransactionEntity? =
        db.transactions()
            .fromOtherSourcesNear(reading.direction, reading.money.currency, source.name, at - TWIN_WINDOW_MS, at + TWIN_WINDOW_MS)
            .firstOrNull { it.amount.compareTo(reading.money.amount) == 0 }

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

    /** Creates a category if the name is new (with the icon picked for it, if any), and returns it either way. */
    suspend fun categoryNamed(name: String, icon: String? = null): CategoryEntity {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "category name must not be empty" }
        db.categories().byName(clean)?.let { return it }
        val id = db.categories().insert(CategoryEntity(name = clean, slot = db.categories().nextSlot(), icon = icon))
        return db.categories().all().first { it.id == id }
    }

    /** Files a transaction under a category, and remembers that choice for the merchant next time. */
    suspend fun setCategory(transactionId: Long, categoryId: Long) {
        val tx = db.transactions().byId(transactionId) ?: return
        db.transactions().setCategory(transactionId, categoryId)
        tx.merchant?.let { db.merchantRules().upsert(MerchantRuleEntity(Categorizer.merchantKey(it), categoryId)) }
    }

    /** Saves what the person chose on the category sheet. A typed new category is created and used. */
    suspend fun file(transactionId: Long, categoryId: Long?, newCategoryName: String?, note: String?, newCategoryIcon: String? = null) {
        val chosen = newCategoryName?.takeIf { it.isNotBlank() }?.let { categoryNamed(it, newCategoryIcon).id } ?: categoryId
        chosen?.let { setCategory(transactionId, it) }
        setNote(transactionId, note)
    }

    /**
     * Saves a payment or money received that the person typed in (#51). It is confirmed, since they chose everything,
     * and a payee filed under a category teaches the app that payee, like a correction does.
     */
    suspend fun addByHand(r: AddResult, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long {
        // Today means now; an earlier day gets noon, so it sorts sensibly within that day.
        val at = if (r.date == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()) now else r.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val id = db.transactions().insert(
            TransactionEntity(
                direction = r.direction,
                amount = r.amount,
                currency = r.currency,
                merchant = r.payee,
                accountLast4 = null,
                occurredAt = at,
                categoryId = r.categoryId,
                note = r.note.ifBlank { null },
                source = MANUAL,
                fingerprint = "manual-${UUID.randomUUID()}",
                confirmed = true,
            ),
        )
        if (r.payee != null && r.categoryId != null) {
            db.merchantRules().upsert(MerchantRuleEntity(Categorizer.merchantKey(r.payee), r.categoryId))
        }
        return id
    }

    /**
     * Saves changes from the edit sheet (#52). An entry the person typed in changes in full. One read from a bank SMS or
     * notification keeps the amount, currency and direction the bank sent, so the record never drifts from it.
     */
    suspend fun edit(transactionId: Long, r: AddResult, zone: ZoneId = ZoneId.systemDefault()) {
        val tx = db.transactions().byId(transactionId) ?: return
        val oldDay = Instant.ofEpochMilli(tx.occurredAt).atZone(zone).toLocalDate()
        val at = if (r.date == oldDay) tx.occurredAt else r.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val common = tx.copy(merchant = r.payee, occurredAt = at, note = r.note.ifBlank { null })
        db.transactions().update(
            if (tx.source == MANUAL) common.copy(direction = r.direction, amount = r.amount, currency = r.currency) else common,
        )
        r.categoryId?.let { setCategory(transactionId, it) }
    }

    /** Hides an entry. It stays in the database so a catch-up never files the same message again. */
    suspend fun delete(transactionId: Long) = db.transactions().setDeleted(transactionId, true)

    suspend fun restore(transactionId: Long) = db.transactions().setDeleted(transactionId, false)

    suspend fun setNote(transactionId: Long, note: String?) =
        db.transactions().setNote(transactionId, note?.trim()?.takeIf { it.isNotEmpty() })

    companion object {
        /** The source of an entry the person typed in. */
        const val MANUAL = "MANUAL"
        private const val TWIN_WINDOW_MS = 10 * 60 * 1000L
    }
}
