package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.Kurir
import com.example.ui.theme.HeroGlowAmber
import com.example.ui.theme.HeroGlowAmberSoft
import com.example.ui.theme.HeroGlowCyan
import com.example.ui.theme.HeroGlowCyanSoft
import com.example.ui.theme.HeroGlowEmerald
import com.example.ui.theme.HeroGlowEmeraldSoft
import com.example.ui.theme.HeroGlowRose
import com.example.ui.theme.HeroGlowRoseSoft
import com.example.ui.theme.SantoyaLeafGreen
import com.example.ui.theme.SantoyaNavy
import com.example.ui.theme.SantoyaNavyBorder
import com.example.ui.theme.SantoyaNavyPanel
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaWaterBlue
import com.example.ui.theme.pressClickEffect
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.KaryawanDashboardMetrics
import com.example.util.PerawatanEvaluation
import com.example.util.PerawatanStatus
import kotlin.math.PI
import kotlin.math.sin

/**
 * Kartu Hero Hydro-Fintech dengan animasi gelombang air dua lapis berbasis Canvas
 * yang menyajikan KPI finansial dan volume galon secara real-time untuk Pemilik maupun Karyawan.
 */
@Composable
fun HydroWaveHeroCard(
    isKaryawan: Boolean,
    isCompact: Boolean,
    pendapatanLunasHariIni: Double,
    labaBersihHariIni: Double,
    totalPiutangAktif: Double,
    totalGalonHariIni: Int,
    estimasiGalonBulanIni: Int,
    karyawanMetrics: KaryawanDashboardMetrics,
    activeKaryawan: Kurir?,
    pelangganCount: Int,
    onPrimaryHeroClick: () -> Unit,
    onNetProfitClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onGalonTodayClick: () -> Unit,
    onMonthEstimateClick: () -> Unit,
    onStaffCustomersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val infiniteTransition = rememberInfiniteTransition(label = "hydro_wave_transition")
    val wavePhasePrimary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_primary"
    )
    val wavePhaseSecondary by infiniteTransition.animateFloat(
        initialValue = (2f * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_secondary"
    )

    val wavePathPrimary = remember { Path() }
    val wavePathSecondary = remember { Path() }
    val veinCurveTop = remember { Path() }
    val veinCurveInner = remember { Path() }

    val cardPadding = if (isCompact) 12.dp else 16.dp
    val subPanelPadding = if (isCompact) 9.dp else 11.dp

    Card(
        shape = RoundedCornerShape(SantoyaRadius.xxl),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = SantoyaNavy),
        border = BorderStroke(1.dp, SantoyaNavyBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_wave_card")
            .animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SantoyaNavy)
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // Gelombang air lapis belakang (Emerald Leaf Tint)
                    wavePathSecondary.reset()
                    val baseYSecondary = h * 0.80f
                    val ampSecondary = 7.dp.toPx()
                    wavePathSecondary.moveTo(0f, h)
                    wavePathSecondary.lineTo(0f, baseYSecondary)
                    var x2 = 0f
                    while (x2 <= w + 16f) {
                        val angle = (x2 / w) * 2.2f * PI.toFloat() + wavePhaseSecondary
                        val y = baseYSecondary + sin(angle) * ampSecondary
                        wavePathSecondary.lineTo(x2, y)
                        x2 += 16f
                    }
                    wavePathSecondary.lineTo(w, h)
                    wavePathSecondary.close()
                    drawPath(
                        path = wavePathSecondary,
                        color = HeroGlowEmeraldSoft.copy(alpha = 0.12f)
                    )

                    // Gelombang air lapis depan (Hydro Cyan)
                    wavePathPrimary.reset()
                    val baseYPrimary = h * 0.85f
                    val ampPrimary = 6.dp.toPx()
                    wavePathPrimary.moveTo(0f, h)
                    wavePathPrimary.lineTo(0f, baseYPrimary)
                    var x1 = 0f
                    while (x1 <= w + 16f) {
                        val angle = (x1 / w) * 2.6f * PI.toFloat() + wavePhasePrimary
                        val y = baseYPrimary + sin(angle) * ampPrimary
                        wavePathPrimary.lineTo(x1, y)
                        x1 += 16f
                    }
                    wavePathPrimary.lineTo(w, h)
                    wavePathPrimary.close()
                    drawPath(
                        path = wavePathPrimary,
                        color = HeroGlowCyanSoft.copy(alpha = 0.15f)
                    )

                    // Garis lengkung organik daun & tetesan air di kanan atas
                    veinCurveTop.reset()
                    veinCurveTop.moveTo(w * 0.70f, 0f)
                    veinCurveTop.cubicTo(w * 0.75f, h * 0.24f, w * 0.88f, h * 0.30f, w, h * 0.32f)
                    drawPath(
                        path = veinCurveTop,
                        color = santoyaColors.onHero.copy(alpha = 0.20f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    veinCurveInner.reset()
                    veinCurveInner.moveTo(w * 0.81f, 0f)
                    veinCurveInner.quadraticTo(w * 0.86f, h * 0.18f, w, h * 0.22f)
                    drawPath(
                        path = veinCurveInner,
                        color = HeroGlowEmeraldSoft.copy(alpha = 0.34f),
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                .padding(cardPadding)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)) {
                // Baris Atas: Pendapatan/Setoran Utama + Badge Tanggal & Toren/Karyawan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SantoyaRadius.md))
                        .pressClickEffect(onClick = onPrimaryHeroClick),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = HeroGlowCyan.copy(alpha = 0.35f),
                                modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = HeroGlowCyanSoft,
                                        modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isKaryawan) {
                                    stringResource(R.string.dashboard_hero_staff_deposit)
                                } else {
                                    stringResource(R.string.dashboard_hero_owner_revenue)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = santoyaColors.onHero.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))
                        SantoyaAdaptiveCurrencyText(
                            amount = if (isKaryawan) karyawanMetrics.setoranTunaiHariIni else pendapatanLunasHariIni,
                            style = if (isCompact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = santoyaColors.onHero,
                            minFontSize = if (isCompact) 18.sp else 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.pill),
                            color = SantoyaNavyPanel,
                            border = BorderStroke(1.dp, SantoyaNavyBorder)
                        ) {
                            Text(
                                text = FormatUtils.formatDateUi(System.currentTimeMillis()),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = santoyaColors.onHero,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompact) 9.dp else 11.dp,
                                    vertical = if (isCompact) 4.dp else 5.dp
                                )
                            )
                        }
                        if (isKaryawan && activeKaryawan != null) {
                            Surface(
                                shape = RoundedCornerShape(SantoyaRadius.pill),
                                color = SantoyaLeafGreen,
                                border = BorderStroke(1.dp, HeroGlowEmeraldSoft)
                            ) {
                                Text(
                                    text = activeKaryawan.nama,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = santoyaColors.onHero,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(
                                        horizontal = if (isCompact) 8.dp else 10.dp,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // Panel Tengah & Bawah sesuai Role
                if (!isKaryawan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                    ) {
                        val isPositiveProfit = labaBersihHariIni >= 0.0
                        HeroSubMetricPanel(
                            label = stringResource(R.string.dashboard_net_profit_label),
                            amount = labaBersihHariIni,
                            badgeColor = if (isPositiveProfit) SantoyaLeafGreen else HeroGlowRose,
                            amountColor = if (isPositiveProfit) HeroGlowEmeraldSoft else HeroGlowRoseSoft,
                            icon = Icons.AutoMirrored.Outlined.TrendingUp,
                            isCompact = isCompact,
                            subPanelPadding = subPanelPadding,
                            onClick = onNetProfitClick,
                            modifier = Modifier.weight(1f)
                        )
                        HeroSubMetricPanel(
                            label = stringResource(R.string.dashboard_receivables_label),
                            amount = totalPiutangAktif,
                            badgeColor = HeroGlowAmber,
                            amountColor = HeroGlowAmberSoft,
                            icon = Icons.Outlined.HourglassEmpty,
                            isCompact = isCompact,
                            subPanelPadding = subPanelPadding,
                            onClick = onReceivablesClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Dock Bawah Pemilik: Galon Hari Ini & Proyeksi Bulan Ini
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(SantoyaRadius.lg),
                        color = SantoyaNavyPanel.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, SantoyaNavyBorder.copy(alpha = 0.75f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompact) 10.dp else 12.dp,
                                    vertical = if (isCompact) 7.dp else 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroVolumeTile(
                                label = stringResource(R.string.dashboard_galon_today_label),
                                valueText = FormatUtils.formatNumber(totalGalonHariIni),
                                unitText = stringResource(R.string.unit_galon),
                                valueColor = santoyaColors.onHero,
                                badgeColor = SantoyaWaterBlue,
                                icon = Icons.Outlined.WaterDrop,
                                isCompact = isCompact,
                                onClick = onGalonTodayClick,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = if (isCompact) 6.dp else 10.dp)
                                    .width(1.dp)
                                    .height(if (isCompact) 28.dp else 34.dp)
                                    .background(SantoyaNavyBorder.copy(alpha = 0.7f))
                            )

                            HeroVolumeTile(
                                label = stringResource(R.string.dashboard_galon_month_estimate_label),
                                valueText = FormatUtils.formatNumber(estimasiGalonBulanIni),
                                unitText = stringResource(R.string.unit_galon),
                                valueColor = HeroGlowEmeraldSoft,
                                badgeColor = SantoyaLeafGreen,
                                icon = Icons.Outlined.AutoGraph,
                                isCompact = isCompact,
                                onClick = onMonthEstimateClick,
                                modifier = Modifier.weight(1.12f)
                            )
                        }
                    }
                } else {
                    // Panel Karyawan: Komisi Hari Ini & Komisi Bulan Ini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                    ) {
                        HeroSubMetricPanel(
                            label = stringResource(R.string.dashboard_staff_commission_today),
                            amount = karyawanMetrics.komisiHariIni,
                            badgeColor = HeroGlowEmerald,
                            amountColor = HeroGlowEmeraldSoft,
                            icon = Icons.Outlined.WaterDrop,
                            isCompact = isCompact,
                            subPanelPadding = subPanelPadding,
                            onClick = onPrimaryHeroClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_komisi_hari_ini")
                        )
                        HeroSubMetricPanel(
                            label = stringResource(R.string.dashboard_staff_commission_month),
                            amount = karyawanMetrics.komisiBulanIni,
                            badgeColor = HeroGlowCyan,
                            amountColor = HeroGlowCyanSoft,
                            icon = Icons.Outlined.AutoGraph,
                            isCompact = isCompact,
                            subPanelPadding = subPanelPadding,
                            onClick = onPrimaryHeroClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_komisi_bulan_ini")
                        )
                    }

                    // Dock Bawah Karyawan: Piutang Pelanggan & Pelanggan Saya
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(SantoyaRadius.lg),
                        color = SantoyaNavyPanel.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, SantoyaNavyBorder.copy(alpha = 0.75f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompact) 10.dp else 12.dp,
                                    vertical = if (isCompact) 7.dp else 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(SantoyaRadius.sm))
                                    .pressClickEffect(onClick = onReceivablesClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.sm),
                                    color = HeroGlowAmber,
                                    modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.HourglassEmpty,
                                            contentDescription = null,
                                            tint = santoyaColors.onHero,
                                            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.dashboard_staff_receivables),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = santoyaColors.onHero.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    SantoyaAdaptiveCurrencyText(
                                        amount = totalPiutangAktif,
                                        style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HeroGlowAmberSoft,
                                        minFontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = if (isCompact) 6.dp else 10.dp)
                                    .width(1.dp)
                                    .height(if (isCompact) 28.dp else 34.dp)
                                    .background(SantoyaNavyBorder.copy(alpha = 0.7f))
                            )

                            HeroVolumeTile(
                                label = stringResource(R.string.dashboard_staff_customers),
                                valueText = FormatUtils.formatNumber(pelangganCount),
                                unitText = stringResource(R.string.unit_orang),
                                valueColor = santoyaColors.onHero,
                                badgeColor = HeroGlowCyan,
                                icon = Icons.Outlined.People,
                                isCompact = isCompact,
                                onClick = onStaffCustomersClick,
                                modifier = Modifier.weight(0.95f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSubMetricPanel(
    label: String,
    amount: Double,
    badgeColor: Color,
    amountColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCompact: Boolean,
    subPanelPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    Surface(
        modifier = modifier.pressClickEffect(onClick = onClick),
        shape = RoundedCornerShape(SantoyaRadius.lg),
        color = SantoyaNavyPanel.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, SantoyaNavyBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = subPanelPadding,
                vertical = if (isCompact) 8.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = santoyaColors.onHero,
                            modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = santoyaColors.onHero.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SantoyaAdaptiveCurrencyText(
                amount = amount,
                style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                minFontSize = 10.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeroVolumeTile(
    label: String,
    valueText: String,
    unitText: String,
    valueColor: Color,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SantoyaRadius.sm))
            .pressClickEffect(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(SantoyaRadius.sm),
            color = badgeColor,
            modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = santoyaColors.onHero,
                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = santoyaColors.onHero.copy(alpha = 0.74f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueText,
                    style = (if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
                        .copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = santoyaColors.onHero.copy(alpha = 0.78f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Kartu Bento Utama (Featured Full-Width) untuk Transaksi / Buat Nota Baru.
 */
@Composable
fun BentoFeaturedTransaksiCard(
    todayGallons: Int,
    todayOrderCount: Int,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val primary = MaterialTheme.colorScheme.primary
    val cdText = stringResource(R.string.dashboard_menu_transaksi_cd)

    HydroBentoCard(
        accentColor = primary,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.13f,
        showWaterRipple = true,
        contentAlignment = Alignment.CenterStart,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (isCompact) 78.dp else 88.dp)
            .testTag("nav_transaksi_button")
            .semantics(mergeDescendants = true) {
                contentDescription = cdText
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 14.dp else 16.dp,
                    vertical = if (isCompact) 12.dp else 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HydroGradientBadge(
                gradientColors = listOf(primary, SantoyaWaterBlue),
                size = if (isCompact) 46.dp else 52.dp,
                cornerRadius = SantoyaRadius.lg
            ) {
                HydroGalonIcon(
                    tint = santoyaColors.onHero,
                    leafAccent = santoyaColors.heroLeafTint,
                    modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_menu_transaksi_title),
                    style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (todayOrderCount > 0) {
                        stringResource(R.string.dashboard_menu_transaksi_today_stats, todayGallons, todayOrderCount)
                    } else {
                        stringResource(R.string.dashboard_menu_transaksi_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(SantoyaRadius.pill),
                color = primary,
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_menu_transaksi_cta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Daftar Menu Utama vertikal seragam untuk Pemilik (Owner):
 * Menampilkan Pelanggan, Pengeluaran, dan Laporan dalam satu kolom penuh yang sejajar dan rapi.
 */
@Composable
fun OwnerVerticalMenuList(
    customerCountText: String,
    totalPiutang: Double,
    todayExpense: Double,
    profitMarginPercentText: String,
    isCompact: Boolean,
    onPelangganClick: () -> Unit,
    onPengeluaranClick: () -> Unit,
    onLaporanClick: () -> Unit,
    modifier: Modifier = Modifier,
    fitToScreen: Boolean = false
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val spacing = if (isCompact) 8.dp else 10.dp

    val hasDebt = totalPiutang > 0.0
    val pelangganChipBg = if (hasDebt) santoyaColors.warningContainer else santoyaColors.successContainer
    val pelangganChipFg = if (hasDebt) santoyaColors.warning else santoyaColors.success
    val pelangganChipText = if (hasDebt) {
        stringResource(R.string.dashboard_menu_pelanggan_kasbon_chip, FormatUtils.formatRupiah(totalPiutang))
    } else {
        stringResource(R.string.dashboard_menu_pelanggan_clear_chip)
    }

    val hasExpense = todayExpense > 0.0
    val expenseChipText = if (hasExpense) {
        stringResource(R.string.dashboard_menu_pengeluaran_today, FormatUtils.formatRupiah(todayExpense))
    } else {
        stringResource(R.string.dashboard_menu_pengeluaran_clear_chip)
    }

    val laporanChipText = if (profitMarginPercentText.isNotBlank()) {
        stringResource(R.string.dashboard_menu_laporan_margin, profitMarginPercentText)
    } else {
        stringResource(R.string.dashboard_menu_laporan_badge)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val itemModifier = if (fitToScreen) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier.fillMaxWidth()
        }
        UniformVerticalMenuRow(
            title = stringResource(R.string.dashboard_menu_pelanggan_title),
            subtitle = stringResource(R.string.dashboard_menu_pelanggan_badge_count, customerCountText),
            chipText = pelangganChipText,
            accentColor = santoyaColors.success,
            containerColor = santoyaColors.successContainer,
            chipBackgroundColor = pelangganChipBg,
            chipTextColor = pelangganChipFg,
            accessibilityDescription = stringResource(R.string.dashboard_menu_pelanggan_cd, customerCountText),
            testTag = "nav_pelanggan_button",
            isCompact = isCompact,
            onClick = onPelangganClick,
            modifier = itemModifier
        ) {
            HydroCustomerDropIcon(
                tint = santoyaColors.success,
                waterAccent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }

        UniformVerticalMenuRow(
            title = stringResource(R.string.dashboard_menu_pengeluaran_title),
            subtitle = stringResource(R.string.dashboard_menu_pengeluaran_subtitle),
            chipText = expenseChipText,
            accentColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            chipBackgroundColor = MaterialTheme.colorScheme.errorContainer,
            chipTextColor = MaterialTheme.colorScheme.error,
            accessibilityDescription = stringResource(R.string.dashboard_menu_pengeluaran_cd),
            testTag = "nav_pengeluaran_button",
            isCompact = isCompact,
            onClick = onPengeluaranClick,
            modifier = itemModifier
        ) {
            HydroExpenseFlowIcon(
                tint = MaterialTheme.colorScheme.error,
                leafAccent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }

        UniformVerticalMenuRow(
            title = stringResource(R.string.dashboard_menu_laporan_title),
            subtitle = stringResource(R.string.dashboard_menu_laporan_subtitle),
            chipText = laporanChipText,
            accentColor = santoyaColors.info,
            containerColor = santoyaColors.infoContainer,
            chipBackgroundColor = santoyaColors.infoContainer,
            chipTextColor = santoyaColors.info,
            accessibilityDescription = stringResource(R.string.dashboard_menu_laporan_cd),
            testTag = "nav_laporan_button",
            isCompact = isCompact,
            onClick = onLaporanClick,
            modifier = itemModifier
        ) {
            HydroReportChartIcon(
                tint = santoyaColors.info,
                leafAccent = santoyaColors.success,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }
    }
}

/**
 * Daftar Menu Utama vertikal seragam untuk Karyawan (Staff) dengan akses terbatas:
 * Menampilkan Pelanggan dan Ringkasan Kerja / Riwayat Hari Ini secara konsisten.
 */
@Composable
fun KaryawanVerticalMenuList(
    customerCountText: String,
    totalPiutang: Double,
    galonHariIni: Int,
    pesananHariIni: Int,
    isCompact: Boolean,
    onPelangganClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    fitToScreen: Boolean = false
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val spacing = if (isCompact) 8.dp else 10.dp

    val hasDebt = totalPiutang > 0.0
    val pelangganChipBg = if (hasDebt) santoyaColors.warningContainer else santoyaColors.successContainer
    val pelangganChipFg = if (hasDebt) santoyaColors.warning else santoyaColors.success
    val pelangganChipText = if (hasDebt) {
        stringResource(R.string.dashboard_menu_pelanggan_kasbon_chip, FormatUtils.formatRupiah(totalPiutang))
    } else {
        stringResource(R.string.dashboard_menu_pelanggan_clear_chip)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val itemModifier = if (fitToScreen) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier.fillMaxWidth()
        }
        UniformVerticalMenuRow(
            title = stringResource(R.string.dashboard_menu_pelanggan_title),
            subtitle = stringResource(R.string.dashboard_menu_pelanggan_badge_count, customerCountText),
            chipText = pelangganChipText,
            accentColor = santoyaColors.success,
            containerColor = santoyaColors.successContainer,
            chipBackgroundColor = pelangganChipBg,
            chipTextColor = pelangganChipFg,
            accessibilityDescription = stringResource(R.string.dashboard_menu_pelanggan_cd, customerCountText),
            testTag = "nav_pelanggan_button",
            isCompact = isCompact,
            onClick = onPelangganClick,
            modifier = itemModifier
        ) {
            HydroCustomerDropIcon(
                tint = santoyaColors.success,
                waterAccent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }

        UniformVerticalMenuRow(
            title = stringResource(R.string.dashboard_staff_summary_title),
            subtitle = stringResource(R.string.dashboard_staff_galon_orders, galonHariIni, pesananHariIni),
            chipText = stringResource(R.string.dashboard_staff_summary_cta),
            accentColor = santoyaColors.info,
            containerColor = santoyaColors.infoContainer,
            chipBackgroundColor = santoyaColors.infoContainer,
            chipTextColor = santoyaColors.info,
            accessibilityDescription = stringResource(
                R.string.dashboard_staff_summary_cd,
                galonHariIni,
                pesananHariIni
            ),
            testTag = "nav_riwayat_kerja_button",
            isCompact = isCompact,
            onClick = onHistoryClick,
            modifier = itemModifier
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = santoyaColors.info,
                modifier = Modifier.size(if (isCompact) 21.dp else 23.dp)
            )
        }
    }
}

/**
 * Baris kartu menu vertikal seragam dengan badge ikon di kiri, judul & subjudul di tengah,
 * serta chip metrik real-time dan panah navigasi di kanan.
 */
@Composable
fun UniformVerticalMenuRow(
    title: String,
    subtitle: String,
    chipText: String,
    accentColor: Color,
    containerColor: Color,
    chipBackgroundColor: Color,
    chipTextColor: Color,
    accessibilityDescription: String,
    testTag: String,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.06f,
        showWaterRipple = false,
        contentAlignment = Alignment.CenterStart,
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = if (isCompact) 66.dp else 72.dp)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 14.dp else 16.dp,
                    vertical = if (isCompact) 10.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = containerColor,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                modifier = Modifier.size(if (isCompact) 42.dp else 46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    iconContent()
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(SantoyaRadius.pill),
                color = chipBackgroundColor,
                border = BorderStroke(1.dp, chipTextColor.copy(alpha = 0.24f)),
                modifier = Modifier.widthIn(max = if (isCompact) 128.dp else 148.dp)
            ) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = chipTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = if (isCompact) 8.dp else 10.dp,
                        vertical = if (isCompact) 4.dp else 5.dp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(if (isCompact) 26.dp else 28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Strip pengingat perawatan mesin RO ringkas khusus Pemilik di Beranda ketika ada jadwal
 * yang terlambat, jatuh tempo hari ini, atau mendekati (H-3).
 */
@Composable
fun DashboardPerawatanReminderStrip(
    reminders: List<PerawatanEvaluation>,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return

    val santoyaColors = MaterialTheme.santoyaColors
    val overdueCount = remember(reminders) {
        reminders.count { it.status == PerawatanStatus.TERLAMBAT }
    }
    val todayCount = remember(reminders) {
        reminders.count { it.status == PerawatanStatus.HARI_INI }
    }
    val soonCount = remember(reminders) {
        reminders.count { it.status == PerawatanStatus.MENDEKATI }
    }

    val isOverdue = overdueCount > 0
    val accentColor = if (isOverdue) MaterialTheme.colorScheme.error else santoyaColors.warning
    val badgeBg = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        santoyaColors.warningContainer
    }
    val badgeFg = if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        santoyaColors.onWarningContainer
    }

    val overdueText = if (overdueCount > 0) {
        stringResource(R.string.dashboard_perawatan_status_overdue, overdueCount)
    } else null
    val todayText = if (todayCount > 0) {
        stringResource(R.string.dashboard_perawatan_status_today, todayCount)
    } else null
    val soonText = if (soonCount > 0) {
        stringResource(R.string.dashboard_perawatan_status_soon, soonCount)
    } else null

    val statusSummary = remember(overdueText, todayText, soonText) {
        listOfNotNull(overdueText, todayText, soonText).joinToString(" • ")
    }
    val leadItemName = reminders.first().item.nama
    val subtitleText = stringResource(
        R.string.dashboard_perawatan_single_line,
        leadItemName,
        statusSummary
    )
    val cdText = stringResource(R.string.dashboard_perawatan_open_cd)

    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = if (isOverdue) 0.11f else 0.08f,
        showWaterRipple = false,
        contentAlignment = Alignment.CenterStart,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("card_dashboard_perawatan_reminder")
            .semantics(mergeDescendants = true) {
                contentDescription = cdText
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 12.dp else 14.dp,
                    vertical = if (isCompact) 9.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.sm),
                color = badgeBg,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                modifier = Modifier.size(if (isCompact) 34.dp else 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HydroMaintenanceWrenchIcon(
                        tint = accentColor,
                        modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_perawatan_title, reminders.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(SantoyaRadius.pill),
                color = badgeBg,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_perawatan_manage),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

