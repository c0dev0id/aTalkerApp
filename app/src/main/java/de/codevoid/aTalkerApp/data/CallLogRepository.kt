package de.codevoid.aTalkerApp.data

import android.content.Context
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CallLogRepository {

    private const val LIMIT = 100

    suspend fun load(context: Context): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
            ),
            null, null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && entries.size < LIMIT) {
                val name   = cursor.getString(0).takeUnless { it.isNullOrBlank() }
                val number = cursor.getString(1) ?: continue
                val type   = cursor.getInt(2)
                val date   = cursor.getLong(3)
                entries.add(CallLogEntry(name ?: number, number, type, date))
            }
        }
        entries
    }
}
