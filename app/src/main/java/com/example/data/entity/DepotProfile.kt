package com.example.data.entity

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.util.Locale

/**
 * Bidang identitas profil depot yang dihitung dalam indikator kelengkapan profil usaha.
 */
enum class DepotProfileField {
    OWNER_NAME,
    DEPOT_NAME,
    PHONE_NUMBER,
    ADDRESS,
    RECEIPT_FOOTER
}

/**
 * Model profil pemilik dan identitas usaha depot air minum untuk pemantauan pendaftar,
 * sinkronisasi identitas ke perangkat karyawan, serta kop dinamis pada struk, nota WhatsApp, dan PDF.
 */
@Immutable
@Keep
@IgnoreExtraProperties
data class DepotProfile(
    val uid: String = "",
    val email: String = "",
    val googleDisplayName: String = "",
    val ownerName: String = "",
    val depotName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val receiptFooter: String = "",
    val role: String = ROLE_OWNER,
    val subscriptionTier: String = TIER_FREE,
    val appVersion: String = "",
    val depotType: String = TYPE_RO,
    val tankCapacityLiters: Double = DEFAULT_TANK_CAPACITY_LITERS,
    val currentTankStockLiters: Double = 0.0
) {
    /**
     * Mengembalikan nama depot yang siap dicetak pada kop, atau fallback "DEPOT AIR MINUM" jika masih kosong.
     */
    fun effectiveDepotName(): String = depotName.trim().ifBlank { DEFAULT_DEPOT_NAME }

    /**
     * Mengembalikan nama pemilik efektif dengan fallback ke nama tampilan Google bila belum diisi.
     */
    fun effectiveOwnerName(): String = ownerName.trim().ifBlank { googleDisplayName.trim() }

    /**
     * Menunjukkan apakah pengguna memiliki paket berbayar PRO (bebas watermark).
     */
    fun isProUser(): Boolean = subscriptionTier.trim().equals(TIER_PRO, ignoreCase = true)

    /**
     * Menunjukkan apakah nama depot sudah diisi oleh pemilik (bukan kosong/spasi).
     */
    fun hasConfiguredDepotName(): Boolean = depotName.trim().isNotEmpty()

    /**
     * Menunjukkan apakah peran pengguna saat ini adalah OWNER.
     */
    fun isOwnerRole(): Boolean = role.trim().equals(ROLE_OWNER, ignoreCase = true)

    /**
     * Menunjukkan apakah depot mendukung produk/stok air mineral (mode MINERAL atau BOTH).
     */
    fun isMineralSupported(): Boolean =
        depotType.equals(TYPE_MINERAL, ignoreCase = true) || depotType.equals(TYPE_BOTH, ignoreCase = true)

    /**
     * Menunjukkan apakah depot mendukung perawatan mesin RO (mode RO atau BOTH).
     */
    fun isRoSupported(): Boolean =
        depotType.equals(TYPE_RO, ignoreCase = true) || depotType.equals(TYPE_BOTH, ignoreCase = true)

    /**
     * Menunjukkan apakah depot menjalankan kedua jenis usaha sekaligus (RO dan Mineral).
     */
    fun isBothType(): Boolean = depotType.equals(TYPE_BOTH, ignoreCase = true)

    /**
     * Mengembalikan inisial 1–2 huruf dari nama depot (atau nama pemilik jika nama depot belum diatur)
     * untuk ditampilkan pada avatar kartu profil.
     */
    fun avatarInitials(): String {
        val source = depotName.trim().ifBlank { effectiveOwnerName() }.ifBlank { "SR" }
        val words = source.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "SR"
        }
    }

    /**
     * Mengembalikan daftar bidang profil utama yang masih kosong.
     */
    fun missingProfileFields(): List<DepotProfileField> {
        return buildList {
            if (effectiveOwnerName().isBlank()) add(DepotProfileField.OWNER_NAME)
            if (!hasConfiguredDepotName()) add(DepotProfileField.DEPOT_NAME)
            if (phoneNumber.trim().isEmpty()) add(DepotProfileField.PHONE_NUMBER)
            if (address.trim().isEmpty()) add(DepotProfileField.ADDRESS)
            if (receiptFooter.trim().isEmpty()) add(DepotProfileField.RECEIPT_FOOTER)
        }
    }

    /**
     * Mengembalikan jumlah bidang profil utama yang sudah terisi (0 s/d [TOTAL_COMPLETENESS_FIELDS]).
     */
    fun completenessCount(): Int = TOTAL_COMPLETENESS_FIELDS - missingProfileFields().size

    /**
     * Mengembalikan rasio kelengkapan profil antara 0.0f hingga 1.0f.
     */
    fun completenessRatio(): Float =
        (completenessCount().toFloat() / TOTAL_COMPLETENESS_FIELDS.toFloat()).coerceIn(0f, 1f)

    /**
     * Mengembalikan persentase kelengkapan profil dalam bilangan bulat (0..100).
     */
    fun completenessPercent(): Int = (completenessRatio() * 100f).toInt()

    /**
     * Menunjukkan apakah seluruh 5 komponen profil utama sudah lengkap.
     */
    fun isProfileFullyComplete(): Boolean = missingProfileFields().isEmpty()

    /**
     * Menormalisasi nomor telepon/WhatsApp agar rapi saat disimpan dan ditampilkan pada kop struk.
     */
    fun normalizedPhoneNumber(): String = normalizePhoneInput(phoneNumber)

    /**
     * Membentuk teks ringkasan profil depot yang rapi untuk disalin ke papan klip atau dibagikan.
     */
    fun toShareableSummaryText(): String {
        val lines = mutableListOf<String>()
        lines.add(effectiveDepotName().uppercase())
        val owner = effectiveOwnerName()
        if (owner.isNotBlank()) {
            lines.add("Pemilik: $owner")
        }
        val cleanPhone = normalizedPhoneNumber()
        if (cleanPhone.isNotBlank()) {
            lines.add("WA/Telp: $cleanPhone")
        }
        val cleanAddress = address.trim()
        if (cleanAddress.isNotBlank()) {
            lines.add("Alamat: $cleanAddress")
        }
        val typeText = when (depotType.uppercase()) {
            TYPE_MINERAL -> "Depot Air Mineral / Gunung"
            TYPE_BOTH -> "RO & Air Mineral"
            else -> "Depot Air RO"
        }
        if (isMineralSupported()) {
            val formattedCap = NumberFormat.getNumberInstance(Locale("id", "ID"))
                .format(tankCapacityLiters.toLong())
            lines.add("Jenis Usaha: $typeText (Kapasitas Toren $formattedCap L)")
        } else {
            lines.add("Jenis Usaha: $typeText")
        }
        return lines.joinToString("\n")
    }

    companion object {
        const val DEFAULT_DEPOT_NAME = "DEPOT AIR MINUM"
        const val ROLE_OWNER = "OWNER"
        const val ROLE_STAFF = "STAFF"
        const val TIER_FREE = "FREE"
        const val TIER_PRO = "PRO"
        const val WATERMARK_TEXT = "Dicetak dengan Aplikasi TOYA"

        const val TYPE_RO = "RO"
        const val TYPE_MINERAL = "MINERAL"
        const val TYPE_BOTH = "BOTH"
        const val DEFAULT_TANK_CAPACITY_LITERS = 5000.0
        const val TOTAL_COMPLETENESS_FIELDS = 5

        val TANK_CAPACITY_PRESETS_LITERS = listOf(1000L, 2000L, 3000L, 5000L, 8000L)

        /**
         * Merapikan input nomor telepon dengan menghapus spasi ganda namun tetap mempertahankan
         * format yang mudah dibaca pada kop struk.
         */
        fun normalizePhoneInput(raw: String): String {
            val trimmed = raw.trim().replace(Regex("\\s+"), " ")
            if (trimmed.isEmpty()) return ""
            val allowed = trimmed.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
            return allowed.trim().take(40)
        }
    }
}
