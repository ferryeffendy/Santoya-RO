package com.example

import com.example.util.ContactLookupHelper
import com.example.util.RawContactRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactLookupHelperUnitTest {

    @Test
    fun `normalizeAndDeduplicateContacts normalizes prefixes, filters invalid numbers, deduplicates, and sorts alphabetically`() {
        val raw = listOf(
            RawContactRecord(displayName = "Zaki Galon", phoneNumber = "+62 812-3456-7890", phoneLabel = "Seluler"),
            RawContactRecord(displayName = "zaki galon", phoneNumber = "0812 3456 7890", phoneLabel = null),
            RawContactRecord(displayName = "Bu Ani", phoneNumber = "6285611112222", phoneLabel = "WhatsApp"),
            RawContactRecord(displayName = "Kosong", phoneNumber = "---"),
            RawContactRecord(displayName = "Pendek", phoneNumber = "12345"),
            RawContactRecord(displayName = null, phoneNumber = "081399998888", phoneLabel = "Rumah")
        )

        val result = ContactLookupHelper.normalizeAndDeduplicateContacts(raw)

        assertEquals(3, result.size)
        assertEquals("Bu Ani", result[0].name)
        assertEquals("085611112222", result[0].phone)
        assertEquals("WhatsApp", result[0].label)

        assertEquals("Tanpa Nama", result[1].name)
        assertEquals("081399998888", result[1].phone)

        assertEquals("Zaki Galon", result[2].name)
        assertEquals("081234567890", result[2].phone)
        assertEquals("Seluler", result[2].label)
    }

    @Test
    fun `filterContacts matches by customer name and normalized phone digits`() {
        val entries = ContactLookupHelper.normalizeAndDeduplicateContacts(
            listOf(
                RawContactRecord(displayName = "Bu Ani Warung", phoneNumber = "+62 812-3456-7890"),
                RawContactRecord(displayName = "Pak Budi", phoneNumber = "0856-9988-7766")
            )
        )

        val byName = ContactLookupHelper.filterContacts(entries, "warung")
        assertEquals(listOf("Bu Ani Warung"), byName.map { it.name })

        val byPhone = ContactLookupHelper.filterContacts(entries, "9988")
        assertEquals(listOf("Pak Budi"), byPhone.map { it.name })

        val emptyQuery = ContactLookupHelper.filterContacts(entries, "   ")
        assertEquals(2, emptyQuery.size)

        val noMatch = ContactLookupHelper.filterContacts(entries, "zzzzzz")
        assertTrue(noMatch.isEmpty())
    }
}
