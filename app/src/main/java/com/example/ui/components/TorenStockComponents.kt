package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.DepotProfile
import com.example.data.entity.TangkiRefill
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.TorenWaterCalculator
import com.example.util.WaterLossEvaluation

/**
 * Kartu Indikator Sisa Air Toren utama untuk layar Perawatan (Tab Stok Air Toren).
 * Menampilkan sisa liter, estimasi galon (19L), progress bar visual (0f..1f), peringatan stok tipis,
 * serta tombol aksi khusus Owner (Catat Beli Air Tangki, Sesuaikan Stok/Kapasitas, dan Kuras Toren).
 */
@Composable
fun TorenStockIndicatorCard(
    profile: DepotProfile,
    isOwner: Boolean,
    onBeliTangkiClick: () -> Unit,
    onAdjustStockClick: () -> Unit,
    onKurasTorenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val stockLiters = profile.currentTankStockLiters.coerceAtLeast(0.0)
    val capacityLiters = profile.tankCapacityLiters.coerceAtLeast(100.0)
    val estimatedGallons = TorenWaterCalculator.litersToEstimatedGallons(stockLiters)
    val progress = TorenWaterCalculator.calculateProgressRatio(stockLiters, capacityLiters)
    val isLow = TorenWaterCalculator.isLowStock(stockLiters)

    Card(
        shape = RoundedCornerShape(SantoyaRadius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLow) {
                santoyaColors.warning.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_toren_stock_indicator")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = if (isLow) santoyaColors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = null,
                                tint = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.toren_stock_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.toren_stock_capacity_caption,
                                FormatUtils.formatNumber(capacityLiters)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = if (isLow) santoyaColors.warningContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(
                            R.string.toren_stock_gallon_estimate,
                            FormatUtils.formatNumber(estimatedGallons.toDouble())
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLow) santoyaColors.onWarningContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(
                        R.string.toren_stock_liter_display,
                        FormatUtils.formatNumber(stockLiters)
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                color = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(SantoyaRadius.xs))
            )

            if (isLow) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = santoyaColors.warningContainer,
                    border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = santoyaColors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.toren_stock_warning_low),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = santoyaColors.onWarningContainer
                        )
                    }
                }
            }

            if (isOwner) {
                Button(
                    onClick = onBeliTangkiClick,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_catat_beli_tangki")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.toren_stock_btn_beli_tangki),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAdjustStockClick,
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_sesuaikan_stok_toren")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Atur Stok",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onKurasTorenClick,
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_kuras_toren")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kuras (0 L)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu ringkas sisa air toren untuk layar Beranda (Dashboard) saat depot menjual Air Mineral.
 */
@Composable
fun DashboardTorenStockCard(
    profile: DepotProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val stockLiters = profile.currentTankStockLiters.coerceAtLeast(0.0)
    val capacityLiters = profile.tankCapacityLiters.coerceAtLeast(100.0)
    val estimatedGallons = TorenWaterCalculator.litersToEstimatedGallons(stockLiters)
    val progress = TorenWaterCalculator.calculateProgressRatio(stockLiters, capacityLiters)
    val isLow = TorenWaterCalculator.isLowStock(stockLiters)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(SantoyaRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) santoyaColors.warningContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLow) santoyaColors.warning.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_dashboard_toren_stock")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLow) Icons.Outlined.WarningAmber else Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.toren_stock_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${FormatUtils.formatNumber(stockLiters)} L (~$estimatedGallons galon)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                color = if (isLow) santoyaColors.warning else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(SantoyaRadius.xs))
            )
        }
    }
}

/**
 * Kartu Evaluasi Susut Air (Water Loss) yang membandingkan total liter air tangki yang masuk
 * dengan estimasi air yang keluar lewat penjualan galon mineral (19L/galon).
 */
@Composable
fun EvaluasiSusutAirCard(
    evaluation: WaterLossEvaluation,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val isHighLoss = evaluation.susutPersen >= 5.0

    Card(
        shape = RoundedCornerShape(SantoyaRadius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_water_loss_evaluation")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.water_loss_card_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.water_loss_in_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${FormatUtils.formatNumber(evaluation.totalAirMasukLiter)} L",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.water_loss_out_label, evaluation.totalGalonMineralTerjual),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${FormatUtils.formatNumber(evaluation.estimasiAirTerjualLiter)} L",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (evaluation.totalAirMasukLiter > 0.0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.water_loss_diff_label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${FormatUtils.formatNumber(evaluation.selisihSusutLiter)} L (${FormatUtils.formatPercent(evaluation.susutPersen)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighLoss) santoyaColors.warning else santoyaColors.success
                    )
                }
                Text(
                    text = if (isHighLoss) {
                        stringResource(R.string.water_loss_status_alert)
                    } else {
                        stringResource(R.string.water_loss_status_normal)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHighLoss) santoyaColors.warning else santoyaColors.success
                )
            }
        }
    }
}

/**
 * Kartu item riwayat pembelian air tangki dengan tombol hapus khusus Owner.
 */
@Composable
fun TangkiRefillItemCard(
    refill: TangkiRefill,
    isOwner: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val gallonsEq = TorenWaterCalculator.litersToEstimatedGallons(refill.volumeLiter)

    Card(
        shape = RoundedCornerShape(SantoyaRadius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_tangki_refill_${refill.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "+${FormatUtils.formatNumber(refill.volumeLiter)} Liter (~$gallonsEq gln)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (refill.linkedExpenseId != null || !refill.pengeluaranSyncId.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.xs),
                            color = santoyaColors.successContainer
                        ) {
                            Text(
                                text = stringResource(R.string.tangki_badge_linked_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = santoyaColors.onSuccessContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = buildString {
                        append(FormatUtils.formatDate(refill.tanggal))
                        append(" • ")
                        append(FormatUtils.formatRupiah(refill.totalHarga))
                        if (refill.namaPemasok.isNotBlank()) {
                            append(" • ")
                            append(refill.namaPemasok)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (refill.catatan.isNotBlank()) {
                    Text(
                        text = refill.catatan,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isOwner) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(SantoyaSize.touchTarget)
                        .testTag("btn_delete_tangki_${refill.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet form "Catat Beli Air Tangki" lengkap dengan tombol pilihan cepat 5.000 L, 7.000 L, 8.000 L
 * dan opsi centang otomatis catat ke Pengeluaran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeliTangkiBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (
        volumeLiter: Double,
        totalHarga: Double,
        namaPemasok: String,
        catatan: String,
        catatKePengeluaran: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var volumeInput by remember { mutableStateOf("5000") }
    var hargaInput by remember { mutableStateOf("") }
    var pemasokInput by remember { mutableStateOf("") }
    var catatanInput by remember { mutableStateOf("") }
    var catatKePengeluaran by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val quickVolumes = listOf(5000, 7000, 8000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_beli_tangki")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tangki_refill_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.tangki_refill_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(SantoyaSize.touchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickVolumes.forEach { vol ->
                    val isSelected = volumeInput == vol.toString()
                    FilterChip(
                        selected = isSelected,
                        onClick = { volumeInput = vol.toString() },
                        label = {
                            Text(
                                text = "${FormatUtils.formatNumber(vol.toDouble())} L",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("chip_tangki_vol_$vol")
                    )
                }
            }

            OutlinedTextField(
                value = volumeInput,
                onValueChange = {
                    volumeInput = it.filter { ch -> ch.isDigit() }.take(7)
                    errorMsg = null
                },
                label = { Text(stringResource(R.string.tangki_refill_volume_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tangki_volume")
            )

            OutlinedTextField(
                value = hargaInput,
                onValueChange = {
                    hargaInput = it.filter { ch -> ch.isDigit() }.take(10)
                    errorMsg = null
                },
                label = { Text(stringResource(R.string.tangki_refill_price_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tangki_price")
            )

            OutlinedTextField(
                value = pemasokInput,
                onValueChange = { pemasokInput = it.take(120) },
                label = { Text(stringResource(R.string.tangki_refill_supplier_label)) },
                placeholder = { Text(stringResource(R.string.tangki_refill_supplier_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tangki_supplier")
            )

            OutlinedTextField(
                value = catatanInput,
                onValueChange = { catatanInput = it.take(300) },
                label = { Text(stringResource(R.string.tangki_refill_notes_label)) },
                placeholder = { Text(stringResource(R.string.tangki_refill_notes_hint)) },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tangki_notes")
            )

            Surface(
                onClick = { catatKePengeluaran = !catatKePengeluaran },
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("checkbox_tangki_auto_expense")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = catatKePengeluaran,
                        onCheckedChange = { catatKePengeluaran = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tangki_refill_auto_expense),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val vol = volumeInput.toDoubleOrNull() ?: 0.0
                    val price = hargaInput.toDoubleOrNull() ?: 0.0
                    if (vol <= 0.0) {
                        errorMsg = "Masukkan jumlah air (Liter) yang valid"
                        return@Button
                    }
                    onSubmit(
                        vol,
                        price,
                        pemasokInput.trim(),
                        catatanInput.trim(),
                        catatKePengeluaran
                    )
                },
                shape = RoundedCornerShape(SantoyaRadius.md),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("btn_submit_tangki_refill")
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tangki_refill_btn_submit),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Dialog untuk menyesuaikan sisa air saat ini dan kapasitas total toren tanpa tercatat sebagai pembelian.
 */
@Composable
fun SesuaikanStokTorenDialog(
    currentStockLiters: Double,
    currentCapacityLiters: Double,
    onDismiss: () -> Unit,
    onSave: (newStock: Double, newCapacity: Double) -> Unit
) {
    var stockStr by remember { mutableStateOf(currentStockLiters.toLong().toString()) }
    var capacityStr by remember { mutableStateOf(currentCapacityLiters.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.toren_adjust_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text(stringResource(R.string.toren_adjust_stock_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_adjust_toren_stock")
                )
                OutlinedTextField(
                    value = capacityStr,
                    onValueChange = { capacityStr = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text(stringResource(R.string.toren_adjust_capacity_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_adjust_toren_capacity")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStock = (stockStr.toDoubleOrNull() ?: currentStockLiters).coerceAtLeast(0.0)
                    val newCap = (capacityStr.toDoubleOrNull() ?: currentCapacityLiters).coerceAtLeast(100.0)
                    onSave(newStock, newCap)
                },
                modifier = Modifier
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("btn_confirm_adjust_toren")
            ) {
                Text(stringResource(R.string.toren_adjust_btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = SantoyaSize.touchTarget)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
