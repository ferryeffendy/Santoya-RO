package com.example

import com.example.util.FormatUtils
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsUnitTest {

    @Test
    fun formatRupiah_formatsZeroPositiveNegativeAndMillionsDeterministically() {
        assertEquals("Rp 0", FormatUtils.formatRupiah(0.0))
        assertEquals("Rp 6.000", FormatUtils.formatRupiah(6000.0))
        assertEquals("Rp 1.250.000", FormatUtils.formatRupiah(1_250_000.0))
        assertEquals("-Rp 45.000", FormatUtils.formatRupiah(-45_000.0))
    }

    @Test
    fun formatNumber_usesIndonesianThousandsSeparator() {
        assertEquals("0", FormatUtils.formatNumber(0))
        assertEquals("1.250", FormatUtils.formatNumber(1250))
        assertEquals("10.000.000", FormatUtils.formatNumber(10_000_000L))
    }

    @Test
    fun formatPercent_usesCommaDecimalSeparator() {
        assertEquals("15,5%", FormatUtils.formatPercent(15.5, decimals = 1))
        assertEquals("100%", FormatUtils.formatPercent(100.0, decimals = 0))
        assertEquals("12,35%", FormatUtils.formatPercent(12.35, decimals = 2))
    }

    @Test
    fun formatMetodeBayarLabel_mapsCashAndEmptyToTunaiAndPreservesOthers() {
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel("Cash"))
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel("cash"))
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel("Tunai"))
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel("-"))
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel(""))
        assertEquals("Tunai", FormatUtils.formatMetodeBayarLabel(null))
        assertEquals("Transfer", FormatUtils.formatMetodeBayarLabel("Transfer"))
        assertEquals("Bayar Sebagian", FormatUtils.formatMetodeBayarLabel("bayar sebagian"))
    }

    @Test
    fun formatDateTableAndTime_produceExpectedPatterns() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 13)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ts = cal.timeInMillis

        assertEquals("13/11/2026", FormatUtils.formatDateTable(ts))
        assertEquals("13/11/2026 14:30", FormatUtils.formatDateTimeTable(ts))
        assertEquals("14:30", FormatUtils.formatTimeOnly(ts))
        assertTrue(FormatUtils.formatDateUi(ts).contains("13"))
        assertTrue(FormatUtils.formatDateUi(ts).contains("2026"))
    }

    @Test
    fun formatPhoneForWhatsApp_normalizesIndonesianNumbers() {
        assertEquals("628123456789", FormatUtils.formatPhoneForWhatsApp("0812-3456-789"))
        assertEquals("628123456789", FormatUtils.formatPhoneForWhatsApp("8123456789"))
        assertEquals("628123456789", FormatUtils.formatPhoneForWhatsApp("+62 812 3456 789"))
        assertEquals("", FormatUtils.formatPhoneForWhatsApp("123"))
    }

    @Test
    fun buildEmployeeWhatsAppMessage_generatesKasbonOrOrderTemplate() {
        val kasbonMsg = FormatUtils.buildEmployeeWhatsAppMessage(
            customerName = "Ibu Sari",
            sisaKasbon = 36_000.0,
            unpaidCount = 2,
            isKasbonReminder = true
        )
        assertTrue(kasbonMsg.contains("Ibu Sari"))
        assertTrue(kasbonMsg.contains("Rp 36.000"))
        assertTrue(kasbonMsg.contains("(2 nota)"))

        val reorderMsg = FormatUtils.buildEmployeeWhatsAppMessage(
            customerName = "",
            sisaKasbon = 0.0,
            unpaidCount = 0,
            isKasbonReminder = false
        )
        assertTrue(reorderMsg.contains("Bapak/Ibu"))
        assertTrue(reorderMsg.contains("Galonnya mau diantar hari ini?"))
    }
}
