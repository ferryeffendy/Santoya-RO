package com.example.util

import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Representasi pelunasan kasbon yang telah dipasangkan dengan nama pelanggan aktif/tercatat.
 */
data class KasbonLogDetail(
    val log: KasbonLog,
    val namaPelanggan: String
)

/**
 * Rincian pengeluaran berdasarkan kategori dalam suatu periode laporan.
 */
data class ExpenseCategorySummary(
    val kategori: String,
    val count: Int,
    val nominal: Double,
    val sharePercent: Double
)

/**
 * Rincian penjualan berdasarkan varian produk dalam suatu periode laporan.
 */
data class ProductSalesSummary(
    val namaProduk: String,
    val totalGalon: Int,
    val totalTransaksi: Int,
    val totalOmzet: Double,
    val sharePercent: Double
)

/**
 * Satu titik data tren keuangan (harian atau bulanan) untuk grafik Laporan Keuangan.
 */
data class FinancialTrendPoint(
    val bucketKey: String,
    val label: String,
    val fullDateLabel: String,
    val totalGalon: Int,
    val totalOmzet: Double,
    val kasMasuk: Double,
    val kasbonMasuk: Double,
    val bebanKeluar: Double,
    val labaBersih: Double
)

/**
 * Ringkasan kesehatan piutang (umur kasbon aktif) lintas seluruh pelanggan.
 */
data class KasbonAgingSummary(
    val totalActiveNotes: Int,
    val totalActiveReceivable: Double,
    val bucket0To7Count: Int,
    val bucket0To7Amount: Double,
    val bucket8To30Count: Int,
    val bucket8To30Amount: Double,
    val bucketOver30Count: Int,
    val bucketOver30Amount: Double
)

/**
 * Ringkasan proyeksi akhir bulan yang dihitung dengan rumus arus kas yang sama persis dengan Laporan & Dashboard.
 */
data class MonthlyProjectionSummary(
    val monthGalon: Int,
    val projectedGalon: Int,
    val monthKasMasuk: Double,
    val monthNetProfit: Double,
    val projectedProfit: Double
)

/**
 * Perbandingan kas masuk bulan berjalan dengan bulan sebelumnya menggunakan sumber data yang seragam.
 */
data class MonthlyComparisonSummary(
    val bulanIniKasMasuk: Double,
    val bulanLaluKasMasuk: Double
)

/**
 * Ringkasan metrik keuangan lengkap untuk suatu rentang waktu laporan.
 * Menjadi Single Source of Truth bagi DashboardScreen, LaporanScreen, Proyeksi, Ekspor PDF, dan Ekspor CSV.
 */
data class LaporanPeriodSummary(
    val startTime: Long,
    val endTime: Long,
    val filteredTx: List<TransaksiDetail>,
    val filteredExp: List<Pengeluaran>,
    val filteredKasbonLogs: List<KasbonLogDetail>,
    val totalGalon: Int,
    val totalGalonRo: Int = 0,
    val totalGalonMineral: Int = 0,
    val totalOmzetPenjualan: Double,
    val penjualanTunaiLangsung: Double,
    val pelunasanKasbonPeriode: Double,
    val totalKasMasuk: Double,
    val piutangKasbonPeriode: Double,
    val totalKomisiKurir: Double,
    val totalPengeluaran: Double,
    val totalBebanKeluar: Double,
    val labaBersih: Double,
    val marginLabaPersen: Double,
    val lunasCount: Int,
    val dpCount: Int,
    val fullKasbonCount: Int,
    val collectionRatio: Float,
    val expenseRatioFraction: Float,
    val expenseRatioActualPercent: Double,
    val kasTunaiDariJual: Double,
    val kasTunaiDariKasbon: Double,
    val kasTransferDariJual: Double,
    val kasTransferDariKasbon: Double,
    val totalKasTunai: Double,
    val totalKasNonTunai: Double,
    val hasPeriodActivity: Boolean,
    val expenseCategories: List<ExpenseCategorySummary>,
    val productBreakdown: List<ProductSalesSummary>,
    val desaEntries: List<RankingRawEntry>,
    val kurirEntries: List<RankingRawEntry>,
    val trendPoints: List<FinancialTrendPoint>,
    val isMonthlyTrendGranularity: Boolean
)

/**
 * Mesin kalkulasi keuangan terpusat untuk Santoya RO.
 * Menjamin konsistensi 100% antara Dashboard, Laporan, Proyeksi, Badge Bulanan, PDF, dan CSV,
 * serta mencegah perhitungan ganda (double-counting) antara pembayaran awal nota (`uangMuka`)
 * dengan pelunasan/cicilan berikutnya (`KasbonLog`).
 */
object LaporanFinanceCalculator {

    private val INDONESIAN_LOCALE: Locale = Locale.forLanguageTag("id-ID")
    private val NOTA_REF_REGEX = Regex("(?:Nota\\s+#|,\\s*#)(\\d+)", RegexOption.IGNORE_CASE)
    private val DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM", INDONESIAN_LOCALE)
    private val DAY_FULL_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", INDONESIAN_LOCALE)
    private val MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yy", INDONESIAN_LOCALE)
    private val MONTH_FULL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", INDONESIAN_LOCALE)

    /**
     * Menghitung uang tunai/DP murni yang diterima pada saat transaksi dibuat (`tx.tanggal`),
     * dengan memisahkan seluruh pelunasan/cicilan lanjutan yang tercatat di [allKasbonLogs].
     */
    fun computeInitialCashByTxId(
        allActiveTransaksi: List<TransaksiDetail>,
        allKasbonLogs: List<KasbonLog>
    ): Map<Int, Double> {
        if (allActiveTransaksi.isEmpty()) return emptyMap()

        val logsByCustomer = allKasbonLogs
            .filter { it.jumlah_bayar > 0.0 }
            .groupBy { it.id_pelanggan }

        val result = HashMap<Int, Double>(allActiveTransaksi.size)
        val txsByCustomer = allActiveTransaksi.groupBy { it.transaksi.id_pelanggan }

        txsByCustomer.forEach { (customerId, customerDetails) ->
            val customerLogs = logsByCustomer[customerId].orEmpty()
                .sortedWith(compareBy<KasbonLog> { it.tanggal }.thenBy { it.id })

            val explicitlyReferencedIds = customerLogs.flatMapTo(mutableSetOf()) { extractReferencedNotaIds(it.catatan) }
            val candidateNotes = mutableListOf<Transaksi>()

            customerDetails.forEach { detail ->
                val tx = detail.transaksi
                val isImmediateLunas = tx.status.equals("Lunas", ignoreCase = true) &&
                    (tx.tanggalBayar == null || tx.tanggalBayar == tx.tanggal) &&
                    tx.id !in explicitlyReferencedIds

                if (isImmediateLunas) {
                    result[tx.id] = tx.total_harga.coerceAtLeast(0.0)
                } else {
                    candidateNotes.add(tx)
                }
            }

            if (candidateNotes.isEmpty()) return@forEach

            val recordedPaidById = HashMap<Int, Double>(candidateNotes.size)
            candidateNotes.forEach { tx ->
                val recordedPaid = if (tx.status.equals("Lunas", ignoreCase = true)) {
                    tx.total_harga.coerceAtLeast(0.0)
                } else {
                    val paidFromRemaining = (tx.total_harga - tx.remainingKasbonAmount()).coerceAtLeast(0.0)
                    maxOf(tx.uangMuka.toDouble(), paidFromRemaining).coerceIn(0.0, tx.total_harga.coerceAtLeast(0.0))
                }
                recordedPaidById[tx.id] = recordedPaid
            }

            val candidateById = candidateNotes.associateBy { it.id }
            val sortedCandidates = candidateNotes.sortedWith(compareBy<Transaksi> { it.tanggal }.thenBy { it.id })
            val unreferencedLogs = mutableListOf<KasbonLog>()

            // Tahap 1: Alokasikan log yang eksplisit menyebut ID nota pada perangkat yang sama
            customerLogs.forEach { log ->
                val matchedNotes = extractReferencedNotaIds(log.catatan)
                    .mapNotNull { candidateById[it] }
                    .distinctBy { it.id }
                    .sortedWith(compareBy<Transaksi> { it.tanggal }.thenBy { it.id })

                if (matchedNotes.isEmpty()) {
                    unreferencedLogs.add(log)
                } else {
                    var remainingLogAmount = log.jumlah_bayar
                    for (note in matchedNotes) {
                        if (remainingLogAmount <= 0.0001) break
                        val currentAvail = recordedPaidById[note.id] ?: 0.0
                        if (currentAvail > 0.0) {
                            val deducted = minOf(currentAvail, remainingLogAmount)
                            recordedPaidById[note.id] = (currentAvail - deducted).coerceAtLeast(0.0)
                            remainingLogAmount -= deducted
                        }
                    }
                }
            }

            // Tahap 2: Alokasikan sisa log (misal hasil sinkronisasi lintas perangkat di mana local ID berbeda)
            // secara kronologis FIFO terhadap nota yang dibuat sebelum/sekitar waktu pelunasan
            unreferencedLogs.forEach { log ->
                var remainingLogAmount = log.jumlah_bayar
                val eligibleNotes = sortedCandidates.filter { it.tanggal <= log.tanggal + 60_000L }
                val targetPool = if (eligibleNotes.isNotEmpty()) eligibleNotes else sortedCandidates

                for (note in targetPool) {
                    if (remainingLogAmount <= 0.0001) break
                    val currentAvail = recordedPaidById[note.id] ?: 0.0
                    if (currentAvail > 0.0) {
                        val deducted = minOf(currentAvail, remainingLogAmount)
                        recordedPaidById[note.id] = (currentAvail - deducted).coerceAtLeast(0.0)
                        remainingLogAmount -= deducted
                    }
                }
            }

            candidateNotes.forEach { tx ->
                result[tx.id] = (recordedPaidById[tx.id] ?: 0.0).coerceAtLeast(0.0)
            }
        }

        return result
    }

    /**
     * Menghitung ringkasan lengkap laporan keuangan untuk periode [startTime]..[endTime].
     */
    fun summarizePeriod(
        allTransaksi: List<TransaksiDetail>,
        allPengeluaran: List<Pengeluaran>,
        allKasbonLogs: List<KasbonLog>,
        allPelanggan: List<Pelanggan> = emptyList(),
        startTime: Long,
        endTime: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        precomputedInitialCashByTxId: Map<Int, Double>? = null
    ): LaporanPeriodSummary {
        val activeAllTx = allTransaksi.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        val initialCashByTxId = precomputedInitialCashByTxId ?: computeInitialCashByTxId(activeAllTx, allKasbonLogs)

        val filteredTx = activeAllTx
            .filter { it.transaksi.tanggal in startTime..endTime }
            .sortedWith(compareByDescending<TransaksiDetail> { it.transaksi.tanggal }.thenByDescending { it.transaksi.id })

        val filteredExp = allPengeluaran
            .filter { it.tanggal in startTime..endTime }
            .sortedWith(compareByDescending<Pengeluaran> { it.tanggal }.thenByDescending { it.id })

        val customerNameById = buildCustomerNameMap(allPelanggan, activeAllTx)
        val filteredLogsRaw = allKasbonLogs
            .filter { it.tanggal in startTime..endTime && it.jumlah_bayar > 0.0 }
            .sortedWith(compareByDescending<KasbonLog> { it.tanggal }.thenByDescending { it.id })

        val filteredKasbonLogs = filteredLogsRaw.map { log ->
            KasbonLogDetail(
                log = log,
                namaPelanggan = customerNameById[log.id_pelanggan] ?: "Pelanggan"
            )
        }

        val totalGalon = filteredTx.sumOf { it.transaksi.jumlah }
        val totalGalonMineral = filteredTx
            .filter {
                it.transaksi.waterType.equals(Transaksi.WATER_TYPE_MINERAL, ignoreCase = true) ||
                    it.produk?.nama_produk?.contains("mineral", ignoreCase = true) == true
            }
            .sumOf { it.transaksi.jumlah }
        val totalGalonRo = (totalGalon - totalGalonMineral).coerceAtLeast(0)
        val totalOmzetPenjualan = filteredTx.sumOf { it.transaksi.total_harga }

        var penjualanTunaiLangsung = 0.0
        var kasTunaiDariJual = 0.0
        var kasTransferDariJual = 0.0

        filteredTx.forEach { detail ->
            val tx = detail.transaksi
            val initialCash = (initialCashByTxId[tx.id] ?: 0.0).coerceAtLeast(0.0)
            penjualanTunaiLangsung += initialCash
            if (initialCash > 0.0) {
                if (isNonCashPaymentMethod(tx.metode_bayar)) {
                    kasTransferDariJual += initialCash
                } else {
                    kasTunaiDariJual += initialCash
                }
            }
        }

        var pelunasanKasbonPeriode = 0.0
        var kasTunaiDariKasbon = 0.0
        var kasTransferDariKasbon = 0.0

        filteredLogsRaw.forEach { log ->
            val amount = log.jumlah_bayar.coerceAtLeast(0.0)
            pelunasanKasbonPeriode += amount
            if (isNonCashPaymentMethod(log.metode_bayar)) {
                kasTransferDariKasbon += amount
            } else {
                kasTunaiDariKasbon += amount
            }
        }

        val totalKasMasuk = penjualanTunaiLangsung + pelunasanKasbonPeriode
        val totalKasTunai = kasTunaiDariJual + kasTunaiDariKasbon
        val totalKasNonTunai = kasTransferDariJual + kasTransferDariKasbon

        val piutangKasbonPeriode = filteredTx
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .sumOf { it.transaksi.remainingKasbonAmount() }

        val totalKomisiKurir = filteredTx.sumOf { it.transaksi.total_komisi }
        val totalPengeluaran = filteredExp.sumOf { it.nominal }
        val totalBebanKeluar = totalKomisiKurir + totalPengeluaran
        val labaBersih = totalKasMasuk - totalBebanKeluar

        val marginLabaPersen = if (totalKasMasuk > 0.0) {
            (labaBersih / totalKasMasuk) * 100.0
        } else {
            0.0
        }

        val lunasCount = filteredTx.count { it.transaksi.status.equals("Lunas", ignoreCase = true) }
        val dpCount = filteredTx.count {
            it.transaksi.status.equals("Kasbon", ignoreCase = true) &&
                ((initialCashByTxId[it.transaksi.id] ?: 0.0) > 0.0 || it.transaksi.uangMuka > 0L)
        }
        val fullKasbonCount = filteredTx.count {
            it.transaksi.status.equals("Kasbon", ignoreCase = true) &&
                (initialCashByTxId[it.transaksi.id] ?: 0.0) <= 0.0 &&
                it.transaksi.uangMuka <= 0L
        }

        // Rasio tertagih khusus untuk omzet penjualan periode ini (tidak terdistorsi pelunasan hutang lama)
        val collectionRatio = when {
            totalOmzetPenjualan > 0.0 -> {
                val settledOfPeriod = (totalOmzetPenjualan - piutangKasbonPeriode).coerceAtLeast(0.0)
                (settledOfPeriod / totalOmzetPenjualan).toFloat().coerceIn(0f, 1f)
            }
            pelunasanKasbonPeriode > 0.0 -> 1f
            else -> 0f
        }

        val expenseRatioActualPercent = when {
            totalKasMasuk > 0.0 -> (totalBebanKeluar / totalKasMasuk) * 100.0
            totalBebanKeluar > 0.0 -> 100.0
            else -> 0.0
        }
        val expenseRatioFraction = when {
            totalKasMasuk > 0.0 -> (totalBebanKeluar / totalKasMasuk).toFloat().coerceIn(0f, 1f)
            totalBebanKeluar > 0.0 -> 1f
            else -> 0f
        }

        val expenseCategories = buildExpenseCategoryBreakdown(filteredExp, totalPengeluaran)
        val productBreakdown = buildProductSalesBreakdown(filteredTx, totalOmzetPenjualan)
        val desaEntries = buildDesaRankingEntries(filteredTx)
        val kurirEntries = buildKurirRankingEntries(filteredTx)

        val (trendPoints, isMonthlyTrend) = buildTrendPoints(
            filteredTx = filteredTx,
            filteredExp = filteredExp,
            filteredLogs = filteredLogsRaw,
            initialCashByTxId = initialCashByTxId,
            startTime = startTime,
            endTime = endTime,
            zoneId = zoneId
        )

        val hasPeriodActivity = filteredTx.isNotEmpty() || filteredExp.isNotEmpty() || filteredLogsRaw.isNotEmpty()

        return LaporanPeriodSummary(
            startTime = startTime,
            endTime = endTime,
            filteredTx = filteredTx,
            filteredExp = filteredExp,
            filteredKasbonLogs = filteredKasbonLogs,
            totalGalon = totalGalon,
            totalGalonRo = totalGalonRo,
            totalGalonMineral = totalGalonMineral,
            totalOmzetPenjualan = totalOmzetPenjualan,
            penjualanTunaiLangsung = penjualanTunaiLangsung,
            pelunasanKasbonPeriode = pelunasanKasbonPeriode,
            totalKasMasuk = totalKasMasuk,
            piutangKasbonPeriode = piutangKasbonPeriode,
            totalKomisiKurir = totalKomisiKurir,
            totalPengeluaran = totalPengeluaran,
            totalBebanKeluar = totalBebanKeluar,
            labaBersih = labaBersih,
            marginLabaPersen = marginLabaPersen,
            lunasCount = lunasCount,
            dpCount = dpCount,
            fullKasbonCount = fullKasbonCount,
            collectionRatio = collectionRatio,
            expenseRatioFraction = expenseRatioFraction,
            expenseRatioActualPercent = expenseRatioActualPercent,
            kasTunaiDariJual = kasTunaiDariJual,
            kasTunaiDariKasbon = kasTunaiDariKasbon,
            kasTransferDariJual = kasTransferDariJual,
            kasTransferDariKasbon = kasTransferDariKasbon,
            totalKasTunai = totalKasTunai,
            totalKasNonTunai = totalKasNonTunai,
            hasPeriodActivity = hasPeriodActivity,
            expenseCategories = expenseCategories,
            productBreakdown = productBreakdown,
            desaEntries = desaEntries,
            kurirEntries = kurirEntries,
            trendPoints = trendPoints,
            isMonthlyTrendGranularity = isMonthlyTrend
        )
    }

    /**
     * Menghitung perbandingan kas masuk bulan ini vs bulan lalu dengan formula arus kas yang seragam.
     */
    fun calculateMonthlyComparison(
        allTransaksi: List<TransaksiDetail>,
        allPengeluaran: List<Pengeluaran>,
        allKasbonLogs: List<KasbonLog>,
        allPelanggan: List<Pelanggan> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyComparisonSummary {
        val currentYm = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        val previousYm = currentYm.minusMonths(1)

        val currStart = currentYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currEnd = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        val prevStart = previousYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val prevEnd = previousYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        val activeAllTx = allTransaksi.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        val initialCashMap = computeInitialCashByTxId(activeAllTx, allKasbonLogs)

        val currSummary = summarizePeriod(
            allTransaksi = activeAllTx,
            allPengeluaran = allPengeluaran,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            startTime = currStart,
            endTime = currEnd,
            zoneId = zoneId,
            precomputedInitialCashByTxId = initialCashMap
        )
        val prevSummary = summarizePeriod(
            allTransaksi = activeAllTx,
            allPengeluaran = allPengeluaran,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            startTime = prevStart,
            endTime = prevEnd,
            zoneId = zoneId,
            precomputedInitialCashByTxId = initialCashMap
        )

        return MonthlyComparisonSummary(
            bulanIniKasMasuk = currSummary.totalKasMasuk,
            bulanLaluKasMasuk = prevSummary.totalKasMasuk
        )
    }

    /**
     * Menghitung proyeksi run-rate akhir bulan menggunakan kas masuk & laba bersih riil bulan berjalan.
     */
    fun calculateMonthlyProjection(
        allTransaksi: List<TransaksiDetail>,
        allPengeluaran: List<Pengeluaran>,
        allKasbonLogs: List<KasbonLog>,
        allPelanggan: List<Pelanggan> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyProjectionSummary {
        val todayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val ym = YearMonth.from(todayDate)
        val daysInMonth = ym.lengthOfMonth().coerceAtLeast(1)
        val currentDay = todayDate.dayOfMonth.coerceIn(1, daysInMonth)
        val remainingDays = (daysInMonth - currentDay).coerceAtLeast(0)

        val monthStart = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        val monthSummary = summarizePeriod(
            allTransaksi = allTransaksi,
            allPengeluaran = allPengeluaran,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            startTime = monthStart,
            endTime = monthEnd,
            zoneId = zoneId
        )

        val avgGalon = monthSummary.totalGalon.toDouble() / currentDay.toDouble()
        val projectedGalon = (monthSummary.totalGalon + (avgGalon * remainingDays)).toInt()
        val avgProfit = monthSummary.labaBersih / currentDay.toDouble()
        val projectedProfit = monthSummary.labaBersih + (avgProfit * remainingDays)

        return MonthlyProjectionSummary(
            monthGalon = monthSummary.totalGalon,
            projectedGalon = projectedGalon,
            monthKasMasuk = monthSummary.totalKasMasuk,
            monthNetProfit = monthSummary.labaBersih,
            projectedProfit = projectedProfit
        )
    }

    /**
     * Menghitung rincian umur piutang kasbon aktif di seluruh depot (0–7 hari, 8–30 hari, > 30 hari).
     */
    fun calculateKasbonAging(
        allTransaksi: List<TransaksiDetail>,
        allPelanggan: List<Pelanggan>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): KasbonAgingSummary {
        val activeUnpaidNotes = allTransaksi
            .asSequence()
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .map { it.transaksi to it.transaksi.remainingKasbonAmount() }
            .filter { (_, sisa) -> sisa > 0.0 }
            .toList()

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        var b0Count = 0
        var b0Amount = 0.0
        var b8Count = 0
        var b8Amount = 0.0
        var b30Count = 0
        var b30Amount = 0.0

        activeUnpaidNotes.forEach { (tx, sisa) ->
            val txDate = Instant.ofEpochMilli(tx.tanggal).atZone(zoneId).toLocalDate()
            val ageDays = ChronoUnit.DAYS.between(txDate, today).coerceAtLeast(0L)
            when {
                ageDays <= 7L -> {
                    b0Count++
                    b0Amount += sisa
                }
                ageDays <= 30L -> {
                    b8Count++
                    b8Amount += sisa
                }
                else -> {
                    b30Count++
                    b30Amount += sisa
                }
            }
        }

        val totalFromPelanggan = allPelanggan
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) && it.sisa_kasbon > 0.0 }
            .sumOf { it.sisa_kasbon }
        val totalFromNotes = b0Amount + b8Amount + b30Amount
        val effectiveTotal = if (totalFromPelanggan > 0.0) totalFromPelanggan else totalFromNotes

        return KasbonAgingSummary(
            totalActiveNotes = activeUnpaidNotes.size,
            totalActiveReceivable = effectiveTotal,
            bucket0To7Count = b0Count,
            bucket0To7Amount = b0Amount,
            bucket8To30Count = b8Count,
            bucket8To30Amount = b8Amount,
            bucketOver30Count = b30Count,
            bucketOver30Amount = b30Amount
        )
    }

    /**
     * Menghasilkan baris ringkasan harian (atau bulanan jika rentang > 45 hari) yang jumlah kolomnya
     * konsisten 100% dengan ringkasan [summary] (memasukkan pelunasan kasbon, komisi, pengeluaran, dan laba).
     */
    fun buildDailySummaryRows(
        summary: LaporanPeriodSummary,
        allTransaksi: List<TransaksiDetail>,
        allKasbonLogs: List<KasbonLog>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<FinancialTrendPoint> {
        val activeAllTx = allTransaksi.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        val initialCashMap = computeInitialCashByTxId(activeAllTx, allKasbonLogs)
        val filteredLogs = summary.filteredKasbonLogs.map { it.log }
        return buildTrendPoints(
            filteredTx = summary.filteredTx,
            filteredExp = summary.filteredExp,
            filteredLogs = filteredLogs,
            initialCashByTxId = initialCashMap,
            startTime = summary.startTime,
            endTime = summary.endTime,
            zoneId = zoneId,
            forceDaily = true
        ).first
    }

    /**
     * Menormalisasi nama desa/wilayah agar pengelompokan tidak terpecah akibat huruf besar/kecil,
     * nomor rumah, atau penanda placeholder pelanggan terhapus.
     */
    fun normalizeDesaName(rawAlamat: String?): String {
        val cleanDeletedPrefix = KaryawanScopeHelper.cleanPelangganAlamat(rawAlamat ?: "")
        if (cleanDeletedPrefix.isBlank() || cleanDeletedPrefix.trim() == "-") {
            return "Umum / Santoya"
        }
        val stripped = cleanDeletedPrefix.trim()
            .replace(Regex("(?i)^(jl\\.?\\s*raya|jalan\\s*raya|jl\\.?|jalan|desa|ds\\.?|kp\\.?|kampung|kel\\.?|kelurahan|dusun|dsn\\.?|perum\\.?|perumahan)\\s+"), "")
            .replace(Regex("(?i)\\s+(no\\.?|nomor|blok|rt|rw|#)\\s*[0-9a-zA-Z/.-]+.*$"), "")
            .replace(Regex("\\s+#?[0-9]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (stripped.isBlank() || stripped == "-" || stripped.equals("umum", ignoreCase = true)) {
            return "Umum / Santoya"
        }

        return stripped.split(" ").joinToString(" ") { word ->
            val lower = word.lowercase(INDONESIAN_LOCALE)
            lower.replaceFirstChar { ch -> ch.titlecase(INDONESIAN_LOCALE) }
        }
    }

    /**
     * Menentukan apakah label metode bayar termasuk pembayaran non-tunai (Transfer/QRIS/Bank/E-Wallet).
     */
    fun isNonCashPaymentMethod(metode: String?): Boolean {
        val norm = metode?.trim()?.lowercase(INDONESIAN_LOCALE).orEmpty()
        if (norm.isBlank() || norm == "-" || norm == "cash" || norm == "tunai" || norm == "bayar sebagian") {
            return false
        }
        return norm.contains("transfer") ||
            norm.contains("qris") ||
            norm.contains("bank") ||
            norm.contains("bca") ||
            norm.contains("bri") ||
            norm.contains("bni") ||
            norm.contains("mandiri") ||
            norm.contains("dana") ||
            norm.contains("ovo") ||
            norm.contains("gopay") ||
            norm.contains("non")
    }

    /**
     * Meng-escape teks bebas agar aman disisipkan ke tabel HTML laporan PDF.
     */
    fun escapeHtml(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val sb = StringBuilder(text.length + 16)
        text.forEach { ch ->
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Meng-escape satu sel CSV sesuai standar RFC 4180 dan menetralkan pemicu rumus spreadsheet
     * (`=`, `+`, `@`, `\t`, `\r`, atau `-` formula) agar aman dari CSV Formula Injection
     * saat dibuka di Microsoft Excel & Google Sheets.
     */
    fun escapeCsvField(raw: String?): String {
        val rawValue = raw.orEmpty()
        val sanitized = if (isCsvFormulaPayload(rawValue)) {
            "'$rawValue"
        } else {
            rawValue
        }
        val needsQuotes = sanitized.contains(',') || sanitized.contains('"') || sanitized.contains('\n') || sanitized.contains('\r')
        return if (needsQuotes) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    private fun isCsvFormulaPayload(value: String): Boolean {
        if (value.isEmpty()) return false
        val first = value[0]
        if (first == '=' || first == '+' || first == '@' || first == '\t' || first == '\r') {
            return true
        }
        if (first == '-' && value.length > 1) {
            val rest = value.substring(1).trim()
            val isPlainNegativeNumber = rest.isNotEmpty() && rest.all { it.isDigit() || it == '.' || it == ',' || it == '%' }
            if (!isPlainNegativeNumber) return true
        }
        return false
    }

    private fun extractReferencedNotaIds(catatan: String?): List<Int> {
        if (catatan.isNullOrBlank()) return emptyList()
        return NOTA_REF_REGEX.findAll(catatan)
            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
            .toList()
    }

    private fun buildCustomerNameMap(
        allPelanggan: List<Pelanggan>,
        allTransaksi: List<TransaksiDetail>
    ): Map<Int, String> {
        val map = HashMap<Int, String>()
        allPelanggan.forEach { p ->
            val clean = p.nama.trim()
            if (clean.isNotBlank()) {
                map[p.id] = clean
            }
        }
        allTransaksi.forEach { d ->
            val cust = d.pelanggan ?: return@forEach
            if (!map.containsKey(cust.id)) {
                val clean = cust.nama.trim()
                if (clean.isNotBlank()) {
                    map[cust.id] = clean
                }
            }
        }
        return map
    }

    private fun buildExpenseCategoryBreakdown(
        filteredExp: List<Pengeluaran>,
        totalPengeluaran: Double
    ): List<ExpenseCategorySummary> {
        if (filteredExp.isEmpty()) return emptyList()
        return filteredExp
            .groupBy { it.kategori.trim().ifBlank { "Lainnya" } }
            .map { (kategori, items) ->
                val sum = items.sumOf { it.nominal }
                val share = if (totalPengeluaran > 0.0) ((sum / totalPengeluaran) * 100.0).coerceIn(0.0, 100.0) else 0.0
                ExpenseCategorySummary(
                    kategori = kategori,
                    count = items.size,
                    nominal = sum,
                    sharePercent = share
                )
            }
            .sortedByDescending { it.nominal }
    }

    private fun buildProductSalesBreakdown(
        filteredTx: List<TransaksiDetail>,
        totalOmzetPenjualan: Double
    ): List<ProductSalesSummary> {
        if (filteredTx.isEmpty()) return emptyList()
        return filteredTx
            .groupBy { it.produk?.nama_produk?.trim()?.ifBlank { "Galon RO" } ?: "Galon RO" }
            .map { (namaProduk, list) ->
                val sumOmzet = list.sumOf { it.transaksi.total_harga }
                val share = if (totalOmzetPenjualan > 0.0) ((sumOmzet / totalOmzetPenjualan) * 100.0).coerceIn(0.0, 100.0) else 0.0
                ProductSalesSummary(
                    namaProduk = namaProduk,
                    totalGalon = list.sumOf { it.transaksi.jumlah },
                    totalTransaksi = list.size,
                    totalOmzet = sumOmzet,
                    sharePercent = share
                )
            }
            .sortedWith(compareByDescending<ProductSalesSummary> { it.totalGalon }.thenByDescending { it.totalOmzet })
    }

    private fun buildDesaRankingEntries(filteredTx: List<TransaksiDetail>): List<RankingRawEntry> {
        return filteredTx
            .groupBy { tx -> normalizeDesaName(tx.pelanggan?.alamat) }
            .map { (desa, list) ->
                RankingRawEntry(
                    title = desa,
                    totalGalon = list.sumOf { it.transaksi.jumlah },
                    totalTransaksi = list.size,
                    totalPendapatan = list.sumOf { it.transaksi.total_harga }
                )
            }
    }

    private fun buildKurirRankingEntries(filteredTx: List<TransaksiDetail>): List<RankingRawEntry> {
        return filteredTx
            .groupBy { it.kurir?.nama?.trim()?.takeIf { name -> name.isNotBlank() && !name.equals("Unknown", ignoreCase = true) } ?: "Ambil Sendiri" }
            .map { (kurir, list) ->
                RankingRawEntry(
                    title = kurir,
                    totalGalon = list.sumOf { it.transaksi.jumlah },
                    totalTransaksi = list.size,
                    totalPendapatan = list.sumOf { it.transaksi.total_harga },
                    totalKomisi = list.sumOf { it.transaksi.total_komisi }
                )
            }
    }

    private fun buildTrendPoints(
        filteredTx: List<TransaksiDetail>,
        filteredExp: List<Pengeluaran>,
        filteredLogs: List<KasbonLog>,
        initialCashByTxId: Map<Int, Double>,
        startTime: Long,
        endTime: Long,
        zoneId: ZoneId,
        forceDaily: Boolean = false
    ): Pair<List<FinancialTrendPoint>, Boolean> {
        val timestamps = mutableListOf<Long>()
        filteredTx.forEach { timestamps.add(it.transaksi.tanggal) }
        filteredExp.forEach { timestamps.add(it.tanggal) }
        filteredLogs.forEach { timestamps.add(it.tanggal) }

        if (timestamps.isEmpty()) return Pair(emptyList(), false)

        val effectiveStart = if (startTime > 0L) startTime else (timestamps.minOrNull() ?: 0L)
        val effectiveEnd = maxOf(effectiveStart, minOf(endTime, System.currentTimeMillis() + 86_400_000L))
        val spanDays = ((effectiveEnd - effectiveStart) / 86_400_000L).coerceAtLeast(0L)
        val useMonthly = !forceDaily && spanDays > 45L

        data class MutableBucket(
            val key: String,
            val label: String,
            val fullLabel: String,
            val sortOrder: Long,
            var galon: Int = 0,
            var omzet: Double = 0.0,
            var kasJual: Double = 0.0,
            var kasbon: Double = 0.0,
            var beban: Double = 0.0
        )

        val buckets = LinkedHashMap<String, MutableBucket>()

        fun bucketFor(millis: Long): MutableBucket {
            val zdt = Instant.ofEpochMilli(millis).atZone(zoneId)
            return if (useMonthly) {
                val ym = YearMonth.from(zdt)
                val key = ym.toString()
                buckets.getOrPut(key) {
                    MutableBucket(
                        key = key,
                        label = ym.atDay(1).format(MONTH_LABEL_FORMATTER),
                        fullLabel = ym.atDay(1).format(MONTH_FULL_FORMATTER),
                        sortOrder = ym.atDay(1).toEpochDay()
                    )
                }
            } else {
                val date: LocalDate = zdt.toLocalDate()
                val key = date.toString()
                buckets.getOrPut(key) {
                    MutableBucket(
                        key = key,
                        label = date.format(DAY_LABEL_FORMATTER),
                        fullLabel = date.format(DAY_FULL_FORMATTER),
                        sortOrder = date.toEpochDay()
                    )
                }
            }
        }

        filteredTx.forEach { detail ->
            val tx = detail.transaksi
            val b = bucketFor(tx.tanggal)
            b.galon += tx.jumlah
            b.omzet += tx.total_harga
            b.kasJual += (initialCashByTxId[tx.id] ?: 0.0)
            b.beban += tx.total_komisi
        }

        filteredLogs.forEach { log ->
            val b = bucketFor(log.tanggal)
            b.kasbon += log.jumlah_bayar.coerceAtLeast(0.0)
        }

        filteredExp.forEach { exp ->
            val b = bucketFor(exp.tanggal)
            b.beban += exp.nominal.coerceAtLeast(0.0)
        }

        val sortedPoints = buckets.values
            .sortedBy { it.sortOrder }
            .map { b ->
                val kasMasuk = b.kasJual + b.kasbon
                FinancialTrendPoint(
                    bucketKey = b.key,
                    label = b.label,
                    fullDateLabel = b.fullLabel,
                    totalGalon = b.galon,
                    totalOmzet = b.omzet,
                    kasMasuk = kasMasuk,
                    kasbonMasuk = b.kasbon,
                    bebanKeluar = b.beban,
                    labaBersih = kasMasuk - b.beban
                )
            }

        return Pair(sortedPoints, useMonthly)
    }

    /**
     * Menghitung selisih persentase pertumbuhan antara [current] dan [previous].
     */
    fun calculateGrowthPercent(current: Double, previous: Double): Double {
        if (abs(previous) < 0.01) return 0.0
        return ((current - previous) / previous) * 100.0
    }
}
