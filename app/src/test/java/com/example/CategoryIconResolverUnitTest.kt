package com.example

import com.example.util.CategoryIconResolver
import com.example.util.PengeluaranIconType
import com.example.util.PerawatanIconType
import com.example.util.PerawatanScheduler
import com.example.util.ProdukIconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryIconResolverUnitTest {

    @Test
    fun `pengeluaran standard categories resolve to dedicated icon types`() {
        assertEquals(
            PengeluaranIconType.LISTRIK_AIR,
            CategoryIconResolver.resolvePengeluaranIconType("Listrik & Air")
        )
        assertEquals(
            PengeluaranIconType.LISTRIK_AIR,
            CategoryIconResolver.resolvePengeluaranIconType("Bahan Baku")
        )
        assertEquals(
            PengeluaranIconType.FILTER_MEMBRAN,
            CategoryIconResolver.resolvePengeluaranIconType("Filter & Membran")
        )
        assertEquals(
            PengeluaranIconType.TUTUP_TISU_GALON,
            CategoryIconResolver.resolvePengeluaranIconType("Tutup & Tisu Galon")
        )
        assertEquals(
            PengeluaranIconType.BENSIN_TRANSPORT,
            CategoryIconResolver.resolvePengeluaranIconType("Bensin & Transport")
        )
        assertEquals(
            PengeluaranIconType.PERAWATAN,
            CategoryIconResolver.resolvePengeluaranIconType("Perawatan")
        )
        assertEquals(
            PengeluaranIconType.OPERASIONAL,
            CategoryIconResolver.resolvePengeluaranIconType("Operasional")
        )
    }

    @Test
    fun `pengeluaran generic or empty category falls back to item keywords while explicit category takes precedence`() {
        assertEquals(
            PengeluaranIconType.BENSIN_TRANSPORT,
            CategoryIconResolver.resolvePengeluaranIconType("Lainnya", "Bensin motor kurir")
        )
        assertEquals(
            PengeluaranIconType.LISTRIK_AIR,
            CategoryIconResolver.resolvePengeluaranIconType("", "Token PLN 200rb")
        )
        assertEquals(
            PengeluaranIconType.TUTUP_TISU_GALON,
            CategoryIconResolver.resolvePengeluaranIconType("Lainnya", "Beli tutup galon 1 pak")
        )
        assertEquals(
            PengeluaranIconType.OPERASIONAL,
            CategoryIconResolver.resolvePengeluaranIconType("Operasional", "Token PLN cadangan")
        )
        assertEquals(
            PengeluaranIconType.LAINNYA,
            CategoryIconResolver.resolvePengeluaranIconType("Lainnya", "Donasi warga")
        )
    }

    @Test
    fun `all 7 default RO perawatan templates resolve to specific non-generic component icons`() {
        PerawatanScheduler.DEFAULT_TEMPLATES.forEach { tpl ->
            val resolved = CategoryIconResolver.resolvePerawatanIconType(
                syncId = tpl.syncId,
                nama = tpl.nama
            )
            assertNotEquals(
                "Template ${tpl.nama} must map to a dedicated icon",
                PerawatanIconType.PERAWATAN_UMUM,
                resolved
            )
        }

        assertEquals(
            PerawatanIconType.CEK_TDS,
            CategoryIconResolver.resolvePerawatanIconType("default_perawatan_cek_tds", "Cek TDS")
        )
        assertEquals(
            PerawatanIconType.CUCI_TANGKI,
            CategoryIconResolver.resolvePerawatanIconType("", "Kuras Tandon Utama")
        )
        assertEquals(
            PerawatanIconType.LAMPU_UV,
            CategoryIconResolver.resolvePerawatanIconType("", "Ganti Lampu Ultraviolet")
        )
        assertEquals(
            PerawatanIconType.SERVICE_POMPA,
            CategoryIconResolver.resolvePerawatanIconType("", "Servis Booster Pompa")
        )
    }

    @Test
    fun `produk variants resolve to appropriate galon, bottle, or accessory icons`() {
        assertEquals(
            ProdukIconType.GALON_ISI_ULANG,
            CategoryIconResolver.resolveProdukIconType("Isi Ulang RO 19L")
        )
        assertEquals(
            ProdukIconType.GALON_BARU_TUKAR,
            CategoryIconResolver.resolveProdukIconType("Galon Baru + Isi")
        )
        assertEquals(
            ProdukIconType.TUTUP_TISU_SEGEL,
            CategoryIconResolver.resolveProdukIconType("Tutup & Segel Galon")
        )
        assertEquals(
            ProdukIconType.BOTOL_GELAS_KEMASAN,
            CategoryIconResolver.resolveProdukIconType("Air Mineral Botol 600ml")
        )
    }
}
