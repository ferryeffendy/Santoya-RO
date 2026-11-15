package com.example

import com.example.util.redactEmailForLog
import com.example.util.redactUidForLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreErrorHandlerUnitTest {

    @Test
    fun `redactEmailForLog masks local part when redact is true and preserves when false`() {
        assertEquals("fe***@gmail.com", redactEmailForLog("ferryeffendy@gmail.com", redact = true))
        assertEquals("a***@x.id", redactEmailForLog("ab@x.id", redact = true))
        assertEquals("***", redactEmailForLog("invalid-email", redact = true))
        assertEquals(
            "ferryeffendy@gmail.com",
            redactEmailForLog("ferryeffendy@gmail.com", redact = false)
        )
        assertNull(redactEmailForLog(null, redact = true))
    }

    @Test
    fun `redactUidForLog masks middle characters when redact is true and preserves when false`() {
        assertEquals("abc1***89", redactUidForLog("abc123456789", redact = true))
        assertEquals("***", redactUidForLog("short", redact = true))
        assertEquals("abc123456789", redactUidForLog("abc123456789", redact = false))
        assertNull(redactUidForLog(null, redact = true))
    }
}
