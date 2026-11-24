package com.choomhub.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** Fills the current canvas with a soft, rounded heart. */
internal fun DrawScope.drawHeart(color: Color) {
    val w = size.width
    val h = size.height
    val heart = Path().apply {
        moveTo(w * 0.5f, h * 0.92f)
        cubicTo(w * 0.10f, h * 0.64f, w * 0.0f, h * 0.36f, w * 0.17f, h * 0.17f)
        cubicTo(w * 0.31f, h * 0.03f, w * 0.47f, h * 0.10f, w * 0.5f, h * 0.27f)
        cubicTo(w * 0.53f, h * 0.10f, w * 0.69f, h * 0.03f, w * 0.83f, h * 0.17f)
        cubicTo(w * 1.0f, h * 0.36f, w * 0.90f, h * 0.64f, w * 0.5f, h * 0.92f)
        close()
    }
    drawPath(heart, color)
}

@Composable
fun HeartGlyph(modifier: Modifier = Modifier, color: Color = ChoomHubTheme.colors.pink) {
    Canvas(modifier) { drawHeart(color) }
}

/** A heart held in two little arms. */
@Composable
fun HugGlyph(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawInRect(Rect(w * 0.22f, h * 0.12f, w * 0.78f, h * 0.66f)) { drawHeart(colors.pink) }
        val arm = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawArc(colors.lavender, 110f, 110f, false, Offset(w * 0.04f, h * 0.18f), Size(w * 0.56f, h * 0.68f), style = arm)
        drawArc(colors.lavender, -40f, 110f, false, Offset(w * 0.40f, h * 0.18f), Size(w * 0.56f, h * 0.68f), style = arm)
    }
}

/** Two hearts drifting together. */
@Composable
fun KissGlyph(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawInRect(Rect(w * 0.06f, h * 0.28f, w * 0.70f, h * 0.92f)) { drawHeart(colors.pink) }
        drawInRect(Rect(w * 0.54f, h * 0.08f, w * 0.94f, h * 0.48f)) { drawHeart(colors.tomato) }
    }
}

/** A cat-paw for head pats. */
@Composable
fun PawGlyph(modifier: Modifier = Modifier, color: Color = ChoomHubTheme.colors.lavender) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawOval(color, topLeft = Offset(w * 0.27f, h * 0.48f), size = Size(w * 0.46f, h * 0.40f))
        val toe = w * 0.095f
        drawCircle(color, toe, Offset(w * 0.20f, h * 0.42f))
        drawCircle(color, toe, Offset(w * 0.38f, h * 0.24f))
        drawCircle(color, toe, Offset(w * 0.62f, h * 0.24f))
        drawCircle(color, toe, Offset(w * 0.80f, h * 0.42f))
    }
}
