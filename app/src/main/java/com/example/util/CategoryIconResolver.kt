package com.example.util

/**
 * Klasifikasi ikon semantik untuk setiap kategori pengeluaran depot air minum Santoya RO.
 */
enum class PengeluaranIconType {
    LISTRIK_AIR,
    FILTER_MEMBRAN,
    TUTUP_TISU_GALON,
    BENSIN_TRANSPORT,
    PERAWATAN,
    OPERASIONAL,
    LAINNYA
}

/**
 * Klasifikasi ikon semantik untuk setiap komponen jadwal dan riwayat perawatan mesin RO.
 */
enum class PerawatanIconType {
    CEK_TDS,
    FILTER_RO,
    CUCI_TANGKI,
    MEMBRAN_RO,
    LAMPU_UV,
    SERVICE_POMPA,
    PERAWATAN_UMUM
}

/**
 * Klasifikasi ikon semantik untuk varian produk dan kemasan depot air minum.
 */
enum class ProdukIconType {
    GALON_ISI_ULANG,
    GALON_BARU_TUKAR,
    TUTUP_TISU_SEGEL,
    BOTOL_GELAS_KEMASAN
}

/**
 * Resolver pemetaan semantik untuk menentukan jenis ikon yang akurat sesuai kategori pengeluaran,
 * jenis komponen perawatan mesin RO, maupun varian produk Santoya RO.
 */
object CategoryIconResolver {

    /**
     * Memetakan kategori pengeluaran (dan fallback nama item jika kategori bersifat umum/lama)
     * menjadi [PengeluaranIconType] yang konsisten.
     */
    fun resolvePengeluaranIconType(
        kategori: String,
        namaItem: String = ""
    ): PengeluaranIconType {
        val normKat = kategori.trim().lowercase()
        val fromCategory = matchPengeluaranCategory(normKat)
        if (fromCategory != null) return fromCategory

        val normItem = namaItem.trim().lowercase()
        if (normItem.isBlank()) return PengeluaranIconType.LAINNYA

        return matchPengeluaranKeywords(normItem) ?: PengeluaranIconType.LAINNYA
    }

    /**
     * Memetakan item perawatan mesin RO berdasarkan [syncId] template bawaan atau [nama] jadwal
     * ke [PerawatanIconType] yang sesuai.
     */
    fun resolvePerawatanIconType(
        syncId: String = "",
        nama: String = ""
    ): PerawatanIconType {
        val normSync = syncId.trim().lowercase()
        when (normSync) {
            "default_perawatan_cek_tds" -> return PerawatanIconType.CEK_TDS
            "default_perawatan_filter_sedimen",
            "default_perawatan_filter_karbon" -> return PerawatanIconType.FILTER_RO
            "default_perawatan_cuci_tangki" -> return PerawatanIconType.CUCI_TANGKI
            "default_perawatan_membran_ro" -> return PerawatanIconType.MEMBRAN_RO
            "default_perawatan_lampu_uv" -> return PerawatanIconType.LAMPU_UV
            "default_perawatan_service_pompa" -> return PerawatanIconType.SERVICE_POMPA
        }

        val normNama = nama.trim().lowercase()
        if (normNama.isBlank()) return PerawatanIconType.PERAWATAN_UMUM

        return when {
            containsAny(normNama, "tds", "ppm", "ukur air", "kualitas") -> PerawatanIconType.CEK_TDS
            containsAny(normNama, "tangki", "tandon", "cuci", "kuras", "reservoir") -> PerawatanIconType.CUCI_TANGKI
            containsAny(normNama, "membran", "membrane") -> PerawatanIconType.MEMBRAN_RO
            containsAny(normNama, "uv", "lampu", "ultraviolet", "steril") -> PerawatanIconType.LAMPU_UV
            containsAny(normNama, "pompa", "pump", "booster", "tekanan") -> PerawatanIconType.SERVICE_POMPA
            containsAny(normNama, "filter", "sedimen", "karbon", "carbon", "cartridge", "resin") -> PerawatanIconType.FILTER_RO
            else -> PerawatanIconType.PERAWATAN_UMUM
        }
    }

    /**
     * Memetakan nama produk depot ke [ProdukIconType] yang paling menggambarkan jenis produk.
     */
    fun resolveProdukIconType(namaProduk: String): ProdukIconType {
        val norm = namaProduk.trim().lowercase()
        if (norm.isBlank()) return ProdukIconType.GALON_ISI_ULANG

        return when {
            containsAny(norm, "tutup", "tisu", "tissue", "segel", "seal") -> ProdukIconType.TUTUP_TISU_SEGEL
            containsAny(norm, "botol", "gelas", "cup", "dus", "kardus", "kemasan", "600ml", "330ml", "1500ml", "220ml") ->
                ProdukIconType.BOTOL_GELAS_KEMASAN
            containsAny(norm, "galon baru", "kosong", "tabung", "tukar", "plus galon", "isi + galon", "beli galon") ->
                ProdukIconType.GALON_BARU_TUKAR
            else -> ProdukIconType.GALON_ISI_ULANG
        }
    }

    private fun matchPengeluaranCategory(normKat: String): PengeluaranIconType? {
        if (normKat.isBlank()) return null
        return when {
            normKat == "semua" -> null
            normKat == "lainnya" || normKat == "umum" || normKat == "lain-lain" -> null
            containsAny(normKat, "listrik", "bahan baku", "pdam", "pln", "token") -> PengeluaranIconType.LISTRIK_AIR
            containsAny(normKat, "filter", "membran", "sedimen", "karbon") -> PengeluaranIconType.FILTER_MEMBRAN
            containsAny(normKat, "tutup", "tisu", "tissue", "segel") -> PengeluaranIconType.TUTUP_TISU_GALON
            containsAny(normKat, "bensin", "transport", "bbm", "solar", "ongkir") -> PengeluaranIconType.BENSIN_TRANSPORT
            containsAny(normKat, "perawatan", "servis", "service", "maintenance") -> PengeluaranIconType.PERAWATAN
            containsAny(normKat, "operasional", "harian", "kantor", "kas") -> PengeluaranIconType.OPERASIONAL
            else -> matchPengeluaranKeywords(normKat)
        }
    }

    private fun matchPengeluaranKeywords(normText: String): PengeluaranIconType? {
        return when {
            containsAny(normText, "bensin", "pertalite", "pertamax", "solar", "bbm", "oli", "ban", "motor", "parkir", "transport") ->
                PengeluaranIconType.BENSIN_TRANSPORT
            containsAny(normText, "listrik", "token", "pln", "pdam", "air baku", "tangki air") ->
                PengeluaranIconType.LISTRIK_AIR
            containsAny(normText, "tutup", "tisu", "tissue", "segel", "plastik galon") ->
                PengeluaranIconType.TUTUP_TISU_GALON
            containsAny(normText, "filter", "membran", "sedimen", "karbon", "cartridge") ->
                PengeluaranIconType.FILTER_MEMBRAN
            containsAny(normText, "perawatan", "servis", "service", "pompa", "lampu uv", "cuci tangki", "tds") ->
                PengeluaranIconType.PERAWATAN
            containsAny(normText, "operasional", "gaji", "komisi", "kertas", "nota", "lakban", "sabun", "atk", "makan", "minum") ->
                PengeluaranIconType.OPERASIONAL
            else -> null
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { kw -> text.contains(kw) }
    }
}
