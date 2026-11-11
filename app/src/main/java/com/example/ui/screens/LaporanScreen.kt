package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Pengeluaran
import com.example.data.entity.TransaksiDetail
import com.example.util.ExportHelper
import com.example.util.FormatUtils
import com.example.util.PdfExporter
import com.example.viewmodel.LaporanPeriode
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class PenjualanDesaItem(
    val desa: String,
    val total_galon: Int,
    val total_transaksi: Int,
    val total_pendapatan: Double
)

data class PenjualanKurirItem(
    val nama_kurir: String,
    val total_galon: Int,
    val total_transaksi: Int,
    val total_pendapatan: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail> = emptyList(),
    pengeluaranList: List<Pengeluaran> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactionsFromVm by viewModel.allTransaksi.collectAsStateWithLifecycle()
    val allPengeluaranFromVm by viewModel.allPengeluaran.collectAsStateWithLifecycle()

    val effectiveTransaksiList = if (transaksiList.isNotEmpty()) transaksiList else allTransactionsFromVm
    val effectivePengeluaranList = if (pengeluaranList.isNotEmpty()) pengeluaranList else allPengeluaranFromVm

    val selectedPeriode by viewModel.laporanPeriode.collectAsStateWithLifecycle()
    var isCustomDateRange by remember { mutableStateOf(false) }
    var customStartMillis by remember { mutableLongStateOf(FormatUtils.getStartOfMonth()) }
    var customEndMillis by remember { mutableLongStateOf(FormatUtils.getEndOfDay()) }

    var isSummaryMode by remember { mutableStateOf(false) }
    var detailTabIndex by remember { mutableIntStateOf(0) }
    var rankingTabIndex by remember { mutableIntStateOf(0) }

    val todayEnd = FormatUtils.getEndOfDay()
    val (startTime, endTime) = if (isCustomDateRange) {
        Pair(customStartMillis, customEndMillis)
    } else {
        when (selectedPeriode) {
            LaporanPeriode.HARI_INI -> Pair(FormatUtils.getStartOfDay(), todayEnd)
            LaporanPeriode.MINGGU_INI -> Pair(FormatUtils.getStartOfWeek(), todayEnd)
            LaporanPeriode.BULAN_INI -> Pair(FormatUtils.getStartOfMonth(), todayEnd)
            LaporanPeriode.TAHUN_INI -> Pair(FormatUtils.getStartOfYear(), todayEnd)
            LaporanPeriode.SEMUA -> Pair(0L, todayEnd + 86400000L)
        }
    }

    // --- Core Financial Calculations (Preserved 100% + Memoized for 60fps performance) ---
    val filteredTx = remember(effectiveTransaksiList, startTime, endTime) {
        effectiveTransaksiList
            .filter { it.transaksi.tanggal in startTime..endTime }
            .sortedWith(compareByDescending<TransaksiDetail> { it.transaksi.tanggal }.thenByDescending { it.transaksi.id })
    }
    val filteredExp = remember(effectivePengeluaranList, startTime, endTime) {
        effectivePengeluaranList
            .filter { it.tanggal in startTime..endTime }
            .sortedWith(compareByDescending<Pengeluaran> { it.tanggal }.thenByDescending { it.id })
    }

    val pendapatanLunas = remember(filteredTx) {
        filteredTx.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
    }
    val piutangKasbonPeriode = remember(filteredTx) {
        filteredTx
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .sumOf { it.transaksi.remainingKasbonAmount() }
    }
    val totalKomisiKurir = remember(filteredTx) {
        filteredTx.sumOf { it.transaksi.total_komisi }
    }
    val totalPengeluaran = remember(filteredExp) {
        filteredExp.sumOf { it.nominal }
    }
    val totalBebanKeluar = totalKomisiKurir + totalPengeluaran
    val labaBersih = pendapatanLunas - totalKomisiKurir - totalPengeluaran
    val isProfit = labaBersih >= 0
    val marginLabaPersen = if (pendapatanLunas > 0.0) {
        (labaBersih / pendapatanLunas) * 100.0
    } else {
        0.0
    }
    val hasPeriodActivity = filteredTx.isNotEmpty() || filteredExp.isNotEmpty()

    // --- Perbandingan Omzet Bulanan (Bulan Ini vs Bulan Lalu menggunakan java.time.YearMonth + DAO Query) ---
    val (thisMonthRange, lastMonthRange) = remember {
        val zoneId = ZoneId.systemDefault()
        val currentYm = YearMonth.now(zoneId)
        val previousYm = currentYm.minusMonths(1)
        val currStart = currentYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currEnd = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        val prevStart = previousYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val prevEnd = previousYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        Pair(Pair(currStart, currEnd), Pair(prevStart, prevEnd))
    }
    val daoPendapatanBulanIni by remember(thisMonthRange) {
        viewModel.getTotalPendapatanByDateRange(thisMonthRange.first, thisMonthRange.second)
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val daoPendapatanBulanLalu by remember(lastMonthRange) {
        viewModel.getTotalPendapatanByDateRange(lastMonthRange.first, lastMonthRange.second)
    }.collectAsStateWithLifecycle(initialValue = 0.0)

    val effectiveBulanIniRevenue = if (daoPendapatanBulanIni > 0.0) daoPendapatanBulanIni else pendapatanLunas
    val showMonthlyComparison = !isCustomDateRange && selectedPeriode == LaporanPeriode.BULAN_INI

    var showAllRanking by remember { mutableStateOf(false) }
    var showAllDetails by remember { mutableStateOf(false) }

    // Smart Grouping for Penjualan per Desa / Wilayah (normalizes house numbers like "Jl. Raya Sukamaju No. 12" -> "Sukamaju")
    val penjualanDesaList = remember(filteredTx) {
        filteredTx.groupBy { tx ->
            extractNormalizedDesaName(tx.pelanggan?.alamat)
        }.map { (desa, list) ->
            PenjualanDesaItem(
                desa = desa,
                total_galon = list.sumOf { it.transaksi.jumlah },
                total_transaksi = list.size,
                total_pendapatan = list.sumOf { it.transaksi.total_harga }
            )
        }.sortedByDescending { it.total_galon }
    }

    // Grouping for Penjualan per Kurir
    val penjualanKurirList = remember(filteredTx) {
        filteredTx.groupBy { it.kurir?.nama ?: "Ambil Sendiri" }
            .map { (kurir, list) ->
                PenjualanKurirItem(
                    nama_kurir = kurir,
                    total_galon = list.sumOf { it.transaksi.jumlah },
                    total_transaksi = list.size,
                    total_pendapatan = list.sumOf { it.transaksi.total_harga }
                )
            }.sortedByDescending { it.total_galon }
    }

    // Run-Rate Proyeksi Bulan Ini
    val daysInMonth = FormatUtils.getDaysInCurrentMonth()
    val currentDay = FormatUtils.getCurrentDayOfMonth().coerceAtLeast(1)
    val remainingDays = (daysInMonth - currentDay).coerceAtLeast(0)

    val monthStart = FormatUtils.getStartOfMonth()
    val monthStats = remember(effectiveTransaksiList, effectivePengeluaranList, monthStart, currentDay, remainingDays) {
        val mTx = effectiveTransaksiList.filter { it.transaksi.tanggal >= monthStart }
        val mExp = effectivePengeluaranList.filter { it.tanggal >= monthStart }
        val mGalon = mTx.sumOf { it.transaksi.jumlah }
        val mLunas = mTx.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
        val mKomisi = mTx.sumOf { it.transaksi.total_komisi }
        val mPengeluaran = mExp.sumOf { it.nominal }
        val mNetProfit = mLunas - mKomisi - mPengeluaran

        val avgGalon = if (currentDay > 0) mGalon.toDouble() / currentDay else 0.0
        val projGalon = (mGalon + (avgGalon * remainingDays)).toInt()
        val avgProfit = if (currentDay > 0) mNetProfit / currentDay else 0.0
        val projProfit = mNetProfit + (avgProfit * remainingDays)
        QuadrupleMonthStats(mGalon, projGalon, mNetProfit, projProfit)
    }
    val monthGalon = monthStats.monthGalon
    val projectedGalon = monthStats.projectedGalon
    val monthNetProfit = monthStats.monthNetProfit
    val projectedProfit = monthStats.projectedProfit

    val profitColor = Color(0xFF16A34A)
    val lossColor = MaterialTheme.colorScheme.error

    fun openCustomDateRangePicker() {
        val startCal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedStartCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val pickedStart = pickedStartCal.timeInMillis
                val endCal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                        val pickedEndCal = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDay, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        val pickedEnd = pickedEndCal.timeInMillis
                        customStartMillis = minOf(pickedStart, pickedEnd)
                        customEndMillis = maxOf(pickedStart, pickedEnd)
                        isCustomDateRange = true
                        showAllDetails = false
                    },
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Pilih Tanggal Akhir")
                    show()
                }
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Pilih Tanggal Mulai")
            show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("laporan_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            // 1. Prominent Period Filter Bar (Date Picker & FilterChips)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Filter Periode Laporan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (!isCustomDateRange && startTime == 0L) {
                                            "Semua Riwayat Transaksi"
                                        } else {
                                            "${FormatUtils.formatDateShort(startTime)} – ${FormatUtils.formatDateShort(endTime)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { openCustomDateRangePicker() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustomDateRange) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Pilih Rentang Tanggal",
                                            tint = if (isCustomDateRange) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Kalender",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCustomDateRange) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = "${filteredTx.sumOf { it.transaksi.jumlah }} Galon",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            val chipLabels = mapOf(
                                LaporanPeriode.HARI_INI to "Harian",
                                LaporanPeriode.MINGGU_INI to "Mingguan",
                                LaporanPeriode.BULAN_INI to "Bulanan",
                                LaporanPeriode.TAHUN_INI to "Tahunan",
                                LaporanPeriode.SEMUA to "Semua"
                            )
                            LaporanPeriode.values().forEach { filter ->
                                val isSelected = !isCustomDateRange && selectedPeriode == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        isCustomDateRange = false
                                        viewModel.setLaporanPeriode(filter)
                                        showAllDetails = false
                                    },
                                    label = {
                                        Text(
                                            text = chipLabels[filter] ?: filter.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.height(34.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            FilterChip(
                                selected = isCustomDateRange,
                                onClick = { openCustomDateRangePicker() },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pilih Rentang Tanggal",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Custom",
                                        fontWeight = if (isCustomDateRange) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 2. Main Profit & Loss Summary Card OR Empty State
            if (!hasPeriodActivity) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Dompet Kosong",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Belum ada transaksi di periode ini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Silakan pilih periode tanggal lain atau catat penjualan baru untuk melihat analisis Laba Bersih & Margin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 2A. Hero Card: Laba Bersih & Cashflow Ratio Visualizer (Luxury Fintech Style)
                item {
                    val heroGradient = if (isProfit) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0B192C),
                                Color(0xFF0F2C4C),
                                Color(0xFF0A4D46)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF280B14),
                                Color(0xFF471220),
                                Color(0xFF611626)
                            )
                        )
                    }
                    val heroAccent = if (isProfit) Color(0xFF34D399) else Color(0xFFFB7185)
                    val formattedMargin = String.format(Locale.US, "%.1f", marginLabaPersen)
                    val formattedLabaBersih = FormatUtils.formatRupiah(labaBersih)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(heroGradient)
                                .padding(20.dp)
                        ) {
                            // Top Header Row: Label (Weighted so it never collides on small screens)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = heroAccent.copy(alpha = 0.2f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isProfit) {
                                                    Icons.AutoMirrored.Outlined.TrendingUp
                                                } else {
                                                    Icons.AutoMirrored.Outlined.TrendingDown
                                                },
                                                contentDescription = "Status Laba",
                                                tint = heroAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isProfit) "LABA BERSIH (NET PROFIT)" else "DEFISIT / RUGI BERSIH",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = heroAccent,
                                            letterSpacing = 0.6.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Pendapatan Lunas dikurangi Beban & Komisi",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Hero Number: Laba Bersih with Adaptive Font Size so tens/hundreds of millions never clip
                            Text(
                                text = formattedLabaBersih,
                                style = if (formattedLabaBersih.length > 14) {
                                    MaterialTheme.typography.headlineLarge
                                } else {
                                    MaterialTheme.typography.displaySmall
                                },
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dedicated Pill Row directly beneath Laba Bersih
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = heroAccent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, heroAccent.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "Margin Laba: $formattedMargin%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = heroAccent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = if (isProfit) "Bisnis Sehat & Untung" else "Perlu Evaluasi Biaya",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Glassmorphic Multi-Segment Cashflow Bar
                            val positiveProfit = labaBersih.coerceAtLeast(0.0)
                            val totalFlowBase = (positiveProfit + totalPengeluaran + totalKomisiKurir).coerceAtLeast(1.0)
                            val profitShare = (positiveProfit / totalFlowBase).toFloat().coerceIn(0f, 1f)
                            val opShare = (totalPengeluaran / totalFlowBase).toFloat().coerceIn(0f, 1f)
                            val komisiShare = (totalKomisiKurir / totalFlowBase).toFloat().coerceIn(0f, 1f)

                            val profitPct = (profitShare * 100).toInt()
                            val opPct = (opShare * 100).toInt()
                            val komisiPct = (komisiShare * 100).toInt()

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.09f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Komposisi Arus Kas Masuk vs Beban",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = "${filteredTx.size} Trx • ${filteredExp.size} Biaya",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = heroAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Multi-segment Pure Compose Progress Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        if (profitShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(profitShare.coerceAtLeast(0.03f))
                                                    .height(8.dp)
                                                    .background(Color(0xFF34D399))
                                            )
                                        }
                                        if (opShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(opShare.coerceAtLeast(0.03f))
                                                    .height(8.dp)
                                                    .background(Color(0xFFFB7185))
                                            )
                                        }
                                        if (komisiShare > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(komisiShare.coerceAtLeast(0.03f))
                                                    .height(8.dp)
                                                    .background(Color(0xFFFBBF24))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        CashflowLegendDot(
                                            color = Color(0xFF34D399),
                                            label = "Laba ($profitPct%)"
                                        )
                                        CashflowLegendDot(
                                            color = Color(0xFFFB7185),
                                            label = "Operasional ($opPct%)"
                                        )
                                        CashflowLegendDot(
                                            color = Color(0xFFFBBF24),
                                            label = "Komisi ($komisiPct%)"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2B. Bento Grid Breakdown: Total Pendapatan & Total Pengeluaran + Detail Cards
                item {
                    val totalOmzetKotor = pendapatanLunas + piutangKasbonPeriode
                    val collectionRatio = if (totalOmzetKotor > 0.0) {
                        (pendapatanLunas / totalOmzetKotor).toFloat().coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val lunasCount = filteredTx.count { it.transaksi.status.equals("Lunas", ignoreCase = true) }
                    val dpCount = filteredTx.count {
                        !it.transaksi.status.equals("Lunas", ignoreCase = true) && it.transaksi.uangMuka > 0L
                    }
                    val pendapatanSubtitle = if (dpCount > 0) {
                        "$lunasCount Lunas + $dpCount DP (${(collectionRatio * 100).toInt()}% Tertagih)"
                    } else {
                        "$lunasCount Trx Lunas (${(collectionRatio * 100).toInt()}% Tertagih)"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Top 2-Column Bento Grid: Total Pendapatan vs Total Pengeluaran
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BentoFinancialCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                icon = Icons.Outlined.AccountBalanceWallet,
                                label = "Total Pendapatan",
                                amount = FormatUtils.formatRupiah(pendapatanLunas),
                                subtitle = pendapatanSubtitle,
                                accentColor = profitColor,
                                progress = collectionRatio,
                                comparisonBadge = if (showMonthlyComparison) {
                                    {
                                        MonthlyRevenueComparisonBadge(
                                            bulanIni = effectiveBulanIniRevenue,
                                            bulanLalu = daoPendapatanBulanLalu,
                                            upColor = profitColor,
                                            downColor = lossColor
                                        )
                                    }
                                } else {
                                    null
                                }
                            )

                            val expenseRatio = if (pendapatanLunas > 0) {
                                (totalBebanKeluar / pendapatanLunas).toFloat().coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                            BentoFinancialCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                icon = Icons.Outlined.ShoppingCart,
                                label = "Total Pengeluaran",
                                amount = FormatUtils.formatRupiah(totalBebanKeluar),
                                subtitle = "Rasio Beban vs Pendapatan",
                                accentColor = lossColor,
                                progress = expenseRatio
                            )
                        }

                        // Secondary Breakdown Card: Biaya Operasional Kas & Komisi Kurir (+ Piutang Kasbon if any)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                FinancialBreakdownRow(
                                    icon = Icons.Outlined.ShoppingCart,
                                    title = "Biaya Operasional Kas",
                                    subtitle = "${filteredExp.size} catatan pengeluaran depot",
                                    nominalText = "- ${FormatUtils.formatRupiah(totalPengeluaran)}",
                                    accentColor = lossColor
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )

                                FinancialBreakdownRow(
                                    icon = Icons.Outlined.LocalShipping,
                                    title = "Total Komisi Kurir",
                                    subtitle = "Akumulasi komisi pengantaran galon",
                                    nominalText = "- ${FormatUtils.formatRupiah(totalKomisiKurir)}",
                                    accentColor = Color(0xFFD97706)
                                )

                                if (piutangKasbonPeriode > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    )

                                    FinancialBreakdownRow(
                                        icon = Icons.Outlined.AccountBalanceWallet,
                                        title = "Piutang Kasbon (Belum Lunas)",
                                        subtitle = "Belum dihitung ke Laba Bersih sampai lunas",
                                        nominalText = FormatUtils.formatRupiah(piutangKasbonPeriode),
                                        accentColor = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Estimasi Proyeksi Run-Rate Bulan Ini (Hanya ditampilkan saat mode Bulanan & ada aktivitas)
            if (showMonthlyComparison && hasPeriodActivity) {
            item {
                val monthProgress = (currentDay.toFloat() / daysInMonth.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val projectedColor = if (projectedProfit >= 0) profitColor else lossColor
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Insights,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Proyeksi Run-Rate Bulan Ini",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Hari ke-$currentDay dari $daysInMonth hari berjalan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = "${(monthProgress * 100).toInt()}% Bulan",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { monthProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Proyeksi Volume",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$projectedGalon Galon",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Aktual: $monthGalon Galon",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Proyeksi Laba",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatRupiah(projectedProfit),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = projectedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Aktual: ${FormatUtils.formatRupiah(monthNetProfit)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // 4. Peringkat Penjualan Terbanyak (Desa & Kurir) with Top 5 Expand/Collapse
            if (hasPeriodActivity) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Distribusi Penjualan Terbanyak",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Peringkat volume galon per wilayah desa & kurir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = rankingTabIndex == 0,
                                onClick = {
                                    rankingTabIndex = 0
                                    showAllRanking = false
                                },
                                label = { Text("Desa (${penjualanDesaList.size})", maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = rankingTabIndex == 1,
                                onClick = {
                                    rankingTabIndex = 1
                                    showAllRanking = false
                                },
                                label = { Text("Kurir (${penjualanKurirList.size})", maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeliveryDining,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (rankingTabIndex == 0) {
                            if (penjualanDesaList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada transaksi di periode ini",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                val maxGalonDesa = (penjualanDesaList.maxOfOrNull { it.total_galon } ?: 1).coerceAtLeast(1)
                                val displayedDesa = if (showAllRanking) penjualanDesaList else penjualanDesaList.take(5)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    displayedDesa.forEachIndexed { index, item ->
                                        PenjualanDesaItemRow(
                                            rank = index + 1,
                                            item = item,
                                            maxGalon = maxGalonDesa
                                        )
                                    }
                                    if (penjualanDesaList.size > 5) {
                                        OutlinedButton(
                                            onClick = { showAllRanking = !showAllRanking },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Text(
                                                text = if (showAllRanking) {
                                                    "Tampilkan Top 5 Wilayah Saja"
                                                } else {
                                                    "Lihat Semua Wilayah (${penjualanDesaList.size} Desa)"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            if (penjualanKurirList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada transaksi di periode ini",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                val maxGalonKurir = (penjualanKurirList.maxOfOrNull { it.total_galon } ?: 1).coerceAtLeast(1)
                                val displayedKurir = if (showAllRanking) penjualanKurirList else penjualanKurirList.take(5)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    displayedKurir.forEachIndexed { index, item ->
                                        PenjualanKurirItemRow(
                                            rank = index + 1,
                                            item = item,
                                            maxGalon = maxGalonKurir
                                        )
                                    }
                                    if (penjualanKurirList.size > 5) {
                                        OutlinedButton(
                                            onClick = { showAllRanking = !showAllRanking },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Text(
                                                text = if (showAllRanking) {
                                                    "Tampilkan Top 5 Kurir Saja"
                                                } else {
                                                    "Lihat Semua Kurir (${penjualanKurirList.size})"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Strategic Export Panel (PDF & CSV + Mode Ringkasan) — Placed ABOVE transaction list so it's always reachable
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ekspor & Cetak Laporan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mode Ringkasan Harian (PDF)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kelompokkan penjualan Lunas & Kasbon per tanggal saat cetak PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSummaryMode,
                                onCheckedChange = { isSummaryMode = it }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val html = buildString {
                                        val totalGalon = filteredTx.sumOf { it.transaksi.jumlah }
                                        val totalLunas = pendapatanLunas
                                        val totalKasbon = piutangKasbonPeriode
                                        val totalKomisi = totalKomisiKurir
                                        val netProfit = labaBersih
                                        val marginFormatted = String.format(Locale.US, "%.1f", marginLabaPersen)

                                        append("<html><head><style>")
                                        append("body { font-family: Arial, sans-serif; padding: 20px; color: #333; }")
                                        append("h1 { color: #0284C7; font-size: 20px; margin-bottom: 2px; }")
                                        append(".subtitle { font-size: 12px; color: #666; margin-bottom: 20px; }")
                                        append(".card { background: #f8fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 8px; margin-bottom: 15px; }")
                                        append("table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }")
                                        append("th, td { border: 1px solid #cbd5e1; padding: 6px 8px; text-align: left; }")
                                        append("th { background: #f1f5f9; }")
                                        append(".right { text-align: right; }")
                                        append("</style></head><body>")
                                        append("<h1>Laporan Keuangan Santoya RO</h1>")
                                        append("<div class='subtitle'>Periode: ${if (!isCustomDateRange && startTime == 0L) "Semua Riwayat" else "${FormatUtils.formatDate(startTime)} s/d ${FormatUtils.formatDate(endTime)}"}</div>")

                                        append("<div class='card'>")
                                        append("<b>Ringkasan Finansial:</b><br/>")
                                        append("• Total Galon Terjual: $totalGalon Galon<br/>")
                                        append("• Pendapatan Lunas: ${FormatUtils.formatRupiah(totalLunas)}<br/>")
                                        append("• Piutang Kasbon: ${FormatUtils.formatRupiah(totalKasbon)}<br/>")
                                        append("• Komisi Kurir: ${FormatUtils.formatRupiah(totalKomisi)}<br/>")
                                        append("• Total Pengeluaran Operasional: ${FormatUtils.formatRupiah(totalPengeluaran)}<br/>")
                                        append("<b>• Laba Bersih: ${FormatUtils.formatRupiah(netProfit)} (Margin: $marginFormatted%)</b>")
                                        append("</div>")

                                        if (isSummaryMode) {
                                            val groupedByDate = filteredTx
                                                .groupBy { FormatUtils.formatDateShort(it.transaksi.tanggal) }
                                                .map { (tgl, list) ->
                                                    val galonSum = list.sumOf { it.transaksi.jumlah }
                                                    val lunasSum = list.sumOf {
                                                        if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                                                            it.transaksi.total_harga
                                                        } else {
                                                            it.transaksi.uangMuka.toDouble()
                                                        }
                                                    }
                                                    val kasbonSum = list
                                                        .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
                                                        .sumOf { it.transaksi.remainingKasbonAmount() }
                                                    arrayOf(tgl, galonSum.toString(), FormatUtils.formatRupiah(lunasSum), FormatUtils.formatRupiah(kasbonSum))
                                                }
                                            append("<h3>Ringkasan Penjualan Harian (${groupedByDate.size} Hari)</h3>")
                                            append("<table><thead><tr><th>Tanggal</th><th>Galon</th><th class='right'>Pendapatan Lunas</th><th class='right'>Kasbon</th></tr></thead><tbody>")
                                            groupedByDate.forEach { row ->
                                                append("<tr>")
                                                append("<td>${row[0]}</td>")
                                                append("<td>${row[1]} Galon</td>")
                                                append("<td class='right'>${row[2]}</td>")
                                                append("<td class='right'>${row[3]}</td>")
                                                append("</tr>")
                                            }
                                            append("</tbody></table>")
                                        } else {
                                            val limitedTx = filteredTx.take(100)
                                            append("<h3>Daftar Transaksi (${filteredTx.size})</h3>")
                                            append("<table><thead><tr><th>No</th><th>Tanggal</th><th>Pelanggan</th><th>Jumlah</th><th class='right'>Total</th><th>Status</th></tr></thead><tbody>")
                                            limitedTx.forEachIndexed { idx, item ->
                                                val trx = item.transaksi
                                                val statusDetail = if (trx.status.equals("Kasbon", ignoreCase = true) && trx.uangMuka > 0L) {
                                                    "Kasbon (DP: ${FormatUtils.formatRupiah(trx.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(trx.remainingKasbonAmount())})"
                                                } else {
                                                    trx.status
                                                }
                                                append("<tr>")
                                                append("<td>${idx + 1}</td>")
                                                append("<td>${FormatUtils.formatDateShort(trx.tanggal)}</td>")
                                                append("<td>${item.pelanggan?.nama ?: "-"}</td>")
                                                append("<td>${trx.jumlah}</td>")
                                                append("<td class='right'>${FormatUtils.formatRupiah(trx.total_harga)}</td>")
                                                append("<td>$statusDetail</td>")
                                                append("</tr>")
                                            }
                                            append("</tbody></table>")
                                            if (filteredTx.size > 100) {
                                                append("<p style='text-align:center; font-style:italic;'>*Menampilkan 100 transaksi terbaru. Gunakan Ekspor CSV untuk data lengkap.</p>")
                                            }
                                        }

                                        append("</body></html>")
                                    }
                                    PdfExporter.printHtmlAsPdf(context, html, "Laporan_Keuangan_Santoya")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_pdf")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val csvContent = ExportHelper.generateCsv(filteredTx, filteredExp)
                                    ExportHelper.shareExportFile(
                                        context = context,
                                        filename = "Laporan_Santoya_${System.currentTimeMillis()}.csv",
                                        content = csvContent,
                                        mimeType = "text/csv"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableView,
                                    contentDescription = "Export CSV",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 6. Rincian Transaksi & Pengeluaran (Sorted Terbaru + Paginated 15 Default with Expand/Collapse)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    TabRow(
                        selectedTabIndex = detailTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = detailTabIndex == 0,
                            onClick = {
                                detailTabIndex = 0
                                showAllDetails = false
                            },
                            text = {
                                Text(
                                    text = "Transaksi (${filteredTx.size})",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        )
                        Tab(
                            selected = detailTabIndex == 1,
                            onClick = {
                                detailTabIndex = 1
                                showAllDetails = false
                            },
                            text = {
                                Text(
                                    text = "Pengeluaran (${filteredExp.size})",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            if (detailTabIndex == 0) {
                if (filteredTx.isEmpty()) {
                    item {
                        EmptyPeriodListCard(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            message = "Belum ada transaksi di periode ini"
                        )
                    }
                } else {
                    val displayedTx = if (showAllDetails) filteredTx else filteredTx.take(15)
                    items(displayedTx, key = { it.transaksi.id }) { item ->
                        val trx = item.transaksi
                        val isLunas = trx.status.equals("Lunas", ignoreCase = true)
                        val isBayarSebagian = !isLunas && trx.uangMuka > 0L
                        val metodeInfo = when {
                            isLunas -> trx.metode_bayar.ifBlank { "Cash" }
                            isBayarSebagian -> "Bayar Sebagian"
                            else -> "Belum Dibayar"
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.pelanggan?.nama ?: "Umum"} • ${trx.jumlah} Galon",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${FormatUtils.formatDateShort(trx.tanggal)} • $metodeInfo • Komisi: ${FormatUtils.formatRupiah(trx.total_komisi)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isBayarSebagian) {
                                        Text(
                                            text = "DP: ${FormatUtils.formatRupiah(trx.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(trx.remainingKasbonAmount())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFD97706),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatRupiah(trx.total_harga),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLunas) profitColor else Color(0xFFD97706),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = trx.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLunas) profitColor else Color(0xFFD97706),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (filteredTx.size > 15) {
                        item {
                            OutlinedButton(
                                onClick = { showAllDetails = !showAllDetails },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = if (showAllDetails) {
                                        "Tampilkan 15 Transaksi Terbaru Saja"
                                    } else {
                                        "Tampilkan Semua Transaksi (${filteredTx.size} Nota)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                if (filteredExp.isEmpty()) {
                    item {
                        EmptyPeriodListCard(
                            icon = Icons.Outlined.ShoppingCart,
                            message = "Belum ada pengeluaran di periode ini"
                        )
                    }
                } else {
                    val displayedExp = if (showAllDetails) filteredExp else filteredExp.take(15)
                    items(displayedExp, key = { it.id }) { pengeluaran ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pengeluaran.nama_item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${pengeluaran.kategori} • ${FormatUtils.formatDateShort(pengeluaran.tanggal)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "- ${FormatUtils.formatRupiah(pengeluaran.nominal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = lossColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (filteredExp.size > 15) {
                        item {
                            OutlinedButton(
                                onClick = { showAllDetails = !showAllDetails },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = if (showAllDetails) {
                                        "Tampilkan 15 Pengeluaran Terbaru Saja"
                                    } else {
                                        "Tampilkan Semua Pengeluaran (${filteredExp.size} Catatan)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            }
    }
}

private data class QuadrupleMonthStats(
    val monthGalon: Int,
    val projectedGalon: Int,
    val monthNetProfit: Double,
    val projectedProfit: Double
)

private fun extractNormalizedDesaName(rawAlamat: String?): String {
    if (rawAlamat.isNullOrBlank()) return "Umum / Santoya"
    val cleaned = rawAlamat.trim()
        .replace(Regex("(?i)^(jl\\.?\\s*raya|jalan\\s*raya|jl\\.?|jalan|desa|kp\\.?|kampung|kel\\.?|kelurahan)\\s+"), "")
        .replace(Regex("(?i)\\s+(no\\.?|nomor|blok|rt|rw|#)\\s*[0-9a-zA-Z/.-]+.*$"), "")
        .replace(Regex("\\s+#?[0-9]+$"), "")
        .trim()
    return if (cleaned.isBlank()) "Umum / Santoya" else cleaned
}

@Composable
private fun CashflowLegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MonthlyRevenueComparisonBadge(
    bulanIni: Double,
    bulanLalu: Double,
    upColor: Color,
    downColor: Color
) {
    val isPreviousZero = bulanLalu <= 0.0
    val growthPercent = if (!isPreviousZero) {
        ((bulanIni - bulanLalu) / bulanLalu) * 100.0
    } else {
        0.0
    }
    val isUp = isPreviousZero || growthPercent >= 0.0
    val badgeColor = when {
        isPreviousZero -> MaterialTheme.colorScheme.primary
        isUp -> upColor
        else -> downColor
    }
    val badgeIcon = if (isUp) {
        Icons.AutoMirrored.Outlined.TrendingUp
    } else {
        Icons.AutoMirrored.Outlined.TrendingDown
    }
    val badgeText = when {
        isPreviousZero -> "Baru bulan ini"
        growthPercent > 0.0 -> "+${growthPercent.roundToInt()}% vs bulan lalu"
        growthPercent < 0.0 -> "${growthPercent.roundToInt()}% vs bulan lalu"
        else -> "0% vs bulan lalu"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BentoFinancialCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    progress: Float,
    comparisonBadge: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = amount,
                style = if (amount.length > 13) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (comparisonBadge != null) {
                Spacer(modifier = Modifier.height(5.dp))
                comparisonBadge()
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun FinancialBreakdownRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    nominalText: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = nominalText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyPeriodListCard(
    icon: ImageVector,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PenjualanDesaItemRow(
    rank: Int,
    item: PenjualanDesaItem,
    maxGalon: Int
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFB300)
        2 -> Color(0xFF78909C)
        3 -> Color(0xFFB07238)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = rankColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = rankColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.desa,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.total_transaksi} Transaksi • Omzet ${FormatUtils.formatRupiah(item.total_pendapatan)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${item.total_galon} Galon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (item.total_galon.toFloat() / maxGalon.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = rankColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PenjualanKurirItemRow(
    rank: Int,
    item: PenjualanKurirItem,
    maxGalon: Int
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFB300)
        2 -> Color(0xFF78909C)
        3 -> Color(0xFFB07238)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = rankColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = rankColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.nama_kurir,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.total_transaksi} Kirim • Omzet ${FormatUtils.formatRupiah(item.total_pendapatan)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${item.total_galon} Galon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (item.total_galon.toFloat() / maxGalon.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = rankColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
