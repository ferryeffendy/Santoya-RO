package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.AiBusinessInsight
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.SantoyaSpacing
import com.example.ui.theme.santoyaColors
import com.example.util.AiBusinessSummaryBuilder
import com.example.util.FormatUtils
import com.example.viewmodel.AiAnalyticsUiState
import kotlin.math.abs

/**
 * Strip ringkas 1-baris untuk membuka AI Business Insights dari layar Beranda Pemilik
 * tanpa memaksa pengguna menggulir halaman ke bawah.
 */
@Composable
fun AiInsightDashboardStrip(
    uiState: AiAnalyticsUiState,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val displayInsight = when (uiState) {
        is AiAnalyticsUiState.Success -> uiState.insight
        is AiAnalyticsUiState.Loading -> uiState.cachedInsight
        is AiAnalyticsUiState.Error -> uiState.cachedInsight
        is AiAnalyticsUiState.Idle -> null
    }
    val isLoading = uiState is AiAnalyticsUiState.Loading
    val isErrorWithoutCache = uiState is AiAnalyticsUiState.Error && displayInsight == null

    val accentColor = when {
        isErrorWithoutCache -> MaterialTheme.colorScheme.error
        displayInsight?.normalizedHealthStatus() == AiBusinessInsight.HEALTH_KRITIS -> MaterialTheme.colorScheme.error
        displayInsight?.normalizedHealthStatus() == AiBusinessInsight.HEALTH_WASPADA -> santoyaColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    val subtitleText = when {
        isLoading && displayInsight == null -> stringResource(R.string.ai_insights_strip_loading)
        isErrorWithoutCache -> stringResource(R.string.ai_insights_strip_error)
        displayInsight != null -> stringResource(
            R.string.ai_insights_strip_summary_format,
            FormatUtils.formatRupiah(displayInsight.labaBersih),
            FormatUtils.formatNumber(displayInsight.totalGalon)
        )
        else -> stringResource(R.string.ai_insights_strip_empty_cta)
    }

    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.08f,
        showWaterRipple = false,
        contentAlignment = Alignment.CenterStart,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (isCompact) 48.dp else 52.dp)
            .testTag("ai_insight_dashboard_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 10.dp else 12.dp,
                    vertical = if (isCompact) 7.dp else 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                modifier = Modifier.size(if (isCompact) 32.dp else 34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_insights_icon_cd),
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_insights_strip_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (displayInsight != null) {
                        HealthStatusPill(status = displayInsight.normalizedHealthStatus(), compact = true)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.pill),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.ai_insights_badge_ai),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isErrorWithoutCache) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.10f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(R.string.ai_insights_strip_cd),
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet detail lengkap AI Business Insights untuk Pemilik Depot:
 * menyediakan pilihan periode 7/30/90 hari, status kesehatan bisnis, metrik arus kas & laba bersih,
 * 3 insight strategis, saran stok/RO, tombol salin ringkasan, dan aksi prioritas 1-ketuk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBusinessInsightsSheet(
    uiState: AiAnalyticsUiState,
    selectedPeriodDays: Int,
    depotName: String,
    onDismiss: () -> Unit,
    onSelectPeriod: (Int) -> Unit,
    onAnalyzeClick: () -> Unit,
    onPriorityActionClick: (String) -> Unit,
    onSummaryCopied: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayInsight = when (uiState) {
        is AiAnalyticsUiState.Success -> uiState.insight
        is AiAnalyticsUiState.Loading -> uiState.cachedInsight
        is AiAnalyticsUiState.Error -> uiState.cachedInsight
        is AiAnalyticsUiState.Idle -> null
    }
    val isLoading = uiState is AiAnalyticsUiState.Loading
    val isSavedOffline = uiState is AiAnalyticsUiState.Success && uiState.isSavedOffline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_insights_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(SantoyaSpacing.md)
        ) {
            // Header Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HydroGradientBadge(
                    gradientColors = listOf(MaterialTheme.colorScheme.primaryContainer),
                    size = 42.dp,
                    cornerRadius = SantoyaRadius.md
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.ai_insights_icon_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SantoyaSize.iconGlyph)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_insights_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayInsight != null) {
                            HealthStatusPill(status = displayInsight.normalizedHealthStatus())
                        }
                    }
                    Text(
                        text = stringResource(R.string.ai_insights_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(SantoyaSize.touchTarget)
                        .testTag("btn_close_ai_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.ai_insights_sheet_close_cd)
                    )
                }
            }

            // Pemilih Rentang Evaluasi (7 / 30 / 90 Hari)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiBusinessInsight.SUPPORTED_PERIOD_DAYS.forEach { days ->
                    val isSelected = selectedPeriodDays == days
                    val label = when (days) {
                        7 -> stringResource(R.string.ai_insights_period_7d)
                        90 -> stringResource(R.string.ai_insights_period_90d)
                        else -> stringResource(R.string.ai_insights_period_30d)
                    }
                    FilterChip(
                        selected = isSelected,
                        enabled = !isLoading,
                        onClick = { onSelectPeriod(days) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("chip_ai_period_${days}d")
                    )
                }
            }

            // Banner Loading Non-Blocking (tetap menampilkan data lama di bawahnya)
            if (isLoading) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_insights_loading")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SantoyaSpacing.md, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.ai_insights_loading_text),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Banner Error Transparan
            if (uiState is AiAnalyticsUiState.Error) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.lg),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_insights_error")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SantoyaSpacing.md),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isSavedOffline) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.santoyaColors.warningContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.santoyaColors.warning.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_insights_offline_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SantoyaSpacing.md, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.santoyaColors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.ai_insights_offline_saved_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.santoyaColors.onWarningContainer
                        )
                    }
                }
            }

            if (displayInsight != null) {
                AiInsightDetailContent(
                    insight = displayInsight,
                    isCompact = false,
                    onPriorityActionClick = { target ->
                        onDismiss()
                        onPriorityActionClick(target)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textToCopy = AiBusinessSummaryBuilder.buildShareableText(
                                insight = displayInsight,
                                depotName = depotName
                            )
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("AI Business Insight", textToCopy))
                            onSummaryCopied()
                        },
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_copy_ai_insight")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ai_insights_btn_copy),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onAnalyzeClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = SantoyaSize.touchTarget)
                            .testTag("ai_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState is AiAnalyticsUiState.Error) {
                                stringResource(R.string.ai_insights_btn_retry)
                            } else {
                                stringResource(R.string.ai_insights_btn_refresh)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.lg),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ai_insights_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(SantoyaSpacing.md)
                    )
                }

                Button(
                    onClick = onAnalyzeClick,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SantoyaSize.touchTarget)
                        .testTag("ai_analyze_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(SantoyaSpacing.sm))
                    Text(
                        text = if (uiState is AiAnalyticsUiState.Error) {
                            stringResource(R.string.ai_insights_btn_retry)
                        } else {
                            stringResource(R.string.ai_insights_btn_analyze)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Kartu inline AI Business Insights (dipertahankan untuk kompatibilitas pratinjau & komponen modular).
 */
@Composable
fun AiBusinessInsightsCard(
    uiState: AiAnalyticsUiState,
    isCompact: Boolean,
    onAnalyzeClick: () -> Unit,
    onPriorityActionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayInsight = when (uiState) {
        is AiAnalyticsUiState.Success -> uiState.insight
        is AiAnalyticsUiState.Loading -> uiState.cachedInsight
        is AiAnalyticsUiState.Error -> uiState.cachedInsight
        is AiAnalyticsUiState.Idle -> null
    }
    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.primary,
        cornerRadius = SantoyaRadius.xl,
        tintAlpha = 0.08f,
        showWaterRipple = true,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("ai_insights_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) SantoyaSpacing.md else SantoyaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SantoyaSpacing.md)
        ) {
            if (uiState is AiAnalyticsUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (displayInsight != null) {
                AiInsightDetailContent(
                    insight = displayInsight,
                    isCompact = isCompact,
                    onPriorityActionClick = onPriorityActionClick
                )
            } else {
                Text(
                    text = stringResource(R.string.ai_insights_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAnalyzeClick,
                enabled = uiState !is AiAnalyticsUiState.Loading,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SantoyaSize.touchTarget)
                    .testTag("ai_analyze_button")
            ) {
                Text(
                    text = if (displayInsight == null) {
                        stringResource(R.string.ai_insights_btn_analyze)
                    } else {
                        stringResource(R.string.ai_insights_btn_refresh)
                    }
                )
            }
        }
    }
}

@Composable
private fun AiInsightDetailContent(
    insight: AiBusinessInsight,
    isCompact: Boolean,
    onPriorityActionClick: (String) -> Unit
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val updatedMillis = insight.timestamp?.toDate()?.time
    if (updatedMillis != null && updatedMillis > 0L) {
        Text(
            text = stringResource(
                R.string.ai_insights_updated_at,
                FormatUtils.formatDateTimeUi(updatedMillis),
                insight.periodDays.coerceAtLeast(7)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("ai_insights_timestamp")
        )
    }

    // 2x2 Grid Metrik Keuangan & Operasional
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
        ) {
            AiSummaryMetricCard(
                label = stringResource(R.string.ai_insights_metric_omzet, insight.periodDays.coerceAtLeast(7)),
                value = FormatUtils.formatRupiah(insight.totalOmzet),
                growthPercent = insight.omzetGrowthPercent,
                subCaption = null,
                modifier = Modifier.weight(1f)
            )
            val isProfitPositive = insight.labaBersih >= 0.0
            AiSummaryMetricCard(
                label = stringResource(R.string.ai_insights_metric_laba),
                value = FormatUtils.formatRupiah(insight.labaBersih),
                valueColor = if (isProfitPositive) santoyaColors.success else MaterialTheme.colorScheme.error,
                growthPercent = insight.labaGrowthPercent,
                subCaption = stringResource(
                    R.string.ai_insights_margin_caption,
                    FormatUtils.formatPercent(insight.marginLabaPersen, 1)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
        ) {
            AiSummaryMetricCard(
                label = stringResource(R.string.ai_insights_metric_galon),
                value = "${FormatUtils.formatNumber(insight.totalGalon)} gln",
                growthPercent = insight.galonGrowthPercent,
                subCaption = "${stringResource(R.string.ai_insights_metric_customers)}: ${FormatUtils.formatNumber(insight.activeCustomers)}",
                modifier = Modifier.weight(1f)
            )
            AiSummaryMetricCard(
                label = stringResource(R.string.ai_insights_metric_piutang),
                value = FormatUtils.formatRupiah(insight.totalPiutang),
                valueColor = if (insight.overduePiutangOver30d > 0.0) {
                    santoyaColors.warning
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                growthPercent = null,
                subCaption = if (insight.overduePiutangOver30d > 0.0) {
                    stringResource(
                        R.string.ai_insights_overdue_caption,
                        FormatUtils.formatRupiah(insight.overduePiutangOver30d)
                    )
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    // 3 Bullet Insights
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_insights_bullets"),
        verticalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.ai_insights_section_points_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        insight.insights.forEach { bullet ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = bullet,
                    style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Saran Stok & Operasional Box
    val adviceContainerColor = when (insight.normalizedHealthStatus()) {
        AiBusinessInsight.HEALTH_KRITIS -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        AiBusinessInsight.HEALTH_WASPADA -> santoyaColors.warningContainer.copy(alpha = 0.65f)
        else -> santoyaColors.successContainer.copy(alpha = 0.65f)
    }
    val adviceBorderColor = when (insight.normalizedHealthStatus()) {
        AiBusinessInsight.HEALTH_KRITIS -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        AiBusinessInsight.HEALTH_WASPADA -> santoyaColors.warning.copy(alpha = 0.35f)
        else -> santoyaColors.success.copy(alpha = 0.35f)
    }
    val adviceContentColor = when (insight.normalizedHealthStatus()) {
        AiBusinessInsight.HEALTH_KRITIS -> MaterialTheme.colorScheme.onErrorContainer
        AiBusinessInsight.HEALTH_WASPADA -> santoyaColors.onWarningContainer
        else -> santoyaColors.onSuccessContainer
    }

    Surface(
        shape = RoundedCornerShape(SantoyaRadius.lg),
        color = adviceContainerColor,
        border = BorderStroke(1.dp, adviceBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_insights_stock_advice")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SantoyaSpacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(SantoyaSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = adviceContentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_insights_stock_advice_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = adviceContentColor
                )
                Text(
                    text = insight.stockAdvice,
                    style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = adviceContentColor
                )
            }
        }
    }

    // Tombol Aksi Prioritas 1-Ketuk
    val target = insight.normalizedPriorityTarget()
    val ctaLabel = insight.priorityActionTitle.trim()
        .ifBlank { AiBusinessSummaryBuilder.defaultActionTitleForTarget(target) }
    FilledTonalButton(
        onClick = { onPriorityActionClick(target) },
        shape = RoundedCornerShape(SantoyaRadius.md),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SantoyaSize.touchTarget)
            .testTag("btn_ai_priority_action")
    ) {
        Text(
            text = ctaLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(SantoyaSpacing.sm))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HealthStatusPill(
    status: String,
    compact: Boolean = false
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val (label, bgColor, textColor) = when (AiBusinessInsight.normalizeHealthStatus(status)) {
        AiBusinessInsight.HEALTH_KRITIS -> Triple(
            stringResource(R.string.ai_insights_health_kritis),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        AiBusinessInsight.HEALTH_WASPADA -> Triple(
            stringResource(R.string.ai_insights_health_waspada),
            santoyaColors.warningContainer,
            santoyaColors.onWarningContainer
        )
        else -> Triple(
            stringResource(R.string.ai_insights_health_sehat),
            santoyaColors.successContainer,
            santoyaColors.onSuccessContainer
        )
    }
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.pill),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 1.dp else 2.dp
            )
        )
    }
}

@Composable
private fun AiSummaryMetricCard(
    label: String,
    value: String,
    growthPercent: Double?,
    subCaption: String?,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val santoyaColors = MaterialTheme.santoyaColors
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (growthPercent != null && abs(growthPercent) >= 0.1) {
                    val isUp = growthPercent > 0.0
                    val pillColor = if (isUp) santoyaColors.successContainer else MaterialTheme.colorScheme.errorContainer
                    val pillText = if (isUp) santoyaColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                    val sign = if (isUp) "+" else ""
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = pillColor
                    ) {
                        Text(
                            text = "$sign${FormatUtils.formatPercent(growthPercent, 0)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = pillText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subCaption.isNullOrBlank()) {
                Text(
                    text = subCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
