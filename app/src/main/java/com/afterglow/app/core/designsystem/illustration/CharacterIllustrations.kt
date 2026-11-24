package com.afterglow.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TomatoHighlight = Color(0xFFF4A0A0)
private val TomatoShadow = Color(0xFFB04A52)
private val TomatoFace = Color(0xFF3A1E24)
private val CatFur = Color(0xFF0C0A12)

/** A round, slightly squashed tomato, optionally with a sleepy little face. */
@Composable
fun Tomato(
    modifier: Modifier = Modifier,
    withFace: Boolean = true,
) {
    val colors = AfterglowTheme.colors
    Canvas(modifier) {
        drawTomato(
            body = colors.tomato,
            leaf = colors.leaf,
            blush = colors.pink,
            withFace = withFace,
        )
    }
}

internal fun DrawScope.drawTomato(body: Color, leaf: Color, blush: Color, withFace: Boolean) {
    val w = size.width
    val h = size.height
    val bodyTopLeft = Offset(w * 0.08f, h * 0.24f)
    val bodySize = Size(w * 0.84f, h * 0.70f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(TomatoHighlight, body, TomatoShadow),
            center = Offset(w * 0.38f, h * 0.45f),
            radius = w * 0.66f,
        ),
        topLeft = bodyTopLeft,
        size = bodySize,
    )
    // Glossy highlight.
    drawOval(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(w * 0.22f, h * 0.36f),
        size = Size(w * 0.17f, h * 0.09f),
    )
    // Five-pointed calyx.
    val cx = w * 0.5f
    val cy = h * 0.28f
    val outer = w * 0.22f
    val inner = w * 0.07f
    val calyx = Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle) * 0.55f).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(calyx, leaf)
    drawLine(
        color = leaf,
        start = Offset(cx, cy),
        end = Offset(cx + w * 0.04f, h * 0.10f),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )
    if (withFace) {
        val eyeY = h * 0.60f
        drawCircle(TomatoFace, radius = w * 0.035f, center = Offset(w * 0.38f, eyeY))
        drawCircle(TomatoFace, radius = w * 0.035f, center = Offset(w * 0.62f, eyeY))
        drawArc(
            color = TomatoFace,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.45f, h * 0.60f),
            size = Size(w * 0.10f, h * 0.08f),
            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round),
        )
        drawOval(blush.copy(alpha = 0.55f), topLeft = Offset(w * 0.22f, h * 0.65f), size = Size(w * 0.11f, h * 0.05f))
        drawOval(blush.copy(alpha = 0.55f), topLeft = Offset(w * 0.67f, h * 0.65f), size = Size(w * 0.11f, h * 0.05f))
    }
}

/**
 * A small black cat sitting upright. With [eyesOpen] false it looks content
 * and sleepy. A faint rim light keeps the silhouette readable on dark UI.
 */
@Composable
fun BlackCat(
    modifier: Modifier = Modifier,
    eyesOpen: Boolean = false,
) {
    val colors = AfterglowTheme.colors
    Canvas(modifier) {
        drawBlackCat(rim = colors.lavender, eyes = colors.candle, innerEar = colors.pink, eyesOpen = eyesOpen)
    }
}

internal fun DrawScope.drawBlackCat(rim: Color, eyes: Color, innerEar: Color, eyesOpen: Boolean) {
    val w = size.width
    val h = size.height

    val head = Path().apply { addOval(Rect(Offset(w * 0.5f, h * 0.36f), w * 0.21f)) }
    val leftEar = Path().apply {
        moveTo(w * 0.30f, h * 0.32f)
        lineTo(w * 0.32f, h * 0.08f)
        lineTo(w * 0.48f, h * 0.20f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.70f, h * 0.32f)
        lineTo(w * 0.68f, h * 0.08f)
        lineTo(w * 0.52f, h * 0.20f)
        close()
    }
    val body = Path().apply {
        moveTo(w * 0.36f, h * 0.48f)
        cubicTo(w * 0.18f, h * 0.62f, w * 0.18f, h * 0.98f, w * 0.30f, h * 0.98f)
        lineTo(w * 0.70f, h * 0.98f)
        cubicTo(w * 0.82f, h * 0.98f, w * 0.82f, h * 0.62f, w * 0.64f, h * 0.48f)
        close()
    }
    val silhouette = Path().apply {
        op(head, leftEar, PathOperation.Union)
    }.let { headAndEar ->
        Path().apply { op(headAndEar, rightEar, PathOperation.Union) }
    }.let { headAndEars ->
        Path().apply { op(headAndEars, body, PathOperation.Union) }
    }

    // Tail curls around the front paws.
    val tail = Path().apply {
        moveTo(w * 0.70f, h * 0.94f)
        cubicTo(w * 0.94f, h * 0.94f, w * 0.98f, h * 0.70f, w * 0.86f, h * 0.62f)
    }
    drawPath(tail, rim.copy(alpha = 0.35f), style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
    drawPath(tail, CatFur, style = Stroke(width = w * 0.075f, cap = StrokeCap.Round))

    drawPath(silhouette, CatFur)
    drawPath(silhouette, rim.copy(alpha = 0.35f), style = Stroke(width = w * 0.012f))

    // Inner ears.
    val innerLeft = Path().apply {
        moveTo(w * 0.34f, h * 0.27f); lineTo(w * 0.345f, h * 0.15f); lineTo(w * 0.43f, h * 0.21f); close()
    }
    val innerRight = Path().apply {
        moveTo(w * 0.66f, h * 0.27f); lineTo(w * 0.655f, h * 0.15f); lineTo(w * 0.57f, h * 0.21f); close()
    }
    drawPath(innerLeft, innerEar.copy(alpha = 0.35f))
    drawPath(innerRight, innerEar.copy(alpha = 0.35f))

    // Eyes.
    val eyeY = h * 0.37f
    if (eyesOpen) {
        listOf(0.41f, 0.59f).forEach { ex ->
            drawOval(eyes, topLeft = Offset(w * (ex - 0.045f), eyeY - h * 0.035f), size = Size(w * 0.09f, h * 0.07f))
            drawOval(CatFur, topLeft = Offset(w * (ex - 0.012f), eyeY - h * 0.03f), size = Size(w * 0.024f, h * 0.06f))
        }
    } else {
        listOf(0.41f, 0.59f).forEach { ex ->
            drawArc(
                color = eyes,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(w * (ex - 0.05f), eyeY - h * 0.03f),
                size = Size(w * 0.10f, h * 0.05f),
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round),
            )
        }
    }
    // Nose.
    drawCircle(innerEar.copy(alpha = 0.8f), radius = w * 0.014f, center = Offset(w * 0.5f, h * 0.425f))
    // Whiskers.
    val whisker = rim.copy(alpha = 0.3f)
    val stroke = w * 0.006f
    drawLine(whisker, Offset(w * 0.40f, h * 0.44f), Offset(w * 0.20f, h * 0.42f), stroke)
    drawLine(whisker, Offset(w * 0.40f, h * 0.455f), Offset(w * 0.21f, h * 0.47f), stroke)
    drawLine(whisker, Offset(w * 0.60f, h * 0.44f), Offset(w * 0.80f, h * 0.42f), stroke)
    drawLine(whisker, Offset(w * 0.60f, h * 0.455f), Offset(w * 0.79f, h * 0.47f), stroke)
}
