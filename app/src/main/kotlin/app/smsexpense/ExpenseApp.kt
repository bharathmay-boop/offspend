package app.smsexpense

import android.app.Application
import android.util.Log
import app.smsexpense.capture.InboxImporter
import app.smsexpense.capture.InboxMessage
import app.smsexpense.capture.OnDeviceModel
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.theme.ThemeChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class ExpenseApp : Application() {
    // ponytail: one database and ledger on the Application, add DI if the graph grows
    val db: AppDatabase by lazy { AppDatabase.create(this) }
    val ledger: Ledger by lazy { Ledger(db, model = OnDeviceModel::read) }
    // A failure in background capture must never crash the app. Only the exception type is logged, never message text.
    private val contained = CoroutineExceptionHandler { _, e -> Log.w("ExpenseApp", "background work failed: ${e.javaClass.simpleName}") }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + contained)

    private val uiPrefs by lazy { getSharedPreferences("ui", MODE_PRIVATE) }

    /** Look › Theme, remembered on the phone (#84). Every open screen follows a change at once. */
    val theme by lazy { MutableStateFlow(ThemeChoice.entries.firstOrNull { it.name == uiPrefs.getString(KEY_THEME, null) } ?: ThemeChoice.SYSTEM) }

    fun setTheme(choice: ThemeChoice) {
        uiPrefs.edit().putString(KEY_THEME, choice.name).apply()
        theme.value = choice
    }

    /** The entry just deleted from a sheet, so the home screen can offer Undo (#52). */
    val justDeleted = MutableStateFlow<Long?>(null)

    sealed interface History {
        data object NotStarted : History
        data object Reading : History
        data class Done(val found: Int) : History
        data object Failed : History
    }

    private val _history = MutableStateFlow<History>(History.NotStarted)
    val history: StateFlow<History> = _history

    private val prefs by lazy { getSharedPreferences("capture", MODE_PRIVATE) }

    /** On first setup, files the last 90 days of bank messages so the first screen already has history. */
    fun importHistoryOnce(readInbox: (Long) -> List<InboxMessage> = InboxImporter.systemInbox(this)) {
        // Claimed before returning: the permission callback and the home screen can both call this at once.
        if (claimHistory()) scope.launch { readHistory(readInbox) }
    }

    internal suspend fun importHistory(readInbox: (Long) -> List<InboxMessage>) {
        if (claimHistory()) readHistory(readInbox)
    }

    private fun claimHistory(): Boolean {
        if (prefs.getBoolean(KEY_HISTORY_DONE, false)) return false
        while (true) {
            val now = _history.value
            if (now == History.Reading) return false
            if (_history.compareAndSet(now, History.Reading)) return true
        }
    }

    private suspend fun readHistory(readInbox: (Long) -> List<InboxMessage>) {
        try {
            val since = System.currentTimeMillis() - InboxImporter.HISTORY_DAYS * 24 * 60 * 60 * 1000
            val found = InboxImporter(readInbox) { s, from, body, at -> onMessage(s, from, body, at, notify = false, fromInbox = true) }.importSince(since)
            prefs.edit().putBoolean(KEY_HISTORY_DONE, true).putLong(KEY_LAST_SEEN, System.currentTimeMillis()).apply()
            _history.value = History.Done(found)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not marked done, so the next launch tries again.
            _history.value = History.Failed
        }
    }

    /** Files each message on its own, so one that fails does not stop the rest. */
    suspend fun onMessages(source: Source, messages: List<InboxMessage>) =
        eachContained(messages) { onMessage(source, it.sender, it.body, it.receivedAt) }

    /**
     * Some phones stop background apps, so an SMS can arrive without the app seeing it.
     * Each time the app opens, file anything that arrived since the last message it saw.
     * @return how many missed payments were filed.
     */
    suspend fun catchUp(readInbox: (Long) -> List<InboxMessage> = InboxImporter.systemInbox(this)): Int {
        if (!prefs.getBoolean(KEY_HISTORY_DONE, false)) return 0
        // Every app start launches one; a second while the first runs would read and file the same messages again (#218).
        if (!catchingUp.tryLock()) return 0
        try {
            return readMissed(readInbox)
        } finally {
            catchingUp.unlock()
        }
    }

    private val catchingUp = Mutex()

    private suspend fun readMissed(readInbox: (Long) -> List<InboxMessage>): Int {
        // Overlap by a minute; the fingerprint stops anything already saved from being saved twice.
        val since = prefs.getLong(KEY_LAST_SEEN, 0L) - 60_000
        val messages = readInbox(since)
        val found = InboxImporter({ messages }) { s, from, body, at -> onMessage(s, from, body, at, notify = false, fromInbox = true) }.importSince(since)
        messages.maxOfOrNull { it.receivedAt }?.let { markSeen(it) }
        return found
    }

    private fun markSeen(receivedAt: Long) {
        if (receivedAt > prefs.getLong(KEY_LAST_SEEN, 0L)) prefs.edit().putLong(KEY_LAST_SEEN, receivedAt).apply()
    }

    /** For tests: start as if setup already finished at this time. */
    internal fun markHistoryDone(at: Long) = prefs.edit().putBoolean(KEY_HISTORY_DONE, true).putLong(KEY_LAST_SEEN, at).apply()

    override fun onCreate() {
        super.onCreate()
        scope.launch { ledger.seedDefaultCategories() }
    }

    /** Every captured message goes through here. Returns the saved transaction's id, or null. */
    suspend fun onMessage(source: Source, sender: String, body: String, receivedAt: Long, notify: Boolean = true, fromInbox: Boolean = false): Long? {
        val id = ledger.record(source, sender, body, receivedAt, fromInbox) ?: return null
        if (notify) {
            val tx = db.transactions().byId(id) ?: return id
            if (tx.deleted) {
                // Filed hidden as the twin of a hand-added entry: ask instead of showing a new payment.
                ledger.handAddedTwinOf(tx)?.let { TransactionNotifier.showSamePayment(this, tx, it) }
                return id
            }
            showPayment(tx)
        }
        return id
    }

    /** Shows a payment's notification with its category, and whether this payee was filed under it before (#167). */
    suspend fun showPayment(tx: TransactionEntity) {
        val category = tx.categoryId?.let { cid -> db.categories().all().firstOrNull { it.id == cid } }
        TransactionNotifier.show(this, tx, category, ledger.filedBefore(tx))
    }

    private companion object {
        const val KEY_HISTORY_DONE = "history_done"
        const val KEY_LAST_SEEN = "last_seen_sms"
        const val KEY_THEME = "theme"
    }
}

/** Runs [block] for every item; one that throws is logged by type and skipped, and the rest still run. */
internal suspend fun <T> eachContained(items: List<T>, block: suspend (T) -> Unit) {
    for (item in items) {
        try {
            block(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ExpenseApp", "could not file a message: ${e.javaClass.simpleName}")
        }
    }
}
