package com.choomhub.app.feature.home.meetup

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** Expressions for the countdown's moon. */
enum class MoonExpression { Crying, Sad, Happy, Excited }

private val Ink = Color(0xFF2A2333)
private val Tear = Color(0xFF9FB3D9)

/**
 * A full moon with a face, drawn in ink. Dim and teary when nothing is
 * planned, brighter and livelier the closer the meetup gets.
 */
@Composable
fun MoonFace(expression: MoonExpression, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) { drawMoonFace(expression, colors.lavender, colors.candle, colors.pink) }
        }
    }
}

private fun DrawScope.drawMoonFace(expression: MoonExpression, moon: Color, gold: Color, blush: Color) {
    val (glow, brightness) = when (expression) {
        MoonExpression.Crying -> 0.08f to 0.62f
        MoonExpression.Sad -> 0.12f to 0.75f
        MoonExpression.Happy -> 0.22f to 0.9f
        MoonExpression.Excited -> 0.38f to 1f
    }
    val c = Offset(50f, 50f)
    drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = glow), Color.Transparent), c, 50f), 50f, c)
    drawCircle(moon.copy(alpha = brightness), 34f, c)
    // A few craters for texture.
    listOf(Triple(68f, 30f, 4.5f), Triple(29f, 64f, 3.5f), Triple(66f, 71f, 2.8f), Triple(33f, 31f, 2.4f)).forEach { (x, y, r) ->
        drawCircle(Ink.copy(alpha = 0.10f), r, Offset(x, y))
    }
    val line = Stroke(width = 2.4f, cap = StrokeCap.Round)
    when (expression) {
        MoonExpression.Crying, MoonExpression.Sad -> {
            // Brows lifted at the inner ends, eyes, downturned mouth.
            drawLine(Ink, Offset(34.5f, 40.5f), Offset(43.5f, 36.5f), 2.2f, StrokeCap.Round)
            drawLine(Ink, Offset(65.5f, 40.5f), Offset(56.5f, 36.5f), 2.2f, StrokeCap.Round)
            drawCircle(Ink, 2.5f, Offset(40f, 47f))
            drawCircle(Ink, 2.5f, Offset(60f, 47f))
            drawArc(Ink, 200f, 140f, false, Offset(41f, 60f), Size(18f, 11f), style = line)
            if (expression == MoonExpression.Crying) {
                val tear = Path().apply {
                    moveTo(39f, 50.5f)
                    cubicTo(42.5f, 55.5f, 44f, 59.5f, 39.5f, 63f)
                    cubicTo(35f, 59.5f, 35.8f, 55.5f, 39f, 50.5f)
                    close()
                }
                drawPath(tear, Tear)
            }
        }
        MoonExpression.Happy -> {
            drawCircle(Ink, 2.5f, Offset(40f, 46f))
            drawCircle(Ink, 2.5f, Offset(60f, 46f))
            drawArc(Ink, 20f, 140f, false, Offset(39f, 51f), Size(22f, 13f), style = line)
            drawOval(blush.copy(alpha = 0.35f), Offset(29f, 53f), Size(8f, 4f))
            drawOval(blush.copy(alpha = 0.35f), Offset(63f, 53f), Size(8f, 4f))
        }
        MoonExpression.Excited -> {
            // Eyes squeezed into happy arcs, open mouth, a little starlight.
            drawArc(Ink, 200f, 140f, false, Offset(35f, 43f), Size(10f, 8f), style = line)
            drawArc(Ink, 200f, 140f, false, Offset(55f, 43f), Size(10f, 8f), style = line)
            drawArc(Ink, 0f, 180f, true, Offset(40f, 53f), Size(20f, 15f))
            drawOval(blush.copy(alpha = 0.45f), Offset(28f, 52f), Size(9f, 4.5f))
            drawOval(blush.copy(alpha = 0.45f), Offset(63f, 52f), Size(9f, 4.5f))
            listOf(Offset(86f, 18f) to 5f, Offset(14f, 24f) to 3.5f, Offset(88f, 78f) to 3f).forEach { (p, r) ->
                drawLine(gold, Offset(p.x, p.y - r), Offset(p.x, p.y + r), 1.6f, StrokeCap.Round)
                drawLine(gold, Offset(p.x - r, p.y), Offset(p.x + r, p.y), 1.6f, StrokeCap.Round)
            }
        }
    }
}
