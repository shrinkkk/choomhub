package com.choomhub.app.feature.home.meetup

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.theme.ChoomHubColors
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** Expressions for the countdown's cat. */
enum class CatExpression { Crying, Sad, Happy, Excited }

private val Fur = Color(0xFF0B0A0F)
private val Tear = Color(0xFF9FB3D9)
private val MouthInside = Color(0xFF5C2233)

/**
 * The face of the shared black cat (the same cat that sits on the windowsill
 * and will become your pet). Ears droop and eyes go heavy when nothing is
 * planned; pupils go wide and round when the meetup is close.
 */
@Composable
fun CatFace(expression: CatExpression, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) { drawCatFace(expression, colors) }
        }
    }
}

private fun DrawScope.drawCatFace(expression: CatExpression, colors: ChoomHubColors) {
    val droopy = expression == CatExpression.Crying || expression == CatExpression.Sad
    val glow = when (expression) {
        CatExpression.Crying -> 0.06f
        CatExpression.Sad -> 0.10f
        CatExpression.Happy -> 0.20f
        CatExpression.Excited -> 0.34f
    }
    val center = Offset(50f, 56f)
    drawCircle(Brush.radialGradient(listOf(colors.lavender.copy(alpha = glow), Color.Transparent), center, 50f), 50f, center)

    // Head and ears as one silhouette with a rim of light.
    val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(50f, 58f), 29f)) }
    val (leftEar, rightEar) = if (droopy) {
        tri(23f, 50f, 9f, 30f, 40f, 37f) to tri(77f, 50f, 91f, 30f, 60f, 37f)
    } else {
        tri(24f, 46f, 27f, 14f, 45f, 32f) to tri(76f, 46f, 73f, 14f, 55f, 32f)
    }
    val silhouette = Path().apply { op(head, leftEar, PathOperation.Union) }
        .let { p -> Path().apply { op(p, rightEar, PathOperation.Union) } }
    drawPath(silhouette, Fur)
    drawPath(silhouette, colors.lavender.copy(alpha = 0.45f), style = Stroke(1.6f, join = StrokeJoin.Round))
    // Inner ears.
    val inner = if (droopy) {
        tri(25f, 46f, 15f, 33f, 36f, 38f) to tri(75f, 46f, 85f, 33f, 64f, 38f)
    } else {
        tri(28f, 42f, 29f, 21f, 41f, 33f) to tri(72f, 42f, 71f, 21f, 59f, 33f)
    }
    drawPath(inner.first, colors.pink.copy(alpha = 0.32f))
    drawPath(inner.second, colors.pink.copy(alpha = 0.32f))

    drawEyes(expression, colors)

    // Nose.
    drawPath(tri(46.8f, 64f, 53.2f, 64f, 50f, 67.5f), colors.pink.copy(alpha = 0.85f))

    // Mouth.
    val ink = colors.lavender.copy(alpha = 0.55f)
    val line = Stroke(width = 1.6f, cap = StrokeCap.Round)
    when (expression) {
        CatExpression.Crying, CatExpression.Sad -> drawArc(ink, 200f, 140f, false, Offset(44f, 71f), Size(12f, 6f), style = line)
        CatExpression.Happy -> {
            drawArc(ink, 0f, 160f, false, Offset(44f, 66f), Size(6f, 5f), style = line)
            drawArc(ink, 20f, 160f, false, Offset(50f, 66f), Size(6f, 5f), style = line)
        }
        CatExpression.Excited -> {
            drawArc(MouthInside, 0f, 180f, true, Offset(45f, 68f), Size(10f, 9f))
            drawArc(ink, 0f, 160f, false, Offset(43.5f, 65.5f), Size(6.5f, 5f), style = line)
            drawArc(ink, 20f, 160f, false, Offset(50f, 65.5f), Size(6.5f, 5f), style = line)
        }
    }

    // Whiskers droop when sad.
    val drop = if (droopy) 5f else 0f
    val whisker = colors.lavender.copy(alpha = 0.35f)
    listOf(-3f, 0f, 3f).forEach { dy ->
        drawLine(whisker, Offset(38f, 66f + dy * 0.5f), Offset(14f, 63f + dy * 1.6f + drop), 0.9f, StrokeCap.Round)
        drawLine(whisker, Offset(62f, 66f + dy * 0.5f), Offset(86f, 63f + dy * 1.6f + drop), 0.9f, StrokeCap.Round)
    }

    if (expression == CatExpression.Excited) {
        listOf(Offset(88f, 16f) to 5f, Offset(12f, 20f) to 3.5f, Offset(90f, 82f) to 3f).forEach { (p, r) ->
            drawLine(colors.candle, Offset(p.x, p.y - r), Offset(p.x, p.y + r), 1.6f, StrokeCap.Round)
            drawLine(colors.candle, Offset(p.x - r, p.y), Offset(p.x + r, p.y), 1.6f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEyes(expression: CatExpression, colors: ChoomHubColors) {
    val gold = colors.candle
    val eyes = listOf(39f, 61f)
    when (expression) {
        CatExpression.Excited -> eyes.forEach { x ->
            // Wide, round, pupils dilated.
            drawCircle(gold, 7f, Offset(x, 54f))
            drawCircle(Fur, 5f, Offset(x, 54.5f))
            drawCircle(Color.White.copy(alpha = 0.85f), 1.6f, Offset(x - 2f, 52.3f))
        }
        CatExpression.Happy -> eyes.forEach { x ->
            drawOval(gold, Offset(x - 6.5f, 49.5f), Size(13f, 9.5f))
            drawOval(Fur, Offset(x - 1.3f, 49.8f), Size(2.6f, 9f))
        }
        CatExpression.Sad, CatExpression.Crying -> {
            eyes.forEachIndexed { i, x ->
                drawOval(gold, Offset(x - 6.5f, 49.5f), Size(13f, 9.5f))
                drawOval(Fur, Offset(x - 1.6f, 53f), Size(3.2f, 5.5f))
                // Lids droop toward the outer corners (inner corner higher): reads as sad, not cross.
                val outer = if (i == 0) x - 7.5f else x + 7.5f
                val inner = if (i == 0) x + 7.5f else x - 7.5f
                drawPath(
                    Path().apply {
                        moveTo(outer, 48f); lineTo(inner, 48f); lineTo(inner, 50.8f); lineTo(outer, 55.2f); close()
                    },
                    Fur,
                )
            }
            val brow = colors.lavender.copy(alpha = 0.5f)
            drawLine(brow, Offset(32f, 48.5f), Offset(43f, 45f), 1.4f, StrokeCap.Round)
            drawLine(brow, Offset(68f, 48.5f), Offset(57f, 45f), 1.4f, StrokeCap.Round)
            if (expression == CatExpression.Crying) {
                val tear = Path().apply {
                    moveTo(36f, 59f)
                    cubicTo(39f, 63.5f, 40f, 67f, 36.5f, 69.5f)
                    cubicTo(33f, 67f, 33.6f, 63.5f, 36f, 59f)
                    close()
                }
                drawPath(tear, Tear)
            }
        }
    }
}

private fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Path().apply {
    moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
}
