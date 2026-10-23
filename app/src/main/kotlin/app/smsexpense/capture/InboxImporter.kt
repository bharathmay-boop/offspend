package app.smsexpense.capture

import android.content.Context
import android.provider.Telephony
import app.smsexpense.core.Source
import app.smsexpense.eachContained

/**
 * One message from the phone's SMS inbox.
 * [receivedAt] is when the phone got it; [sentAt] is the time the message centre stamped on it,
 * which is the time the live receiver sees too.
 */
data class InboxMessage(val sender: String, val body: String, val receivedAt: Long, val sentAt: Long = receivedAt)

/**
 * Reads past messages from the SMS inbox and files the transactions among them, without notifications.
 * Everything happens on the phone.
 */
class InboxImporter(
    private val readInbox: (sinceMillis: Long) -> List<InboxMessage>,
    private val onMessage: suspend (Source, String, String, Long) -> Long?,
) {
    /** @return how many new transactions were saved. */
    suspend fun importSince(sinceMillis: Long): Int {
        var saved = 0
        // One message that fails is skipped, as on the live path, so it cannot hide the rest or pin the catch-up cursor (#236).
        eachContained(readInbox(sinceMillis)) { if (onMessage(Source.SMS, it.sender, it.body, it.sentAt) != null) saved++ }
        return saved
    }

    companion object {
        const val HISTORY_DAYS = 90L

        /** Reads the real inbox. Needs READ_SMS. */
        fun systemInbox(context: Context): (Long) -> List<InboxMessage> = { since ->
            val columns = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT)
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI, columns, "${Telephony.Sms.DATE} >= ?", arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) add(InboxMessage(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getLong(2), c.getLong(3).takeIf { it > 0 } ?: c.getLong(2)))
                }
            } ?: emptyList()
        }
    }
}
