package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.AiBusinessInsight
import com.example.data.entity.DepotProfile
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.AiBusinessSummary
import com.example.util.AiBusinessSummaryBuilder
import com.example.util.AiKasbonLogRecord
import com.example.util.AiPelangganRecord
import com.example.util.AiPengeluaranRecord
import com.example.util.AiPerawatanRecord
import com.example.util.AiProdukRecord
import com.example.util.AiTangkiRefillRecord
import com.example.util.AiTransaksiRecord
import com.example.util.DepotProfileStore
import com.example.util.KaryawanScopeHelper
import com.example.util.OperationType
import com.example.util.SyncCollections
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Penanda khusus ketika depot belum memiliki cukup aktivitas/data operasional pada periode terpilih
 * sehingga pemanggilan kuota Gemini dilewati.
 */
class InsufficientAiDataException(message: String) : IllegalStateException(message)

/**
 * Hasil eksekusi analisis bisnis AI beserta status penyimpanan sinkronisasi ke Cloud Firestore.
 */
data class AiAnalysisOutcome(
    val insight: AiBusinessInsight,
    val savedToCloud: Boolean,
    val cloudSaveError: Throwable? = null
)

/**
 * Repositori untuk mengamati, menghasilkan, dan menyimpan AI Business Insights per akun pemilik depot.
 * Mengutamakan data lokal Room agar cepat & tahan offline dengan fallback kueri Firestore terfilter tanggal.
 */
class AiAnalyticsRepository(
    private val db: FirebaseFirestore,
    private val localDb: AppDatabase? = null,
    private val profileStore: DepotProfileStore? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
        localDb = AppDatabase.getInstance(context),
        profileStore = DepotProfileStore(context)
    )

    private val responseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "healthStatus" to Schema.enumeration(
                    values = listOf(
                        AiBusinessInsight.HEALTH_SEHAT,
                        AiBusinessInsight.HEALTH_WASPADA,
                        AiBusinessInsight.HEALTH_KRITIS
                    ),
                    description = "Status kesehatan bisnis depot: SEHAT, WASPADA, atau KRITIS"
                ),
                "insights" to Schema.array(
                    items = Schema.string(description = "Insight singkat bisnis depot air dalam Bahasa Indonesia"),
                    description = "Tepat 3 poin insight singkat dan relevan berdasarkan data keuangan dan operasional"
                ),
                "stockAdvice" to Schema.string(
                    description = "Saran praktis pengelolaan stok air toren mineral atau jadwal perawatan mesin RO"
                ),
                "priorityActionTitle" to Schema.string(
                    description = "Label singkat aksi prioritas paling mendesak (maks 36 karakter)"
                ),
                "priorityActionTarget" to Schema.enumeration(
                    values = listOf(
                        AiBusinessInsight.TARGET_KASBON,
                        AiBusinessInsight.TARGET_CRM,
                        AiBusinessInsight.TARGET_STOK,
                        AiBusinessInsight.TARGET_PERAWATAN,
                        AiBusinessInsight.TARGET_LAPORAN
                    ),
                    description = "Target layar aksi prioritas di aplikasi"
                )
            )
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = AiBusinessInsight.DEFAULT_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = this@AiAnalyticsRepository.responseSchema
            },
            systemInstruction = content {
                text(AiBusinessSummaryBuilder.SYSTEM_INSTRUCTION)
            }
        )
    }

    /**
     * Mengamati dokumen insight terbaru milik [uid] secara real-time pada `/ai_insights/latest_{uid}`.
     */
    fun observeLatestInsight(uid: String): Flow<AiBusinessInsight?> {
        val docId = AiBusinessInsight.latestDocId(uid)
        return db.collection(AI_INSIGHTS_COLLECTION)
            .document(docId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        AiBusinessInsight::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
    }

    /**
     * Menghitung ringkasan data bisnis [periodDays] hari terakhir dari cache lokal Room (atau Firestore terfilter
     * jika cache lokal kosong), memanggil Gemini melalui Firebase AI Logic, lalu menyimpannya ke Firestore.
     * Jika penulisan ke Cloud gagal saat koneksi bermasalah, insight hasil Gemini tetap dipertahankan lokal.
     */
    suspend fun analyzeAndSaveLatestInsight(
        uid: String,
        fallbackDepotProfile: DepotProfile,
        periodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS
    ): Result<AiAnalysisOutcome> = withContext(Dispatchers.IO) {
        val cleanUid = uid.trim()
        val authUid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (cleanUid.isBlank() || authUid != cleanUid) {
            return@withContext Result.failure(
                SecurityException("Sesi Google tidak valid untuk akun pemilik ini.")
            )
        }

        val safePeriodDays = AiBusinessSummaryBuilder.sanitizePeriodDays(periodDays)
        val depotId = "depot_$cleanUid"
        val insightDocId = AiBusinessInsight.latestDocId(cleanUid)
        val insightDocPath = "$AI_INSIGHTS_COLLECTION/$insightDocId"
        val nowMillis = System.currentTimeMillis()

        try {
            val effectiveProfile = resolveEffectiveProfile(fallbackDepotProfile)
            val localSummary = buildSummaryFromLocalDb(
                fallbackDepotProfile = effectiveProfile,
                periodDays = safePeriodDays,
                nowMillis = nowMillis
            )

            val summary = localSummary ?: withTimeout(FIRESTORE_FETCH_TIMEOUT_MS) {
                fetchOwnerSummaryFromFirestore(
                    depotId = depotId,
                    fallbackDepotProfile = effectiveProfile,
                    periodDays = safePeriodDays,
                    nowMillis = nowMillis
                )
            }

            if (!summary.hasSufficientData) {
                return@withContext Result.failure(
                    InsufficientAiDataException(
                        "Belum ada transaksi atau aktivitas operasional dalam $safePeriodDays hari terakhir untuk dianalisis."
                    )
                )
            }

            val prompt = AiBusinessSummaryBuilder.buildPrompt(summary)
            val response = withTimeout(GEMINI_TIMEOUT_MS) {
                model.generateContent(prompt)
            }

            val parsed = AiBusinessSummaryBuilder.parseGeminiResponse(response.text.orEmpty())

            val omzet = summary.totalOmzet30d.coerceAtLeast(0.0)
            val kasMasuk = summary.totalKasMasuk30d.coerceAtLeast(0.0)
            val komisiKurir = summary.totalKomisiKurir.coerceAtLeast(0.0)
            val pengeluaran = summary.totalPengeluaran30d.coerceAtLeast(0.0)
            val labaBersih = safeFinite(summary.labaBersih)
            val marginPersen = safeFinite(summary.marginLabaPersen, -999.9, 999.9)
            val piutang = summary.totalPiutang.coerceAtLeast(0.0)
            val piutangOver30 = summary.overduePiutangOver30d.coerceAtLeast(0.0)
            val omzetGrowth = safeFinite(summary.omzetGrowthPercent, -9999.9, 9999.9)
            val galonGrowth = safeFinite(summary.galonGrowthPercent, -9999.9, 9999.9)
            val labaGrowth = safeFinite(summary.labaGrowthPercent, -9999.9, 9999.9)

            val generatedInsight = AiBusinessInsight(
                id = insightDocId,
                ownerId = cleanUid,
                depotId = depotId,
                periodDays = safePeriodDays,
                insights = parsed.insights,
                stockAdvice = parsed.stockAdvice,
                healthStatus = parsed.healthStatus,
                priorityActionTitle = parsed.priorityActionTitle,
                priorityActionTarget = parsed.priorityActionTarget,
                totalOmzet = omzet,
                totalKasMasuk = kasMasuk,
                totalKomisiKurir = komisiKurir,
                totalPengeluaran = pengeluaran,
                labaBersih = labaBersih,
                marginLabaPersen = marginPersen,
                totalGalon = summary.totalGalon30d.coerceAtLeast(0),
                activeCustomers = summary.activeCustomerCount.coerceAtLeast(0),
                totalPiutang = piutang,
                overduePiutangOver30d = piutangOver30,
                omzetGrowthPercent = omzetGrowth,
                galonGrowthPercent = galonGrowth,
                labaGrowthPercent = labaGrowth,
                modelName = AiBusinessInsight.DEFAULT_MODEL_NAME,
                timestamp = Timestamp.now()
            )

            val payload = hashMapOf<String, Any>(
                "ownerId" to cleanUid,
                "depotId" to depotId,
                "periodDays" to safePeriodDays,
                "insights" to parsed.insights,
                "stockAdvice" to parsed.stockAdvice,
                "healthStatus" to parsed.healthStatus,
                "priorityActionTitle" to parsed.priorityActionTitle,
                "priorityActionTarget" to parsed.priorityActionTarget,
                "totalOmzet" to omzet,
                "totalKasMasuk" to kasMasuk,
                "totalKomisiKurir" to komisiKurir,
                "totalPengeluaran" to pengeluaran,
                "labaBersih" to labaBersih,
                "marginLabaPersen" to marginPersen,
                "totalGalon" to summary.totalGalon30d.coerceAtLeast(0),
                "activeCustomers" to summary.activeCustomerCount.coerceAtLeast(0),
                "totalPiutang" to piutang,
                "overduePiutangOver30d" to piutangOver30,
                "omzetGrowthPercent" to omzetGrowth,
                "galonGrowthPercent" to galonGrowth,
                "labaGrowthPercent" to labaGrowth,
                "modelName" to AiBusinessInsight.DEFAULT_MODEL_NAME,
                "timestamp" to FieldValue.serverTimestamp()
            )

            var savedToCloud = false
            var cloudError: Throwable? = null
            try {
                withTimeout(FIRESTORE_SAVE_TIMEOUT_MS) {
                    db.collection(AI_INSIGHTS_COLLECTION)
                        .document(insightDocId)
                        .set(payload)
                        .await()
                }
                savedToCloud = true
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Cloud save AI insight timed out; keeping local insight", e)
                cloudError = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.WRITE, insightDocPath)
                cloudError = e
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving AI insight to Firestore; keeping local insight", e)
                cloudError = e
            }

            Result.success(
                AiAnalysisOutcome(
                    insight = generatedInsight,
                    savedToCloud = savedToCloud,
                    cloudSaveError = cloudError
                )
            )
        } catch (e: InsufficientAiDataException) {
            Result.failure(e)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI analytics timed out for uid", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.GET, insightDocPath)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "AI analytics failed", e)
            Result.failure(e)
        }
    }

    private fun resolveEffectiveProfile(fallback: DepotProfile): DepotProfile {
        val stored = profileStore?.currentProfile()
        return if (stored != null && stored.uid.isNotBlank() && stored.uid == fallback.uid) {
            stored
        } else {
            fallback
        }
    }

    private suspend fun buildSummaryFromLocalDb(
        fallbackDepotProfile: DepotProfile,
        periodDays: Int,
        nowMillis: Long
    ): AiBusinessSummary? = coroutineScope {
        val room = localDb ?: return@coroutineScope null
        val txDeferred = async { room.transaksiDao().getAllOnce() }
        val pelDeferred = async { room.pelangganDao().getAllOnce() }
        val prodDeferred = async { room.produkDao().getAllOnce() }
        val expDeferred = async { room.pengeluaranDao().getAllOnce() }
        val kasbonDeferred = async { room.kasbonLogDao().getAllOnce() }
        val perawatanDeferred = async { room.perawatanDao().getAllOnce() }
        val tangkiDeferred = async { room.tangkiRefillDao().getAllOnce() }

        val rawTxs = txDeferred.await()
        val rawPelanggan = pelDeferred.await()
        val rawProduk = prodDeferred.await()
        val rawExp = expDeferred.await()
        val rawKasbon = kasbonDeferred.await()
        val rawPerawatan = perawatanDeferred.await()
        val rawTangki = tangkiDeferred.await()

        if (rawTxs.isEmpty() && rawPelanggan.isEmpty() && rawExp.isEmpty()) {
            return@coroutineScope null
        }

        val pelMap = rawPelanggan.associateBy { it.id }
        val prodMap = rawProduk.associateBy { it.id }
        val detailList = rawTxs.map { tx ->
            TransaksiDetail(
                transaksi = tx,
                pelanggan = pelMap[tx.id_pelanggan],
                kurir = null,
                produk = tx.id_produk?.let { prodMap[it] }
            )
        }

        AiBusinessSummaryBuilder.buildSummaryFromEntities(
            transaksiList = detailList,
            pelangganList = rawPelanggan,
            produkList = rawProduk,
            pengeluaranList = rawExp,
            kasbonLogs = rawKasbon,
            perawatanItems = rawPerawatan,
            tangkiRefillList = rawTangki,
            depotProfile = fallbackDepotProfile,
            periodDays = periodDays,
            nowMillis = nowMillis
        )
    }

    private suspend fun fetchOwnerSummaryFromFirestore(
        depotId: String,
        fallbackDepotProfile: DepotProfile,
        periodDays: Int,
        nowMillis: Long
    ): AiBusinessSummary = coroutineScope {
        val comparisonWindowStart = nowMillis - TimeUnit.DAYS.toMillis(periodDays.toLong() * 2L)
        val depotRef = db.collection(DEPOTS_COLLECTION).document(depotId)
        val depotDocDeferred = async { depotRef.get().await() }
        val produkDeferred = async { depotRef.collection(SyncCollections.PRODUK).get().await() }
        val pelangganDeferred = async { depotRef.collection(SyncCollections.PELANGGAN).get().await() }
        val transaksiDeferred = async {
            depotRef.collection(SyncCollections.TRANSAKSI)
                .whereGreaterThanOrEqualTo("tanggal", comparisonWindowStart)
                .get()
                .await()
        }
        val pengeluaranDeferred = async {
            depotRef.collection(SyncCollections.PENGELUARAN)
                .whereGreaterThanOrEqualTo("tanggal", comparisonWindowStart)
                .get()
                .await()
        }
        val kasbonDeferred = async {
            depotRef.collection(SyncCollections.KASBON_LOGS)
                .whereGreaterThanOrEqualTo("tanggal", comparisonWindowStart)
                .get()
                .await()
        }
        val perawatanDeferred = async { depotRef.collection(SyncCollections.PERAWATAN_JADWAL).get().await() }
        val tangkiDeferred = async {
            depotRef.collection(SyncCollections.TANGKI_REFILL)
                .whereGreaterThanOrEqualTo("tanggal", comparisonWindowStart)
                .get()
                .await()
        }

        val depotDoc = depotDocDeferred.await()
        val produkSnap = produkDeferred.await()
        val pelangganSnap = pelangganDeferred.await()
        val transaksiSnap = transaksiDeferred.await()
        val pengeluaranSnap = pengeluaranDeferred.await()
        val kasbonSnap = kasbonDeferred.await()
        val perawatanSnap = perawatanDeferred.await()
        val tangkiSnap = tangkiDeferred.await()

        val produkNameBySyncId = HashMap<String, String>()
        val produkRecords = produkSnap.documents.map { doc ->
            val syncId = doc.getString("syncId") ?: doc.id
            val isDeleted = doc.getBoolean("isDeleted") == true
            val nama = doc.getString("nama_produk")?.trim().orEmpty()
            val harga = (doc.get("harga") as? Number)?.toDouble() ?: 0.0
            if (!isDeleted && nama.isNotBlank()) {
                produkNameBySyncId[syncId] = nama
            }
            AiProdukRecord(
                namaProduk = nama,
                harga = harga,
                isDeleted = isDeleted || nama.isBlank()
            )
        }

        val customerIntIdBySyncId = HashMap<String, Int>()
        val pelangganRecords = pelangganSnap.documents.mapIndexed { idx, doc ->
            val syncId = doc.getString("syncId") ?: doc.id
            val intId = idx + 1
            customerIntIdBySyncId[syncId] = intId
            val isDeleted = doc.getBoolean("isDeleted") == true
            val sisaKasbon = (doc.get("sisa_kasbon") as? Number)?.toDouble() ?: 0.0
            AiPelangganRecord(
                id = intId,
                sisaKasbon = sisaKasbon,
                isDeleted = isDeleted
            )
        }

        val transaksiRecords = transaksiSnap.documents.mapIndexed { idx, doc ->
            val status = doc.getString("status") ?: "Lunas"
            val isHardDeleted = doc.getBoolean("_hardDeleted") == true ||
                (doc.getBoolean("isDeleted") == true && doc.getString("pelangganSyncId") == "deleted")
            val isSoftDeleted = doc.getBoolean("isDeleted") == true ||
                status.equals(KaryawanScopeHelper.STATUS_DIHAPUS, ignoreCase = true)
            val jumlah = ((doc.get("jumlah") as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            val hargaSatuan = (doc.get("harga_satuan") as? Number)?.toDouble() ?: 0.0
            val totalHarga = (doc.get("total_harga") as? Number)?.toDouble() ?: (jumlah * hargaSatuan)
            val totalKomisi = (doc.get("total_komisi") as? Number)?.toDouble() ?: 0.0
            val uangMuka = (doc.get("uangMuka") as? Number)?.toLong() ?: 0L
            val sisaKasbon = (doc.get("sisaKasbon") as? Number)?.toLong() ?: 0L
            val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: 0L
            val tanggalBayar = (doc.get("tanggalBayar") as? Number)?.toLong()
            val metodeBayar = doc.getString("metode_bayar") ?: "Tunai"
            val rawWaterType = doc.getString("waterType")?.trim()?.uppercase()
            val waterType = if (rawWaterType == Transaksi.WATER_TYPE_MINERAL) {
                Transaksi.WATER_TYPE_MINERAL
            } else {
                Transaksi.WATER_TYPE_RO
            }
            val produkSyncId = doc.getString("produkSyncId")
            val produkName = produkSyncId?.let { produkNameBySyncId[it] }
            val pelangganSyncId = doc.getString("pelangganSyncId").orEmpty()
            val pelangganIntId = customerIntIdBySyncId[pelangganSyncId] ?: (idx + 10_000)
            val isDelivery = !doc.getString("kurirSyncId").isNullOrBlank()

            AiTransaksiRecord(
                tanggal = tanggal,
                jumlah = jumlah,
                totalHarga = totalHarga,
                status = status,
                uangMuka = uangMuka,
                sisaKasbon = sisaKasbon,
                waterType = waterType,
                produkName = produkName,
                isDeleted = isHardDeleted || isSoftDeleted,
                id = idx + 1,
                pelangganId = pelangganIntId,
                totalKomisi = totalKomisi,
                metodeBayar = metodeBayar,
                tanggalBayar = tanggalBayar,
                isDelivery = isDelivery
            )
        }

        val pengeluaranRecords = pengeluaranSnap.documents.map { doc ->
            val isDeleted = doc.getBoolean("isDeleted") == true
            val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: 0L
            val kategori = doc.getString("kategori") ?: "Operasional"
            val nominal = (doc.get("nominal") as? Number)?.toDouble() ?: 0.0
            AiPengeluaranRecord(
                tanggal = tanggal,
                kategori = kategori,
                nominal = nominal,
                isDeleted = isDeleted
            )
        }

        val kasbonRecords = kasbonSnap.documents.mapIndexedNotNull { idx, doc ->
            if (doc.getBoolean("isDeleted") == true) return@mapIndexedNotNull null
            val pelangganSyncId = doc.getString("pelangganSyncId").orEmpty()
            val pelangganIntId = customerIntIdBySyncId[pelangganSyncId] ?: return@mapIndexedNotNull null
            val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: 0L
            val jumlahBayar = (doc.get("jumlah_bayar") as? Number)?.toDouble() ?: 0.0
            val metodeBayar = doc.getString("metode_bayar") ?: "Tunai"
            val catatan = doc.getString("catatan").orEmpty()
            AiKasbonLogRecord(
                tanggal = tanggal,
                jumlahBayar = jumlahBayar,
                pelangganId = pelangganIntId,
                metodeBayar = metodeBayar,
                catatan = catatan,
                id = idx + 1
            )
        }

        val perawatanRecords = perawatanSnap.documents.mapNotNull { doc ->
            if (doc.getBoolean("isDeleted") == true) return@mapNotNull null
            val nama = doc.getString("nama")?.trim().orEmpty()
            if (nama.isBlank()) return@mapNotNull null
            val jadwal = (doc.get("jadwalBerikutnya") as? Number)?.toLong() ?: 0L
            val interval = ((doc.get("intervalHari") as? Number)?.toInt() ?: 30).coerceAtLeast(1)
            AiPerawatanRecord(
                nama = nama,
                jadwalBerikutnya = jadwal,
                intervalHari = interval
            )
        }

        val tangkiRecords = tangkiSnap.documents.map { doc ->
            val isDeleted = doc.getBoolean("isDeleted") == true
            val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: 0L
            val volume = (doc.get("volumeLiter") as? Number)?.toDouble() ?: 0.0
            AiTangkiRefillRecord(
                tanggal = tanggal,
                volumeLiter = volume,
                isDeleted = isDeleted
            )
        }

        val depotType = depotDoc.getString("depotType")?.takeIf { it.isNotBlank() }
            ?: fallbackDepotProfile.depotType
        val tankCapacity = (depotDoc.get("tankCapacityLiters") as? Number)?.toDouble()
            ?: fallbackDepotProfile.tankCapacityLiters
        val currentTankStock = (depotDoc.get("currentTankStockLiters") as? Number)?.toDouble()
            ?: fallbackDepotProfile.currentTankStockLiters

        AiBusinessSummaryBuilder.buildSummary(
            transaksiList = transaksiRecords,
            pelangganList = pelangganRecords,
            produkList = produkRecords,
            pengeluaranList = pengeluaranRecords,
            depotType = depotType,
            tankCapacityLiters = tankCapacity,
            currentTankStockLiters = currentTankStock,
            nowMillis = nowMillis,
            periodDays = periodDays,
            kasbonLogs = kasbonRecords,
            perawatanList = perawatanRecords,
            tangkiRefillList = tangkiRecords
        )
    }

    private fun safeFinite(value: Double, min: Double = -1_000_000_000.0, max: Double = 1_000_000_000.0): Double {
        return if (value.isFinite()) value.coerceIn(min, max) else 0.0
    }

    companion object {
        private const val TAG = "AiAnalyticsRepository"
        private const val AI_INSIGHTS_COLLECTION = "ai_insights"
        private const val DEPOTS_COLLECTION = "depots"
        private const val FIRESTORE_FETCH_TIMEOUT_MS = 20_000L
        private const val FIRESTORE_SAVE_TIMEOUT_MS = 10_000L
        private const val GEMINI_TIMEOUT_MS = 60_000L
    }
}
