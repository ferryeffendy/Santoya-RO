package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

@Immutable
data class SantoyaExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val rankGold: Color,
    val rankSilver: Color,
    val rankBronze: Color,
    val onHero: Color,
    val heroLeafTint: Color,
    val receiptSurface: Color,
    val receiptOnSurface: Color,
    val receiptMuted: Color,
    val receiptDivider: Color,
    val isDark: Boolean
)

private val LightExtendedColors = SantoyaExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight,
    danger = ErrorLight,
    onDanger = OnErrorLight,
    dangerContainer = ErrorContainerLight,
    onDangerContainer = OnErrorContainerLight,
    info = InfoLight,
    onInfo = OnInfoLight,
    infoContainer = InfoContainerLight,
    onInfoContainer = OnInfoContainerLight,
    rankGold = RankGold,
    rankSilver = RankSilver,
    rankBronze = RankBronze,
    onHero = HeroOnSurface,
    heroLeafTint = HeroLeafTint,
    receiptSurface = ReceiptPaperLight,
    receiptOnSurface = ReceiptInkLight,
    receiptMuted = ReceiptMutedLight,
    receiptDivider = ReceiptDividerLight,
    isDark = false
)

private val DarkExtendedColors = SantoyaExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark,
    danger = ErrorDark,
    onDanger = OnErrorDark,
    dangerContainer = ErrorContainerDark,
    onDangerContainer = OnErrorContainerDark,
    info = InfoDark,
    onInfo = OnInfoDark,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark,
    rankGold = RankGoldDark,
    rankSilver = RankSilverDark,
    rankBronze = RankBronzeDark,
    onHero = HeroOnSurface,
    heroLeafTint = HeroLeafTint,
    receiptSurface = ReceiptPaperDark,
    receiptOnSurface = ReceiptInkDark,
    receiptMuted = ReceiptMutedDark,
    receiptDivider = ReceiptDividerDark,
    isDark = true
)

val LocalSantoyaColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.santoyaColors: SantoyaExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSantoyaColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

/**
 * Efek skala taktil ringan tanpa rekomposisi untuk tombol dan kartu POS menggunakan [ModifierNodeElement].
 */
fun Modifier.pressClickEffect(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this
    .then(PressScaleElement(pressedScale = pressedScale, enabled = enabled))
    .clickable(enabled = enabled, onClick = onClick)

private data class PressScaleElement(
    val pressedScale: Float,
    val enabled: Boolean
) : ModifierNodeElement<PressScaleNode>() {
    override fun create(): PressScaleNode = PressScaleNode(pressedScale, enabled)

    override fun update(node: PressScaleNode) {
        node.updateParams(pressedScale, enabled)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
        properties["enabled"] = enabled
    }
}

private class PressScaleNode(
    private var pressedScale: Float,
    private var enabled: Boolean
) : Modifier.Node(), PointerInputModifierNode, LayoutModifierNode {

    private val scaleAnim = Animatable(1f)
    private val scaleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    private var isPressed: Boolean = false

    fun updateParams(newPressedScale: Float, newEnabled: Boolean) {
        pressedScale = newPressedScale
        enabled = newEnabled
        if (!newEnabled) {
            setPressed(false)
        }
    }

    private fun setPressed(pressed: Boolean) {
        val effective = pressed && enabled
        if (isPressed == effective) return
        isPressed = effective
        if (!isAttached) return
        val target = if (effective) pressedScale else 1f
        coroutineScope.launch {
            scaleAnim.animateTo(targetValue = target, animationSpec = scaleSpec)
        }
    }

    override fun onDetach() {
        isPressed = false
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass != PointerEventPass.Main) return
        when (pointerEvent.type) {
            PointerEventType.Press -> setPressed(true)
            PointerEventType.Release, PointerEventType.Exit -> setPressed(false)
            PointerEventType.Move -> {
                if (pointerEvent.changes.none { it.pressed }) {
                    setPressed(false)
                }
            }
        }
    }

    override fun onCancelPointerInput() {
        setPressed(false)
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val s = scaleAnim.value
                scaleX = s
                scaleY = s
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalSantoyaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SantoyaShapes,
            content = content
        )
    }
}
