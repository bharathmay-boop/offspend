package app.smsexpense.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import app.smsexpense.ExpenseApp
import kotlinx.coroutines.launch

/** Handles "Looks right" and "Add note" straight from the notification, without opening the app. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getLongExtra(TransactionNotifier.EXTRA_TX, -1)
        if (txId < 0) return
        val app = context.applicationContext as ExpenseApp
        val pending = goAsync()
        app.scope.launch {
            try {
                handle(app, intent, txId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        suspend fun handle(app: ExpenseApp, intent: Intent, txId: Long) {
            when (intent.action) {
                TransactionNotifier.ACTION_CONFIRM -> {
                    val categoryId = intent.getLongExtra(TransactionNotifier.EXTRA_CATEGORY, -1)
                    if (categoryId >= 0) app.ledger.setCategory(txId, categoryId)
                    TransactionNotifier.dismiss(app, txId)
                }
                TransactionNotifier.ACTION_NOTE -> {
                    val note = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(TransactionNotifier.KEY_NOTE)
                    app.ledger.setNote(txId, note?.toString())
                    // Re-post so the reply spinner stops and the note is acknowledged.
                    app.db.transactions().byId(txId)?.let { tx ->
                        val category = tx.categoryId?.let { id -> app.db.categories().all().firstOrNull { it.id == id } }
                        TransactionNotifier.show(app, tx, category)
                    }
                }
            }
        }
    }
}
