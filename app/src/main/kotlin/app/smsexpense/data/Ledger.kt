package app.smsexpense.data

import app.smsexpense.core.Bounds
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
import kotlinx.coroutines.withTimeoutOrNull

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
     * [fromInbox] is true for messages read back from the SMS inbox (first import and catch-up) rather than caught live.
     * @return the new transaction's id, or null when the message was ignored or already saved.
     */
    suspend fun record(source: Source, sender: String, body: String, receivedAt: Long, fromInbox: Boolean = false): Long? {
        if (fromInbox && caughtLiveAtAnotherTime(sender, body, receivedAt)) return null
        val print = fingerprint(source, sender, body, receivedAt)
        val reading = when (val r = SmsParser.parse(source, sender, body)) {
            is SmsReading.Transaction -> r
            // The rules could not read the amount; the on-device model gets one try, and its answer is checked.
            SmsReading.NotUnderstood -> askModel(body, print) ?: return null
            is SmsReading.Ignored -> return null
        }.let(Bounds::check) ?: return null
        fun seen(txId: Long) = SightingEntity(
            transactionId = txId, source = source.name, sender = sender, merchant = reading.merchant,
            accountLast4 = reading.account, seenAt = receivedAt, fingerprint = print,
        )
        // A payment app can post the same alert again with a new time; that is the same payment, even if deleted (#191).
        if (source == Source.NOTIFICATION && repostOf(reading, sender, receivedAt)) return null
        sameTransactionFromOtherSource(reading, source, receivedAt)?.let { twin ->
            // Keep the first one; it was seen again, and the second source may name the payee better.
            db.sightings().insert(seen(twin.id))
            useClearestName(twin)
            return null
        }
        // Added by hand earlier? File the bank's copy hidden and ask whether it's the same payment (#53).
        val handAdded = handAddedMatch(reading, receivedAt)
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
                fingerprint = print,
                deleted = handAdded != null,
            ),
        )
        if (id == -1L) return null
        // "Yes, same" needs no tap, so the bank's sighting confirms the hand-added payment until the person says otherwise (#171).
        db.sightings().insert(seen(handAdded?.id ?: id))
        if (handAdded != null) {
            // Matched once only, and it gains the account and, if none was typed, the payee the bank named (#179).
            db.transactions().update(
                handAdded.copy(
                    fingerprint = "manual-matched-${handAdded.fingerprint.removePrefix("manual-")}",
                    accountLast4 = handAdded.accountLast4 ?: reading.account,
                    merchant = handAdded.merchant ?: reading.merchant,
                ),
            )
        }
        return id
    }

    /**
     * The model's reading of a message the rules couldn't read (#218). A message already filed isn't sent again, and a
     * model that is slow to answer is given up on well inside the SMS receiver's deadline; the message is then not
     * filed, so the next catch-up gives it another try.
     */
    private suspend fun askModel(body: String, print: String): SmsReading.Transaction? {
        val ask = model ?: return null
        if (db.transactions().fingerprintSeen(print)) return null
        val answer = withTimeoutOrNull(MODEL_TIMEOUT_MS) { ask(ModelFallback.prompt(body)) }
        return ModelFallback.accept(body, answer)
    }

    /**
     * A hand-added entry from the day before up to an hour after the bank's message, with the same direction, amount
     * and currency, not matched yet. Two genuine equal payments are common (two ₹20 teas), so this only proposes;
     * the notification asks.
     */
    private suspend fun handAddedMatch(reading: SmsReading.Transaction, at: Long): TransactionEntity? =
        db.transactions()
            .unmatchedHandAdded(reading.direction, reading.money.currency, at - HAND_ADDED_BEFORE_MS, at + HAND_ADDED_AFTER_MS)
            .firstOrNull { it.amount.compareTo(reading.money.amount) == 0 }

    /** The live hand-added entry a hidden bank entry was matched with, for the notification. A deleted one is no longer a pair (#232). */
    suspend fun handAddedTwinOf(bank: TransactionEntity): TransactionEntity? =
        db.transactions()
            .fromOtherSourcesNear(bank.direction, bank.currency, bank.source, bank.occurredAt - HAND_ADDED_BEFORE_MS, bank.occurredAt + HAND_ADDED_AFTER_MS)
            .firstOrNull { it.source == MANUAL && !it.deleted && it.fingerprint.startsWith("manual-matched-") && it.amount.compareTo(bank.amount) == 0 }

    /**
     * The bank SMS and the payment app's notification often both report one payment.
     * Same direction, amount and currency from the other source within ten minutes counts as the same payment.
     */
    // ponytail: two genuine equal payments minutes apart, one seen by SMS and one by notification, would merge; pair rows explicitly if that shows up
    private suspend fun sameTransactionFromOtherSource(reading: SmsReading.Transaction, source: Source, at: Long): TransactionEntity? =
        db.transactions()
            .fromOtherSourcesNear(reading.direction, reading.money.currency, source.name, at - TWIN_WINDOW_MS, at + TWIN_WINDOW_MS)
            // Hand-added entries are never merged silently; they get asked about instead (#53).
            // A row the person deleted is not a twin; a hidden bank copy matched to a hand-added entry still is (#187).
            .firstOrNull { it.source != MANUAL && it.amount.compareTo(reading.money.amount) == 0 && (!it.deleted || handAddedTwinOf(it) != null) }

    // ponytail: two genuine equal payments to one payee through one app within ten minutes count once; key on the alert's own id if that shows up
    private suspend fun repostOf(reading: SmsReading.Transaction, app: String, at: Long): Boolean =
        db.transactions()
            .seenFromAppNear(app, reading.merchant, reading.direction, reading.money.currency, at - TWIN_WINDOW_MS, at + TWIN_WINDOW_MS)
            .any { it.amount.compareTo(reading.money.amount) == 0 }

    /**
     * The inbox gives a message's sent time, which is what the live receiver saw, unless the SMS app stored something
     * else there (#219). So a message from the inbox also counts as filed when this sender's text with the very same
     * body was filed live within a few minutes: its fingerprint, worked out at that sighting's own time, matches.
     * Only for inbox reads: two real payments can have identical texts minutes apart, and live capture sees both.
     */
    // ponytail: identical texts from one sender within 5 minutes, one missed live, would file once; store a body-only hash if that shows up
    private suspend fun caughtLiveAtAnotherTime(sender: String, body: String, at: Long): Boolean =
        db.sightings().fromSenderNear(Source.SMS.name, sender, at - INBOX_TIME_SLACK_MS, at + INBOX_TIME_SLACK_MS)
            .any { it.seenAt != at && it.fingerprint == fingerprint(Source.SMS, sender, body, it.seenAt) }

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

    /**
     * Files a transaction under a category. A payee seen for the first time is remembered under it. A payee the app
     * already knows keeps what it was taught, so a one-off doesn't change every payment after it, unless [alwaysUse] (#168).
     */
    suspend fun setCategory(transactionId: Long, categoryId: Long, alwaysUse: Boolean = false, shownMerchant: String? = null) {
        val tx = db.transactions().byId(transactionId) ?: return
        db.transactions().setCategory(transactionId, categoryId)
        val key = tx.merchant?.let(Categorizer::merchantKey) ?: return
        // A button on a notification approved a category for the payee it showed. If another source renamed the payee since,
        // the person never saw this name, so nothing is learned for it (#233).
        if (shownMerchant != null && Categorizer.merchantKey(shownMerchant) != key) return
        if (alwaysUse || db.merchantRules().byKey(key) == null) db.merchantRules().upsert(MerchantRuleEntity(key, categoryId))
    }

    /**
     * The payment app's name for the payee beats the bank's ("Zomato", not "zomatoorder.payu@hdfcbank"), unless it gave
     * none (#170). A name the person typed always wins: it matches no source's wording. When the name changes and the
     * person hasn't picked a category, the guess is redone for the new name.
     */
    private suspend fun useClearestName(tx: TransactionEntity) {
        val seen = db.sightings().forTransaction(tx.id)
        val typedByPerson = tx.merchant != null && seen.none { it.merchant == tx.merchant }
        if (typedByPerson) return
        val clearest = seen.filter { it.source == Source.NOTIFICATION.name }.firstNotNullOfOrNull { it.merchant }
            ?: seen.firstNotNullOfOrNull { it.merchant }
        if (clearest == null || clearest == tx.merchant) return
        val guess = if (tx.confirmed) null else guessCategory(clearest, tx.direction)
        db.transactions().update(tx.copy(merchant = clearest, categoryId = guess?.id ?: tx.categoryId))
    }

    /** True when this payment's payee has been filed under its category before, so the notification can just say "Okay". */
    suspend fun filedBefore(tx: TransactionEntity): Boolean {
        val merchant = tx.merchant ?: return false
        return tx.categoryId != null && db.merchantRules().byKey(Categorizer.merchantKey(merchant))?.categoryId == tx.categoryId
    }

    /** True when the app has been taught a category for this payee. */
    suspend fun knowsPayee(merchant: String?): Boolean =
        merchant != null && db.merchantRules().byKey(Categorizer.merchantKey(merchant)) != null

    /** Saves what the person chose on the category sheet. A typed new category is created and used. */
    suspend fun file(
        transactionId: Long, categoryId: Long?, newCategoryName: String?, note: String?, newCategoryIcon: String? = null, alwaysUse: Boolean = false,
    ) {
        val chosen = newCategoryName?.takeIf { it.isNotBlank() }?.let { categoryNamed(it, newCategoryIcon).id } ?: categoryId
        chosen?.let { setCategory(transactionId, it, alwaysUse) }
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
        db.sightings().insert(SightingEntity(transactionId = id, source = MANUAL, sender = null, merchant = r.payee, accountLast4 = null, seenAt = now, fingerprint = null))
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

    /** "No, add it" on the same-payment question: the bank's copy becomes its own payment and takes its sighting back. */
    suspend fun keepSeparate(bankCopyId: Long) {
        val bank = db.transactions().byId(bankCopyId) ?: return
        // Not the same payment: whatever the match took from the bank's text goes back, and the hand-added entry
        // can match the real bank text later (#179, #193).
        handAddedTwinOf(bank)?.let { manual ->
            val typed = db.sightings().forTransaction(manual.id).firstOrNull { it.source == MANUAL }
            db.transactions().update(
                manual.copy(
                    merchant = if (typed?.merchant == null && manual.merchant == bank.merchant) null else manual.merchant,
                    accountLast4 = if (typed?.accountLast4 == null && manual.accountLast4 == bank.accountLast4) null else manual.accountLast4,
                    fingerprint = "manual-${manual.fingerprint.removePrefix("manual-matched-")}",
                ),
            )
        }
        restore(bankCopyId)
        db.sightings().moveTo(bank.fingerprint, bankCopyId)
    }

    suspend fun setNote(transactionId: Long, note: String?) =
        // Capped here too: a reply typed on the notification never passes through a sheet's limit (#189).
        db.transactions().setNote(transactionId, note?.trim()?.take(NOTE_MAX)?.takeIf { it.isNotEmpty() })

    companion object {
        /** The source of an entry the person typed in. */
        const val MANUAL = "MANUAL"
        /** The longest note kept, in characters. */
        const val NOTE_MAX = 200
        private const val TWIN_WINDOW_MS = 10 * 60 * 1000L

        /** How far apart the inbox's time and the live receiver's time can be for one message. */
        private const val INBOX_TIME_SLACK_MS = 5 * 60 * 1000L

        /** The SMS receiver has about 10 seconds; the model gets half. */
        const val MODEL_TIMEOUT_MS = 5_000L
        private const val HAND_ADDED_BEFORE_MS = 24 * 60 * 60 * 1000L
        private const val HAND_ADDED_AFTER_MS = 60 * 60 * 1000L
    }
}
