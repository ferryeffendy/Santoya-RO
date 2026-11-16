package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.R
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroMembraneLayerIcon
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.RoDiagnosisCause
import com.example.util.RoMembraneCalculator
import com.example.util.RoMembraneCategory
import com.example.util.RoMembranePreset
import com.example.util.RoMembraneResult
import com.example.util.RoMembraneStatus
import com.example.util.RoValidationFieldError
import com.example.viewmodel.RoMembraneUiState
import com.example.viewmodel.SantoyaViewModel

/**
 * Tab Kalkulator Kesehatan Membran RO (khusus pemilik depot) dengan dukungan multi-membran paralel,
 * preset tipe membran populer Indonesia (XLP/ULP/LP/XLE/GPD), konverter stopwatch, dan simpan riwayat.
 */
@Composable
fun MembranCalculatorTab(
    viewModel: SantoyaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.roMembraneUiState.collectAsStateWithLifecycle()
    val logName = stringResource(R.string.ro_calc_log_name)
    var showStopwatchDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp)
                .testTag("ro_membran_calculator_tab"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MembranCalculatorBanner()

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MembranCalculatorFormColumn(
                            uiState = uiState,
                            onTdsBakuChange = viewModel::updateRoTdsBaku,
                            onTdsJadiChange = viewModel::updateRoTdsJadi,
                            onParallelChange = viewModel::updateRoParallelCount,
                            onSelectCategory = viewModel::selectRoCategory,
                            onTogglePreset = viewModel::toggleRoPreset,
                            onGpdChange = viewModel::updateRoGpdPerUnit,
                            onFlowChange = viewModel::updateRoActualFlowLpm,
                            onOpenStopwatch = { showStopwatchDialog = true },
                            onReset = viewModel::resetRoMembraneCalculator,
                            onCalculate = viewModel::calculateRoMembraneHealth
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MembranResultAndGuideColumn(
                            uiState = uiState,
                            onSaveHistory = { statusLabel, diagText ->
                                viewModel.saveRoMembraneCheckToHistory(
                                    namaPerawatan = logName,
                                    statusLabel = statusLabel,
                                    diagnosisText = diagText
                                )
                            }
                        )
                    }
                }
            } else {
                MembranCalculatorFormColumn(
                    uiState = uiState,
                    onTdsBakuChange = viewModel::updateRoTdsBaku,
                    onTdsJadiChange = viewModel::updateRoTdsJadi,
                    onParallelChange = viewModel::updateRoParallelCount,
                    onSelectCategory = viewModel::selectRoCategory,
                    onTogglePreset = viewModel::toggleRoPreset,
                    onGpdChange = viewModel::updateRoGpdPerUnit,
                    onFlowChange = viewModel::updateRoActualFlowLpm,
                    onOpenStopwatch = { showStopwatchDialog = true },
                    onReset = viewModel::resetRoMembraneCalculator,
                    onCalculate = viewModel::calculateRoMembraneHealth
                )

                MembranResultAndGuideColumn(
                    uiState = uiState,
                    onSaveHistory = { statusLabel, diagText ->
                        viewModel.saveRoMembraneCheckToHistory(
                            namaPerawatan = logName,
                            statusLabel = statusLabel,
                            diagnosisText = diagText
                        )
                    }
                )
            }
        }
    }

    if (showStopwatchDialog) {
        StopwatchToLpmDialog(
            onDismiss = { showStopwatchDialog = false },
            onApplyLpm = { computedLpm ->
                viewModel.updateRoActualFlowLpm(String.format(java.util.Locale.US, "%.2f", computedLpm))
                showStopwatchDialog = false
            }
        )
    }
}

@Composable
private fun MembranCalculatorBanner() {
    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.primary,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.09f,
        showWaterRipple = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HydroGradientBadge(
                gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                size = 40.dp,
                cornerRadius = SantoyaRadius.md
            ) {
                HydroMembraneLayerIcon(
                    tint = MaterialTheme.santoyaColors.onHero,
                    waterAccent = MaterialTheme.santoyaColors.heroLeafTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ro_calc_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.ro_calc_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MembranCalculatorFormColumn(
    uiState: RoMembraneUiState,
    onTdsBakuChange: (String) -> Unit,
    onTdsJadiChange: (String) -> Unit,
    onParallelChange: (Int) -> Unit,
    onSelectCategory: (RoMembraneCategory) -> Unit,
    onTogglePreset: (RoMembranePreset) -> Unit,
    onGpdChange: (String) -> Unit,
    onFlowChange: (String) -> Unit,
    onOpenStopwatch: () -> Unit,
    onReset: () -> Unit,
    onCalculate: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val tdsJadiFocus = remember { FocusRequester() }
    val flowFocus = remember { FocusRequester() }

    val gpdPerUnit = remember(uiState.gpdPerUnitInput) {
        RoMembraneCalculator.parseDecimalInput(uiState.gpdPerUnitInput)
    }

    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.primary,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.04f,
        showWaterRipple = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.ro_calc_section_tds_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = uiState.tdsBakuInput,
                onValueChange = { onTdsBakuChange(sanitizeDecimalText(it)) },
                label = { Text(stringResource(R.string.ro_calc_tds_baku_label)) },
                placeholder = { Text(stringResource(R.string.ro_calc_tds_baku_hint)) },
                isError = uiState.tdsBakuError != null,
                supportingText = uiState.tdsBakuError?.let { err ->
                    { Text(validationErrorMessage(err)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { tdsJadiFocus.requestFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                    .testTag("ro_input_tds_baku")
            )

            OutlinedTextField(
                value = uiState.tdsJadiInput,
                onValueChange = { onTdsJadiChange(sanitizeDecimalText(it)) },
                label = { Text(stringResource(R.string.ro_calc_tds_jadi_label)) },
                placeholder = { Text(stringResource(R.string.ro_calc_tds_jadi_hint)) },
                isError = uiState.tdsJadiError != null,
                supportingText = uiState.tdsJadiError?.let { err ->
                    { Text(validationErrorMessage(err)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(tdsJadiFocus)
                    .bringIntoViewOnFocus()
                    .testTag("ro_input_tds_jadi")
            )
        }
    }

    HydroBentoCard(
        accentColor = AccentCyan,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.04f,
        showWaterRipple = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.ro_calc_section_flow_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ro_calc_section_flow_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ro_calc_parallel_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onParallelChange(uiState.parallelCount - 1) },
                        enabled = uiState.parallelCount > RoMembraneCalculator.MIN_PARALLEL_MEMBRANES,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("ro_btn_parallel_minus")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.ro_calc_parallel_dec_cd)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.ro_calc_parallel_unit, uiState.parallelCount),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { onParallelChange(uiState.parallelCount + 1) },
                        enabled = uiState.parallelCount < RoMembraneCalculator.MAX_PARALLEL_MEMBRANES,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("ro_btn_parallel_plus")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.ro_calc_parallel_inc_cd)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(SantoyaRadius.sm),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.ro_calc_parallel_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Text(
                text = stringResource(R.string.ro_calc_size_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoMembraneCategory.entries.forEach { category ->
                    val isCategorySelected = uiState.selectedCategory == category
                    SantoyaFilterPill(
                        label = categoryChipLabel(category),
                        selected = isCategorySelected,
                        onClick = { onSelectCategory(category) },
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("ro_size_${category.name.lowercase()}")
                    )
                }
            }

            val visiblePresets = remember(uiState.selectedCategory) {
                RoMembraneCalculator.presetsForCategory(uiState.selectedCategory)
            }
            val selectedPreset = remember(uiState.selectedPresetId) {
                RoMembraneCalculator.findPresetById(uiState.selectedPresetId)
            }
            val sizeName = categoryChipLabel(uiState.selectedCategory)

            Text(
                text = stringResource(R.string.ro_calc_subtype_label, sizeName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                visiblePresets.forEach { preset ->
                    val isSelected = uiState.selectedPresetId == preset.id
                    SantoyaFilterPill(
                        label = presetChipLabel(preset),
                        selected = isSelected,
                        onClick = { onTogglePreset(preset) },
                        leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                        modifier = Modifier.testTag("ro_preset_${preset.id}")
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedPreset != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedPreset?.let { preset ->
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ro_preset_info_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(17.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(
                                        R.string.ro_calc_preset_info_header,
                                        preset.typeName ?: preset.seriesCode,
                                        preset.psiHint
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                preset.brandReference?.let { brand ->
                                    Text(
                                        text = stringResource(R.string.ro_calc_preset_info_brand, brand),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.ro_calc_preset_info_feed,
                                        preset.maxFeedTdsHint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            val gpdError = uiState.gpdError
            val previewTotalGpd = if (gpdPerUnit != null && gpdPerUnit > 0.0) {
                gpdPerUnit * uiState.parallelCount
            } else {
                null
            }
            val previewStdLpm = previewTotalGpd?.let { RoMembraneCalculator.convertGpdToLpm(it) }
            val gpdSupportingText: (@Composable () -> Unit)? = when {
                gpdError != null -> {
                    { Text(validationErrorMessage(gpdError)) }
                }
                gpdPerUnit != null && previewTotalGpd != null && previewStdLpm != null -> {
                    {
                        Text(
                            text = stringResource(
                                R.string.ro_calc_gpd_preview,
                                uiState.parallelCount,
                                RoMembraneCalculator.formatDecimalId(gpdPerUnit, 0),
                                RoMembraneCalculator.formatDecimalId(previewTotalGpd, 0),
                                RoMembraneCalculator.formatDecimalId(previewStdLpm, 2)
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                else -> null
            }

            OutlinedTextField(
                value = uiState.gpdPerUnitInput,
                onValueChange = { onGpdChange(sanitizeDecimalText(it)) },
                label = { Text(stringResource(R.string.ro_calc_gpd_label)) },
                placeholder = { Text(stringResource(R.string.ro_calc_gpd_hint)) },
                isError = gpdError != null,
                supportingText = gpdSupportingText,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { flowFocus.requestFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                    .testTag("ro_input_gpd")
            )

            OutlinedTextField(
                value = uiState.actualFlowLpmInput,
                onValueChange = { onFlowChange(sanitizeDecimalText(it)) },
                label = { Text(stringResource(R.string.ro_calc_flow_label)) },
                placeholder = { Text(stringResource(R.string.ro_calc_flow_hint)) },
                isError = uiState.flowAktualError != null,
                supportingText = uiState.flowAktualError?.let { err ->
                    { Text(validationErrorMessage(err)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(flowFocus)
                    .bringIntoViewOnFocus()
                    .testTag("ro_input_flow_aktual")
            )

            OutlinedButton(
                onClick = onOpenStopwatch,
                shape = RoundedCornerShape(SantoyaRadius.md),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("ro_btn_stopwatch_helper")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ro_calc_stopwatch_cta),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                onReset()
            },
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .heightIn(min = SantoyaSize.touchTarget)
                .testTag("ro_btn_reset")
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_reset))
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                onCalculate()
            },
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SantoyaSize.touchTarget)
                .testTag("ro_btn_hitung_membran")
        ) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ro_calc_btn_calculate),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MembranResultAndGuideColumn(
    uiState: RoMembraneUiState,
    onSaveHistory: (statusLabel: String, diagnosisText: String) -> Unit
) {
    AnimatedVisibility(
        visible = uiState.result != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        uiState.result?.let { result ->
            MembranResultCard(
                result = result,
                isSaved = uiState.isSavedToHistory,
                onSaveHistory = onSaveHistory
            )
        }
    }

    MembranDiagnosisGuideCard()
}

@Composable
private fun MembranResultCard(
    result: RoMembraneResult,
    isSaved: Boolean,
    onSaveHistory: (statusLabel: String, diagnosisText: String) -> Unit
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val statusLabel = when (result.status) {
        RoMembraneStatus.SEHAT -> stringResource(R.string.ro_calc_status_sehat)
        RoMembraneStatus.WASPADA -> stringResource(R.string.ro_calc_status_waspada)
        RoMembraneStatus.KRITIS -> stringResource(R.string.ro_calc_status_kritis)
    }
    val diagnosisText = when (result.status) {
        RoMembraneStatus.SEHAT -> stringResource(R.string.ro_calc_diag_sehat)
        RoMembraneStatus.WASPADA -> when (result.diagnosisCause) {
            RoDiagnosisCause.FLOW_DEGRADED -> stringResource(R.string.ro_calc_diag_waspada_flow)
            RoDiagnosisCause.TDS_DEGRADED -> stringResource(R.string.ro_calc_diag_waspada_tds)
            RoDiagnosisCause.BOTH_DEGRADED -> stringResource(R.string.ro_calc_diag_waspada_both)
            RoDiagnosisCause.OPTIMAL -> stringResource(R.string.ro_calc_diag_waspada)
        }
        RoMembraneStatus.KRITIS -> when (result.diagnosisCause) {
            RoDiagnosisCause.FLOW_DEGRADED -> stringResource(R.string.ro_calc_diag_kritis_flow)
            RoDiagnosisCause.TDS_DEGRADED -> stringResource(R.string.ro_calc_diag_kritis_tds)
            RoDiagnosisCause.BOTH_DEGRADED -> stringResource(R.string.ro_calc_diag_kritis_both)
            RoDiagnosisCause.OPTIMAL -> stringResource(R.string.ro_calc_diag_kritis)
        }
    }
    val (accentColor, badgeContainer, badgeOnContainer, statusIcon) = when (result.status) {
        RoMembraneStatus.SEHAT -> Quad(
            santoyaColors.success,
            santoyaColors.successContainer,
            santoyaColors.onSuccessContainer,
            Icons.Outlined.CheckCircle
        )
        RoMembraneStatus.WASPADA -> Quad(
            santoyaColors.warning,
            santoyaColors.warningContainer,
            santoyaColors.onWarningContainer,
            Icons.Outlined.WarningAmber
        )
        RoMembraneStatus.KRITIS -> Quad(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Outlined.ErrorOutline
        )
    }
    val tdsTileAccent = when (result.tdsStatus) {
        RoMembraneStatus.SEHAT -> santoyaColors.success
        RoMembraneStatus.WASPADA -> santoyaColors.warning
        RoMembraneStatus.KRITIS -> MaterialTheme.colorScheme.error
    }
    val flowTileAccent = when (result.flowStatus) {
        RoMembraneStatus.SEHAT -> santoyaColors.success
        RoMembraneStatus.WASPADA -> santoyaColors.warning
        RoMembraneStatus.KRITIS -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.outline
    }

    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.14f,
        showWaterRipple = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ro_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ro_calc_result_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = badgeContainer,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = badgeOnContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeOnContainer,
                            modifier = Modifier.testTag("ro_result_status_badge")
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(SantoyaRadius.md),
                color = badgeContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = diagnosisText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeOnContainer,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("ro_result_diagnosis_text")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultMetricTile(
                    title = stringResource(R.string.ro_calc_metric_rejection),
                    value = FormatUtils.formatPercent(result.rejectionPercent, 1),
                    subtitle = stringResource(
                        R.string.ro_calc_result_tds_pair,
                        RoMembraneCalculator.formatDecimalId(result.tdsBakuPpm, 1),
                        RoMembraneCalculator.formatDecimalId(result.tdsJadiPpm, 1)
                    ),
                    accentColor = tdsTileAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ro_result_rejection_tile")
                )

                val flowDrop = result.flowDropPercent
                val stdLpm = result.standardFlowLpm
                val actualLpm = result.actualFlowLpm
                if (flowDrop != null && stdLpm != null && actualLpm != null) {
                    ResultMetricTile(
                        title = stringResource(R.string.ro_calc_metric_flow_drop),
                        value = FormatUtils.formatPercent(flowDrop, 1),
                        subtitle = stringResource(
                            R.string.ro_calc_result_flow_pair,
                            RoMembraneCalculator.formatDecimalId(stdLpm, 2),
                            RoMembraneCalculator.formatDecimalId(actualLpm, 2)
                        ),
                        accentColor = flowTileAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ro_result_flow_tile")
                    )
                } else {
                    ResultMetricTile(
                        title = stringResource(R.string.ro_calc_metric_flow_drop),
                        value = "-",
                        subtitle = stringResource(R.string.ro_calc_metric_flow_ignored),
                        accentColor = flowTileAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ro_result_flow_tile")
                    )
                }
            }

            val gpdUnit = result.gpdPerUnit
            val totalGpd = result.totalGpd
            if (gpdUnit != null && totalGpd != null) {
                val unitDesc = result.presetTypeName?.let {
                    "$it (${RoMembraneCalculator.formatDecimalId(gpdUnit, 0)} GPD)"
                } ?: "${RoMembraneCalculator.formatDecimalId(gpdUnit, 0)} GPD"
                Text(
                    text = stringResource(
                        R.string.ro_calc_result_config_summary,
                        result.parallelCount,
                        unitDesc,
                        RoMembraneCalculator.formatDecimalId(totalGpd, 0)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val maxFeedLimit = result.presetMaxFeedTdsPpm
            if (result.isFeedTdsAbovePresetLimit && maxFeedLimit != null) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = santoyaColors.warningContainer,
                    border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ro_warning_feed_tds_limit")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = santoyaColors.onWarningContainer,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(17.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.ro_calc_warning_feed_tds_limit,
                                RoMembraneCalculator.formatDecimalId(result.tdsBakuPpm, 0),
                                result.presetTypeName ?: "membran",
                                RoMembraneCalculator.formatDecimalId(maxFeedLimit.toDouble(), 0)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = santoyaColors.onWarningContainer
                        )
                    }
                }
            }

            if (result.isTdsJadiHigherThanBaku) {
                Text(
                    text = stringResource(R.string.ro_calc_note_tds_inverted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (result.isFlowAboveStandard) {
                Text(
                    text = stringResource(R.string.ro_calc_note_flow_above),
                    style = MaterialTheme.typography.bodySmall,
                    color = santoyaColors.success
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Button(
                onClick = { onSaveHistory(statusLabel, diagnosisText) },
                enabled = !isSaved,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) santoyaColors.success else MaterialTheme.colorScheme.primary,
                    contentColor = if (isSaved) santoyaColors.onSuccess else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("ro_btn_save_history")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Outlined.Check else Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (isSaved) R.string.ro_calc_btn_saved_history else R.string.ro_calc_btn_save_history
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ResultMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MembranDiagnosisGuideCard() {
    val santoyaColors = MaterialTheme.santoyaColors
    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.outline,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.03f,
        showWaterRipple = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ro_calc_guide_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            GuideLegendRow(
                dotColor = santoyaColors.success,
                text = stringResource(R.string.ro_calc_guide_green)
            )
            GuideLegendRow(
                dotColor = santoyaColors.warning,
                text = stringResource(R.string.ro_calc_guide_yellow)
            )
            GuideLegendRow(
                dotColor = MaterialTheme.colorScheme.error,
                text = stringResource(R.string.ro_calc_guide_red)
            )
        }
    }
}

@Composable
private fun GuideLegendRow(
    dotColor: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopwatchToLpmDialog(
    onDismiss: () -> Unit,
    onApplyLpm: (Double) -> Unit
) {
    var volumeStr by remember { mutableStateOf("19") }
    var secondsStr by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val volume = RoMembraneCalculator.parseDecimalInput(volumeStr)
    val seconds = RoMembraneCalculator.parseDecimalInput(secondsStr)
    val previewLpm = remember(volume, seconds) {
        if (volume != null && seconds != null) {
            RoMembraneCalculator.calculateFlowLpmFromStopwatch(volume, seconds)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.ro_calc_stopwatch_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ro_calc_stopwatch_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.ro_calc_stopwatch_preset_volume),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "1" to stringResource(R.string.ro_calc_stopwatch_vol_1),
                        "5" to stringResource(R.string.ro_calc_stopwatch_vol_5),
                        "19" to stringResource(R.string.ro_calc_stopwatch_vol_19)
                    ).forEach { (valText, label) ->
                        SantoyaFilterPill(
                            label = label,
                            selected = volumeStr.trim() == valText,
                            onClick = {
                                volumeStr = valText
                                showError = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = volumeStr,
                    onValueChange = {
                        volumeStr = sanitizeDecimalText(it)
                        showError = false
                    },
                    label = { Text(stringResource(R.string.ro_calc_stopwatch_volume_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ro_stopwatch_volume_input")
                )

                OutlinedTextField(
                    value = secondsStr,
                    onValueChange = {
                        secondsStr = sanitizeDecimalText(it)
                        showError = false
                    },
                    label = { Text(stringResource(R.string.ro_calc_stopwatch_seconds_label)) },
                    placeholder = { Text(stringResource(R.string.ro_calc_stopwatch_seconds_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ro_stopwatch_seconds_input")
                )

                if (previewLpm != null) {
                    Text(
                        text = stringResource(
                            R.string.ro_calc_stopwatch_preview,
                            RoMembraneCalculator.formatDecimalId(previewLpm, 2)
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (showError) {
                    Text(
                        text = stringResource(R.string.ro_calc_stopwatch_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val computed = previewLpm
                    if (computed != null) {
                        onApplyLpm(computed)
                    } else {
                        showError = true
                    }
                },
                shape = RoundedCornerShape(SantoyaRadius.sm),
                modifier = Modifier.testTag("ro_stopwatch_apply_btn")
            ) {
                Text(stringResource(R.string.ro_calc_stopwatch_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun categoryChipLabel(category: RoMembraneCategory): String {
    return when (category) {
        RoMembraneCategory.SIZE_4040 -> stringResource(R.string.ro_calc_size_4040)
        RoMembraneCategory.SIZE_4021 -> stringResource(R.string.ro_calc_size_4021)
        RoMembraneCategory.SIZE_8040 -> stringResource(R.string.ro_calc_size_8040)
        RoMembraneCategory.RUMAHAN -> stringResource(R.string.ro_calc_size_rumahan)
    }
}

@Composable
private fun presetChipLabel(preset: RoMembranePreset): String {
    return when (preset.id) {
        "4040_ulp", "4040_ulp_lp" -> stringResource(R.string.ro_calc_preset_4040_ulp)
        "4040_lp", "4040_lp21" -> stringResource(R.string.ro_calc_preset_4040_lp)
        "4040_xlp" -> stringResource(R.string.ro_calc_preset_4040_xlp)
        "4040_xle" -> stringResource(R.string.ro_calc_preset_4040_xle)
        "4040_taptec" -> stringResource(R.string.ro_calc_preset_4040_taptec)
        "4021_ulp" -> stringResource(R.string.ro_calc_preset_4021_ulp)
        "4021_lp" -> stringResource(R.string.ro_calc_preset_4021_lp)
        "8040_lp", "8040_lp21" -> stringResource(R.string.ro_calc_preset_8040_lp)
        "8040_ulp22" -> stringResource(R.string.ro_calc_preset_8040_ulp22)
        "8040_ulp32" -> stringResource(R.string.ro_calc_preset_8040_ulp32)
        "8040_xlp", "8040_xlp12" -> stringResource(R.string.ro_calc_preset_8040_xlp)
        "gpd_50" -> stringResource(R.string.ro_calc_preset_50)
        "gpd_75" -> stringResource(R.string.ro_calc_preset_75)
        "gpd_100" -> stringResource(R.string.ro_calc_preset_100)
        "gpd_200" -> stringResource(R.string.ro_calc_preset_200)
        "gpd_300" -> stringResource(R.string.ro_calc_preset_300)
        "gpd_400" -> stringResource(R.string.ro_calc_preset_400)
        "gpd_500" -> stringResource(R.string.ro_calc_preset_500)
        "gpd_600" -> stringResource(R.string.ro_calc_preset_600)
        "gpd_800" -> stringResource(R.string.ro_calc_preset_800)
        "gpd_1000" -> stringResource(R.string.ro_calc_preset_1000)
        else -> "${preset.shortTypeLabel} (${preset.gpdPerUnit} GPD)"
    }
}

@Composable
private fun validationErrorMessage(error: RoValidationFieldError): String {
    return when (error) {
        RoValidationFieldError.REQUIRED -> stringResource(R.string.ro_calc_err_required)
        RoValidationFieldError.MUST_BE_GREATER_THAN_ZERO -> stringResource(R.string.ro_calc_err_gt_zero)
        RoValidationFieldError.CANNOT_BE_NEGATIVE -> stringResource(R.string.ro_calc_err_negative)
        RoValidationFieldError.INVALID_NUMBER -> stringResource(R.string.ro_calc_err_invalid_number)
        RoValidationFieldError.TDS_JADI_EXCEEDS_BAKU -> stringResource(R.string.ro_calc_err_tds_jadi_exceeds_baku)
        RoValidationFieldError.PAIR_REQUIRED_WITH_FLOW -> stringResource(R.string.ro_calc_err_pair_with_flow)
        RoValidationFieldError.PAIR_REQUIRED_WITH_GPD -> stringResource(R.string.ro_calc_err_pair_with_gpd)
    }
}

private fun sanitizeDecimalText(raw: String): String {
    var hasDecimal = false
    val builder = StringBuilder()
    for (ch in raw) {
        when {
            ch.isDigit() -> builder.append(ch)
            (ch == '.' || ch == ',') && !hasDecimal -> {
                builder.append(ch)
                hasDecimal = true
            }
        }
    }
    return builder.toString().take(12)
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
