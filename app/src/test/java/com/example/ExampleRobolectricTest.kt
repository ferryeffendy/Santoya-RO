package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Santoya RO", appName)
    assertEquals("Kurir", context.getString(R.string.dashboard_chip_kurir))
    assertEquals("Mesin RO", context.getString(R.string.dashboard_chip_perawatan))
    assertEquals("Produk", context.getString(R.string.dashboard_chip_produk))
    assertEquals("Terhubung", context.getString(R.string.sync_status_live))
    assertEquals("Mode Offline", context.getString(R.string.sync_status_offline))
    assertEquals(
      "2 Pelanggan • 1 terlambat",
      context.getString(R.string.dashboard_crm_alert_single_line, "2 Pelanggan", "1 terlambat")
    )
    assertEquals(
      "Perawatan Mesin RO (1) • 1 hari ini",
      context.getString(R.string.dashboard_perawatan_single_line, "Perawatan Mesin RO (1)", "1 hari ini")
    )
    assertEquals("Menu Utama", context.getString(R.string.dashboard_menu_title))
    assertEquals("12 Pelanggan", context.getString(R.string.dashboard_menu_pelanggan_badge_count, "12"))
    assertEquals("Pelanggan, 12 pelanggan", context.getString(R.string.dashboard_menu_pelanggan_cd, "12"))
  }

  @Test
  fun `security hardening validates syncId courier email csv injection and appcheck token`() {
    // 1. Validasi sanitasi syncId sesuai firestore.rules
    assertEquals("pelanggan_01-abc", com.example.util.ExportHelper.sanitizeSyncId("  pelanggan_01-abc  "))
    assertEquals(null, com.example.util.ExportHelper.sanitizeSyncId("../etc/passwd"))
    assertEquals(null, com.example.util.ExportHelper.sanitizeSyncId("id with spaces"))
    assertEquals(null, com.example.util.ExportHelper.sanitizeSyncId("a".repeat(129)))

    // 2. Pencegahan injeksi email pemilik depot aktif sebagai kurir saat impor JSON
    assertEquals("", com.example.util.ExportHelper.sanitizeImportedCourierEmail("pemilik@depot.id", ownerEmail = "pemilik@depot.id"))
    assertEquals("", com.example.util.ExportHelper.sanitizeImportedCourierEmail("bukan-email-valid"))
    assertEquals("kurir01@depot.id", com.example.util.ExportHelper.sanitizeImportedCourierEmail("  Kurir01@depot.id ", ownerEmail = "pemilik@depot.id"))

    // 3. Proteksi CSV Formula / Spreadsheet Injection
    assertEquals("'=SUM(A1:A10)", com.example.util.LaporanFinanceCalculator.escapeCsvField("=SUM(A1:A10)"))
    assertEquals("\"'=CMD|'/C calc'!A0,1\"", com.example.util.LaporanFinanceCalculator.escapeCsvField("=CMD|'/C calc'!A0,1"))
    assertEquals("'+62812345", com.example.util.LaporanFinanceCalculator.escapeCsvField("+62812345"))
    assertEquals("'@import", com.example.util.LaporanFinanceCalculator.escapeCsvField("@import"))
    assertEquals("-", com.example.util.LaporanFinanceCalculator.escapeCsvField("-"))
    assertEquals("-15000", com.example.util.LaporanFinanceCalculator.escapeCsvField("-15000"))

    // 4. Validasi format UUID ketat pada token debug App Check
    assertEquals(true, com.example.util.isValidAppCheckDebugToken("550e8400-e29b-41d4-a716-446655440000"))
    assertEquals(false, com.example.util.isValidAppCheckDebugToken("token-palsu-bukan-uuid"))
    assertEquals(false, com.example.util.isValidAppCheckDebugToken(""))
  }
}
