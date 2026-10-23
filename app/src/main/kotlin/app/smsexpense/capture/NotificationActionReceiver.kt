package app.smsexpense.capture

import android.app.KeyguardManager
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
            // From Android 12 the system makes these buttons wait for unlock. On Android 8 to 11 it does not, so a locked phone must
            // not change the ledger from its lock screen (a missing service counts as locked). The notification stays, and the person taps it again once unlocked (#237).
            if (intent.action != TransactionNotifier.ACTION_SAME && app.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false) {
                if (intent.action == TransactionNotifier.ACTION_NOTE) app.db.transactions().byId(txId)?.let { app.showPayment(it) }
                return
            }
            // An old notification for a payment deleted in the app since: its buttons do nothing but clear it (#216).
            // "No, add it" is the exception, since its bank copy is hidden on purpose until then.
            if (intent.action != TransactionNotifier.ACTION_SEPARATE && app.db.transactions().byId(txId)?.deleted != false) {
                TransactionNotifier.dismiss(app, txId)
                return
            }
            // "No, add it" brings back a bank copy that is hidden behind a hand-added entry. A bank copy that is live, or deleted by
            // the person, is not that, so an old button does nothing to it (#234).
            if (intent.action == TransactionNotifier.ACTION_SEPARATE) {
                val bank = app.db.transactions().byId(txId)
                if (bank == null || !bank.deleted || app.ledger.handAddedTwinOf(bank) == null) {
                    TransactionNotifier.dismiss(app, txId)
                    return
                }
            }
            when (intent.action) {
                TransactionNotifier.ACTION_CONFIRM -> {
                    val categoryId = intent.getLongExtra(TransactionNotifier.EXTRA_CATEGORY, -1)
                    if (categoryId >= 0) app.ledger.setCategory(txId, categoryId, shownMerchant = intent.getStringExtra(TransactionNotifier.EXTRA_SHOWN))
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
