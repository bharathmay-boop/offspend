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
    const val ACTION_SAME = "app.smsexpense.SAME"
    const val ACTION_SEPARATE = "app.smsexpense.SEPARATE"
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

    // The guess is already filed (#167), so the text says what it is; tapping the notification changes it.
    fun text(category: CategoryEntity?, note: String? = null): String {
        val filed = if (category != null) "${category.name} · Tap to change" else "Needs a category"
        return if (note.isNullOrBlank()) filed else "Note: $note · $filed"
    }

    /** "Okay" for a payee filed under this category before, "Yes, Food" the first time. Cut to fit a narrow phone. */
    fun confirmLabel(categoryName: String, filedBefore: Boolean): String =
        if (filedBefore) "Okay" else "Yes, " + if (categoryName.length > 15) categoryName.take(14).trimEnd() + "…" else categoryName

    /**
     * @param filedBefore this payee has been filed under [category] before, so the confirm button just says "Okay".
     */
    fun show(context: Context, tx: TransactionEntity, category: CategoryEntity?, filedBefore: Boolean = false) {
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
            builder.addAction(0, confirmLabel(category.name, filedBefore), broadcast(context, ACTION_CONFIRM, tx.id) { putExtra(EXTRA_CATEGORY, category.id) })
        }
        val noteInput = RemoteInput.Builder(KEY_NOTE).setLabel("Add a note").build()
        builder.addAction(
            NotificationCompat.Action.Builder(0, "Add note", broadcast(context, ACTION_NOTE, tx.id, mutable = true))
                .addRemoteInput(noteInput).setAllowGeneratedReplies(false).build(),
        )

        NotificationManagerCompat.from(context).notify(tx.id.toInt(), builder.build())
        updateSummary(context, posted = tx.id.toInt())
    }

    /**
     * The bank reported a payment the person already added by hand (#53). "Yes, same" is the default and needs no tap;
     * "No, add it" files the bank's copy as a separate payment.
     */
    fun showSamePayment(context: Context, bank: TransactionEntity, handAdded: TransactionEntity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title(bank))
            .setContentText("You added this by hand. Same payment?")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(context, handAdded.id))
            .setAutoCancel(true)
            .setGroup(GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .addAction(0, "Yes, same", broadcast(context, ACTION_SAME, bank.id))
            .addAction(0, "No, add it", broadcast(context, ACTION_SEPARATE, bank.id))
            .build()
        NotificationManagerCompat.from(context).notify(bank.id.toInt(), n)
        updateSummary(context)
    }

    fun dismiss(context: Context, txId: Long) {
        NotificationManagerCompat.from(context).cancel(txId.toInt())
        updateSummary(context, removed = txId.toInt())
    }

    /**
     * Payments in the tray. Android posts and cancels in the background, so the tray may not show the call just made
     * yet; that one is counted from what we did, not from the tray.
     */
    internal fun waiting(shown: Set<Int>, posted: Int? = null, removed: Int? = null): Int =
        (shown + listOfNotNull(posted) - setOfNotNull(removed)).size

    /**
     * With two or more payments waiting, a silent summary holds the group together, so each new payment still pops up
     * on its own. With fewer, there is nothing to group and the summary goes.
     */
    private fun updateSummary(context: Context, posted: Int? = null, removed: Int? = null) {
        val manager = NotificationManagerCompat.from(context)
        val shown = context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.notification.group == GROUP && it.id != SUMMARY_ID }.map { it.id }.toSet()
        val waiting = waiting(shown, posted, removed)
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
        val requestCode = (txId * 4 + when (action) { ACTION_NOTE -> 1; ACTION_SAME -> 2; ACTION_SEPARATE -> 3; else -> 0 }).toInt()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
