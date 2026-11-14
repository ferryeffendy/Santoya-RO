package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SantoyaLeafGreen
import com.example.ui.theme.SantoyaWaterBlue
import com.example.ui.theme.pressClickEffect

/**
 * Solid Organic Squircle Badge for Santoya RO Iconography.
 * Uses pure solid brand colors with a crisp 1.dp organic border — zero gradients and zero glassmorphism.
 */
@Composable
fun HydroGradientBadge(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val primaryAccent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(primaryAccent)
            .border(
                width = 1.dp,
                color = primaryAccent.copy(alpha = 0.85f),
                shape = shape
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Editorial Bento Card with solid Pure White or Mist Blue surface, crisp 1.dp border,
 * and optional organic leaf-vein curve motif inspired by the Santoya RO logo.
 * Completely free of gradients and glassmorphic blur.
 */
@Composable
fun HydroBentoCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    tintAlpha: Float = 0.08f,
    showWaterRipple: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseSurface = MaterialTheme.colorScheme.surface
    val cardBackground = if (tintAlpha >= 0.12f) {
        accentColor.copy(alpha = 0.09f)
    } else {
        baseSurface
    }
    val borderColor = if (tintAlpha >= 0.12f) {
        accentColor.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val shape = RoundedCornerShape(cornerRadius)
    val interactiveModifier = if (onClick != null) {
        modifier.pressClickEffect(pressedScale = 0.98f, onClick = onClick)
    } else {
        modifier
    }

    Surface(
        shape = shape,
        color = baseSurface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        modifier = interactiveModifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackground)
                .drawBehind {
                    if (showWaterRipple) {
                        // Subtle organic leaf-vein & droplet arc in bottom-right corner
                        val w = size.width
                        val h = size.height
                        val organicArc = Path().apply {
                            moveTo(w * 0.78f, h)
                            quadraticTo(w * 0.86f, h * 0.62f, w, h * 0.52f)
                        }
                        drawPath(
                            path = organicArc,
                            color = accentColor.copy(alpha = 0.10f),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                },
            content = content
        )
    }
}

/**
 * 1. TRANSAKSI ICON — Organic Minimalist Line Art 2.0dp
 * Rounded 19L RO Gallon silhouette with a soft water wave and a signature Santoya Leaf sprout on the shoulder.
 */
@Composable
fun HydroGalonIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val resolvedLeafColor = leafAccent ?: if (tint == Color.White) {
        Color(0xFFA7F3B5)
    } else {
        SantoyaLeafGreen
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Rounded bottle cap / neck
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.09f),
            size = Size(w * 0.24f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Organic curved gallon body
        val bodyPath = Path().apply {
            moveTo(w * 0.39f, h * 0.20f)
            lineTo(w * 0.39f, h * 0.25f)
            cubicTo(w * 0.22f, h * 0.29f, w * 0.17f, h * 0.38f, w * 0.17f, h * 0.48f)
            lineTo(w * 0.17f, h * 0.78f)
            cubicTo(w * 0.17f, h * 0.88f, w * 0.25f, h * 0.91f, w * 0.35f, h * 0.91f)
            lineTo(w * 0.61f, h * 0.91f)
            cubicTo(w * 0.71f, h * 0.91f, w * 0.79f, h * 0.88f, w * 0.79f, h * 0.78f)
            lineTo(w * 0.79f, h * 0.50f)
            cubicTo(w * 0.79f, h * 0.39f, w * 0.74f, h * 0.29f, w * 0.57f, h * 0.25f)
            lineTo(w * 0.57f, h * 0.20f)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Minimalist continuous water wave inside gallon
        val wavePath = Path().apply {
            moveTo(w * 0.25f, h * 0.63f)
            cubicTo(w * 0.36f, h * 0.55f, w * 0.48f, h * 0.71f, w * 0.59f, h * 0.62f)
            quadraticTo(w * 0.66f, h * 0.57f, w * 0.71f, h * 0.61f)
        }
        drawPath(
            path = wavePath,
            color = tint,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Signature Santoya Leaf accent on top-right shoulder
        val leafPath = Path().apply {
            moveTo(w * 0.66f, h * 0.34f)
            cubicTo(w * 0.68f, h * 0.19f, w * 0.83f, h * 0.14f, w * 0.92f, h * 0.15f)
            cubicTo(w * 0.92f, h * 0.26f, w * 0.82f, h * 0.36f, w * 0.66f, h * 0.34f)
            close()
        }
        drawPath(
            path = leafPath,
            color = resolvedLeafColor,
            style = Stroke(width = stroke * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Pure Santoya RO Logo Mark Icon — Organic Water Droplet + Curved Leaf Line Art 2.0dp.
 */
@Composable
fun HydroDropletSparkIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val resolvedLeafColor = leafAccent ?: if (tint == Color.White) {
        Color(0xFFA7F3B5)
    } else {
        SantoyaLeafGreen
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Left organic water droplet
        val dropPath = Path().apply {
            moveTo(w * 0.44f, h * 0.10f)
            cubicTo(w * 0.44f, h * 0.10f, w * 0.14f, h * 0.48f, w * 0.14f, h * 0.66f)
            cubicTo(w * 0.14f, h * 0.82f, w * 0.27f, h * 0.91f, w * 0.43f, h * 0.91f)
            cubicTo(w * 0.42f, h * 0.72f, w * 0.50f, h * 0.54f, w * 0.66f, h * 0.43f)
            cubicTo(w * 0.58f, h * 0.28f, w * 0.44f, h * 0.10f, w * 0.44f, h * 0.10f)
            close()
        }
        drawPath(
            path = dropPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Right hugging botanical leaf
        val leafPath = Path().apply {
            moveTo(w * 0.48f, h * 0.90f)
            cubicTo(w * 0.47f, h * 0.64f, w * 0.63f, h * 0.45f, w * 0.88f, h * 0.40f)
            cubicTo(w * 0.90f, h * 0.67f, w * 0.74f, h * 0.88f, w * 0.48f, h * 0.90f)
            close()
        }
        drawPath(
            path = leafPath,
            color = resolvedLeafColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Inner leaf vein
        val veinPath = Path().apply {
            moveTo(w * 0.52f, h * 0.86f)
            quadraticTo(w * 0.67f, h * 0.68f, w * 0.78f, h * 0.52f)
        }
        drawPath(
            path = veinPath,
            color = resolvedLeafColor,
            style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round)
        )
    }
}

/**
 * 2. PELANGGAN ICON — Organic Minimalist Line Art 2.0dp
 * Rounded customer silhouette paired with the Santoya droplet & leaf emblem.
 */
@Composable
fun HydroCustomerDropIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val resolvedWaterColor = waterAccent ?: if (tint == Color.White) {
        Color(0xFFA7F3B5)
    } else {
        SantoyaWaterBlue
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Primary customer head circle
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.39f, h * 0.30f),
            style = Stroke(width = stroke)
        )

        // Smooth rounded shoulders curve
        val shoulderPath = Path().apply {
            moveTo(w * 0.13f, h * 0.85f)
            cubicTo(w * 0.13f, h * 0.62f, w * 0.24f, h * 0.54f, w * 0.39f, h * 0.54f)
            cubicTo(w * 0.54f, h * 0.54f, w * 0.65f, h * 0.62f, w * 0.65f, h * 0.85f)
        }
        drawPath(
            path = shoulderPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Organic droplet + leaf crest on right
        val miniDrop = Path().apply {
            moveTo(w * 0.79f, h * 0.22f)
            cubicTo(w * 0.79f, h * 0.22f, w * 0.91f, h * 0.40f, w * 0.91f, h * 0.50f)
            cubicTo(w * 0.91f, h * 0.58f, w * 0.86f, h * 0.63f, w * 0.79f, h * 0.63f)
            cubicTo(w * 0.72f, h * 0.63f, w * 0.67f, h * 0.58f, w * 0.67f, h * 0.50f)
            cubicTo(w * 0.67f, h * 0.40f, w * 0.79f, h * 0.22f, w * 0.79f, h * 0.22f)
            close()
        }
        drawPath(
            path = miniDrop,
            color = resolvedWaterColor,
            style = Stroke(width = stroke * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * 3. PENGELUARAN ICON — Organic Minimalist Line Art 2.0dp
 * Rounded capsule wallet with smooth curved outgoing arrow and single fluid water wave at base.
 */
@Composable
fun HydroExpenseFlowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val resolvedAccent = leafAccent ?: if (tint == Color.White) {
        Color(0xFFA7F3B5)
    } else {
        SantoyaLeafGreen
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Rounded capsule wallet outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.14f, h * 0.20f),
            size = Size(w * 0.72f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Smooth curved outgoing flow arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.34f, h * 0.55f)
            quadraticTo(w * 0.46f, h * 0.53f, w * 0.62f, h * 0.33f)
            moveTo(w * 0.46f, h * 0.33f)
            lineTo(w * 0.62f, h * 0.33f)
            lineTo(w * 0.62f, h * 0.49f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Organic fluid wave line at bottom
        val wavePath = Path().apply {
            moveTo(w * 0.18f, h * 0.85f)
            cubicTo(w * 0.35f, h * 0.77f, w * 0.52f, h * 0.93f, w * 0.68f, h * 0.84f)
            quadraticTo(w * 0.76f, h * 0.80f, w * 0.82f, h * 0.84f)
        }
        drawPath(
            path = wavePath,
            color = resolvedAccent,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * 4. LAPORAN ICON — Organic Minimalist Line Art 2.0dp
 * Rounded pill analytics bars paired with a rising botanical growth curve ending in a Santoya leaf tip.
 */
@Composable
fun HydroReportChartIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val resolvedLeafColor = leafAccent ?: if (tint == Color.White) {
        Color(0xFFA7F3B5)
    } else {
        SantoyaLeafGreen
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Three rounded line-art vertical pill bars
        val barWidth = w * 0.14f
        val radius = CornerRadius(w * 0.07f, w * 0.07f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.16f, h * 0.58f),
            size = Size(barWidth, h * 0.28f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.43f, h * 0.44f),
            size = Size(barWidth, h * 0.42f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.70f, h * 0.30f),
            size = Size(barWidth, h * 0.56f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )

        // Smooth curved growth line
        val curvePath = Path().apply {
            moveTo(w * 0.16f, h * 0.42f)
            cubicTo(w * 0.34f, h * 0.40f, w * 0.48f, h * 0.22f, w * 0.72f, h * 0.16f)
        }
        drawPath(
            path = curvePath,
            color = resolvedLeafColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Small organic leaf tip at the peak of the curve
        val peakLeaf = Path().apply {
            moveTo(w * 0.70f, h * 0.18f)
            cubicTo(w * 0.72f, h * 0.08f, w * 0.84f, h * 0.07f, w * 0.90f, h * 0.10f)
            cubicTo(w * 0.88f, h * 0.20f, w * 0.78f, h * 0.22f, w * 0.70f, h * 0.18f)
            close()
        }
        drawPath(
            path = peakLeaf,
            color = resolvedLeafColor,
            style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Custom Receipt / Nota Icon — Minimalist Line Art 2.0dp with rounded wave bottom edge.
 */
@Composable
fun HydroReceiptNoteIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        val receiptPath = Path().apply {
            moveTo(w * 0.22f, h * 0.22f)
            quadraticTo(w * 0.22f, h * 0.14f, w * 0.30f, h * 0.14f)
            lineTo(w * 0.70f, h * 0.14f)
            quadraticTo(w * 0.78f, h * 0.14f, w * 0.78f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.84f)
            quadraticTo(w * 0.69f, h * 0.76f, w * 0.60f, h * 0.84f)
            quadraticTo(w * 0.50f, h * 0.91f, w * 0.40f, h * 0.84f)
            quadraticTo(w * 0.31f, h * 0.76f, w * 0.22f, h * 0.84f)
            close()
        }
        drawPath(
            path = receiptPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.36f),
            end = Offset(w * 0.65f, h * 0.36f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.52f),
            end = Offset(w * 0.56f, h * 0.52f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Custom Delivery Courier Icon — Minimalist Line Art 2.0dp with rounded cargo & fluid wave.
 */
@Composable
fun HydroCourierWaveIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Fluid motion lines on left
        drawLine(
            color = tint,
            start = Offset(w * 0.10f, h * 0.38f),
            end = Offset(w * 0.24f, h * 0.38f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.14f, h * 0.52f),
            end = Offset(w * 0.24f, h * 0.52f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Rounded cargo box
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.26f),
            size = Size(w * 0.34f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Rounded cab silhouette
        val cabPath = Path().apply {
            moveTo(w * 0.64f, h * 0.36f)
            lineTo(w * 0.77f, h * 0.36f)
            quadraticTo(w * 0.88f, h * 0.38f, w * 0.88f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.62f)
            lineTo(w * 0.64f, h * 0.62f)
            close()
        }
        drawPath(
            path = cabPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Wheels
        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = Offset(w * 0.42f, h * 0.75f),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = Offset(w * 0.76f, h * 0.75f),
            style = Stroke(width = stroke)
        )
    }
}
