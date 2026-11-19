package com.example.data.entity

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Representasi dokumen analisis bisnis AI milik pemilik depot pada koleksi `/ai_insights/{insightId}`.
 */
@Keep
@IgnoreExtraProperties
data class AiBusinessInsight(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val depotId: String = "",
    val periodDays: Int = DEFAULT_PERIOD_DAYS,
    val insights: List<String> = emptyList(),
    val stockAdvice: String = "",
    val healthStatus: String = HEALTH_SEHAT,
    val priorityActionTitle: String = "",
    val priorityActionTarget: String = TARGET_LAPORAN,
    val totalOmzet: Double = 0.0,
    val totalKasMasuk: Double = 0.0,
    val totalKomisiKurir: Double = 0.0,
    val totalPengeluaran: Double = 0.0,
    val labaBersih: Double = 0.0,
    val marginLabaPersen: Double = 0.0,
    val totalGalon: Int = 0,
    val activeCustomers: Int = 0,
    val totalPiutang: Double = 0.0,
    val overduePiutangOver30d: Double = 0.0,
    val omzetGrowthPercent: Double = 0.0,
    val galonGrowthPercent: Double = 0.0,
    val labaGrowthPercent: Double = 0.0,
    val modelName: String = DEFAULT_MODEL_NAME,
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    /**
     * Memastikan status kesehatan bisnis terpetakan ke salah satu konstanta resmi (`SEHAT`, `WASPADA`, `KRITIS`).
     */
    fun normalizedHealthStatus(): String = normalizeHealthStatus(healthStatus)

    /**
     * Memastikan target navigasi aksi prioritas terpetakan ke salah satu target layar yang valid.
     */
    fun normalizedPriorityTarget(): String = normalizePriorityTarget(priorityActionTarget)

    companion object {
        const val DEFAULT_MODEL_NAME = "gemini-3.8-flash"
        const val DEFAULT_PERIOD_DAYS = 30

        const val HEALTH_SEHAT = "SEHAT"
        const val HEALTH_WASPADA = "WASPADA"
        const val HEALTH_KRITIS = "KRITIS"

        const val TARGET_KASBON = "KASBON"
        const val TARGET_CRM = "CRM"
        const val TARGET_STOK = "STOK"
        const val TARGET_PERAWATAN = "PERAWATAN"
        const val TARGET_LAPORAN = "LAPORAN"

        val SUPPORTED_PERIOD_DAYS: List<Int> = listOf(7, 30, 90)

        /**
         * Menghasilkan ID dokumen deterministik untuk insight terbaru per pemilik depot.
         */
        fun latestDocId(uid: String): String = "latest_${uid.trim()}"

        /**
         * Menormalkan nilai status kesehatan bisnis dari respons AI atau dokumen lama.
         */
        fun normalizeHealthStatus(raw: String?): String {
            return when (raw?.trim()?.uppercase()) {
                HEALTH_WASPADA -> HEALTH_WASPADA
                HEALTH_KRITIS -> HEALTH_KRITIS
                else -> HEALTH_SEHAT
            }
        }

        /**
         * Menormalkan target aksi prioritas agar selalu menunjuk ke fitur yang valid di aplikasi.
         */
        fun normalizePriorityTarget(raw: String?): String {
            return when (raw?.trim()?.uppercase()) {
                TARGET_KASBON -> TARGET_KASBON
                TARGET_CRM -> TARGET_CRM
                TARGET_STOK -> TARGET_STOK
                TARGET_PERAWATAN -> TARGET_PERAWATAN
                else -> TARGET_LAPORAN
            }
        }
    }
}
