package app.smsexpense.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import app.smsexpense.ExpenseApp
import app.smsexpense.core.Source
import kotlinx.coroutines.launch

/**
 * Runs when an SMS arrives, even if the app is closed. The message is read and stored on the phone;
 * nothing is sent anywhere.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        // A long SMS arrives in parts; join them back into one message per sender.
        val messages = parts.groupBy { it.originatingAddress.orEmpty() }
            .map { (sender, pieces) -> Triple(sender, pieces.joinToString("") { it.messageBody.orEmpty() }, pieces.first().timestampMillis) }

        val app = context.applicationContext as ExpenseApp
        val pending = goAsync()
        app.scope.launch {
            try {
                for ((sender, body, receivedAt) in messages) app.onMessage(Source.SMS, sender, body, receivedAt)
            } finally {
                pending.finish()
            }
        }
    }
}
