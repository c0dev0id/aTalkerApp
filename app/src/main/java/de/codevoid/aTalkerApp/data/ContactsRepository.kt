package de.codevoid.aTalkerApp.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactsRepository {

    suspend fun load(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        val seen = mutableSetOf<Long>()
        val contacts = mutableListOf<Contact>()
        val resolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                if (!seen.add(id)) continue

                val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources,
                    cursor.getInt(typeCol),
                    ""
                ).toString()

                contacts += Contact(
                    id = id,
                    displayName = cursor.getString(nameCol) ?: "",
                    phoneNumber = cursor.getString(numberCol) ?: "",
                    phoneType = typeLabel,
                )
            }
        }

        contacts
    }

    /** Reverse-lookup a phone number → contact display name, or null if not found. */
    suspend fun lookupName(context: Context, number: String): String? =
        withContext(Dispatchers.IO) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number),
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
}
