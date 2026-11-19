package com.example.util

import com.example.data.entity.AiBusinessInsight
import com.example.data.entity.DepotProfile
import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Ringkasan transaksi teranonimisasi untuk analisis bisnis depot air.
 */
data class AiTransaksiRecord(
    val tanggal: Long,
    val jumlah: Int,
    val totalHarga: Double,
    val status: String,
    val uangMuka: Long,
    val sisaKasbon: Long,
    val waterType: String,
    val produkName: String?,
    val isDeleted: Boolean,
    val id: Int = 0,
    val pelangganId: Int = 0,
    val totalKomisi: Double = 0.0,
    val metodeBayar: String = "Tunai",
    val tanggalBayar: Long? = null,
    val isDelivery: Boolean = false
)

/**
 * Ringkasan pelanggan teranonimisasi (tanpa PII nama/no HP/alamat) untuk analisis bisnis depot air.
 */
data class AiPelangganRecord(
    val sisaKasbon: Double,
    val isDeleted: Boolean,
    val id: Int = 0,
    val hasDueOrOverdueCrm: Boolean = false
)

/**
 * Ringkasan produk depot untuk analisis bisnis.
 */
data class AiProdukRecord(
    val namaProduk: String,
    val harga: Double,
    val isDeleted: Boolean
)

/**
 * Ringkasan pengeluaran depot untuk analisis bisnis.
 */
data class AiPengeluaranRecord(
    val tanggal: Long,
    val kategori: String,
    val nominal: Double,
    val isDeleted: Boolean
)

/**
 * Ringkasan log pelunasan kasbon teranonimisasi untuk rekonsiliasi arus kas AI.
 */
data class AiKasbonLogRecord(
    val tanggal: Long,
    val jumlahBayar: Double,
    val pelangganId: Int = 0,
    val metodeBayar: String = "Tunai",
    val catatan: String = "",
    val id: Int = 0
)

/**
 * Ringkasan jadwal perawatan mesin RO teranonimisasi.
 */
data class AiPerawatanRecord(
    val nama: String,
    val jadwalBerikutnya: Long,
    val intervalHari: Int = 30
)

/**
 * Ringkasan isi ulang tangki air mineral teranonimisasi.
 */
data class AiTangkiRefillRecord(
    val tanggal: Long,
    val volumeLiter: Double,
    val isDeleted: Boolean = false
)

/**
 * Agregat metrik bisnis depot lengkap (arus kas, laba bersih, piutang, CRM, toren, perawatan, dan tren pertumbuhan)
 * sebelum dikirim ke Gemini. Seluruh metrik bebas PII pelanggan.
 */
data class AiBusinessSummary(
    val totalOmzet30d: Double,
    val totalKasMasuk30d: Double,
    val totalGalon30d: Int,
    val galonRo30d: Int,
    val galonMineral30d: Int,
    val totalTransaksi30d: Int,
    val activeCustomerCount: Int,
    val kasbonCustomerCount: Int,
    val totalPiutang: Double,
    val totalPengeluaran30d: Double,
    val topProducts: List<Pair<String, Int>>,
    val topExpenseCategories: List<Pair<String, Double>>,
    val depotType: String,
    val tankCapacityLiters: Double,
    val currentTankStockLiters: Double,
    val periodDays: Int = 30,
    val pelunasanKasbon: Double = 0.0,
    val totalKomisiKurir: Double = 0.0,
    val totalBebanKeluar: Double = 0.0,
    val labaBersih: Double = 0.0,
    val marginLabaPersen: Double = 0.0,
    val collectionRatioPercent: Double = 0.0,
    val deliveryGalonCount: Int = 0,
    val pickupGalonCount: Int = 0,
    val transactingCustomerCount: Int = 0,
    val overduePiutangOver30d: Double = 0.0,
    val overduePiutangOver30Count: Int = 0,
    val crmDueOrOverdueCount: Int = 0,
    val crmOverdueCount: Int = 0,
    val omzetGrowthPercent: Double = 0.0,
    val galonGrowthPercent: Double = 0.0,
    val labaGrowthPercent: Double = 0.0,
    val hasComparisonBaseline: Boolean = false,
    val mineralEstimatedDaysRemaining: Double? = null,
    val waterLossPercent: Double? = null,
    val perawatanOverdueCount: Int = 0,
    val perawatanDueSoonCount: Int = 0,
    val overduePerawatanNames: List<String> = emptyList(),
    val hasSufficientData: Boolean = false
)

/**
 * Hasil parsing terstruktur dari respons JSON Gemini.
 */
data class ParsedAiInsightResponse(
    val insights: List<String>,
    val stockAdvice: String,
    val healthStatus: String = AiBusinessInsight.HEALTH_SEHAT,
    val priorityActionTitle: String = "",
    val priorityActionTarget: String = AiBusinessInsight.TARGET_LAPORAN
)

/**
 * Pembangun ringkasan metrik bisnis, prompt Gemini, dan parser JSON terstruktur untuk fitur AI Business Insights.
 * Menjamin konsistensi angka 100% dengan [LaporanFinanceCalculator], [TorenWaterCalculator], dan [PerawatanScheduler].
 */
object AiBusinessSummaryBuilder {

    const val WINDOW_DAYS = 30L
    const val MAX_INSIGHTS_COUNT = 5
    const val MAX_INSIGHT_LENGTH = 500
    const val MAX_STOCK_ADVICE_LENGTH = 1000
    const val MAX_ACTION_TITLE_LENGTH = 80

    const val SYSTEM_INSTRUCTION =
        "Anda adalah analis operasional dan keuangan senior khusus usaha depot air minum isi ulang (RO & Air Mineral) di Indonesia. " +
            "Tugas Anda memberi ringkasan strategis yang tajam, akurat secara angka, langsung bisa dieksekusi, dan bebas dari data pribadi (PII) pelanggan. " +
            "Selalu gunakan Bahasa Indonesia yang lugas dan natural, tanpa jargon berlebihan maupun format markdown. " +
            "Klasifikasikan healthStatus dengan tegas: SEHAT jika laba bersih positif, piutang macet >30 hari terkendali, dan stok/perawatan aman; " +
            "WASPADA jika margin tipis, piutang macet mulai naik, stok mineral <3 hari, atau ada perawatan H-3/terlambat; " +
            "KRITIS jika arus kas/laba defisit, piutang macet >30 hari besar, stok toren menipis (<1 hari), atau perawatan kritis terlambat."

    /**
     * Menghitung ringkasan bisnis komprehensif langsung dari entitas domain Room dengan kalkulasi terpadu
     * [LaporanFinanceCalculator], [TorenWaterCalculator], [PerawatanScheduler], dan [FormatUtils].
     */
    fun buildSummaryFromEntities(
        transaksiList: List<TransaksiDetail>,
        pelangganList: List<Pelanggan>,
        produkList: List<Produk>,
        pengeluaranList: List<Pengeluaran>,
        kasbonLogs: List<KasbonLog>,
        perawatanItems: List<PerawatanItem>,
        tangkiRefillList: List<TangkiRefill>,
        depotProfile: DepotProfile,
        periodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): AiBusinessSummary {
        val safePeriodDays = sanitizePeriodDays(periodDays)
        val activeCustomers = pelangganList.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
        val activeTxs = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.jumlah > 0 }
        val txByCustomerId = activeTxs.groupBy { it.transaksi.id_pelanggan }
        val todayLocal = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        var crmDueOrOverdueCount = 0
        var crmOverdueCount = 0
        activeCustomers.forEach { customer ->
            val customerTxs = txByCustomerId[customer.id].orEmpty()
            if (customerTxs.size >= 2) {
                val pred = FormatUtils.calculateCrmPredictionForCustomer(customerTxs, today = todayLocal, zone = zoneId)
                if (pred.status == CrmOrderStatus.OVERDUE) {
                    crmOverdueCount++
                    crmDueOrOverdueCount++
                } else if (pred.status == CrmOrderStatus.DUE_SOON) {
                    crmDueOrOverdueCount++
                }
            }
        }

        return computeSummaryCore(
            activeAllTx = activeTxs,
            activeCustomers = activeCustomers,
            activeProductNames = produkList.map { it.nama_produk.trim() }.filter { it.isNotBlank() },
            allPengeluaran = pengeluaranList,
            allKasbonLogs = kasbonLogs,
            perawatanItems = perawatanItems,
            tangkiRefills = tangkiRefillList.filter { !it.isDeleted },
            depotType = depotProfile.depotType,
            tankCapacityLiters = depotProfile.tankCapacityLiters,
            currentTankStockLiters = depotProfile.currentTankStockLiters,
            periodDays = safePeriodDays,
            crmDueOrOverdueCountOverride = crmDueOrOverdueCount,
            crmOverdueCountOverride = crmOverdueCount,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
    }

    /**
     * Menghitung ringkasan penjualan, galon, pelanggan, kasbon, komisi, pengeluaran, laba bersih, stok toren,
     * dan jadwal perawatan untuk rentang [periodDays] hari terakhir (default 30 hari).
     */
    fun buildSummary(
        transaksiList: List<AiTransaksiRecord>,
        pelangganList: List<AiPelangganRecord>,
        produkList: List<AiProdukRecord>,
        pengeluaranList: List<AiPengeluaranRecord>,
        depotType: String,
        tankCapacityLiters: Double,
        currentTankStockLiters: Double,
        nowMillis: Long = System.currentTimeMillis(),
        periodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS,
        kasbonLogs: List<AiKasbonLogRecord> = emptyList(),
        perawatanList: List<AiPerawatanRecord> = emptyList(),
        tangkiRefillList: List<AiTangkiRefillRecord> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): AiBusinessSummary {
        val safePeriodDays = sanitizePeriodDays(periodDays)
        val syntheticDetails = transaksiList.mapIndexedNotNull { idx, tx ->
            if (tx.isDeleted || tx.status.equals(KaryawanScopeHelper.STATUS_DIHAPUS, ignoreCase = true) || tx.jumlah <= 0) {
                return@mapIndexedNotNull null
            }
            val syntheticId = if (tx.id > 0) tx.id else idx + 1
            val syntheticPelangganId = if (tx.pelangganId > 0) tx.pelangganId else idx + 1
            val unitPrice = if (tx.jumlah > 0) tx.totalHarga.coerceAtLeast(0.0) / tx.jumlah else 0.0
            val txEntity = Transaksi(
                id = syntheticId,
                tanggal = tx.tanggal,
                id_pelanggan = syntheticPelangganId,
                id_kurir = if (tx.isDelivery) 1 else null,
                id_produk = if (!tx.produkName.isNullOrBlank()) idx + 1 else null,
                jumlah = tx.jumlah,
                harga_satuan = unitPrice,
                total_harga = tx.totalHarga.coerceAtLeast(0.0),
                total_komisi = tx.totalKomisi.coerceAtLeast(0.0),
                metode_bayar = tx.metodeBayar.ifBlank { "Tunai" },
                status = tx.status,
                uangMuka = tx.uangMuka.coerceAtLeast(0L),
                sisaKasbon = tx.sisaKasbon.coerceAtLeast(0L),
                tanggalBayar = tx.tanggalBayar,
                waterType = tx.waterType.ifBlank { Transaksi.WATER_TYPE_RO }
            )
            val syntheticProduk = tx.produkName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
                Produk(id = idx + 1, nama_produk = name, harga = unitPrice)
            }
            TransaksiDetail(
                transaksi = txEntity,
                pelanggan = Pelanggan(id = syntheticPelangganId, nama = "Pelanggan"),
                kurir = null,
                produk = syntheticProduk
            )
        }

        val activeCustomers = pelangganList.filter { !it.isDeleted }.mapIndexed { idx, p ->
            Pelanggan(
                id = if (p.id > 0) p.id else idx + 1,
                nama = "Pelanggan",
                sisa_kasbon = p.sisaKasbon.coerceAtLeast(0.0)
            )
        }
        val activeExpenses = pengeluaranList.filter { !it.isDeleted && it.nominal > 0.0 }.mapIndexed { idx, e ->
            Pengeluaran(
                id = idx + 1,
                nama_item = e.kategori.ifBlank { "Operasional" },
                kategori = e.kategori.ifBlank { "Operasional" },
                nominal = e.nominal.coerceAtLeast(0.0),
                tanggal = e.tanggal
            )
        }
        val activeKasbonLogs = kasbonLogs.filter { it.jumlahBayar > 0.0 }.mapIndexed { idx, log ->
            KasbonLog(
                id = if (log.id > 0) log.id else idx + 1,
                id_pelanggan = if (log.pelangganId > 0) log.pelangganId else idx + 1,
                tanggal = log.tanggal,
                jumlah_bayar = log.jumlahBayar.coerceAtLeast(0.0),
                sisa_kasbon_sebelum = 0.0,
                sisa_kasbon_sesudah = 0.0,
                metode_bayar = log.metodeBayar.ifBlank { "Tunai" },
                catatan = log.catatan
            )
        }
        val mappedPerawatan = perawatanList.mapIndexed { idx, p ->
            PerawatanItem(
                id = idx + 1,
                nama = p.nama,
                intervalHari = p.intervalHari.coerceAtLeast(1),
                jadwalBerikutnya = p.jadwalBerikutnya
            )
        }
        val mappedRefills = tangkiRefillList.filter { !it.isDeleted }.mapIndexed { idx, r ->
            TangkiRefill(
                id = idx + 1,
                tanggal = r.tanggal,
                volumeLiter = r.volumeLiter.coerceAtLeast(0.0),
                totalHarga = 0.0
            )
        }
        val crmDueCount = pelangganList.count { !it.isDeleted && it.hasDueOrOverdueCrm }

        return computeSummaryCore(
            activeAllTx = syntheticDetails,
            activeCustomers = activeCustomers,
            activeProductNames = produkList.filter { !it.isDeleted }.map { it.namaProduk.trim() }.filter { it.isNotBlank() },
            allPengeluaran = activeExpenses,
            allKasbonLogs = activeKasbonLogs,
            perawatanItems = mappedPerawatan,
            tangkiRefills = mappedRefills,
            depotType = depotType,
            tankCapacityLiters = tankCapacityLiters,
            currentTankStockLiters = currentTankStockLiters,
            periodDays = safePeriodDays,
            crmDueOrOverdueCountOverride = crmDueCount,
            crmOverdueCountOverride = 0,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
    }

    private fun computeSummaryCore(
        activeAllTx: List<TransaksiDetail>,
        activeCustomers: List<Pelanggan>,
        activeProductNames: List<String>,
        allPengeluaran: List<Pengeluaran>,
        allKasbonLogs: List<KasbonLog>,
        perawatanItems: List<PerawatanItem>,
        tangkiRefills: List<TangkiRefill>,
        depotType: String,
        tankCapacityLiters: Double,
        currentTankStockLiters: Double,
        periodDays: Int,
        crmDueOrOverdueCountOverride: Int,
        crmOverdueCountOverride: Int,
        nowMillis: Long,
        zoneId: ZoneId
    ): AiBusinessSummary {
        val windowDurationMillis = TimeUnit.DAYS.toMillis(periodDays.toLong())
        val currentStart = nowMillis - windowDurationMillis
        val previousEnd = currentStart - 1L
        val previousStart = currentStart - windowDurationMillis

        val initialCashByTxId = LaporanFinanceCalculator.computeInitialCashByTxId(activeAllTx, allKasbonLogs)

        val currentPeriod = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = activeAllTx,
            allPengeluaran = allPengeluaran,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = activeCustomers,
            startTime = currentStart,
            endTime = nowMillis,
            zoneId = zoneId,
            precomputedInitialCashByTxId = initialCashByTxId
        )

        val previousPeriod = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = activeAllTx,
            allPengeluaran = allPengeluaran,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = activeCustomers,
            startTime = previousStart,
            endTime = previousEnd,
            zoneId = zoneId,
            precomputedInitialCashByTxId = initialCashByTxId
        )

        val kasbonAging = LaporanFinanceCalculator.calculateKasbonAging(
            allTransaksi = activeAllTx,
            allPelanggan = activeCustomers,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        val topProducts = if (currentPeriod.productBreakdown.isNotEmpty()) {
            currentPeriod.productBreakdown.take(3).map { item ->
                item.namaProduk to item.totalGalon
            }
        } else {
            activeProductNames.take(3).map { it to 0 }
        }

        val topExpenseCategories = currentPeriod.expenseCategories.take(3).map { cat ->
            cat.kategori to cat.nominal
        }

        val deliveryGalon = currentPeriod.filteredTx
            .filter { it.transaksi.id_kurir != null }
            .sumOf { it.transaksi.jumlah }
        val pickupGalon = (currentPeriod.totalGalon - deliveryGalon).coerceAtLeast(0)

        val transactingCustomers = currentPeriod.filteredTx
            .map { it.transaksi.id_pelanggan }
            .filter { it > 0 }
            .distinct()
            .size

        val kasbonCustomerCount = activeCustomers.count { it.sisa_kasbon > 0.0 }
        val effectivePiutang = maxOf(
            activeCustomers.sumOf { it.sisa_kasbon.coerceAtLeast(0.0) },
            kasbonAging.totalActiveReceivable
        )

        val hasComparisonBaseline = previousPeriod.hasPeriodActivity
        val omzetGrowth = if (previousPeriod.totalOmzetPenjualan > 0.0) {
            safeFinite(
                LaporanFinanceCalculator.calculateGrowthPercent(
                    current = currentPeriod.totalOmzetPenjualan,
                    previous = previousPeriod.totalOmzetPenjualan
                )
            )
        } else 0.0
        val galonGrowth = if (previousPeriod.totalGalon > 0) {
            safeFinite(
                LaporanFinanceCalculator.calculateGrowthPercent(
                    current = currentPeriod.totalGalon.toDouble(),
                    previous = previousPeriod.totalGalon.toDouble()
                )
            )
        } else 0.0
        val labaGrowth = if (kotlin.math.abs(previousPeriod.labaBersih) >= 1.0) {
            safeFinite(
                LaporanFinanceCalculator.calculateGrowthPercent(
                    current = currentPeriod.labaBersih,
                    previous = previousPeriod.labaBersih
                )
            )
        } else 0.0

        val normalizedDepotType = depotType.ifBlank { DepotProfile.TYPE_RO }.uppercase()
        val supportsMineral = normalizedDepotType == DepotProfile.TYPE_MINERAL ||
            normalizedDepotType == DepotProfile.TYPE_BOTH
        val supportsRo = normalizedDepotType == DepotProfile.TYPE_RO ||
            normalizedDepotType == DepotProfile.TYPE_BOTH

        val mineralDaysRemaining: Double? = if (supportsMineral && currentPeriod.totalGalonMineral > 0) {
            val dailyMineralGallons = currentPeriod.totalGalonMineral.toDouble() / periodDays.toDouble()
            val dailyMineralLiters = dailyMineralGallons * TorenWaterCalculator.LITERS_PER_GALLON
            if (dailyMineralLiters > 0.0) {
                (currentTankStockLiters.coerceAtLeast(0.0) / dailyMineralLiters).coerceIn(0.0, 365.0)
            } else null
        } else {
            null
        }

        val waterLossPercent: Double? = if (supportsMineral && tangkiRefills.isNotEmpty()) {
            val totalRefillLiters = tangkiRefills.sumOf { it.volumeLiter.coerceAtLeast(0.0) }
            val totalMineralGallonsAllTime = activeAllTx
                .filter {
                    it.transaksi.waterType.equals(Transaksi.WATER_TYPE_MINERAL, ignoreCase = true) ||
                        it.produk?.nama_produk?.contains("mineral", ignoreCase = true) == true
                }
                .sumOf { it.transaksi.jumlah }
            if (totalRefillLiters > 0.0) {
                val eval = TorenWaterCalculator.evaluateWaterLoss(
                    startingStockLiters = 0.0,
                    totalRefillLiters = totalRefillLiters,
                    currentStockLiters = currentTankStockLiters.coerceAtLeast(0.0),
                    soldMineralGallons = totalMineralGallonsAllTime
                )
                safeFinite(eval.lossPercent, min = -100.0, max = 100.0)
            } else null
        } else {
            null
        }

        val perawatanEvals = if (supportsRo && perawatanItems.isNotEmpty()) {
            PerawatanScheduler.evaluateAndSort(perawatanItems, todayMillis = nowMillis, zone = zoneId)
        } else {
            emptyList()
        }
        val overduePerawatan = perawatanEvals.filter { it.status == PerawatanStatus.TERLAMBAT }
        val dueSoonPerawatan = perawatanEvals.filter {
            it.status == PerawatanStatus.HARI_INI || it.status == PerawatanStatus.MENDEKATI
        }

        val hasSufficientData = currentPeriod.hasPeriodActivity ||
            effectivePiutang > 0.0 ||
            overduePerawatan.isNotEmpty() ||
            (supportsMineral && currentTankStockLiters > 0.0)

        return AiBusinessSummary(
            totalOmzet30d = currentPeriod.totalOmzetPenjualan.coerceAtLeast(0.0),
            totalKasMasuk30d = currentPeriod.totalKasMasuk.coerceAtLeast(0.0),
            totalGalon30d = currentPeriod.totalGalon.coerceAtLeast(0),
            galonRo30d = currentPeriod.totalGalonRo.coerceAtLeast(0),
            galonMineral30d = currentPeriod.totalGalonMineral.coerceAtLeast(0),
            totalTransaksi30d = currentPeriod.filteredTx.size,
            activeCustomerCount = activeCustomers.size,
            kasbonCustomerCount = kasbonCustomerCount,
            totalPiutang = effectivePiutang.coerceAtLeast(0.0),
            totalPengeluaran30d = currentPeriod.totalPengeluaran.coerceAtLeast(0.0),
            topProducts = topProducts,
            topExpenseCategories = topExpenseCategories,
            depotType = normalizedDepotType,
            tankCapacityLiters = tankCapacityLiters.coerceAtLeast(0.0),
            currentTankStockLiters = currentTankStockLiters.coerceAtLeast(0.0),
            periodDays = periodDays,
            pelunasanKasbon = currentPeriod.pelunasanKasbonPeriode.coerceAtLeast(0.0),
            totalKomisiKurir = currentPeriod.totalKomisiKurir.coerceAtLeast(0.0),
            totalBebanKeluar = currentPeriod.totalBebanKeluar.coerceAtLeast(0.0),
            labaBersih = safeFinite(currentPeriod.labaBersih, min = -1_000_000_000.0, max = 1_000_000_000.0),
            marginLabaPersen = safeFinite(currentPeriod.marginLabaPersen, min = -999.9, max = 999.9),
            collectionRatioPercent = (currentPeriod.collectionRatio * 100.0).coerceIn(0.0, 100.0),
            deliveryGalonCount = deliveryGalon,
            pickupGalonCount = pickupGalon,
            transactingCustomerCount = transactingCustomers,
            overduePiutangOver30d = kasbonAging.bucketOver30Amount.coerceAtLeast(0.0),
            overduePiutangOver30Count = kasbonAging.bucketOver30Count,
            crmDueOrOverdueCount = crmDueOrOverdueCountOverride,
            crmOverdueCount = crmOverdueCountOverride,
            omzetGrowthPercent = omzetGrowth,
            galonGrowthPercent = galonGrowth,
            labaGrowthPercent = labaGrowth,
            hasComparisonBaseline = hasComparisonBaseline,
            mineralEstimatedDaysRemaining = mineralDaysRemaining,
            waterLossPercent = waterLossPercent,
            perawatanOverdueCount = overduePerawatan.size,
            perawatanDueSoonCount = dueSoonPerawatan.size,
            overduePerawatanNames = overduePerawatan.take(3).map { it.item.nama },
            hasSufficientData = hasSufficientData
        )
    }

    /**
     * Menyusun prompt Bahasa Indonesia yang kaya konteks keuangan & operasional tanpa data pribadi pelanggan (PII).
     */
    fun buildPrompt(summary: AiBusinessSummary): String {
        val topProductsText = if (summary.topProducts.isEmpty()) {
            "Belum ada data produk terjual"
        } else {
            summary.topProducts.joinToString(", ") { (name, qty) -> "$name ($qty galon)" }
        }

        val topExpensesText = if (summary.topExpenseCategories.isEmpty()) {
            "Belum ada pengeluaran tercatat"
        } else {
            summary.topExpenseCategories.joinToString(", ") { (cat, nominal) ->
                "$cat (${FormatUtils.formatRupiah(nominal)})"
            }
        }

        val tankStockText = if (summary.depotType.equals(DepotProfile.TYPE_RO, ignoreCase = true)) {
            "Depot sistem RO langsung (tidak memakai toren air mineral curah)"
        } else {
            val estGallons = TorenWaterCalculator.litersToEstimatedGallons(summary.currentTankStockLiters)
            val daysLeftText = summary.mineralEstimatedDaysRemaining?.let { days ->
                ", estimasi cukup ~${FormatUtils.formatPercent(days, 1).removeSuffix("%")} hari"
            }.orEmpty()
            val lossText = summary.waterLossPercent?.let { loss ->
                ", susut air kumulatif ${FormatUtils.formatPercent(loss, 1)}"
            }.orEmpty()
            "${FormatUtils.formatNumber(summary.currentTankStockLiters.toLong())} L dari kapasitas " +
                "${FormatUtils.formatNumber(summary.tankCapacityLiters.toLong())} L (~$estGallons galon$daysLeftText$lossText)"
        }

        val perawatanText = if (summary.depotType.equals(DepotProfile.TYPE_MINERAL, ignoreCase = true)) {
            "Tidak menggunakan mesin RO"
        } else {
            val overdueNames = if (summary.overduePerawatanNames.isNotEmpty()) {
                " (${summary.overduePerawatanNames.joinToString(", ")})"
            } else ""
            "Terlambat: ${summary.perawatanOverdueCount} item$overdueNames, Mendekati/Hari ini: ${summary.perawatanDueSoonCount} item"
        }

        val trendText = if (summary.hasComparisonBaseline) {
            "Omzet ${formatSignedGrowth(summary.omzetGrowthPercent)}, Galon ${formatSignedGrowth(summary.galonGrowthPercent)}, Laba ${formatSignedGrowth(summary.labaGrowthPercent)} vs ${summary.periodDays} hari sebelumnya"
        } else {
            "Belum ada data pembanding ${summary.periodDays} hari sebelumnya"
        }

        val dataBlock = buildString {
            appendLine("- Periode evaluasi: ${summary.periodDays} hari terakhir")
            appendLine("- Jenis layanan depot: ${summary.depotType}")
            appendLine("- Total omzet penjualan: ${FormatUtils.formatRupiah(summary.totalOmzet30d)}")
            appendLine("- Total kas masuk riil (tunai/DP + pelunasan kasbon): ${FormatUtils.formatRupiah(summary.totalKasMasuk30d)} (pelunasan kasbon: ${FormatUtils.formatRupiah(summary.pelunasanKasbon)})")
            appendLine("- Total beban keluar: ${FormatUtils.formatRupiah(summary.totalBebanKeluar)} (Pengeluaran ${FormatUtils.formatRupiah(summary.totalPengeluaran30d)} + Komisi kurir ${FormatUtils.formatRupiah(summary.totalKomisiKurir)})")
            appendLine("- Laba bersih riil: ${FormatUtils.formatRupiah(summary.labaBersih)} (margin kas ${FormatUtils.formatPercent(summary.marginLabaPersen, 1)}, rasio tertagih ${FormatUtils.formatPercent(summary.collectionRatioPercent, 0)})")
            appendLine("- Tren dibanding periode lalu: $trendText")
            appendLine("- Jumlah transaksi aktif: ${summary.totalTransaksi30d} nota")
            appendLine("- Total galon terjual: ${summary.totalGalon30d} galon (RO: ${summary.galonRo30d}, Mineral: ${summary.galonMineral30d})")
            appendLine("- Saluran penjualan: Antar kurir ${summary.deliveryGalonCount} galon vs Ambil sendiri ${summary.pickupGalonCount} galon")
            appendLine("- Pelanggan terdaftar: ${summary.activeCustomerCount} pelanggan (aktif transaksi periode ini: ${summary.transactingCustomerCount}, perlu follow-up isi ulang CRM: ${summary.crmDueOrOverdueCount})")
            appendLine("- Total piutang/kasbon aktif: ${FormatUtils.formatRupiah(summary.totalPiutang)} (${summary.kasbonCustomerCount} pelanggan; macet >30 hari: ${FormatUtils.formatRupiah(summary.overduePiutangOver30d)} dari ${summary.overduePiutangOver30Count} nota)")
            appendLine("- Pengeluaran utama: $topExpensesText")
            appendLine("- Produk terlaris: $topProductsText")
            appendLine("- Kondisi stok air toren: $tankStockText")
            append("- Status perawatan mesin RO: $perawatanText")
        }

        return """
            Anda adalah analis bisnis depot air. Berikan 3 insight singkat dan saran stok berdasarkan data penjualan berikut:
            $dataBlock

            Aturan output JSON:
            - Gunakan Bahasa Indonesia yang ringkas, lugas, dan mudah dipahami pemilik depot air minum.
            - Field "healthStatus" wajib salah satu dari: "SEHAT", "WASPADA", atau "KRITIS".
            - Field "insights" wajib berisi tepat 3 kalimat/poin insight bernilai tinggi (1 soal tren/laba, 1 soal kasbon/CRM pelanggan, 1 soal efisiensi kurir/beban) tanpa markdown atau nomor urut.
            - Field "stockAdvice" berisi 1 saran operasional konkret untuk stok air toren mineral dan/atau jadwal perawatan mesin RO.
            - Field "priorityActionTitle" berisi label CTA aksi terpenting maks 36 karakter (contoh: "Tagih Kasbon Macet" atau "Follow-up Pelanggan CRM").
            - Field "priorityActionTarget" wajib salah satu dari: "KASBON", "CRM", "STOK", "PERAWATAN", atau "LAPORAN".
        """.trimIndent()
    }

    /**
     * Menguraikan respons JSON terstruktur dari Gemini serta menormalkan panjang & enum agar kompatibel dengan Firestore Rules.
     */
    fun parseGeminiResponse(rawJson: String): ParsedAiInsightResponse {
        val trimmed = rawJson.trim()
        if (trimmed.isBlank()) {
            throw IllegalStateException("Respons analisis AI kosong.")
        }
        val cleanJson = if (trimmed.startsWith("```")) {
            trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        } else {
            trimmed
        }
        val json = JSONObject(cleanJson)
        val insightsArray = json.optJSONArray("insights")
        val parsedInsights = buildList {
            if (insightsArray != null) {
                for (i in 0 until insightsArray.length()) {
                    val item = insightsArray.optString(i, "")
                        .trim()
                        .removePrefix("•")
                        .removePrefix("-")
                        .trim()
                        .take(MAX_INSIGHT_LENGTH)
                    if (item.isNotBlank()) {
                        add(item)
                    }
                    if (size >= MAX_INSIGHTS_COUNT) break
                }
            }
        }
        val stockAdvice = json.optString("stockAdvice", "")
            .trim()
            .take(MAX_STOCK_ADVICE_LENGTH)

        if (parsedInsights.isEmpty() || stockAdvice.isBlank()) {
            throw IllegalStateException("Format hasil analisis AI tidak lengkap.")
        }

        val healthStatus = AiBusinessInsight.normalizeHealthStatus(json.optString("healthStatus", ""))
        val priorityActionTarget = AiBusinessInsight.normalizePriorityTarget(json.optString("priorityActionTarget", ""))
        val rawTitle = json.optString("priorityActionTitle", "").trim().take(MAX_ACTION_TITLE_LENGTH)
        val priorityActionTitle = rawTitle.ifBlank { defaultActionTitleForTarget(priorityActionTarget) }

        return ParsedAiInsightResponse(
            insights = parsedInsights,
            stockAdvice = stockAdvice,
            healthStatus = healthStatus,
            priorityActionTitle = priorityActionTitle,
            priorityActionTarget = priorityActionTarget
        )
    }

    /**
     * Memastikan pilihan periode selalu salah satu dari 7, 30, atau 90 hari.
     */
    fun sanitizePeriodDays(periodDays: Int): Int {
        return when {
            periodDays <= 7 -> 7
            periodDays >= 90 -> 90
            else -> 30
        }
    }

    /**
     * Menghasilkan label tombol aksi default bila model AI tidak mengisi judul aksi.
     */
    fun defaultActionTitleForTarget(target: String): String {
        return when (AiBusinessInsight.normalizePriorityTarget(target)) {
            AiBusinessInsight.TARGET_KASBON -> "Kelola & Tagih Kasbon"
            AiBusinessInsight.TARGET_CRM -> "Follow-up Jadwal Pelanggan"
            AiBusinessInsight.TARGET_STOK -> "Catat Pengisian Air Toren"
            AiBusinessInsight.TARGET_PERAWATAN -> "Cek Jadwal Perawatan RO"
            else -> "Buka Laporan Keuangan"
        }
    }

    /**
     * Memformat ringkasan AI menjadi teks rapi siap salin untuk WhatsApp/catatan pemilik.
     */
    fun buildShareableText(insight: AiBusinessInsight, depotName: String): String {
        val headerDepot = depotName.trim().ifBlank { DepotProfile.DEFAULT_DEPOT_NAME }
        val healthLabel = when (insight.normalizedHealthStatus()) {
            AiBusinessInsight.HEALTH_KRITIS -> "KRITIS"
            AiBusinessInsight.HEALTH_WASPADA -> "WASPADA"
            else -> "SEHAT"
        }
        return buildString {
            appendLine("*Ringkasan Bisnis AI — $headerDepot (${insight.periodDays} Hari)*")
            appendLine("Status Kesehatan: *$healthLabel*")
            appendLine("• Omzet: ${FormatUtils.formatRupiah(insight.totalOmzet)} (${formatSignedGrowth(insight.omzetGrowthPercent)})")
            appendLine("• Laba Bersih: ${FormatUtils.formatRupiah(insight.labaBersih)} (${formatSignedGrowth(insight.labaGrowthPercent)})")
            appendLine("• Galon Terjual: ${FormatUtils.formatNumber(insight.totalGalon)} gln (${formatSignedGrowth(insight.galonGrowthPercent)})")
            appendLine("• Piutang Aktif: ${FormatUtils.formatRupiah(insight.totalPiutang)}")
            appendLine()
            appendLine("*3 Insight Utama:*")
            insight.insights.forEachIndexed { idx, line ->
                appendLine("${idx + 1}. $line")
            }
            appendLine()
            appendLine("*Saran Stok & Operasional:*")
            appendLine(insight.stockAdvice)
            val updatedMillis = insight.timestamp?.toDate()?.time
            if (updatedMillis != null && updatedMillis > 0L) {
                appendLine()
                append("Diperbarui: ${FormatUtils.formatDateTimeUi(updatedMillis)}")
            }
        }.trim()
    }

    private fun formatSignedGrowth(value: Double): String {
        val clean = safeFinite(value, min = -999.9, max = 999.9)
        val rounded = (clean * 10.0).roundToInt() / 10.0
        val prefix = if (rounded > 0.0) "+" else ""
        return "$prefix${FormatUtils.formatPercent(rounded, 1)}"
    }

    private fun safeFinite(value: Double, min: Double = -9999.0, max: Double = 9999.0): Double {
        return if (value.isFinite()) value.coerceIn(min, max) else 0.0
    }
}
