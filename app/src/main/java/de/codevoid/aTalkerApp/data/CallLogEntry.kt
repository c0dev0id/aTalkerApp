package de.codevoid.aTalkerApp.data

data class CallLogEntry(
    val displayName: String,   // CACHED_NAME, or number if blank
    val number: String,
    val callType: Int,         // CallLog.Calls.INCOMING_TYPE / OUTGOING_TYPE / MISSED_TYPE / …
    val date: Long,            // epoch millis
)
