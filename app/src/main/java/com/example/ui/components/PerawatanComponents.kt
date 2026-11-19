package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.PerawatanLog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.PerawatanComponentMetrics
import com.example.util.PerawatanEvaluation
import com.example.util.PerawatanHistorySummary
import com.example.util.PerawatanScheduleStats
import com.example.util.PerawatanScheduler
import com.example.util.PerawatanStatus

/**
 * Banner ringkasan kesehatan seluruh jadwal perawatan mesin RO beserta rasio kepatuhan
 * dan tombol opsional pemulihan templat standar yang belum terdaftar.
 */
@Composable
fun PerawatanSummaryBanner(
    stats: PerawatanScheduleStats,
    missingTemplateCount: Int,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val accentColor = when {
        stats.overdueCount > 0 -> MaterialTheme.colorScheme.error
        stats.dueTodayCount > 0 || stats.soonCount > 0 -> santoyaColors.warning
        else -> MaterialTheme.colorScheme.primary
    }
    val complianceBg = when {
        stats.overdueCount > 0 -> MaterialTheme.colorScheme.errorContainer
        stats.dueActionCount > 0 -> santoyaColors.warningContainer
        else -> santoyaColors.successContainer
    }
    val complianceFg = when {
        stats.overdueCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
        stats.dueActionCount > 0 -> santoyaColors.onWarningContainer
        else -> santoyaColors.onSuccessContainer
    }

    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.10f,
        showWaterRipple = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("perawatan_summary_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        gradientColors = listOf(accentColor, AccentCyan),
                        size = 40.dp,
                        cornerRadius = SantoyaRadius.md,
                        elevation = 0.dp
                    ) {
                        if (stats.overdueCount > 0) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = santoyaColors.onHero,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            HydroMaintenanceWrenchIcon(
                                tint = santoyaColors.onHero,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                stats.overdueCount > 0 -> stringResource(
                                    R.string.perawatan_banner_overdue_title,
                                    stats.overdueCount
                                )
                                stats.dueActionCount > 0 -> stringResource(
                                    R.string.perawatan_banner_soon_title,
                                    stats.dueActionCount
                                )
                                else -> stringResource(R.string.perawatan_banner_safe_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.perawatan_banner_subtitle,
                                stats.totalCount,
                                stats.dueActionCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (stats.totalCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = complianceBg
                    ) {
                        Text(
                            text = stringResource(
                                R.string.perawatan_banner_compliance_badge,
                                stats.safeCount,
                                stats.totalCount
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = complianceFg,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (stats.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { stats.complianceRatio },
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(SantoyaRadius.xs))
                )
            }

            if (missingTemplateCount > 0) {
                OutlinedButton(
                    onClick = onRestoreDefaults,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_pulihkan_templat_perawatan")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.perawatan_btn_restore_templates,
                            missingTemplateCount
                        ),
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

/**
 * Banner ringkasan biaya dan frekuensi riwayat perawatan mesin RO sesuai filter aktif.
 */
@Composable
fun PerawatanHistorySummaryBanner(
    summary: PerawatanHistorySummary,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.tertiary,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.09f,
        showWaterRipple = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("perawatan_history_summary_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary
                        ),
                        size = 40.dp,
                        cornerRadius = SantoyaRadius.md,
                        elevation = 0.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = santoyaColors.onHero,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.perawatan_history_banner_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SantoyaAdaptiveCurrencyText(
                            amount = summary.totalBiaya,
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            minFontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.perawatan_history_count_badge, summary.totalLogs),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (summary.paidLogsCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    summary.topComponentName?.let { topName ->
                        Text(
                            text = stringResource(
                                R.string.perawatan_history_top_component,
                                topName,
                                FormatUtils.formatRupiah(summary.topComponentBiaya)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.perawatan_history_avg_cost,
                            FormatUtils.formatRupiah(summary.averageBiaya)
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Kartu jadwal komponen mesin RO dengan status urgensi, metrik riwayat komponen,
 * catatan servis terakhir, serta aksi Tandai Selesai dan Tunda Jadwal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerawatanScheduleCard(
    evaluation: PerawatanEvaluation,
    metrics: PerawatanComponentMetrics?,
    searchQuery: String,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = evaluation.item
    val santoyaColors = MaterialTheme.santoyaColors
    val (statusColor, statusContainer, statusOnContainer) = when (evaluation.status) {
        PerawatanStatus.TERLAMBAT -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        PerawatanStatus.HARI_INI -> Triple(
            santoyaColors.warning,
            santoyaColors.warningContainer,
            santoyaColors.onWarningContainer
        )
        PerawatanStatus.MENDEKATI -> Triple(
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.78f),
            santoyaColors.onWarningContainer
        )
        PerawatanStatus.AMAN -> Triple(
            santoyaColors.success,
            santoyaColors.successContainer,
            santoyaColors.onSuccessContainer
        )
    }

    HydroBentoCard(
        accentColor = statusColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = if (evaluation.isReminderDue) 0.08f else 0.04f,
        showWaterRipple = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("perawatan_card_${item.syncId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = statusColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PerawatanComponentIcon(
                                syncId = item.syncId,
                                nama = item.nama,
                                tint = statusColor,
                                accentColor = statusColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedSearchText(
                            text = item.nama,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.perawatan_card_interval, item.intervalHari),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_edit_perawatan_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(
                                R.string.perawatan_cd_edit_schedule,
                                item.nama
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_hapus_perawatan_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(
                                R.string.perawatan_cd_delete_schedule,
                                item.nama
                            ),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = statusContainer
                ) {
                    Text(
                        text = PerawatanScheduler.formatStatusBadge(evaluation),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusOnContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = stringResource(
                            R.string.perawatan_card_next_due,
                            FormatUtils.formatDateUi(item.jadwalBerikutnya)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                if (metrics != null && metrics.logCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (metrics.totalBiaya > 0.0) {
                                stringResource(
                                    R.string.perawatan_card_history_cost_badge,
                                    metrics.logCount,
                                    FormatUtils.formatRupiah(metrics.totalBiaya)
                                )
                            } else {
                                stringResource(
                                    R.string.perawatan_card_history_free_badge,
                                    metrics.logCount
                                )
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (item.terakhirSelesai != null) {
                        stringResource(
                            R.string.perawatan_card_last_done,
                            FormatUtils.formatDateUi(item.terakhirSelesai)
                        )
                    } else {
                        stringResource(R.string.perawatan_card_never_done)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (metrics != null && metrics.lastNote.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.perawatan_card_last_note, metrics.lastNote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (evaluation.isReminderDue) statusColor else MaterialTheme.colorScheme.primary,
                        contentColor = if (evaluation.isReminderDue) santoyaColors.onHero else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_selesai_perawatan_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.perawatan_btn_complete),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_tunda_perawatan_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.perawatan_btn_snooze),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Kartu riwayat pelaksanaan perawatan dengan biaya terformat, lencana integrasi Pengeluaran,
 * catatan pengerjaan, serta aksi ubah dan hapus riwayat.
 */
@Composable
fun PerawatanHistoryCard(
    log: PerawatanLog,
    searchQuery: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasExpense = log.biaya > 0.0
    val accent = if (hasExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.santoyaColors.success

    HydroBentoCard(
        accentColor = accent,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.04f,
        showWaterRipple = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("perawatan_log_card_${log.syncId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PerawatanComponentIcon(
                                syncId = log.perawatanSyncId,
                                nama = log.namaPerawatan,
                                tint = accent,
                                accentColor = accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedSearchText(
                            text = log.namaPerawatan,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.perawatan_history_done_date,
                                FormatUtils.formatDateUi(log.tanggalSelesai)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hasExpense) {
                            FormatUtils.formatRupiah(log.biaya)
                        } else {
                            stringResource(R.string.perawatan_history_no_cost)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (hasExpense) MaterialTheme.colorScheme.error else MaterialTheme.santoyaColors.success
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                                .testTag("btn_edit_log_perawatan_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.perawatan_cd_edit_log),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                                .testTag("btn_hapus_log_perawatan_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.perawatan_cd_delete_log),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (hasExpense) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.xs),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.perawatan_history_expense_synced),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (log.catatan.isNotBlank()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                HighlightedSearchText(
                    text = log.catatan,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
