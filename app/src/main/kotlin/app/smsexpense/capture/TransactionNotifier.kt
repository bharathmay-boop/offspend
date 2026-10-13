package app.smsexpense.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import app.smsexpense.CategorySheetActivity
import app.smsexpense.R
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.components.displayMerchant
import app.smsexpense.ui.components.formatMoney

/** The one-tap notification shown for each new transaction. */
object TransactionNotifier {
    const val CHANNEL = "transactions"
    const val ACTION_CONFIRM = "app.smsexpense.CONFIRM"
    const val ACTION_NOTE = "app.smsexpense.NOTE"
    const val EXTRA_TX = "transaction_id"
    const val EXTRA_CATEGORY = "category_id"
    const val KEY_NOTE = "note"
    const val GROUP = "payments"
    private const val SUMMARY_ID = Int.MAX_VALUE

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "New payments", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "One notification per payment, so you can file it with a tap"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Opens the category sheet for this transaction. */
    fun openIntent(context: Context, txId: Long): PendingIntent =
        PendingIntent.getActivity(
            context, txId.toInt(),
            CategorySheetActivity.intent(context, txId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun title(tx: TransactionEntity): String {
        val sign = if (tx.direction == Direction.DEBIT) "−" else "+"
        val who = tx.merchant?.let(::displayMerchant) ?: if (tx.direction == Direction.DEBIT) "Payment" else "Money received"
        return "$sign${formatMoney(tx.amount, tx.currency)} · $who"
    }

    fun text(category: CategoryEntity?, note: String? = null): String {
        val ask = if (category != null) "Filed under ${category.name}. Looks right?" else "Which category is this?"
        return if (note.isNullOrBlank()) ask else "Note: $note. $ask"
    }

    fun show(context: Context, tx: TransactionEntity, category: CategoryEntity?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title(tx))
            .setContentText(text(category, tx.note))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(context, tx.id))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            // Our own group: otherwise Android 16 groups loose notifications itself and silences them all (#71).
            .setGroup(GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

        if (category != null) {
            builder.addAction(0, "Looks right", broadcast(context, ACTION_CONFIRM, tx.id) { putExtra(EXTRA_CATEGORY, category.id) })
            builder.addAction(0, "Change", openIntent(context, tx.id))
        } else {
            builder.addAction(0, "Pick category", openIntent(context, tx.id))
        }
        val noteInput = RemoteInput.Builder(KEY_NOTE).setLabel("Add a note").build()
        builder.addAction(
            NotificationCompat.Action.Builder(0, "Add note", broadcast(context, ACTION_NOTE, tx.id, mutable = true))
                .addRemoteInput(noteInput).setAllowGeneratedReplies(false).build(),
        )

        NotificationManagerCompat.from(context).notify(tx.id.toInt(), builder.build())
        updateSummary(context)
    }

    fun dismiss(context: Context, txId: Long) {
        NotificationManagerCompat.from(context).cancel(txId.toInt())
        updateSummary(context)
    }

    /**
     * With two or more payments waiting, a silent summary holds the group together, so each new payment still pops up
     * on its own. With fewer, there is nothing to group and the summary goes.
     */
    private fun updateSummary(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val waiting = context.getSystemService(NotificationManager::class.java).activeNotifications
            .count { it.notification.group == GROUP && it.id != SUMMARY_ID }
        if (waiting < 2) {
            manager.cancel(SUMMARY_ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val summary = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$waiting payments to file")
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .build()
        manager.notify(SUMMARY_ID, summary)
    }

    private fun broadcast(
        context: Context, action: String, txId: Long, mutable: Boolean = false, extras: Intent.() -> Unit = {},
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra(EXTRA_TX, txId).apply(extras)
        // A reply action must be mutable so Android can attach the typed text.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        val requestCode = (txId * 2 + if (action == ACTION_NOTE) 1 else 0).toInt()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
