package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FinancialTrendPoint
import com.example.util.FormatUtils
import com.example.util.KasbonAgingSummary
import com.example.util.KasbonLogDetail
import com.example.util.LaporanFinanceCalculator
import com.example.util.LaporanPeriodSummary
import com.example.util.RankingEntryUi
import com.example.util.RankingMetricsHelper
import com.example.util.RankingSortMode
import kotlin.math.roundToInt

/**
 * Titik legenda berwarna di dalam sub-panel Komposisi Arus Kas Hero Card.
 */
@Composable
fun LaporanCashflowLegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.santoyaColors.onHero.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Lencana persentase pertumbuhan kas masuk bulan ini dibanding bulan sebelumnya.
 */
@Composable
fun LaporanMonthlyComparisonBadge(
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
        shape = RoundedCornerShape(SantoyaRadius.xs),
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
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = badgeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Kartu metrik Bento grid untuk Total Pendapatan Kas Masuk dan Total Pengeluaran + Komisi.
 */
@Composable
fun LaporanBentoFinancialCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    label: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    progress: Float,
    badgePercentOverride: String? = null,
    comparisonBadge: (@Composable () -> Unit)? = null,
    iconContent: @Composable () -> Unit
) {
    val badgeText = badgePercentOverride ?: "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.09f,
        showWaterRipple = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                HydroGradientBadge(
                    gradientColors = gradientColors,
                    size = 38.dp,
                    cornerRadius = 12.dp,
                    elevation = 3.dp
                ) {
                    iconContent()
                }
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            SantoyaAdaptiveCurrencyText(
                amount = 0.0,
                customText = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                minFontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
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
                    .height(5.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Baris rincian finansial dengan ikon gradien di sisi kiri dan nominal adaptif di sisi kanan.
 */
@Composable
fun LaporanFinancialBreakdownRow(
    gradientColors: List<Color>,
    title: String,
    subtitle: String,
    nominalText: String,
    accentColor: Color,
    iconContent: @Composable () -> Unit
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
            HydroGradientBadge(
                gradientColors = gradientColors,
                size = 38.dp,
                cornerRadius = 12.dp,
                elevation = 2.dp
            ) {
                iconContent()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        SantoyaAdaptiveCurrencyText(
            amount = 0.0,
            customText = nominalText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            minFontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 148.dp)
        )
    }
}

/**
 * Kartu Arus Kas & Rekonsiliasi Pembayaran (Omzet Nota vs Kas Masuk Tunai/Transfer + Proporsi Kategori Biaya).
 */
@Composable
fun LaporanPaymentReconciliationCard(
    summary: LaporanPeriodSummary,
    profitColor: Color,
    lossColor: Color
) {
    val santoyaColors = MaterialTheme.santoyaColors
    var expandExpenseCategories by remember { mutableStateOf(false) }

    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.primary,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.05f,
        showWaterRipple = false,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HydroGradientBadge(
                    gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                    size = 36.dp,
                    cornerRadius = 11.dp,
                    elevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = santoyaColors.onHero,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.laporan_cash_bridge_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.laporan_omzet_nota_desc,
                            summary.filteredTx.size,
                            summary.lunasCount,
                            summary.dpCount,
                            summary.fullKasbonCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Omzet Nota (Akrual barang keluar) vs Kas Masuk Tunai & Transfer
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.laporan_omzet_nota),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = FormatUtils.formatRupiah(summary.totalOmzetPenjualan),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            color = profitColor.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, profitColor.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = profitColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.laporan_kas_tunai),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                SantoyaAdaptiveCurrencyText(
                                    amount = summary.totalKasTunai,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = profitColor,
                                    minFontSize = 10.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = stringResource(
                                        R.string.laporan_kas_source_breakdown,
                                        FormatUtils.formatRupiah(summary.kasTunaiDariJual),
                                        FormatUtils.formatRupiah(summary.kasTunaiDariKasbon)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            color = santoyaColors.info.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, santoyaColors.info.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        tint = santoyaColors.info,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.laporan_kas_transfer),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                SantoyaAdaptiveCurrencyText(
                                    amount = summary.totalKasNonTunai,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = santoyaColors.info,
                                    minFontSize = 10.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = stringResource(
                                        R.string.laporan_kas_source_breakdown,
                                        FormatUtils.formatRupiah(summary.kasTransferDariJual),
                                        FormatUtils.formatRupiah(summary.kasTransferDariKasbon)
                                    ),
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

            // Proporsi Kategori Pengeluaran (dapat diperluas jika ada pengeluaran)
            if (summary.expenseCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SantoyaRadius.md))
                        .clickable { expandExpenseCategories = !expandExpenseCategories }
                        .testTag("btn_toggle_expense_categories")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.laporan_expense_category_title)} (${summary.expenseCategories.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (expandExpenseCategories) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = expandExpenseCategories) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                summary.expenseCategories.forEach { cat ->
                                    val catColor = resolvePengeluaranCategoryColor(kategori = cat.kategori)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                PengeluaranCategoryIcon(
                                                    kategori = cat.kategori,
                                                    tint = catColor,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "${cat.kategori} (${cat.count}x)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${FormatUtils.formatRupiah(cat.nominal)} • ${FormatUtils.formatPercent(cat.sharePercent, 0)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.SemiBold,
                                                color = lossColor
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (cat.sharePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = catColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grafik batang interaktif Tren Arus Kas (Harian atau Bulanan) menggunakan Compose Canvas + selector sentuh.
 */
@Composable
fun LaporanTrendChartCard(
    trendPoints: List<FinancialTrendPoint>,
    isMonthly: Boolean,
    profitColor: Color,
    lossColor: Color
) {
    if (trendPoints.size < 2) return

    val santoyaColors = MaterialTheme.santoyaColors
    val visiblePoints = remember(trendPoints) {
        if (trendPoints.size > 14) trendPoints.takeLast(14) else trendPoints
    }
    var selectedIdx by remember(visiblePoints) { mutableIntStateOf(visiblePoints.lastIndex) }
    val safeSelectedIdx = selectedIdx.coerceIn(0, visiblePoints.lastIndex)
    val selectedPoint = visiblePoints[safeSelectedIdx]
    val maxVal = remember(visiblePoints) {
        visiblePoints.maxOf { maxOf(it.kasMasuk, it.bebanKeluar) }.coerceAtLeast(1.0)
    }

    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.primary,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.05f,
        showWaterRipple = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    HydroGradientBadge(
                        gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                        size = 36.dp,
                        cornerRadius = 11.dp,
                        elevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = santoyaColors.onHero,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isMonthly) {
                                stringResource(R.string.laporan_trend_title_monthly)
                            } else {
                                stringResource(R.string.laporan_trend_title_daily)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.laporan_trend_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(profitColor))
                    Text(
                        text = stringResource(R.string.laporan_trend_legend_in),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(lossColor))
                    Text(
                        text = stringResource(R.string.laporan_trend_legend_out),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kartu detail titik terpilih
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedPoint.fullDateLabel} • ${selectedPoint.totalGalon} Galon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Masuk ${FormatUtils.formatRupiah(selectedPoint.kasMasuk)} • Beban ${FormatUtils.formatRupiah(selectedPoint.bebanKeluar)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FormatUtils.formatRupiah(selectedPoint.labaBersih),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPoint.labaBersih >= 0.0) profitColor else lossColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Batang grafik bergulir horizontal jika banyak hari
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                visiblePoints.forEachIndexed { index, point ->
                    val isSelected = index == safeSelectedIdx
                    val inFraction = (point.kasMasuk / maxVal).toFloat().coerceIn(0f, 1f)
                    val outFraction = (point.bebanKeluar / maxVal).toFloat().coerceIn(0f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(44.dp)
                            .clip(RoundedCornerShape(SantoyaRadius.sm))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { selectedIdx = index }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("trend_bar_$index")
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                        ) {
                            val baseY = size.height
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, baseY),
                                end = Offset(size.width, baseY),
                                strokeWidth = 1.dp.toPx()
                            )
                            val barWidth = (size.width * 0.34f).coerceAtLeast(6.dp.toPx())
                            val gap = 3.dp.toPx()
                            val startX = (size.width - (barWidth * 2f + gap)) / 2f
                            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                            val inHeight = if (point.kasMasuk > 0.0) (inFraction * (baseY - 4.dp.toPx())).coerceAtLeast(4.dp.toPx()) else 0f
                            if (inHeight > 0f) {
                                drawRoundRect(
                                    color = if (isSelected) profitColor else profitColor.copy(alpha = 0.72f),
                                    topLeft = Offset(startX, baseY - inHeight),
                                    size = Size(barWidth, inHeight),
                                    cornerRadius = radius
                                )
                            }

                            val outHeight = if (point.bebanKeluar > 0.0) (outFraction * (baseY - 4.dp.toPx())).coerceAtLeast(4.dp.toPx()) else 0f
                            if (outHeight > 0f) {
                                drawRoundRect(
                                    color = if (isSelected) lossColor else lossColor.copy(alpha = 0.72f),
                                    topLeft = Offset(startX + barWidth + gap, baseY - outHeight),
                                    size = Size(barWidth, outHeight),
                                    cornerRadius = radius
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Kesehatan Umur Piutang Kasbon Aktif.
 */
@Composable
fun LaporanAgingCard(
    agingSummary: KasbonAgingSummary,
    onOpenKasbonCustomers: () -> Unit
) {
    val santoyaColors = MaterialTheme.santoyaColors

    if (agingSummary.totalActiveReceivable > 0.0 || agingSummary.totalActiveNotes > 0) {
        HydroBentoCard(
            accentColor = santoyaColors.warning,
            cornerRadius = SantoyaRadius.xl,
            tintAlpha = 0.06f,
            showWaterRipple = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        HydroGradientBadge(
                            gradientColors = listOf(santoyaColors.warning, AccentAmber),
                            size = 36.dp,
                            cornerRadius = 11.dp,
                            elevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = santoyaColors.onHero,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.laporan_aging_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.laporan_aging_subtitle, agingSummary.totalActiveNotes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = FormatUtils.formatRupiah(agingSummary.totalActiveReceivable),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = santoyaColors.warning
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgingBucketChip(
                        title = stringResource(R.string.laporan_aging_bucket_0_7),
                        count = agingSummary.bucket0To7Count,
                        amount = agingSummary.bucket0To7Amount,
                        accentColor = santoyaColors.info,
                        modifier = Modifier.weight(1f)
                    )
                    AgingBucketChip(
                        title = stringResource(R.string.laporan_aging_bucket_8_30),
                        count = agingSummary.bucket8To30Count,
                        amount = agingSummary.bucket8To30Amount,
                        accentColor = santoyaColors.warning,
                        modifier = Modifier.weight(1f)
                    )
                    AgingBucketChip(
                        title = stringResource(R.string.laporan_aging_bucket_over_30),
                        count = agingSummary.bucketOver30Count,
                        amount = agingSummary.bucketOver30Amount,
                        accentColor = santoyaColors.danger,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenKasbonCustomers,
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = santoyaColors.warning),
                    border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_open_kasbon_from_laporan")
                ) {
                    Text(
                        text = stringResource(R.string.laporan_aging_cta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgingBucketChip(
    title: String,
    count: Int,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.sm),
        color = accentColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SantoyaAdaptiveCurrencyText(
                amount = amount,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                minFontSize = 9.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "$count nota",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Kartu peringkat penjualan per desa atau per kurir (menampilkan komisi kurir bila di tab Kurir).
 */
@Composable
fun LaporanSalesRankingItemCard(
    rank: Int,
    entry: RankingEntryUi,
    activityUnit: String,
    sortMode: RankingSortMode,
    showCommission: Boolean = false
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val rankColor = when (rank) {
        1 -> santoyaColors.rankGold
        2 -> santoyaColors.rankSilver
        3 -> santoyaColors.rankBronze
        else -> MaterialTheme.colorScheme.primary
    }
    val rankGradient = when (rank) {
        1 -> listOf(santoyaColors.rankGold, AccentAmber)
        2 -> listOf(santoyaColors.rankSilver, santoyaColors.rankSilver.copy(alpha = 0.78f))
        3 -> listOf(santoyaColors.rankBronze, AccentAmber)
        else -> listOf(MaterialTheme.colorScheme.primary, AccentCyan)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = entry.progressFraction,
        animationSpec = tween(durationMillis = 260),
        label = "ranking_progress_anim"
    )
    val shareText = remember(entry.omzetSharePercent) {
        RankingMetricsHelper.formatOmzetSharePercent(entry.omzetSharePercent)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, rankColor.copy(alpha = if (rank <= 3) 0.25f else 0.12f)),
        shape = RoundedCornerShape(SantoyaRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (rank <= 3) {
                        HydroGradientBadge(
                            gradientColors = rankGradient,
                            size = 30.dp,
                            cornerRadius = 9.dp,
                            elevation = 2.dp
                        ) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = santoyaColors.onHero
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            color = rankColor.copy(alpha = 0.14f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#$rank",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = rankColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = if (sortMode == RankingSortMode.GALON) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        HydroGalonIcon(
                            tint = if (sortMode == RankingSortMode.GALON) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${FormatUtils.formatNumber(entry.totalGalon)} Galon",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = if (sortMode == RankingSortMode.GALON) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCommission && entry.totalKomisi > 0.0) {
                        "${FormatUtils.formatNumber(entry.totalTransaksi)} $activityUnit • Komisi: ${FormatUtils.formatRupiah(entry.totalKomisi)}"
                    } else {
                        "${FormatUtils.formatNumber(entry.totalTransaksi)} $activityUnit • $shareText"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                SantoyaAdaptiveCurrencyText(
                    amount = entry.totalPendapatan,
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = if (sortMode == RankingSortMode.OMZET) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    minFontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = rankColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Kartu satu baris catatan pelunasan kasbon di tab Pelunasan pada Laporan Keuangan.
 */
@Composable
fun LaporanKasbonLogItemCard(
    item: KasbonLogDetail,
    profitColor: Color
) {
    val log = item.log
    val santoyaColors = MaterialTheme.santoyaColors
    val isNonCash = LaporanFinanceCalculator.isNonCashPaymentMethod(log.metode_bayar)
    val badgeColor = if (isNonCash) santoyaColors.info else profitColor

    HydroBentoCard(
        accentColor = badgeColor,
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
                    gradientColors = listOf(badgeColor, if (isNonCash) AccentCyan else AccentEmerald),
                    size = 36.dp,
                    cornerRadius = 11.dp,
                    elevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = santoyaColors.onHero,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.namaPelanggan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FormatUtils.formatDateTimeUi(log.tanggal)} • ${FormatUtils.formatMetodeBayarLabel(log.metode_bayar)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (log.catatan.isNotBlank()) {
                        Text(
                            text = log.catatan,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    amount = log.jumlah_bayar,
                    customText = "+ ${FormatUtils.formatRupiah(log.jumlah_bayar)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    minFontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = if (log.sisa_kasbon_sesudah <= 0.0) {
                        "LUNAS"
                    } else {
                        "Sisa ${FormatUtils.formatRupiah(log.sisa_kasbon_sesudah)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (log.sisa_kasbon_sesudah <= 0.0) profitColor else santoyaColors.warning,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Kartu placeholder saat daftar detail periode kosong pada tab aktif.
 */
@Composable
fun LaporanEmptyPeriodListCard(
    icon: ImageVector,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(SantoyaRadius.lg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
