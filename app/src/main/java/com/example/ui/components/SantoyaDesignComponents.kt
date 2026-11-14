package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.RealtimeSyncState
import com.example.util.SyncConnectionStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time Cloud Firestore Sync Status Pill shared across Dashboard & Settings.
 */
@Composable
fun RealtimeSyncStatusPill(
    syncState: RealtimeSyncState,
    modifier: Modifier = Modifier
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_icon_spin"
    )

    val (icon, label, tint, bgColor, borderColor) = when (syncState.status) {
        SyncConnectionStatus.LIVE -> Tuple5(
            Icons.Default.CloudDone,
            "Terhubung",
            santoyaColors.success,
            santoyaColors.successContainer.copy(alpha = 0.55f),
            santoyaColors.success.copy(alpha = 0.35f)
        )
        SyncConnectionStatus.SYNCING, SyncConnectionStatus.CONNECTING -> Tuple5(
            Icons.Default.CloudSync,
            if (syncState.pendingCount > 0) "Sinkron (${syncState.pendingCount})" else "Sinkron...",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        SyncConnectionStatus.OFFLINE -> Tuple5(
            Icons.Default.CloudOff,
            if (syncState.pendingCount > 0) "Offline (${syncState.pendingCount})" else "Mode Offline",
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.6f),
            santoyaColors.warning.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.REVOKED -> Tuple5(
            Icons.Default.Block,
            "Dibatasi",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.ERROR -> Tuple5(
            Icons.Default.CloudOff,
            "Cek Koneksi",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
    }

    val isRotating = syncState.status == SyncConnectionStatus.SYNCING ||
        syncState.status == SyncConnectionStatus.CONNECTING

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer {
                        if (isRotating) rotationZ = rotation
                    }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

/**
 * Unified Local Storage & Cloud Status Pill shared by Dashboard & sub-screen TopAppBars.
 */
@Composable
fun LocalStorageStatusPill(
    modifier: Modifier = Modifier,
    isCloudActive: Boolean = Firebase.auth.currentUser != null
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val isConnected = isCloudActive
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else santoyaColors.successContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else santoyaColors.success.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                contentDescription = if (isConnected) "Sinkronisasi Cloud Aktif" else "Lokal HP",
                tint = if (isConnected) MaterialTheme.colorScheme.primary else santoyaColors.success,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) "Terhubung" else "Lokal HP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else santoyaColors.onSuccessContainer
            )
        }
    }
}

/**
 * Unified Section Header Banner for Master Screens (Produk, Kurir, etc.)
 * replacing fragmented container colors with a cohesive Fintech surface + accent pill.
 */
@Composable
fun SantoyaSectionBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = 18.dp,
        tintAlpha = 0.09f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HydroGradientBadge(
                gradientColors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.78f)
                ),
                size = 44.dp,
                cornerRadius = 14.dp
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onAccentColor,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Returns true whenever the soft keyboard (IME) is currently visible.
 */
@Composable
fun rememberIsImeVisible(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

/**
 * Automatically scrolls the focused text field cleanly above the soft keyboard.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    delay(180L)
                    requester.bringIntoView()
                }
            }
        }
}

/**
 * Adaptive ModalBottomSheet designed for Santoya RO forms:
 * - Follows IME height smoothly with navigationBarsPadding + imePadding
 * - Scrollable content body that never gets clipped
 * - Sticky bottom action footer with 48dp touch targets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantoyaFormBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    subtitle: String? = null,
    headerIcon: ImageVector? = null,
    headerAccent: Color = MaterialTheme.colorScheme.primary,
    dismissLabel: String = "Batal",
    confirmEnabled: Boolean = true,
    confirmContainerColor: Color = MaterialTheme.colorScheme.primary,
    confirmTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (headerIcon != null) {
                        HydroGradientBadge(
                            gradientColors = listOf(headerAccent, AccentCyan),
                            size = 38.dp,
                            cornerRadius = 12.dp,
                            elevation = 2.dp
                        ) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = dismissLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                val confirmModifier = Modifier
                    .weight(1.35f)
                    .heightIn(min = 48.dp)
                    .let { if (confirmTestTag != null) it.testTag(confirmTestTag) else it }
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmContainerColor),
                    modifier = confirmModifier
                ) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Unified Empty State Card with custom Santoya RO illustrations and contextual actions.
 */
@Composable
fun SantoyaEmptyState(
    @DrawableRes illustrationRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = 20.dp,
        tintAlpha = 0.05f,
        showWaterRipple = false,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (compact) 18.dp else 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f)),
                modifier = Modifier.size(
                    width = if (compact) 152.dp else 188.dp,
                    height = if (compact) 114.dp else 141.dp
                )
            ) {
                Image(
                    painter = painterResource(id = illustrationRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if ((primaryActionLabel != null && onPrimaryAction != null) ||
                (secondaryActionLabel != null && onSecondaryAction != null)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            if (secondaryActionIcon != null) {
                                Icon(
                                    imageVector = secondaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = secondaryActionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            if (primaryActionIcon != null) {
                                Icon(
                                    imageVector = primaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = primaryActionLabel,
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

/**
 * Adaptive Currency Text component that automatically downscales font size in constrained
 * layouts (e.g. Bento cards, financial summaries, narrow dialog rows) so that large Rupiah
 * figures (tens or hundreds of millions) NEVER truncate with ellipsis or cut off.
 */
@Composable
fun SantoyaAdaptiveCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    customText: String? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 11.sp,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1
) {
    val text = remember(amount, customText) { customText ?: FormatUtils.formatRupiah(amount) }
    val baseFontSize = if (style.fontSize.isSp && style.fontSize.value > 0f) style.fontSize else 18.sp
    var scaledFontSize by remember(text, baseFontSize, minFontSize) {
        mutableStateOf(baseFontSize)
    }
    var lastMaxWidthPx by remember { mutableIntStateOf(0) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            fontSize = scaledFontSize,
            fontWeight = fontWeight ?: style.fontWeight ?: FontWeight.Bold,
            color = color,
            textAlign = textAlign
        ),
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            val currentMaxWidth = result.layoutInput.constraints.maxWidth
            if (lastMaxWidthPx in 1 until currentMaxWidth && scaledFontSize < baseFontSize) {
                lastMaxWidthPx = currentMaxWidth
                scaledFontSize = baseFontSize
            } else {
                lastMaxWidthPx = currentMaxWidth
                if ((result.didOverflowWidth || result.hasVisualOverflow) &&
                    scaledFontSize.value > minFontSize.value + 0.1f
                ) {
                    val nextSize = (scaledFontSize.value * 0.88f).coerceAtLeast(minFontSize.value)
                    if (nextSize < scaledFontSize.value) {
                        scaledFontSize = nextSize.sp
                    }
                }
            }
        }
    )
}

/**
 * Smooth pulsing skeleton placeholder displayed while Room / Cloud initial data is loading,
 * preventing abrupt flicker to Empty State on startup or account switch.
 */
@Composable
fun SantoyaLoadingSkeleton(
    itemCount: Int = 4,
    message: String = "Memuat & menyinkronkan data...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        repeat(itemCount) {
            HydroBentoCard(
                accentColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 18.dp,
                tintAlpha = 0.04f,
                showWaterRipple = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(42.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha),
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .height(14.dp)
                        ) {}
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha * 0.8f),
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(11.dp)
                        ) {}
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.75f),
                        modifier = Modifier
                            .width(68.dp)
                            .height(24.dp)
                    ) {}
                }
            }
        }
    }
}

/**
 * Unified top status banner for active background/mutation operations (save, sync, import).
 */
@Composable
fun SantoyaBusyStatusBanner(
    isBusy: Boolean,
    message: String?,
    modifier: Modifier = Modifier
) {
    if (!isBusy || message.isNullOrBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "busy_banner_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "busy_icon_rotation"
    )

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


