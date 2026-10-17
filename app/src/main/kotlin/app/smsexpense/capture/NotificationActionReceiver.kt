package app.smsexpense.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import app.smsexpense.ExpenseApp
import kotlinx.coroutines.launch

/** Handles the notification's buttons ("Yes, Food" or "Okay", Add note, and Same payment? Yes or No) without opening the app. */
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
                TransactionNotifier.ACTION_SAME -> TransactionNotifier.dismiss(app, txId)
                TransactionNotifier.ACTION_SEPARATE -> {
                    // Not the same payment: show the bank's copy and ask for its category like any new payment.
                    app.ledger.keepSeparate(txId)
                    app.db.transactions().byId(txId)?.let { app.showPayment(it) }
                }
                TransactionNotifier.ACTION_NOTE -> {
                    val note = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(TransactionNotifier.KEY_NOTE)
                    // A blank reply is not a request to delete a note; clearing one is done on the category sheet.
                    if (!note.isNullOrBlank()) app.ledger.setNote(txId, note.toString())
                    // Re-post so the reply spinner stops; the new text shows the saved note.
                    app.db.transactions().byId(txId)?.let { app.showPayment(it) }
                }
            }
        }
    }
}
