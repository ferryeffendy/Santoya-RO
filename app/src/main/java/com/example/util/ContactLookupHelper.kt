package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

/**
 * Representasi mentah satu baris nomor kontak yang dibaca dari [ContactsContract].
 */
data class RawContactRecord(
    val displayName: String?,
    val phoneNumber: String?,
    val phoneLabel: String? = null
)

/**
 * Entri kontak perangkat yang sudah dinormalisasi, dideduplikasi, dan siap ditampilkan di pemilih kontak.
 */
data class DeviceContactEntry(
    val key: String,
    val name: String,
    val phone: String,
    val label: String? = null
)

/**
 * Utilitas pembacaan, normalisasi, deduplikasi, dan pencarian kontak perangkat
 * agar seluruh kontak (lokal, Google, WhatsApp, dan SIM) terbaca konsisten.
 */
object ContactLookupHelper {

    private const val TAG = "ContactLookupHelper"

    /**
     * Menelusuri [ContextWrapper] untuk menemukan [Activity] induk dari sebuah [Context].
     */
    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    /**
     * Menormalisasi nomor telepon, membuang entri tanpa nomor valid, mendeduplikasi kombinasi
     * nama dan nomor ternormalisasi, serta mengurutkan secara alfabetis (A–Z).
     */
    fun normalizeAndDeduplicateContacts(
        rawContacts: List<RawContactRecord>,
        fallbackUnknownName: String = "Tanpa Nama"
    ): List<DeviceContactEntry> {
        val seenKeys = LinkedHashMap<String, DeviceContactEntry>()
        for (raw in rawContacts) {
            val normalizedPhone = PelangganAuditHelper.formatLocalPhoneNumber(raw.phoneNumber)
            if (normalizedPhone.isBlank()) continue

            val cleanName = raw.displayName?.trim().orEmpty().ifBlank { fallbackUnknownName }
            val cleanLabel = raw.phoneLabel?.trim()?.takeIf { it.isNotBlank() }
            val dedupeKey = "${cleanName.lowercase()}|$normalizedPhone"

            val existing = seenKeys[dedupeKey]
            if (existing == null || (existing.label == null && cleanLabel != null)) {
                seenKeys[dedupeKey] = DeviceContactEntry(
                    key = dedupeKey,
                    name = cleanName,
                    phone = normalizedPhone,
                    label = cleanLabel
                )
            }
        }

        return seenKeys.values.sortedWith(
            compareBy<DeviceContactEntry> { it.name.lowercase() }
                .thenBy { it.phone }
        )
    }

    /**
     * Menyaring dan mengurutkan [contacts] berdasarkan relevansi kata kunci nama atau nomor HP.
     */
    fun filterContacts(
        contacts: List<DeviceContactEntry>,
        query: String
    ): List<DeviceContactEntry> {
        if (query.isBlank()) return contacts
        return SearchMatcher.rankAndFilter(contacts, query) { entry ->
            listOf(
                SearchField(entry.name, weight = 24, isPrimaryName = true),
                SearchField(entry.phone, weight = 16, isPhone = true),
                SearchField(entry.label.orEmpty(), weight = 4)
            )
        }
    }

    /**
     * Membaca seluruh nomor telepon kontak dari perangkat melalui [ContactsContract.CommonDataKinds.Phone].
     * Pemanggil wajib memastikan izin `READ_CONTACTS` sudah diberikan dan memanggil dari thread IO.
     */
    fun loadDevicePhoneContacts(context: Context): List<DeviceContactEntry> {
        val rawRecords = ArrayList<RawContactRecord>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL",
                null,
                sortOrder
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) else null
                    val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    val customLabel = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                    val resolvedLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        type,
                        customLabel
                    )?.toString()
                    rawRecords.add(
                        RawContactRecord(
                            displayName = name,
                            phoneNumber = number,
                            phoneLabel = resolvedLabel
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Izin baca kontak belum aktif saat memuat semua kontak: ${e.message}")
            throw e
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "URI kontak tidak valid: ${e.message}")
        }

        return normalizeAndDeduplicateContacts(rawRecords)
    }

    /**
     * Mengekstrak nomor HP ternormalisasi dan nama dari [uri] hasil pemilih kontak sistem.
     * Pemilih kontak sistem Android memberikan izin baca sementara (URI permission grant) ke [uri]
     * yang dipilih sehingga nomor telepon dan nama dapat dibaca tanpa memerlukan izin `READ_CONTACTS`.
     */
    fun resolvePickedContactUri(
        context: Context,
        uri: Uri,
        hasReadContactsPermission: Boolean = false
    ): Pair<String, String?>? {
        var foundNumber: String? = null
        var foundName: String? = null

        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        try {
            context.contentResolver.query(uri, phoneProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (numberIdx >= 0) foundNumber = cursor.getString(numberIdx)
                    if (nameIdx >= 0) foundName = cursor.getString(nameIdx)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException saat query URI Phone langsung: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Kolom Phone tidak tersedia di URI pemilih sistem: ${e.message}")
        }

        val directNormalized = PelangganAuditHelper.formatLocalPhoneNumber(foundNumber)
        if (directNormalized.isNotBlank()) {
            return directNormalized to foundName?.trim()?.takeIf { it.isNotBlank() }
        }

        if (!hasReadContactsPermission) return null

        var contactId: String? = null
        try {
            val contactProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            )
            context.contentResolver.query(uri, contactProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    if (idIdx >= 0) contactId = cursor.getString(idIdx)
                    if (foundName.isNullOrBlank() && nameIdx >= 0) {
                        foundName = cursor.getString(nameIdx)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException saat resolve ID kontak: ${e.message}")
            return null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "IllegalArgumentException saat resolve ID kontak: ${e.message}")
            return null
        }

        val resolvedId = contactId ?: return null
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(resolvedId),
                null
            )?.use { phoneCursor ->
                while (phoneCursor.moveToNext()) {
                    val numberIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val candidatePhone = if (numberIdx >= 0) phoneCursor.getString(numberIdx) else null
                    val candidateNorm = PelangganAuditHelper.formatLocalPhoneNumber(candidatePhone)
                    if (candidateNorm.isNotBlank()) {
                        val candidateName = if (nameIdx >= 0) phoneCursor.getString(nameIdx) else foundName
                        return candidateNorm to candidateName?.trim()?.takeIf { it.isNotBlank() }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException saat lookup nomor via CONTACT_ID: ${e.message}")
        }
        return null
    }
}
