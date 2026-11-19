package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TableView
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.DepotProfile
import com.example.data.entity.Pengeluaran
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.DepotQuickFillDialog
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroCourierWaveIcon
import com.example.ui.components.HydroDropletSparkIcon
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroReceiptNoteIcon
import com.example.ui.components.HydroReportChartIcon
import com.example.ui.components.LaporanBentoFinancialCard
import com.example.ui.components.LaporanCashflowLegendDot
import com.example.ui.components.LaporanEmptyPeriodListCard
import com.example.ui.components.LaporanFinancialBreakdownRow
import com.example.ui.components.LaporanKasbonLogItemCard
import com.example.ui.components.LaporanMonthlyComparisonBadge
import com.example.ui.components.LaporanAgingCard
import com.example.ui.components.LaporanSalesRankingItemCard
import com.example.ui.components.LaporanTrendChartCard
import com.example.ui.components.PengeluaranCategoryIcon
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.components.SantoyaSearchBar
import com.example.ui.components.resolvePengeluaranCategoryColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.HeroDeficitPanel
import com.example.ui.theme.HeroDeficitStart
import com.example.ui.theme.HeroGlowAmberSoft
import com.example.ui.theme.HeroGlowEmeraldSoft
import com.example.ui.theme.HeroGlowRoseSoft
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.SantoyaNavyBorder
import com.example.ui.theme.SantoyaNavyPanel
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.ExportHelper
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.LaporanFinanceCalculator
import com.example.util.PdfExporter
import com.example.util.RankingMetricsHelper
import com.example.util.RankingSortMode
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.LaporanPeriode
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Layar Laporan Keuangan pemilik depot Santoya RO yang menyajikan analisis arus kas riil,
 * rekonsiliasi metode bayar, tren harian/bulanan, umur piutang, peringkat penjualan, serta ekspor PDF/CSV.
 */
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
    val allKasbonLogs by viewModel.allKasbonLogs.collectAsStateWithLifecycle()
    val allPelanggan by viewModel.allPelanggan.collectAsStateWithLifecycle()
    val depotProfile by viewModel.depotProfile.collectAsStateWithLifecycle()
    val isKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsStateWithLifecycle()
    var showQuickFillForPdf by remember { mutableStateOf(false) }

    val rawTransaksiList = if (transaksiList.isNotEmpty()) transaksiList else allTransactionsFromVm
    val effectiveTransaksiList = remember(rawTransaksiList) {
        rawTransaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }
    val effectivePengeluaranList = if (pengeluaranList.isNotEmpty()) pengeluaranList else allPengeluaranFromVm

    val selectedPeriode by viewModel.laporanPeriode.collectAsStateWithLifecycle()
    var isCustomDateRange by remember { mutableStateOf(false) }
    var customStartMillis by remember { mutableLongStateOf(FormatUtils.getStartOfMonth()) }
    var customEndMillis by remember { mutableLongStateOf(FormatUtils.getEndOfDay()) }

    var isSummaryMode by remember { mutableStateOf(false) }
    var detailTabIndex by remember { mutableIntStateOf(0) }
    var rankingTabIndex by remember { mutableIntStateOf(0) }
    var showAllRanking by remember { mutableStateOf(false) }
    var rankingSortMode by remember { mutableStateOf(RankingSortMode.GALON) }
    var showAllDetails by remember { mutableStateOf(false) }
    var txSearchQuery by remember { mutableStateOf("") }

    // Kunci tanggal berbasis hari berjalan agar rentang otomatis segar saat pergantian hari/bulan
    val zoneId = ZoneId.systemDefault()
    val currentLocalDate = LocalDate.now(zoneId)
    val nowMillis = System.currentTimeMillis()
    val todayEnd = remember(currentLocalDate) { FormatUtils.getEndOfDay(nowMillis) }

    val (startTime, endTime) = remember(
        isCustomDateRange,
        customStartMillis,
        customEndMillis,
        selectedPeriode,
        currentLocalDate
    ) {
        if (isCustomDateRange) {
            Pair(customStartMillis, customEndMillis)
        } else {
            when (selectedPeriode) {
                LaporanPeriode.HARI_INI -> Pair(FormatUtils.getStartOfDay(nowMillis), todayEnd)
                LaporanPeriode.MINGGU_INI -> Pair(FormatUtils.getStartOfWeek(), todayEnd)
                LaporanPeriode.BULAN_INI -> Pair(FormatUtils.getStartOfMonth(nowMillis), todayEnd)
                LaporanPeriode.TAHUN_INI -> Pair(FormatUtils.getStartOfYear(), todayEnd)
                LaporanPeriode.SEMUA -> Pair(0L, todayEnd + 86_400_000L)
            }
        }
    }

    val summary = remember(
        effectiveTransaksiList,
        effectivePengeluaranList,
        allKasbonLogs,
        allPelanggan,
        startTime,
        endTime
    ) {
        LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = effectiveTransaksiList,
            allPengeluaran = effectivePengeluaranList,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            startTime = startTime,
            endTime = endTime,
            zoneId = zoneId
        )
    }

    val monthlyComparison = remember(
        effectiveTransaksiList,
        effectivePengeluaranList,
        allKasbonLogs,
        allPelanggan,
        currentLocalDate
    ) {
        LaporanFinanceCalculator.calculateMonthlyComparison(
            allTransaksi = effectiveTransaksiList,
            allPengeluaran = effectivePengeluaranList,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
    }

    val monthlyProjection = remember(
        effectiveTransaksiList,
        effectivePengeluaranList,
        allKasbonLogs,
        allPelanggan,
        currentLocalDate
    ) {
        LaporanFinanceCalculator.calculateMonthlyProjection(
            allTransaksi = effectiveTransaksiList,
            allPengeluaran = effectivePengeluaranList,
            allKasbonLogs = allKasbonLogs,
            allPelanggan = allPelanggan,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
    }

    val kasbonAgingSummary = remember(
        effectiveTransaksiList,
        allPelanggan,
        currentLocalDate
    ) {
        LaporanFinanceCalculator.calculateKasbonAging(
            allTransaksi = effectiveTransaksiList,
            allPelanggan = allPelanggan,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
    }

    val rankedDesaEntries = remember(summary.desaEntries, rankingSortMode) {
        RankingMetricsHelper.buildRankedEntries(summary.desaEntries, rankingSortMode)
    }
    val rankedKurirEntries = remember(summary.kurirEntries, rankingSortMode) {
        RankingMetricsHelper.buildRankedEntries(summary.kurirEntries, rankingSortMode)
    }

    val currentYm = YearMonth.from(currentLocalDate)
    val daysInMonth = currentYm.lengthOfMonth().coerceAtLeast(1)
    val currentDay = currentLocalDate.dayOfMonth.coerceIn(1, daysInMonth)
    val showMonthlyComparison = !isCustomDateRange && selectedPeriode == LaporanPeriode.BULAN_INI

    val isProfit = summary.labaBersih >= 0.0
    val santoyaColors = MaterialTheme.santoyaColors
    val profitColor = santoyaColors.success
    val lossColor = santoyaColors.danger

    val periodRangeLabel = if (!isCustomDateRange && selectedPeriode == LaporanPeriode.SEMUA) {
        stringResource(R.string.laporan_all_time)
    } else {
        "${FormatUtils.formatDateUi(startTime)} – ${FormatUtils.formatDateUi(endTime)}"
    }

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
                    setTitle(context.getString(R.string.date_picker_end_title))
                    show()
                }
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(context.getString(R.string.date_picker_start_title))
            show()
        }
    }

    fun launchPdfExport(activeProfile: DepotProfile) {
        val html = ExportHelper.generateLaporanHtml(
            summary = summary,
            allTransaksi = effectiveTransaksiList,
            allKasbonLogs = allKasbonLogs,
            periodeLabel = periodRangeLabel,
            isSummaryMode = isSummaryMode,
            kasbonAgingSummary = kasbonAgingSummary,
            rankedDesaEntries = rankedDesaEntries,
            depotProfile = activeProfile,
            isProUser = activeProfile.isProUser()
        )
        val safePrefix = activeProfile.effectiveDepotName()
            .replace(Regex("[^a-zA-Z0-9]+"), "_")
            .trim('_')
            .ifBlank { "Depot" }
        val pdfJobName = ExportHelper.buildExportFileName(
            prefix = "Laporan_$safePrefix",
            periodeLabel = periodRangeLabel,
            extension = "pdf"
        ).removeSuffix(".pdf")
        PdfExporter.printHtmlAsPdf(context, html, pdfJobName)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("laporan_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bar Filter Periode Laporan
        item {
            HydroBentoCard(
                accentColor = MaterialTheme.colorScheme.primary,
                cornerRadius = SantoyaRadius.xl,
                tintAlpha = 0.07f,
                showWaterRipple = true,
                modifier = Modifier.fillMaxWidth()
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
                            HydroGradientBadge(
                                gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                                size = 36.dp,
                                cornerRadius = 11.dp,
                                elevation = 3.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = santoyaColors.onHero,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.laporan_period_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            onClick = { openCustomDateRangePicker() },
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            color = if (isCustomDateRange) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            },
                            modifier = Modifier
                                .heightIn(min = SantoyaSize.touchTarget)
                                .testTag("btn_laporan_calendar")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = stringResource(R.string.laporan_calendar_cd),
                                    tint = if (isCustomDateRange) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.laporan_calendar_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCustomDateRange) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = periodRangeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.xs),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                HydroGalonIcon(
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${FormatUtils.formatNumber(summary.totalGalon)} ${stringResource(R.string.unit_galon)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (depotProfile.isBothType() || (summary.totalGalonRo > 0 && summary.totalGalonMineral > 0)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("row_laporan_water_type_breakdown"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(SantoyaRadius.xs),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.laporan_galon_ro_label,
                                        FormatUtils.formatNumber(summary.totalGalonRo)
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(SantoyaRadius.xs),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.laporan_galon_mineral_label,
                                        FormatUtils.formatNumber(summary.totalGalonMineral)
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chipLabels = mapOf(
                            LaporanPeriode.HARI_INI to stringResource(R.string.laporan_period_daily),
                            LaporanPeriode.MINGGU_INI to stringResource(R.string.laporan_period_weekly),
                            LaporanPeriode.BULAN_INI to stringResource(R.string.laporan_period_monthly),
                            LaporanPeriode.TAHUN_INI to stringResource(R.string.laporan_period_yearly),
                            LaporanPeriode.SEMUA to stringResource(R.string.laporan_period_all)
                        )
                        LaporanPeriode.entries.forEach { filter ->
                            val isSelected = !isCustomDateRange && selectedPeriode == filter
                            Surface(
                                onClick = {
                                    isCustomDateRange = false
                                    viewModel.setLaporanPeriode(filter)
                                    showAllDetails = false
                                },
                                shape = RoundedCornerShape(SantoyaRadius.sm),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = if (isSelected) {
                                    null
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("chip_periode_${filter.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = SantoyaSize.touchTarget)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = chipLabels[filter] ?: filter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Loading Skeleton ATAU Empty State ATAU Ringkasan Finansial
        if (!isInitialDataLoaded && rawTransaksiList.isEmpty() && effectivePengeluaranList.isEmpty() && allKasbonLogs.isEmpty()) {
            item {
                SantoyaLoadingSkeleton(
                    itemCount = 4,
                    message = stringResource(R.string.laporan_loading)
                )
            }
        } else if (!summary.hasPeriodActivity) {
            item {
                SantoyaEmptyState(
                    emptyType = SantoyaEmptyType.LAPORAN,
                    title = stringResource(R.string.laporan_empty_title),
                    subtitle = stringResource(R.string.laporan_empty_subtitle),
                    primaryActionLabel = stringResource(R.string.laporan_empty_cta),
                    primaryActionIcon = Icons.Outlined.Add,
                    onPrimaryAction = {
                        viewModel.setTransaksiTab(0)
                        viewModel.navigateTo(SantoyaScreen.TRANSAKSI)
                    }
                )
            }
        } else {
            // 2A. Hero Card: Laba Bersih & Visualisasi Komposisi Arus Kas
            item {
                val heroContainerColor = if (isProfit) HeroGradientStart else HeroDeficitStart
                val heroSubPanelColor = if (isProfit) SantoyaNavyPanel else HeroDeficitPanel
                val heroBorderColor = if (isProfit) SantoyaNavyBorder else HeroGlowRoseSoft.copy(alpha = 0.45f)
                val heroAccent = if (isProfit) HeroGlowEmeraldSoft else HeroGlowRoseSoft
                val onHero = santoyaColors.onHero
                val formattedMargin = FormatUtils.formatPercent(summary.marginLabaPersen)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(SantoyaRadius.xl),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = heroContainerColor),
                    border = BorderStroke(1.dp, heroBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroContainerColor)
                            .drawBehind {
                                val w = size.width
                                val h = size.height
                                val curve1 = Path().apply {
                                    moveTo(w * 0.72f, 0f)
                                    cubicTo(w * 0.76f, h * 0.28f, w * 0.88f, h * 0.36f, w, h * 0.38f)
                                }
                                drawPath(
                                    path = curve1,
                                    color = onHero.copy(alpha = 0.24f),
                                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                val curve2 = Path().apply {
                                    moveTo(w * 0.82f, 0f)
                                    quadraticTo(w * 0.86f, h * 0.22f, w, h * 0.26f)
                                }
                                drawPath(
                                    path = curve2,
                                    color = heroAccent.copy(alpha = 0.36f),
                                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HydroGradientBadge(
                                gradientColors = if (isProfit) {
                                    listOf(AccentEmerald, AccentCyan)
                                } else {
                                    listOf(AccentRose, AccentAmber)
                                },
                                size = 40.dp,
                                cornerRadius = 13.dp,
                                elevation = 3.dp
                            ) {
                                HydroReportChartIcon(
                                    tint = onHero,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isProfit) {
                                        stringResource(R.string.laporan_profit_label)
                                    } else {
                                        stringResource(R.string.laporan_loss_label)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = heroAccent,
                                    letterSpacing = 0.6.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.laporan_profit_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onHero.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SantoyaAdaptiveCurrencyText(
                            amount = summary.labaBersih,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = onHero,
                            minFontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = heroSubPanelColor,
                                border = BorderStroke(1.dp, heroAccent.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = stringResource(R.string.laporan_margin_label, formattedMargin),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = heroAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = heroSubPanelColor,
                                border = BorderStroke(1.dp, heroBorderColor)
                            ) {
                                Text(
                                    text = if (isProfit) {
                                        stringResource(R.string.laporan_surplus)
                                    } else {
                                        stringResource(R.string.laporan_deficit)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onHero,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val positiveProfit = summary.labaBersih.coerceAtLeast(0.0)
                        val totalFlowBase = (positiveProfit + summary.totalPengeluaran + summary.totalKomisiKurir).coerceAtLeast(1.0)
                        val profitShare = (positiveProfit / totalFlowBase).toFloat().coerceIn(0f, 1f)
                        val opShare = (summary.totalPengeluaran / totalFlowBase).toFloat().coerceIn(0f, 1f)
                        val komisiShare = (summary.totalKomisiKurir / totalFlowBase).toFloat().coerceIn(0f, 1f)

                        val profitPct = (profitShare * 100).roundToInt()
                        val opPct = (opShare * 100).roundToInt()
                        val komisiPct = (komisiShare * 100).roundToInt()

                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.lg),
                            color = heroSubPanelColor,
                            border = BorderStroke(1.dp, heroBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.laporan_cashflow_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onHero.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.laporan_summary_counts,
                                            summary.filteredTx.size,
                                            summary.filteredKasbonLogs.size,
                                            summary.filteredExp.size
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        color = heroAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(onHero.copy(alpha = 0.15f))
                                ) {
                                    if (profitShare > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(profitShare.coerceAtLeast(0.03f))
                                                .height(8.dp)
                                                .background(HeroGlowEmeraldSoft)
                                        )
                                    }
                                    if (opShare > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(opShare.coerceAtLeast(0.03f))
                                                .height(8.dp)
                                                .background(HeroGlowRoseSoft)
                                        )
                                    }
                                    if (komisiShare > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(komisiShare.coerceAtLeast(0.03f))
                                                .height(8.dp)
                                                .background(HeroGlowAmberSoft)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LaporanCashflowLegendDot(
                                        color = HeroGlowEmeraldSoft,
                                        label = "Laba ($profitPct%)"
                                    )
                                    LaporanCashflowLegendDot(
                                        color = HeroGlowRoseSoft,
                                        label = "Operasional ($opPct%)"
                                    )
                                    LaporanCashflowLegendDot(
                                        color = HeroGlowAmberSoft,
                                        label = "Komisi ($komisiPct%)"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2B. Bento Grid Breakdown + Rincian Beban & Kasbon
            item {
                val pendapatanSubtitle = when {
                    summary.totalOmzetPenjualan <= 0.0 && summary.pelunasanKasbonPeriode > 0.0 ->
                        "${summary.filteredKasbonLogs.size} Pelunasan kasbon lama"
                    summary.dpCount > 0 ->
                        "${summary.lunasCount} Lunas • ${summary.dpCount} Sebagian (${(summary.collectionRatio * 100).roundToInt()}%)"
                    else ->
                        "${summary.lunasCount} Lunas (${(summary.collectionRatio * 100).roundToInt()}% tertagih)"
                }

                val expenseBadgeOverride = when {
                    summary.totalKasMasuk <= 0.0 && summary.totalBebanKeluar > 0.0 -> ">100%"
                    summary.totalKasMasuk <= 0.0 -> "0%"
                    else -> "${summary.expenseRatioActualPercent.roundToInt()}%"
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LaporanBentoFinancialCard(
                            modifier = Modifier
                                .weight(1.14f)
                                .fillMaxHeight(),
                            gradientColors = listOf(profitColor, AccentEmerald),
                            label = stringResource(R.string.laporan_total_revenue),
                            amount = FormatUtils.formatRupiah(summary.totalKasMasuk),
                            subtitle = pendapatanSubtitle,
                            accentColor = profitColor,
                            progress = summary.collectionRatio,
                            comparisonBadge = if (showMonthlyComparison) {
                                {
                                    LaporanMonthlyComparisonBadge(
                                        bulanIni = monthlyComparison.bulanIniKasMasuk,
                                        bulanLalu = monthlyComparison.bulanLaluKasMasuk,
                                        upColor = profitColor,
                                        downColor = lossColor
                                    )
                                }
                            } else {
                                null
                            },
                            iconContent = {
                                HydroDropletSparkIcon(
                                    tint = santoyaColors.onHero,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )

                        LaporanBentoFinancialCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            gradientColors = listOf(lossColor, AccentRose),
                            label = stringResource(R.string.laporan_total_expense),
                            amount = FormatUtils.formatRupiah(summary.totalBebanKeluar),
                            subtitle = stringResource(R.string.laporan_expense_ratio_desc),
                            accentColor = lossColor,
                            progress = summary.expenseRatioFraction,
                            badgePercentOverride = expenseBadgeOverride,
                            iconContent = {
                                HydroExpenseFlowIcon(
                                    tint = santoyaColors.onHero,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )
                    }

                    HydroBentoCard(
                        accentColor = MaterialTheme.colorScheme.primary,
                        cornerRadius = SantoyaRadius.lg,
                        tintAlpha = 0.05f,
                        showWaterRipple = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            LaporanFinancialBreakdownRow(
                                gradientColors = listOf(lossColor, AccentRose),
                                title = stringResource(R.string.laporan_operational_cost),
                                subtitle = "${summary.filteredExp.size} catatan pengeluaran",
                                nominalText = "- ${FormatUtils.formatRupiah(summary.totalPengeluaran)}",
                                accentColor = lossColor,
                                iconContent = {
                                    HydroExpenseFlowIcon(
                                        tint = santoyaColors.onHero,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )

                            LaporanFinancialBreakdownRow(
                                gradientColors = listOf(santoyaColors.warning, AccentAmber),
                                title = stringResource(R.string.laporan_courier_commission),
                                subtitle = stringResource(R.string.laporan_courier_commission_desc),
                                nominalText = "- ${FormatUtils.formatRupiah(summary.totalKomisiKurir)}",
                                accentColor = santoyaColors.warning,
                                iconContent = {
                                    HydroCourierWaveIcon(
                                        tint = santoyaColors.onHero,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            )

                            if (summary.piutangKasbonPeriode > 0.0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )

                                LaporanFinancialBreakdownRow(
                                    gradientColors = listOf(santoyaColors.warning, AccentRose),
                                    title = stringResource(R.string.laporan_receivables),
                                    subtitle = stringResource(R.string.laporan_receivables_desc),
                                    nominalText = FormatUtils.formatRupiah(summary.piutangKasbonPeriode),
                                    accentColor = santoyaColors.warning,
                                    iconContent = {
                                        HydroReceiptNoteIcon(
                                            tint = santoyaColors.onHero,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                )
                            }

                            if (summary.pelunasanKasbonPeriode > 0.0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )

                                LaporanFinancialBreakdownRow(
                                    gradientColors = listOf(profitColor, AccentEmerald),
                                    title = stringResource(R.string.laporan_receivable_payments),
                                    subtitle = stringResource(R.string.laporan_receivable_payments_desc),
                                    nominalText = FormatUtils.formatRupiah(summary.pelunasanKasbonPeriode),
                                    accentColor = profitColor,
                                    iconContent = {
                                        HydroReceiptNoteIcon(
                                            tint = santoyaColors.onHero,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2C. Grafik Tren Arus Kas Harian/Bulanan Interaktif
            if (summary.trendPoints.size >= 2) {
                item {
                    LaporanTrendChartCard(
                        trendPoints = summary.trendPoints,
                        isMonthly = summary.isMonthlyTrendGranularity,
                        profitColor = profitColor,
                        lossColor = lossColor
                    )
                }
            }

            // 2D. Kesehatan Umur Piutang Kasbon Aktif
            item {
                LaporanAgingCard(
                    agingSummary = kasbonAgingSummary,
                    onOpenKasbonCustomers = {
                        viewModel.setPelangganTab(1)
                        viewModel.navigateTo(SantoyaScreen.PELANGGAN)
                    }
                )
            }
        }

        // 3. Proyeksi Akhir Bulan (Mode Bulanan)
        if (showMonthlyComparison && summary.hasPeriodActivity) {
            item {
                val monthProgress = (currentDay.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)
                val projectedColor = if (monthlyProjection.projectedProfit >= 0.0) profitColor else lossColor
                HydroBentoCard(
                    accentColor = MaterialTheme.colorScheme.primary,
                    cornerRadius = SantoyaRadius.xl,
                    tintAlpha = 0.08f,
                    showWaterRipple = true,
                    modifier = Modifier.fillMaxWidth()
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
                                HydroGradientBadge(
                                    gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                                    size = 38.dp,
                                    cornerRadius = 12.dp,
                                    elevation = 3.dp
                                ) {
                                    HydroReportChartIcon(
                                        tint = santoyaColors.onHero,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.laporan_projection_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.laporan_projection_day, currentDay, daysInMonth),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(SantoyaRadius.sm),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = stringResource(R.string.laporan_projection_percent_month, (monthProgress * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { monthProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
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
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        HydroGalonIcon(
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.laporan_projection_volume),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${FormatUtils.formatNumber(monthlyProjection.projectedGalon)} ${stringResource(R.string.unit_galon)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.laporan_projection_actual_galon, monthlyProjection.monthGalon),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = projectedColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, projectedColor.copy(alpha = 0.28f)),
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                modifier = Modifier.weight(1.18f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(R.string.laporan_projection_profit),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    SantoyaAdaptiveCurrencyText(
                                        amount = monthlyProjection.projectedProfit,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = projectedColor,
                                        minFontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    SantoyaAdaptiveCurrencyText(
                                        amount = monthlyProjection.monthNetProfit,
                                        customText = stringResource(
                                            R.string.laporan_projection_actual_profit,
                                            FormatUtils.formatRupiah(monthlyProjection.monthNetProfit)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minFontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Peringkat Penjualan (Desa & Kurir)
        if (summary.hasPeriodActivity) {
            item {
                HydroBentoCard(
                    accentColor = santoyaColors.rankGold,
                    cornerRadius = SantoyaRadius.xl,
                    tintAlpha = 0.06f,
                    showWaterRipple = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HydroGradientBadge(
                                gradientColors = listOf(santoyaColors.rankGold, AccentAmber),
                                size = 38.dp,
                                cornerRadius = 12.dp,
                                elevation = 3.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEvents,
                                    contentDescription = null,
                                    tint = santoyaColors.onHero,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.laporan_ranking_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.laporan_ranking_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val isKurirTab = rankingTabIndex == 1
                        val activeEntries = if (!isKurirTab) rankedDesaEntries else rankedKurirEntries
                        val activityUnit = if (!isKurirTab) "Transaksi" else "Kirim"
                        val unitCategoryLabel = if (!isKurirTab) "Wilayah" else "Kurir"

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
                                label = { Text(stringResource(R.string.laporan_ranking_tab_wilayah, rankedDesaEntries.size), maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("chip_ranking_wilayah"),
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
                                label = { Text(stringResource(R.string.laporan_ranking_tab_kurir, rankedKurirEntries.size), maxLines = 1) },
                                leadingIcon = {
                                    HydroCourierWaveIcon(
                                        tint = if (rankingTabIndex == 1) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("chip_ranking_kurir"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        if (activeEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.laporan_sort_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isGalonSort = rankingSortMode == RankingSortMode.GALON
                                    SantoyaFilterPill(
                                        label = stringResource(R.string.laporan_sort_galon),
                                        selected = isGalonSort,
                                        onClick = { rankingSortMode = RankingSortMode.GALON },
                                        centerContent = true,
                                        leadingIconContent = {
                                            HydroGalonIcon(
                                                tint = if (isGalonSort) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chip_sort_ranking_galon")
                                    )
                                    val isOmzetSort = rankingSortMode == RankingSortMode.OMZET
                                    SantoyaFilterPill(
                                        label = stringResource(R.string.laporan_sort_omzet),
                                        selected = isOmzetSort,
                                        onClick = { rankingSortMode = RankingSortMode.OMZET },
                                        centerContent = true,
                                        leadingIconContent = {
                                            HydroReportChartIcon(
                                                tint = if (isOmzetSort) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chip_sort_ranking_omzet")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        if (activeEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.laporan_empty_tx_period),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            val displayedEntries = if (showAllRanking) activeEntries else activeEntries.take(5)
                            Column(
                                modifier = Modifier.animateContentSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayedEntries.forEachIndexed { index, entry ->
                                    LaporanSalesRankingItemCard(
                                        rank = index + 1,
                                        entry = entry,
                                        activityUnit = activityUnit,
                                        sortMode = rankingSortMode,
                                        showCommission = isKurirTab
                                    )
                                }
                                if (showAllRanking && activeEntries.size > 5) {
                                    val totalGalonTab = activeEntries.sumOf { it.totalGalon }
                                    val totalOmzetTab = activeEntries.sumOf { it.totalPendapatan }
                                    Surface(
                                        shape = RoundedCornerShape(SantoyaRadius.md),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Total ${activeEntries.size} $unitCategoryLabel • ${FormatUtils.formatNumber(totalGalonTab)} Galon",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            SantoyaAdaptiveCurrencyText(
                                                amount = totalOmzetTab,
                                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                minFontSize = 11.sp,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                                if (activeEntries.size > 5) {
                                    OutlinedButton(
                                        onClick = { showAllRanking = !showAllRanking },
                                        shape = RoundedCornerShape(SantoyaRadius.sm),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = SantoyaSize.touchTarget)
                                            .testTag("btn_toggle_all_ranking")
                                    ) {
                                        Text(
                                            text = if (showAllRanking) {
                                                stringResource(R.string.laporan_show_top_5)
                                            } else {
                                                stringResource(R.string.laporan_show_all_count, activeEntries.size)
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Panel Ekspor Laporan PDF & CSV
        item {
            HydroBentoCard(
                accentColor = MaterialTheme.colorScheme.secondary,
                cornerRadius = SantoyaRadius.xl,
                tintAlpha = 0.06f,
                showWaterRipple = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HydroGradientBadge(
                            gradientColors = listOf(MaterialTheme.colorScheme.secondary, AccentCyan),
                            size = 34.dp,
                            cornerRadius = 11.dp,
                            elevation = 2.dp
                        ) {
                            HydroReceiptNoteIcon(
                                tint = santoyaColors.onHero,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.laporan_export_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.laporan_export_summary_title),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.laporan_export_summary_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSummaryMode,
                            onCheckedChange = { isSummaryMode = it },
                            modifier = Modifier.testTag("switch_pdf_summary_mode")
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
                                if (!isKaryawan && depotProfile.isOwnerRole() && !depotProfile.hasConfiguredDepotName()) {
                                    showQuickFillForPdf = true
                                } else {
                                    launchPdfExport(depotProfile)
                                }
                            },
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SantoyaSize.touchTarget)
                                .testTag("btn_export_pdf")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = stringResource(R.string.laporan_export_pdf),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.laporan_export_pdf),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val csvContent = ExportHelper.generateCsv(
                                    transaksiList = effectiveTransaksiList,
                                    pengeluaranList = effectivePengeluaranList,
                                    kasbonLogList = allKasbonLogs,
                                    pelangganList = allPelanggan,
                                    startTime = startTime,
                                    endTime = endTime,
                                    periodeLabel = periodRangeLabel,
                                    kasbonAgingSummary = kasbonAgingSummary,
                                    rankedDesaEntries = rankedDesaEntries
                                )
                                val csvFileName = ExportHelper.buildExportFileName(
                                    prefix = "Laporan_Santoya",
                                    periodeLabel = periodRangeLabel,
                                    extension = "csv"
                                )
                                ExportHelper.shareExportFile(
                                    context = context,
                                    filename = csvFileName,
                                    content = csvContent,
                                    mimeType = "text/csv"
                                )
                            },
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SantoyaSize.touchTarget)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TableView,
                                contentDescription = stringResource(R.string.laporan_export_csv),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.laporan_export_csv),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Rincian Transaksi, Pelunasan Kasbon, & Pengeluaran (3 Tab)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(SantoyaRadius.lg),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        modifier = Modifier.testTag("tab_laporan_transaksi"),
                        text = {
                            Text(
                                text = stringResource(R.string.laporan_tab_transaksi, summary.filteredTx.size),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = detailTabIndex == 1,
                        onClick = {
                            detailTabIndex = 1
                            showAllDetails = false
                        },
                        modifier = Modifier.testTag("tab_laporan_pelunasan"),
                        text = {
                            Text(
                                text = stringResource(R.string.laporan_tab_pelunasan, summary.filteredKasbonLogs.size),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = detailTabIndex == 2,
                        onClick = {
                            detailTabIndex = 2
                            showAllDetails = false
                        },
                        modifier = Modifier.testTag("tab_laporan_pengeluaran"),
                        text = {
                            Text(
                                text = stringResource(R.string.laporan_tab_pengeluaran, summary.filteredExp.size),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        when (detailTabIndex) {
            0 -> {
                item {
                    SantoyaSearchBar(
                        query = txSearchQuery,
                        onQueryChange = { txSearchQuery = it },
                        placeholder = stringResource(R.string.laporan_search_tx_placeholder),
                        testTag = "input_search_laporan_tx",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val searchedTx = if (txSearchQuery.isBlank()) {
                    summary.filteredTx
                } else {
                    val q = txSearchQuery.trim().lowercase()
                    summary.filteredTx.filter { item ->
                        val cName = item.pelanggan?.nama?.lowercase() ?: "umum"
                        val kName = item.kurir?.nama?.lowercase().orEmpty()
                        val pName = item.produk?.nama_produk?.lowercase().orEmpty()
                        val metode = item.transaksi.metode_bayar.lowercase()
                        val statusStr = item.transaksi.status.lowercase()
                        cName.contains(q) || kName.contains(q) || pName.contains(q) || metode.contains(q) || statusStr.contains(q)
                    }
                }

                if (searchedTx.isEmpty()) {
                    item {
                        LaporanEmptyPeriodListCard(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            message = stringResource(R.string.laporan_empty_tx_period)
                        )
                    }
                } else {
                    val displayedTx = if (showAllDetails) searchedTx else searchedTx.take(15)
                    items(displayedTx, key = { it.transaksi.id }) { item ->
                        val trx = item.transaksi
                        val isLunas = trx.status.equals("Lunas", ignoreCase = true)
                        val isBayarSebagian = !isLunas && trx.uangMuka > 0L
                        val metodeInfo = when {
                            isLunas -> FormatUtils.formatMetodeBayarLabel(trx.metode_bayar)
                            isBayarSebagian -> "Bayar Sebagian"
                            else -> "Belum Dibayar"
                        }
                        val itemAccent = if (isLunas) profitColor else santoyaColors.warning
                        val badgeGradient = if (isLunas) {
                            listOf(profitColor, AccentEmerald)
                        } else {
                            listOf(santoyaColors.warning, AccentAmber)
                        }
                        val customerName = item.pelanggan?.nama?.trim()?.ifBlank { "Umum" } ?: "Umum"

                        HydroBentoCard(
                            accentColor = itemAccent,
                            cornerRadius = SantoyaRadius.lg,
                            tintAlpha = 0.06f,
                            showWaterRipple = false,
                            onClick = {
                                val cKasbon = item.pelanggan?.sisa_kasbon ?: trx.remainingKasbonAmount()
                                val receiptText = SantoyaReceiptHelper.generateSaleReceipt(item, cKasbon, depotProfile)
                                viewModel.showReceiptDialog(
                                    ActiveReceiptDialog(
                                        title = "Struk Transaksi Penjualan",
                                        receiptText = receiptText,
                                        customerPhone = item.pelanggan?.no_hp,
                                        data = item,
                                        receiptBuilder = { profile ->
                                            SantoyaReceiptHelper.generateSaleReceipt(item, cKasbon, profile)
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    HydroGradientBadge(
                                        gradientColors = badgeGradient,
                                        size = 36.dp,
                                        cornerRadius = 11.dp,
                                        elevation = 2.dp
                                    ) {
                                        HydroGalonIcon(
                                            tint = santoyaColors.onHero,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$customerName • ${trx.jumlah} ${stringResource(R.string.unit_galon)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${FormatUtils.formatDateUi(trx.tanggal)} • $metodeInfo • Komisi: ${FormatUtils.formatRupiah(trx.total_komisi)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isBayarSebagian) {
                                            Text(
                                                text = "Terbayar: ${FormatUtils.formatRupiah(trx.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(trx.remainingKasbonAmount())}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.Medium,
                                                color = santoyaColors.warning,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.widthIn(max = 136.dp)
                                ) {
                                    SantoyaAdaptiveCurrencyText(
                                        amount = trx.total_harga,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLunas) profitColor else santoyaColors.warning,
                                        minFontSize = 10.sp,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = trx.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLunas) profitColor else santoyaColors.warning,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (searchedTx.size > 15) {
                        item {
                            OutlinedButton(
                                onClick = { showAllDetails = !showAllDetails },
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("btn_toggle_all_tx_details")
                            ) {
                                Text(
                                    text = if (showAllDetails) {
                                        stringResource(R.string.laporan_show_latest_15)
                                    } else {
                                        stringResource(R.string.laporan_show_all_count, searchedTx.size)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                if (summary.filteredKasbonLogs.isEmpty()) {
                    item {
                        LaporanEmptyPeriodListCard(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            message = stringResource(R.string.laporan_empty_pelunasan_period)
                        )
                    }
                } else {
                    val displayedLogs = if (showAllDetails) summary.filteredKasbonLogs else summary.filteredKasbonLogs.take(15)
                    items(displayedLogs, key = { "log_${it.log.id}_${it.log.syncId}" }) { logDetail ->
                        LaporanKasbonLogItemCard(
                            item = logDetail,
                            profitColor = profitColor
                        )
                    }

                    if (summary.filteredKasbonLogs.size > 15) {
                        item {
                            OutlinedButton(
                                onClick = { showAllDetails = !showAllDetails },
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("btn_toggle_all_pelunasan_details")
                            ) {
                                Text(
                                    text = if (showAllDetails) {
                                        stringResource(R.string.laporan_show_latest_15)
                                    } else {
                                        stringResource(R.string.laporan_show_all_count, summary.filteredKasbonLogs.size)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                if (summary.filteredExp.isEmpty()) {
                    item {
                        LaporanEmptyPeriodListCard(
                            icon = Icons.Outlined.ShoppingCart,
                            message = stringResource(R.string.laporan_empty_expense_period)
                        )
                    }
                } else {
                    val displayedExp = if (showAllDetails) summary.filteredExp else summary.filteredExp.take(15)
                    items(displayedExp, key = { it.id }) { pengeluaran ->
                        val catColor = resolvePengeluaranCategoryColor(
                            kategori = pengeluaran.kategori,
                            namaItem = pengeluaran.nama_item
                        )
                        HydroBentoCard(
                            accentColor = catColor,
                            cornerRadius = SantoyaRadius.lg,
                            tintAlpha = 0.06f,
                            showWaterRipple = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    HydroGradientBadge(
                                        gradientColors = listOf(catColor, AccentRose),
                                        size = 36.dp,
                                        cornerRadius = 11.dp,
                                        elevation = 2.dp
                                    ) {
                                        PengeluaranCategoryIcon(
                                            kategori = pengeluaran.kategori,
                                            namaItem = pengeluaran.nama_item,
                                            tint = santoyaColors.onHero,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pengeluaran.nama_item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pengeluaran.kategori} • ${FormatUtils.formatDateUi(pengeluaran.tanggal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SantoyaAdaptiveCurrencyText(
                                    amount = pengeluaran.nominal,
                                    customText = "- ${FormatUtils.formatRupiah(pengeluaran.nominal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = lossColor,
                                    minFontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.widthIn(max = 136.dp)
                                )
                            }
                        }
                    }

                    if (summary.filteredExp.size > 15) {
                        item {
                            OutlinedButton(
                                onClick = { showAllDetails = !showAllDetails },
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("btn_toggle_all_exp_details")
                            ) {
                                Text(
                                    text = if (showAllDetails) {
                                        stringResource(R.string.laporan_show_latest_15)
                                    } else {
                                        stringResource(R.string.laporan_show_all_count, summary.filteredExp.size)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickFillForPdf) {
        DepotQuickFillDialog(
            initialProfile = depotProfile,
            onDismiss = { showQuickFillForPdf = false },
            onSaveAndContinue = { name, addr, phone ->
                showQuickFillForPdf = false
                viewModel.quickSaveDepotProfile(name, addr, phone) { updated ->
                    launchPdfExport(updated)
                }
            },
            onSkip = {
                showQuickFillForPdf = false
                launchPdfExport(depotProfile)
            }
        )
    }
}
