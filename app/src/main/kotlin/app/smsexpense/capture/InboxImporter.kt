package app.smsexpense.capture

import android.content.Context
import android.provider.Telephony
import app.smsexpense.core.Source

/** One message from the phone's SMS inbox. */
data class InboxMessage(val sender: String, val body: String, val receivedAt: Long)

/**
 * Reads past messages from the SMS inbox and files the transactions among them, without notifications.
 * Everything happens on the phone.
 */
class InboxImporter(
    private val readInbox: (sinceMillis: Long) -> List<InboxMessage>,
    private val onMessage: suspend (Source, String, String, Long) -> Long?,
) {
    /** @return how many new transactions were saved. */
    suspend fun importSince(sinceMillis: Long): Int =
        readInbox(sinceMillis).count { onMessage(Source.SMS, it.sender, it.body, it.receivedAt) != null }

    companion object {
        const val HISTORY_DAYS = 90L

        /** Reads the real inbox. Needs READ_SMS. */
        fun systemInbox(context: Context): (Long) -> List<InboxMessage> = { since ->
            val columns = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI, columns, "${Telephony.Sms.DATE} >= ?", arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) add(InboxMessage(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getLong(2)))
                }
            } ?: emptyList()
        }
    }
}
