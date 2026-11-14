package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.santoyaColors

/**
 * Five empty-state categories for Santoya RO screens, rendered as borderless
 * flat vector Canvas art that adapts automatically to Light and Dark themes.
 */
enum class SantoyaEmptyType {
    LAPORAN,
    TRANSAKSI,
    DEPOT,
    KASBON,
    PELANGGAN
}

/**
 * Borderless flat vector illustration tailored to Santoya RO's Hydro visual language.
 * Integrates directly onto the parent card without rectangular borders or boxy frames.
 */
@Composable
fun SantoyaEmptyIllustration(
    type: SantoyaEmptyType,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
    description: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val santoya = MaterialTheme.santoyaColors
    val isDark = santoya.isDark

    val primaryColor = accentColor
    val secondaryColor = colorScheme.secondary
    val surfaceFill = colorScheme.surface
    val surfaceSoft = colorScheme.surfaceVariant
    val mutedLine = colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.45f else 0.35f)
    val haloPrimary = primaryColor.copy(alpha = if (isDark) 0.16f else 0.11f)
    val haloSecondary = secondaryColor.copy(alpha = if (isDark) 0.14f else 0.09f)
    val goldColor = santoya.rankGold
    val goldFill = santoya.warningContainer
    val successColor = santoya.success
    val successFill = santoya.successContainer

    val semanticModifier = if (!description.isNullOrBlank()) {
        modifier.semantics { contentDescription = description }
    } else {
        modifier
    }

    Canvas(
        modifier = semanticModifier.size(
            width = if (compact) 136.dp else 172.dp,
            height = if (compact) 96.dp else 120.dp
        )
    ) {
        val strokeWidth = (if (compact) 1.75.dp else 2.0.dp).toPx()
        when (type) {
            SantoyaEmptyType.LAPORAN -> drawLaporanVector(
                strokeWidth = strokeWidth,
                primary = primaryColor,
                secondary = secondaryColor,
                gold = goldColor,
                goldFill = goldFill,
                surfaceFill = surfaceFill,
                mutedLine = mutedLine,
                haloPrimary = haloPrimary,
                haloSecondary = haloSecondary,
                isDark = isDark
            )

            SantoyaEmptyType.TRANSAKSI -> drawTransaksiVector(
                strokeWidth = strokeWidth,
                primary = primaryColor,
                secondary = secondaryColor,
                surfaceFill = surfaceFill,
                surfaceSoft = surfaceSoft,
                mutedLine = mutedLine,
                haloPrimary = haloPrimary,
                haloSecondary = haloSecondary,
                isDark = isDark
            )

            SantoyaEmptyType.DEPOT -> drawDepotVector(
                strokeWidth = strokeWidth,
                primary = primaryColor,
                secondary = secondaryColor,
                surfaceFill = surfaceFill,
                surfaceSoft = surfaceSoft,
                mutedLine = mutedLine,
                haloPrimary = haloPrimary,
                haloSecondary = haloSecondary,
                isDark = isDark
            )

            SantoyaEmptyType.KASBON -> drawKasbonVector(
                strokeWidth = strokeWidth,
                primary = primaryColor,
                success = successColor,
                successFill = successFill,
                surfaceFill = surfaceFill,
                mutedLine = mutedLine,
                haloPrimary = haloPrimary,
                haloSecondary = haloSecondary,
                isDark = isDark
            )

            SantoyaEmptyType.PELANGGAN -> drawPelangganVector(
                strokeWidth = strokeWidth,
                primary = primaryColor,
                secondary = secondaryColor,
                surfaceFill = surfaceFill,
                surfaceSoft = surfaceSoft,
                mutedLine = mutedLine,
                haloPrimary = haloPrimary,
                haloSecondary = haloSecondary,
                isDark = isDark
            )
        }
    }
}

private fun DrawScope.drawOrganicHalo(
    haloPrimary: Color,
    haloSecondary: Color,
    accentDot: Color
) {
    val w = size.width
    val h = size.height
    drawCircle(
        color = haloPrimary,
        radius = h * 0.42f,
        center = Offset(w * 0.46f, h * 0.50f)
    )
    drawCircle(
        color = haloSecondary,
        radius = h * 0.28f,
        center = Offset(w * 0.64f, h * 0.54f)
    )
    drawCircle(
        color = accentDot.copy(alpha = 0.28f),
        radius = h * 0.032f,
        center = Offset(w * 0.21f, h * 0.25f)
    )
    drawCircle(
        color = accentDot.copy(alpha = 0.20f),
        radius = h * 0.022f,
        center = Offset(w * 0.80f, h * 0.24f)
    )
    drawCircle(
        color = accentDot.copy(alpha = 0.22f),
        radius = h * 0.020f,
        center = Offset(w * 0.77f, h * 0.80f)
    )
}

private fun DrawScope.drawLaporanVector(
    strokeWidth: Float,
    primary: Color,
    secondary: Color,
    gold: Color,
    goldFill: Color,
    surfaceFill: Color,
    mutedLine: Color,
    haloPrimary: Color,
    haloSecondary: Color,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawOrganicHalo(haloPrimary, haloSecondary, primary)

    // Clean baseline
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.20f, h * 0.82f),
        end = Offset(w * 0.78f, h * 0.82f),
        strokeWidth = strokeWidth * 0.85f,
        cap = StrokeCap.Round
    )

    // 4 ascending flat bars
    val barWidth = w * 0.075f
    val barCorner = CornerRadius(barWidth * 0.35f, barWidth * 0.35f)
    val bars = listOf(
        Triple(0.24f, 0.64f, 0.14f),
        Triple(0.34f, 0.52f, 0.20f),
        Triple(0.44f, 0.38f, 0.26f),
        Triple(0.54f, 0.24f, 0.34f)
    )
    bars.forEach { (xFrac, topFrac, fillAlpha) ->
        val left = w * xFrac
        val top = h * topFrac
        val barHeight = h * 0.82f - top
        drawRoundRect(
            color = surfaceFill,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            cornerRadius = barCorner
        )
        drawRoundRect(
            color = primary.copy(alpha = if (isDark) fillAlpha + 0.12f else fillAlpha),
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            cornerRadius = barCorner
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            cornerRadius = barCorner,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
        )
    }

    // Rising botanical growth curve with leaf tip
    val trendPath = Path().apply {
        moveTo(w * 0.24f, h * 0.50f)
        cubicTo(w * 0.36f, h * 0.48f, w * 0.44f, h * 0.28f, w * 0.58f, h * 0.18f)
    }
    drawPath(
        path = trendPath,
        color = secondary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    val leafPath = Path().apply {
        moveTo(w * 0.57f, h * 0.19f)
        cubicTo(w * 0.59f, h * 0.10f, w * 0.68f, h * 0.10f, w * 0.72f, h * 0.14f)
        cubicTo(w * 0.69f, h * 0.22f, w * 0.62f, h * 0.23f, w * 0.57f, h * 0.19f)
        close()
    }
    drawPath(path = leafPath, color = secondary.copy(alpha = 0.26f))
    drawPath(
        path = leafPath,
        color = secondary,
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Foreground flat RO water droplet on bottom-right
    val dropPath = Path().apply {
        moveTo(w * 0.69f, h * 0.40f)
        cubicTo(w * 0.69f, h * 0.40f, w * 0.59f, h * 0.58f, w * 0.59f, h * 0.68f)
        cubicTo(w * 0.59f, h * 0.77f, w * 0.635f, h * 0.82f, w * 0.69f, h * 0.82f)
        cubicTo(w * 0.745f, h * 0.82f, w * 0.79f, h * 0.77f, w * 0.79f, h * 0.68f)
        cubicTo(w * 0.79f, h * 0.58f, w * 0.69f, h * 0.40f, w * 0.69f, h * 0.40f)
        close()
    }
    drawPath(path = dropPath, color = surfaceFill)
    drawPath(path = dropPath, color = primary.copy(alpha = 0.22f))
    drawPath(
        path = dropPath,
        color = primary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Minimalist flat coin accent next to droplet
    val coinCenter = Offset(w * 0.77f, h * 0.73f)
    val coinRadius = h * 0.10f
    drawCircle(color = goldFill, radius = coinRadius, center = coinCenter)
    drawCircle(
        color = gold,
        radius = coinRadius,
        center = coinCenter,
        style = Stroke(width = strokeWidth * 0.9f)
    )
    drawLine(
        color = gold,
        start = Offset(coinCenter.x, coinCenter.y - coinRadius * 0.42f),
        end = Offset(coinCenter.x, coinCenter.y + coinRadius * 0.42f),
        strokeWidth = strokeWidth * 0.8f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawTransaksiVector(
    strokeWidth: Float,
    primary: Color,
    secondary: Color,
    surfaceFill: Color,
    surfaceSoft: Color,
    mutedLine: Color,
    haloPrimary: Color,
    haloSecondary: Color,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawOrganicHalo(haloPrimary, haloSecondary, primary)

    // Receipt sheet with gentle scalloped bottom edge
    val receiptPath = Path().apply {
        moveTo(w * 0.31f, h * 0.22f)
        quadraticTo(w * 0.31f, h * 0.15f, w * 0.36f, h * 0.15f)
        lineTo(w * 0.62f, h * 0.15f)
        quadraticTo(w * 0.67f, h * 0.15f, w * 0.67f, h * 0.22f)
        lineTo(w * 0.67f, h * 0.82f)
        quadraticTo(w * 0.625f, h * 0.75f, w * 0.58f, h * 0.82f)
        quadraticTo(w * 0.535f, h * 0.88f, w * 0.49f, h * 0.82f)
        quadraticTo(w * 0.445f, h * 0.75f, w * 0.40f, h * 0.82f)
        quadraticTo(w * 0.355f, h * 0.88f, w * 0.31f, h * 0.82f)
        close()
    }
    drawPath(path = receiptPath, color = surfaceFill)
    drawPath(
        path = receiptPath,
        color = surfaceSoft.copy(alpha = if (isDark) 0.35f else 0.45f)
    )
    drawPath(
        path = receiptPath,
        color = primary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Header pill inside receipt
    drawRoundRect(
        color = primary.copy(alpha = 0.22f),
        topLeft = Offset(w * 0.37f, h * 0.25f),
        size = Size(w * 0.18f, h * 0.08f),
        cornerRadius = CornerRadius(h * 0.04f, h * 0.04f)
    )
    // Summary line rows
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.37f, h * 0.44f),
        end = Offset(w * 0.60f, h * 0.44f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.37f, h * 0.55f),
        end = Offset(w * 0.55f, h * 0.55f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = primary.copy(alpha = 0.55f),
        start = Offset(w * 0.37f, h * 0.66f),
        end = Offset(w * 0.50f, h * 0.66f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Floating circular Santoya water drop badge on top-right
    val badgeCenter = Offset(w * 0.68f, h * 0.30f)
    val badgeRadius = h * 0.145f
    drawCircle(color = surfaceFill, radius = badgeRadius, center = badgeCenter)
    drawCircle(
        color = secondary.copy(alpha = if (isDark) 0.24f else 0.16f),
        radius = badgeRadius,
        center = badgeCenter
    )
    drawCircle(
        color = secondary,
        radius = badgeRadius,
        center = badgeCenter,
        style = Stroke(width = strokeWidth * 0.9f)
    )
    val miniDrop = Path().apply {
        moveTo(badgeCenter.x, badgeCenter.y - badgeRadius * 0.50f)
        cubicTo(
            badgeCenter.x, badgeCenter.y - badgeRadius * 0.50f,
            badgeCenter.x - badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.04f,
            badgeCenter.x - badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.24f
        )
        cubicTo(
            badgeCenter.x - badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.48f,
            badgeCenter.x - badgeRadius * 0.18f, badgeCenter.y + badgeRadius * 0.58f,
            badgeCenter.x, badgeCenter.y + badgeRadius * 0.58f
        )
        cubicTo(
            badgeCenter.x + badgeRadius * 0.18f, badgeCenter.y + badgeRadius * 0.58f,
            badgeCenter.x + badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.48f,
            badgeCenter.x + badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.24f
        )
        cubicTo(
            badgeCenter.x + badgeRadius * 0.38f, badgeCenter.y + badgeRadius * 0.04f,
            badgeCenter.x, badgeCenter.y - badgeRadius * 0.50f,
            badgeCenter.x, badgeCenter.y - badgeRadius * 0.50f
        )
        close()
    }
    drawPath(
        path = miniDrop,
        color = secondary,
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawDepotVector(
    strokeWidth: Float,
    primary: Color,
    secondary: Color,
    surfaceFill: Color,
    surfaceSoft: Color,
    mutedLine: Color,
    haloPrimary: Color,
    haloSecondary: Color,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawOrganicHalo(haloPrimary, haloSecondary, primary)

    // Ground baseline
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.18f, h * 0.82f),
        end = Offset(w * 0.82f, h * 0.82f),
        strokeWidth = strokeWidth * 0.85f,
        cap = StrokeCap.Round
    )

    // Left: 19L RO Gallon silhouette
    drawRoundRect(
        color = surfaceFill,
        topLeft = Offset(w * 0.285f, h * 0.15f),
        size = Size(w * 0.09f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(w * 0.285f, h * 0.15f),
        size = Size(w * 0.09f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.025f, w * 0.025f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val gallonBody = Path().apply {
        moveTo(w * 0.295f, h * 0.24f)
        lineTo(w * 0.295f, h * 0.29f)
        cubicTo(w * 0.23f, h * 0.33f, w * 0.21f, h * 0.41f, w * 0.21f, h * 0.50f)
        lineTo(w * 0.21f, h * 0.72f)
        cubicTo(w * 0.21f, h * 0.79f, w * 0.24f, h * 0.82f, w * 0.28f, h * 0.82f)
        lineTo(w * 0.38f, h * 0.82f)
        cubicTo(w * 0.42f, h * 0.82f, w * 0.45f, h * 0.79f, w * 0.45f, h * 0.72f)
        lineTo(w * 0.45f, h * 0.50f)
        cubicTo(w * 0.45f, h * 0.41f, w * 0.43f, h * 0.33f, w * 0.365f, h * 0.29f)
        lineTo(w * 0.365f, h * 0.24f)
        close()
    }
    drawPath(path = gallonBody, color = surfaceFill)
    drawPath(
        path = gallonBody,
        color = primary.copy(alpha = if (isDark) 0.22f else 0.14f)
    )
    drawPath(
        path = gallonBody,
        color = primary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Wave inside gallon
    val gallonWave = Path().apply {
        moveTo(w * 0.24f, h * 0.60f)
        cubicTo(w * 0.29f, h * 0.54f, w * 0.36f, h * 0.66f, w * 0.42f, h * 0.59f)
    }
    drawPath(
        path = gallonWave,
        color = primary,
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
    )

    // Signature leaf on gallon shoulder
    val leaf = Path().apply {
        moveTo(w * 0.40f, h * 0.32f)
        cubicTo(w * 0.41f, h * 0.21f, w * 0.48f, h * 0.18f, w * 0.52f, h * 0.20f)
        cubicTo(w * 0.51f, h * 0.28f, w * 0.46f, h * 0.34f, w * 0.40f, h * 0.32f)
        close()
    }
    drawPath(path = leaf, color = secondary.copy(alpha = 0.24f))
    drawPath(
        path = leaf,
        color = secondary,
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Right: Minimalist courier delivery vehicle
    drawRoundRect(
        color = surfaceFill,
        topLeft = Offset(w * 0.50f, h * 0.42f),
        size = Size(w * 0.18f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawRoundRect(
        color = surfaceSoft.copy(alpha = if (isDark) 0.35f else 0.55f),
        topLeft = Offset(w * 0.50f, h * 0.42f),
        size = Size(w * 0.18f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(w * 0.50f, h * 0.42f),
        size = Size(w * 0.18f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val cabPath = Path().apply {
        moveTo(w * 0.68f, h * 0.49f)
        lineTo(w * 0.745f, h * 0.49f)
        quadraticTo(w * 0.80f, h * 0.51f, w * 0.80f, h * 0.61f)
        lineTo(w * 0.80f, h * 0.70f)
        lineTo(w * 0.68f, h * 0.70f)
        close()
    }
    drawPath(path = cabPath, color = surfaceFill)
    drawPath(path = cabPath, color = secondary.copy(alpha = 0.18f))
    drawPath(
        path = cabPath,
        color = primary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Wheels
    val wheelRadius = h * 0.075f
    listOf(w * 0.565f, w * 0.735f).forEach { cx ->
        val center = Offset(cx, h * 0.745f)
        drawCircle(color = surfaceFill, radius = wheelRadius, center = center)
        drawCircle(
            color = primary,
            radius = wheelRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(color = secondary, radius = wheelRadius * 0.34f, center = center)
    }
}

private fun DrawScope.drawKasbonVector(
    strokeWidth: Float,
    primary: Color,
    success: Color,
    successFill: Color,
    surfaceFill: Color,
    mutedLine: Color,
    haloPrimary: Color,
    haloSecondary: Color,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawOrganicHalo(haloPrimary, haloSecondary, success)

    // Wallet / ledger body
    val walletLeft = w * 0.25f
    val walletTop = h * 0.24f
    val walletWidth = w * 0.46f
    val walletHeight = h * 0.52f
    val walletRadius = CornerRadius(w * 0.055f, w * 0.055f)

    drawRoundRect(
        color = surfaceFill,
        topLeft = Offset(walletLeft, walletTop),
        size = Size(walletWidth, walletHeight),
        cornerRadius = walletRadius
    )
    drawRoundRect(
        color = primary.copy(alpha = if (isDark) 0.16f else 0.08f),
        topLeft = Offset(walletLeft, walletTop),
        size = Size(walletWidth, walletHeight),
        cornerRadius = walletRadius
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(walletLeft, walletTop),
        size = Size(walletWidth, walletHeight),
        cornerRadius = walletRadius,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Top ledger flap line
    drawLine(
        color = primary.copy(alpha = 0.45f),
        start = Offset(walletLeft + w * 0.03f, h * 0.37f),
        end = Offset(walletLeft + walletWidth - w * 0.03f, h * 0.37f),
        strokeWidth = strokeWidth * 0.85f,
        cap = StrokeCap.Round
    )

    // Clean horizontal notes lines
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.31f, h * 0.49f),
        end = Offset(w * 0.53f, h * 0.49f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.31f, h * 0.60f),
        end = Offset(w * 0.47f, h * 0.60f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Wallet clasp pill on right edge
    drawRoundRect(
        color = surfaceFill,
        topLeft = Offset(w * 0.61f, h * 0.43f),
        size = Size(w * 0.12f, h * 0.14f),
        cornerRadius = CornerRadius(h * 0.07f, h * 0.07f)
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(w * 0.61f, h * 0.43f),
        size = Size(w * 0.12f, h * 0.14f),
        cornerRadius = CornerRadius(h * 0.07f, h * 0.07f),
        style = Stroke(width = strokeWidth * 0.9f)
    )

    // Overlapping circular "Lunas" checkmark badge
    val checkCenter = Offset(w * 0.69f, h * 0.69f)
    val checkRadius = h * 0.165f
    drawCircle(color = surfaceFill, radius = checkRadius, center = checkCenter)
    drawCircle(color = successFill, radius = checkRadius, center = checkCenter)
    drawCircle(
        color = success,
        radius = checkRadius,
        center = checkCenter,
        style = Stroke(width = strokeWidth)
    )
    val checkPath = Path().apply {
        moveTo(checkCenter.x - checkRadius * 0.42f, checkCenter.y + checkRadius * 0.02f)
        lineTo(checkCenter.x - checkRadius * 0.10f, checkCenter.y + checkRadius * 0.34f)
        lineTo(checkCenter.x + checkRadius * 0.45f, checkCenter.y - checkRadius * 0.30f)
    }
    drawPath(
        path = checkPath,
        color = success,
        style = Stroke(width = strokeWidth * 1.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawPelangganVector(
    strokeWidth: Float,
    primary: Color,
    secondary: Color,
    surfaceFill: Color,
    surfaceSoft: Color,
    mutedLine: Color,
    haloPrimary: Color,
    haloSecondary: Color,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawOrganicHalo(haloPrimary, haloSecondary, primary)

    // Secondary background customer silhouette (right)
    val backHeadCenter = Offset(w * 0.60f, h * 0.36f)
    val backHeadRadius = h * 0.11f
    drawCircle(color = surfaceFill, radius = backHeadRadius, center = backHeadCenter)
    drawCircle(
        color = secondary.copy(alpha = if (isDark) 0.24f else 0.16f),
        radius = backHeadRadius,
        center = backHeadCenter
    )
    drawCircle(
        color = secondary,
        radius = backHeadRadius,
        center = backHeadCenter,
        style = Stroke(width = strokeWidth * 0.9f)
    )

    val backShoulders = Path().apply {
        moveTo(w * 0.48f, h * 0.78f)
        cubicTo(w * 0.48f, h * 0.61f, w * 0.53f, h * 0.54f, w * 0.60f, h * 0.54f)
        cubicTo(w * 0.67f, h * 0.54f, w * 0.72f, h * 0.61f, w * 0.72f, h * 0.78f)
        close()
    }
    drawPath(path = backShoulders, color = surfaceFill)
    drawPath(
        path = backShoulders,
        color = secondary.copy(alpha = if (isDark) 0.20f else 0.14f)
    )
    drawPath(
        path = backShoulders,
        color = secondary,
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Primary foreground customer silhouette (left-center)
    val frontHeadCenter = Offset(w * 0.41f, h * 0.31f)
    val frontHeadRadius = h * 0.135f
    drawCircle(color = surfaceFill, radius = frontHeadRadius, center = frontHeadCenter)
    drawCircle(
        color = surfaceSoft.copy(alpha = if (isDark) 0.35f else 0.55f),
        radius = frontHeadRadius,
        center = frontHeadCenter
    )
    drawCircle(
        color = primary,
        radius = frontHeadRadius,
        center = frontHeadCenter,
        style = Stroke(width = strokeWidth)
    )

    val frontShoulders = Path().apply {
        moveTo(w * 0.26f, h * 0.81f)
        cubicTo(w * 0.26f, h * 0.59f, w * 0.32f, h * 0.51f, w * 0.41f, h * 0.51f)
        cubicTo(w * 0.50f, h * 0.51f, w * 0.56f, h * 0.59f, w * 0.56f, h * 0.81f)
        close()
    }
    drawPath(path = frontShoulders, color = surfaceFill)
    drawPath(
        path = frontShoulders,
        color = primary.copy(alpha = if (isDark) 0.22f else 0.14f)
    )
    drawPath(
        path = frontShoulders,
        color = primary,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Minimalist baseline
    drawLine(
        color = mutedLine,
        start = Offset(w * 0.21f, h * 0.81f),
        end = Offset(w * 0.77f, h * 0.81f),
        strokeWidth = strokeWidth * 0.85f,
        cap = StrokeCap.Round
    )

    // Small Hydro water drop crest on right
    val dropPath = Path().apply {
        moveTo(w * 0.76f, h * 0.34f)
        cubicTo(w * 0.76f, h * 0.34f, w * 0.705f, h * 0.44f, w * 0.705f, h * 0.50f)
        cubicTo(w * 0.705f, h * 0.55f, w * 0.73f, h * 0.58f, w * 0.76f, h * 0.58f)
        cubicTo(w * 0.79f, h * 0.58f, w * 0.815f, h * 0.55f, w * 0.815f, h * 0.50f)
        cubicTo(w * 0.815f, h * 0.44f, w * 0.76f, h * 0.34f, w * 0.76f, h * 0.34f)
        close()
    }
    drawPath(path = dropPath, color = primary.copy(alpha = 0.20f))
    drawPath(
        path = dropPath,
        color = primary,
        style = Stroke(width = strokeWidth * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
