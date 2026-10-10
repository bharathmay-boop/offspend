package app.smsexpense

import android.app.Application
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.core.Source
import app.smsexpense.data.AppDatabase
import app.smsexpense.data.Ledger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseApp : Application() {
    // ponytail: one database and ledger on the Application, add DI if the graph grows
    val db: AppDatabase by lazy { AppDatabase.create(this) }
    val ledger: Ledger by lazy { Ledger(db) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { ledger.seedDefaultCategories() }
    }

    /** Every captured message goes through here. Returns the saved transaction's id, or null. */
    suspend fun onMessage(source: Source, sender: String, body: String, receivedAt: Long, notify: Boolean = true): Long? {
        val id = ledger.record(source, sender, body, receivedAt) ?: return null
        if (notify) {
            val tx = db.transactions().byId(id) ?: return id
            val category = tx.categoryId?.let { cid -> db.categories().all().firstOrNull { it.id == cid } }
            TransactionNotifier.show(this, tx, category)
        }
        return id
    }
}
