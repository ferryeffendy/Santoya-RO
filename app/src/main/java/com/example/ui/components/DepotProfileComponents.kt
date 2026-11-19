package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.DepotProfile
import com.example.data.entity.DepotProfileField
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.SantoyaReceiptHelper
import kotlin.math.abs

/**
 * Mode pratinjau kop identitas depot pada lembar edit profil.
 */
enum class ProfilePreviewFormat {
    STRUK_THERMAL,
    NOTA_WA,
    KOP_PDF
}

/**
 * Kartu ringkas "Profil Pemilik & Depot" yang tampil di bagian paling atas layar Pengaturan.
 * Menampilkan avatar inisial, kelengkapan profil usaha, status sinkronisasi, lencana jenis usaha & kapasitas toren,
 * serta aksi cepat salin identitas depot.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepotProfileSummaryCard(
    profile: DepotProfile,
    isOwner: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasPendingSync: Boolean = false,
    isOfflineMode: Boolean = false,
    onCopyProfile: ((String) -> Unit)? = null
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val hasConfiguredDepot = profile.hasConfiguredDepotName()
    val displayOwner = profile.effectiveOwnerName().ifBlank { "-" }
    val tierLabel = if (profile.isProUser()) {
        stringResource(R.string.depot_profile_tier_pro)
    } else {
        stringResource(R.string.depot_profile_tier_free)
    }

    val completenessCount = remember(profile) { profile.completenessCount() }
    val completenessPercent = remember(profile) { profile.completenessPercent() }
    val completenessRatio by animateFloatAsState(
        targetValue = profile.completenessRatio(),
        label = "profile_completeness_progress"
    )
    val missingFields = remember(profile) { profile.missingProfileFields() }
    val initials = remember(profile.depotName, profile.ownerName, profile.googleDisplayName) {
        profile.avatarInitials()
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(SantoyaRadius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (!hasConfiguredDepot && isOwner) {
                santoyaColors.warning.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_depot_profile")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = if (hasConfiguredDepot) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        santoyaColors.warningContainer
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hasConfiguredDepot) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = santoyaColors.warning,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasConfiguredDepot) {
                            profile.depotName.trim()
                        } else {
                            stringResource(R.string.depot_profile_unset_name)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasConfiguredDepot) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            santoyaColors.warning
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.depot_profile_owner_prefix, displayOwner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onCopyProfile != null && hasConfiguredDepot) {
                    IconButton(
                        onClick = { onCopyProfile(profile.toShareableSummaryText()) },
                        modifier = Modifier
                            .size(SantoyaSize.touchTarget)
                            .testTag("btn_copy_depot_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.depot_profile_copy_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Icon(
                        imageVector = if (isOwner) Icons.Outlined.Edit else Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.depot_profile_edit_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val metaParts = remember(profile.address, profile.phoneNumber) {
                buildList {
                    if (profile.address.isNotBlank()) add(profile.address.trim())
                    val cleanPhone = profile.normalizedPhoneNumber()
                    if (cleanPhone.isNotBlank()) add(cleanPhone)
                }
            }

            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(R.string.depot_profile_unset_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isOwner) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("section_profile_completeness")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.depot_profile_completeness_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (missingFields.isEmpty()) {
                                    stringResource(R.string.depot_profile_completeness_full_badge)
                                } else {
                                    stringResource(
                                        R.string.depot_profile_completeness_score,
                                        completenessCount,
                                        DepotProfile.TOTAL_COMPLETENESS_FIELDS,
                                        completenessPercent
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = if (missingFields.isEmpty()) {
                                    santoyaColors.success
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        LinearProgressIndicator(
                            progress = { completenessRatio },
                            color = if (missingFields.isEmpty()) {
                                santoyaColors.success
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp))
                        )

                        if (missingFields.isNotEmpty()) {
                            val missingLabels = missingFields.map { field ->
                                when (field) {
                                    DepotProfileField.OWNER_NAME -> stringResource(R.string.depot_profile_field_owner)
                                    DepotProfileField.DEPOT_NAME -> stringResource(R.string.depot_profile_field_depot)
                                    DepotProfileField.PHONE_NUMBER -> stringResource(R.string.depot_profile_field_phone)
                                    DepotProfileField.ADDRESS -> stringResource(R.string.depot_profile_field_address)
                                    DepotProfileField.RECEIPT_FOOTER -> stringResource(R.string.depot_profile_field_footer)
                                }
                            }
                            Text(
                                text = stringResource(
                                    R.string.depot_profile_missing_hint,
                                    missingLabels.joinToString(", ")
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

            Spacer(modifier = Modifier.height(10.dp))

            val depotTypeLabel = when (profile.depotType) {
                DepotProfile.TYPE_MINERAL -> stringResource(R.string.depot_type_mineral)
                DepotProfile.TYPE_BOTH -> stringResource(R.string.depot_type_both)
                else -> stringResource(R.string.depot_type_ro)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.xs),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = depotTypeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (profile.isMineralSupported()) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.depot_profile_tank_badge,
                                FormatUtils.formatNumber(profile.tankCapacityLiters)
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.xs),
                    color = if (profile.isProUser()) {
                        santoyaColors.successContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profile.isProUser()) {
                            santoyaColors.onSuccessContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (!isOwner) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.depot_profile_readonly_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (hasPendingSync || isOfflineMode) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = santoyaColors.warningContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                tint = santoyaColors.onWarningContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.depot_profile_sync_pending_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = santoyaColors.onWarningContainer
                            )
                        }
                    }
                } else if (hasConfiguredDepot) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = santoyaColors.successContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = santoyaColors.onSuccessContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.depot_profile_sync_synced_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = santoyaColors.onSuccessContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal lembar bawah (Bottom Sheet) untuk melihat dan mengedit Profil Pemilik & Depot
 * lengkap dengan indikator karakter, chip template catatan kaki struk, preset kapasitas toren,
 * pilihan jenis usaha (RO, Mineral, Keduanya), dan tab pratinjau multi-format (Struk Thermal, Nota WA, Kop PDF).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepotProfileEditSheet(
    profile: DepotProfile,
    isOwner: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        ownerName: String,
        depotName: String,
        phoneNumber: String,
        address: String,
        receiptFooter: String,
        depotType: String,
        tankCapacityLiters: Double
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var ownerName by remember(profile) { mutableStateOf(profile.effectiveOwnerName()) }
    var depotName by remember(profile) { mutableStateOf(profile.depotName) }
    var phoneNumber by remember(profile) { mutableStateOf(profile.phoneNumber) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var receiptFooter by remember(profile) { mutableStateOf(profile.receiptFooter) }
    var selectedDepotType by remember(profile) { mutableStateOf(profile.depotType) }
    var tankCapacityInput by remember(profile) {
        mutableStateOf(profile.tankCapacityLiters.toLong().toString())
    }
    var showOwnerError by remember { mutableStateOf(false) }
    var showUnsavedConfirmDialog by remember { mutableStateOf(false) }

    val isDirty by remember(profile) {
        derivedStateOf {
            val resolvedCap = tankCapacityInput.toDoubleOrNull()?.coerceAtLeast(100.0)
                ?: profile.tankCapacityLiters
            ownerName.trim() != profile.effectiveOwnerName() ||
                depotName.trim() != profile.depotName.trim() ||
                DepotProfile.normalizePhoneInput(phoneNumber) != profile.normalizedPhoneNumber() ||
                address.trim() != profile.address.trim() ||
                receiptFooter.trim() != profile.receiptFooter.trim() ||
                selectedDepotType != profile.depotType ||
                abs(resolvedCap - profile.tankCapacityLiters) > 0.5
        }
    }

    val previewProfile by remember {
        derivedStateOf {
            profile.copy(
                ownerName = ownerName.trim(),
                depotName = depotName.trim(),
                phoneNumber = DepotProfile.normalizePhoneInput(phoneNumber),
                address = address.trim(),
                receiptFooter = receiptFooter.trim(),
                depotType = selectedDepotType
            )
        }
    }

    val handleRequestClose: () -> Unit = {
        if (isOwner && isDirty && !isSaving) {
            showUnsavedConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleRequestClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_depot_profile")
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
                        text = stringResource(R.string.depot_profile_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.depot_profile_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = handleRequestClose,
                    modifier = Modifier.size(SantoyaSize.touchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            if (!isOwner) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.depot_profile_readonly_notice),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ownerName,
                onValueChange = {
                    ownerName = it.take(120)
                    if (showOwnerError && it.isNotBlank()) showOwnerError = false
                },
                label = { Text(stringResource(R.string.depot_profile_owner_label)) },
                placeholder = { Text(stringResource(R.string.depot_profile_owner_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Badge, contentDescription = null) },
                trailingIcon = if (isOwner && !isSaving && ownerName.isNotEmpty()) {
                    {
                        IconButton(onClick = { ownerName = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.depot_profile_clear_field_cd),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                enabled = isOwner && !isSaving,
                readOnly = !isOwner,
                singleLine = true,
                isError = showOwnerError,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showOwnerError) {
                                stringResource(R.string.depot_profile_owner_required)
                            } else {
                                ""
                            }
                        )
                        Text(
                            text = stringResource(R.string.depot_profile_char_counter, ownerName.length, 120),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_owner_name")
            )

            OutlinedTextField(
                value = depotName,
                onValueChange = { depotName = it.take(100) },
                label = { Text(stringResource(R.string.depot_profile_name_label)) },
                placeholder = { Text(stringResource(R.string.depot_profile_name_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Storefront, contentDescription = null) },
                trailingIcon = if (isOwner && !isSaving && depotName.isNotEmpty()) {
                    {
                        IconButton(onClick = { depotName = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.depot_profile_clear_field_cd),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.depot_profile_char_counter, depotName.length, 100),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                        )
                    }
                },
                enabled = isOwner && !isSaving,
                readOnly = !isOwner,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_depot_name")
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.take(40) },
                label = { Text(stringResource(R.string.depot_profile_phone_label)) },
                placeholder = { Text(stringResource(R.string.depot_profile_phone_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Phone, contentDescription = null) },
                trailingIcon = if (isOwner && !isSaving && phoneNumber.isNotEmpty()) {
                    {
                        IconButton(onClick = { phoneNumber = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.depot_profile_clear_field_cd),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                enabled = isOwner && !isSaving,
                readOnly = !isOwner,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_phone")
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it.take(300) },
                label = { Text(stringResource(R.string.depot_profile_address_label)) },
                placeholder = { Text(stringResource(R.string.depot_profile_address_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null) },
                trailingIcon = if (isOwner && !isSaving && address.isNotEmpty()) {
                    {
                        IconButton(onClick = { address = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.depot_profile_clear_field_cd),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.depot_profile_char_counter, address.length, 300),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                        )
                    }
                },
                enabled = isOwner && !isSaving,
                readOnly = !isOwner,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_address")
            )

            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it.take(200) },
                label = { Text(stringResource(R.string.depot_profile_footer_label)) },
                placeholder = { Text(stringResource(R.string.depot_profile_footer_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = null) },
                trailingIcon = if (isOwner && !isSaving && receiptFooter.isNotEmpty()) {
                    {
                        IconButton(onClick = { receiptFooter = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.depot_profile_clear_field_cd),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.depot_profile_char_counter, receiptFooter.length, 200),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                        )
                    }
                },
                enabled = isOwner && !isSaving,
                readOnly = !isOwner,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_profile_footer")
            )

            if (isOwner) {
                val footerPresets = listOf(
                    stringResource(R.string.depot_profile_footer_preset_1),
                    stringResource(R.string.depot_profile_footer_preset_2),
                    stringResource(R.string.depot_profile_footer_preset_3)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.depot_profile_footer_presets_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        footerPresets.forEachIndexed { idx, presetText ->
                            val isPresetSelected = receiptFooter.trim().equals(presetText, ignoreCase = true)
                            FilterChip(
                                selected = isPresetSelected,
                                onClick = { if (!isSaving) receiptFooter = presetText },
                                label = {
                                    Text(
                                        text = presetText,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("chip_footer_preset_$idx")
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.depot_type_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val depotTypeOptions = listOf(
                Triple(
                    DepotProfile.TYPE_RO,
                    stringResource(R.string.depot_type_ro),
                    stringResource(R.string.depot_type_ro_desc)
                ),
                Triple(
                    DepotProfile.TYPE_MINERAL,
                    stringResource(R.string.depot_type_mineral),
                    stringResource(R.string.depot_type_mineral_desc)
                ),
                Triple(
                    DepotProfile.TYPE_BOTH,
                    stringResource(R.string.depot_type_both),
                    stringResource(R.string.depot_type_both_desc)
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                depotTypeOptions.forEach { (typeValue, labelText, descText) ->
                    val isSelected = selectedDepotType == typeValue
                    Surface(
                        onClick = { if (isOwner && !isSaving) selectedDepotType = typeValue },
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("option_depot_type_$typeValue")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = descText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedDepotType != DepotProfile.TYPE_RO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tankCapacityInput,
                        onValueChange = { raw ->
                            tankCapacityInput = raw.filter { it.isDigit() }.take(7)
                        },
                        label = { Text(stringResource(R.string.depot_tank_capacity_label)) },
                        placeholder = { Text(stringResource(R.string.depot_tank_capacity_hint)) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.WaterDrop, contentDescription = null) },
                        enabled = isOwner && !isSaving,
                        readOnly = !isOwner,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_tank_capacity")
                    )

                    if (isOwner) {
                        Text(
                            text = stringResource(R.string.depot_profile_tank_presets_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DepotProfile.TANK_CAPACITY_PRESETS_LITERS.forEach { presetLiters ->
                                val isSelected = tankCapacityInput.toLongOrNull() == presetLiters
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSaving) {
                                            tankCapacityInput = presetLiters.toString()
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(
                                                R.string.depot_profile_tank_preset_item,
                                                FormatUtils.formatNumber(presetLiters.toDouble())
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_tank_preset_$presetLiters")
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.depot_profile_preview_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LiveReceiptHeaderPreviewCard(profile = previewProfile)

            if (isOwner) {
                Button(
                    onClick = {
                        if (ownerName.trim().isEmpty()) {
                            showOwnerError = true
                            return@Button
                        }
                        val resolvedCapacity = tankCapacityInput.toDoubleOrNull()?.coerceAtLeast(100.0)
                            ?: profile.tankCapacityLiters
                        onSave(
                            ownerName.trim(),
                            depotName.trim(),
                            DepotProfile.normalizePhoneInput(phoneNumber),
                            address.trim(),
                            receiptFooter.trim(),
                            selectedDepotType,
                            resolvedCapacity
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_save_depot_profile")
                ) {
                    Text(
                        text = if (isSaving) {
                            stringResource(R.string.depot_profile_saving)
                        } else {
                            stringResource(R.string.depot_profile_btn_save)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showUnsavedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.depot_profile_unsaved_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.depot_profile_unsaved_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_discard_profile_changes")
                ) {
                    Text(stringResource(R.string.depot_profile_unsaved_discard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsavedConfirmDialog = false },
                    modifier = Modifier.testTag("btn_keep_editing_profile")
                ) {
                    Text(stringResource(R.string.depot_profile_unsaved_keep))
                }
            }
        )
    }
}

/**
 * Kartu pratinjau multi-format (Struk Thermal, Nota WhatsApp, & Kop Laporan PDF)
 * langsung di bawah form profil depot agar pemilik dapat melihat tampilan aktual sebelum menyimpan.
 */
@Composable
fun LiveReceiptHeaderPreviewCard(
    profile: DepotProfile,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    var selectedTab by remember { mutableIntStateOf(0) }
    val displayTitle = remember(profile.depotName) { profile.effectiveDepotName().uppercase() }
    val cleanAddress = remember(profile.address) { profile.address.trim() }
    val cleanPhone = remember(profile.phoneNumber) { profile.normalizedPhoneNumber() }
    val cleanFooter = remember(profile.receiptFooter) { profile.receiptFooter.trim() }
    val displayOwner = remember(profile.ownerName, profile.googleDisplayName) {
        profile.effectiveOwnerName().ifBlank { "-" }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple(0, stringResource(R.string.depot_profile_preview_tab_thermal), Icons.Outlined.Print),
                Triple(1, stringResource(R.string.depot_profile_preview_tab_wa), Icons.Outlined.Share),
                Triple(2, stringResource(R.string.depot_profile_preview_tab_pdf), Icons.Outlined.Description)
            )
            tabs.forEach { (idx, label, icon) ->
                val isSelected = selectedTab == idx
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = idx },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_profile_preview_$idx")
                )
            }
        }

        Card(
            shape = RoundedCornerShape(SantoyaRadius.md),
            colors = CardDefaults.cardColors(containerColor = santoyaColors.receiptSurface),
            border = BorderStroke(1.dp, santoyaColors.receiptDivider),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_live_receipt_preview")
        ) {
            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                HydroDropletSparkIcon(
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.receiptOnSurface,
                            textAlign = TextAlign.Center
                        )
                        if (cleanAddress.isNotEmpty()) {
                            Text(
                                text = cleanAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = santoyaColors.receiptMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (cleanPhone.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.depot_profile_preview_phone_prefix, cleanPhone),
                                style = MaterialTheme.typography.bodySmall,
                                color = santoyaColors.receiptMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = stringResource(R.string.receipt_subtitle_sale),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = santoyaColors.receiptMuted,
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = santoyaColors.receiptDivider
                        )
                        Text(
                            text = cleanFooter.ifBlank { stringResource(R.string.receipt_footer_thanks) },
                            style = MaterialTheme.typography.bodySmall,
                            color = santoyaColors.receiptMuted,
                            textAlign = TextAlign.Center
                        )
                        if (!profile.isProUser()) {
                            Text(
                                text = DepotProfile.WATERMARK_TEXT,
                                style = MaterialTheme.typography.labelSmall,
                                color = santoyaColors.receiptMuted.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                1 -> {
                    val thermalHeaderLines = remember(displayTitle, cleanAddress, cleanPhone, cleanFooter, profile.subscriptionTier) {
                        buildList {
                            addAll(SantoyaReceiptHelper.centerWrappedText(displayTitle))
                            if (cleanAddress.isNotEmpty()) {
                                addAll(SantoyaReceiptHelper.centerWrappedText(cleanAddress))
                            }
                            if (cleanPhone.isNotEmpty()) {
                                addAll(SantoyaReceiptHelper.centerWrappedText("WA/Telp: $cleanPhone"))
                            }
                            add("-".repeat(32))
                            add(SantoyaReceiptHelper.leftRightText("1x Galon RO", "Rp 6.000"))
                            add("-".repeat(32))
                            val footerStr = cleanFooter.ifBlank { "Terima Kasih" }
                            addAll(SantoyaReceiptHelper.centerWrappedText(footerStr))
                            if (!profile.isProUser()) {
                                add(SantoyaReceiptHelper.centerText(DepotProfile.WATERMARK_TEXT))
                            }
                        }.joinToString("\n")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = thermalHeaderLines,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = santoyaColors.receiptOnSurface
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val pdfAddressLine = buildList {
                            if (cleanAddress.isNotEmpty()) add(cleanAddress)
                            if (cleanPhone.isNotEmpty()) add(stringResource(R.string.depot_profile_preview_phone_prefix, cleanPhone))
                        }.joinToString(" • ")
                        if (pdfAddressLine.isNotEmpty()) {
                            Text(
                                text = pdfAddressLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = santoyaColors.receiptMuted
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            thickness = 1.5.dp
                        )
                        Text(
                            text = stringResource(R.string.depot_profile_preview_pdf_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.receiptOnSurface
                        )
                        Text(
                            text = stringResource(R.string.depot_profile_preview_pdf_period, displayOwner),
                            style = MaterialTheme.typography.bodySmall,
                            color = santoyaColors.receiptMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog pengisian cepat Nama, Nomor WA/Telp, & Alamat Depot yang muncul ketika pemilik mencetak struk Bluetooth,
 * membagikan nota WhatsApp, atau mengekspor laporan PDF saat nama depot masih kosong.
 */
@Composable
fun DepotQuickFillDialog(
    initialProfile: DepotProfile,
    onDismiss: () -> Unit,
    onSaveAndContinue: (depotName: String, address: String, phoneNumber: String) -> Unit,
    onSkip: () -> Unit
) {
    var depotName by remember(initialProfile) { mutableStateOf(initialProfile.depotName) }
    var phoneNumber by remember(initialProfile) { mutableStateOf(initialProfile.phoneNumber) }
    var address by remember(initialProfile) { mutableStateOf(initialProfile.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(SantoyaRadius.lg),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.depot_quick_fill_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.depot_quick_fill_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = depotName,
                    onValueChange = { depotName = it.take(100) },
                    label = { Text(stringResource(R.string.depot_quick_fill_name_label)) },
                    placeholder = { Text(stringResource(R.string.depot_quick_fill_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_depot_name")
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.take(40) },
                    label = { Text(stringResource(R.string.depot_quick_fill_phone_label)) },
                    placeholder = { Text(stringResource(R.string.depot_quick_fill_phone_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_depot_phone")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(300) },
                    label = { Text(stringResource(R.string.depot_quick_fill_address_label)) },
                    placeholder = { Text(stringResource(R.string.depot_quick_fill_address_hint)) },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_depot_address")
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSaveAndContinue(
                            depotName.trim(),
                            address.trim(),
                            DepotProfile.normalizePhoneInput(phoneNumber)
                        )
                    },
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_quick_save_depot")
                ) {
                    Text(
                        text = stringResource(R.string.depot_quick_fill_btn_save),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_quick_skip_depot")
                ) {
                    Text(
                        text = stringResource(R.string.depot_quick_fill_btn_skip),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Kartu pengingat di bagian atas layar Beranda untuk mengajak Pemilik melengkapi nama dan alamat depot
 * ketika `depotName` masih kosong. Otomatis hilang setelah nama depot tersimpan.
 */
@Composable
fun DashboardDepotReminderCard(
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    Surface(
        onClick = onSetupClick,
        shape = RoundedCornerShape(SantoyaRadius.lg),
        color = santoyaColors.warningContainer,
        border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_dashboard_depot_reminder")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(santoyaColors.warning.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = santoyaColors.warning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_depot_reminder_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = santoyaColors.onWarningContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.dashboard_depot_reminder_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = santoyaColors.onWarningContainer.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.sm),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = stringResource(R.string.dashboard_depot_reminder_cta),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}
