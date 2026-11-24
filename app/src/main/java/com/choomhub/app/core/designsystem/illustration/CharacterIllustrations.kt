package com.choomhub.app.core.designsystem.illustration

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
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A tomato, drawn plainly: no face, muted colours, a slightly uneven calyx.
 * Tomatoes are part of the app's identity; they just don't need to be cartoons.
 */
@Composable
fun Tomato(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) { drawTomato(body = colors.tomato, leaf = colors.leaf) }
}

internal fun DrawScope.drawTomato(body: Color, leaf: Color) {
    val w = size.width
    val h = size.height
    val fruit = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.08f, h * 0.26f), Size(w * 0.84f, h * 0.68f))) }
    drawPath(fruit, body)
    // Ink shading and a scratchy edge instead of a glossy gradient.
    drawArc(InkPalette.Ink.copy(alpha = 0.35f), 20f, 150f, true, Offset(w * 0.08f, h * 0.26f), Size(w * 0.84f, h * 0.68f))
    scratchEdge(fruit, InkPalette.Ink, strands = 30f, length = (w * 0.03f)..(w * 0.08f), width = w * 0.008f, seed = 91)
    // A faint vertical seam gives it some form without cartoon shine.
    drawArc(
        color = Color.Black.copy(alpha = 0.18f),
        startAngle = 250f,
        sweepAngle = 40f,
        useCenter = false,
        topLeft = Offset(w * 0.30f, h * 0.30f),
        size = Size(w * 0.40f, h * 0.60f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.02f),
    )
    val cx = w * 0.5f
    val cy = h * 0.29f
    val calyx = Path().apply {
        val radii = listOf(0.22f, 0.07f, 0.19f, 0.07f, 0.24f, 0.07f, 0.18f, 0.07f, 0.21f, 0.07f)
        radii.forEachIndexed { i, r ->
            val angle = -PI / 2 + i * PI / 5
            val x = cx + (w * r * cos(angle)).toFloat()
            val y = cy + (w * r * sin(angle) * 0.5f).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(calyx, leaf.copy(alpha = 0.9f))
    drawLine(leaf, Offset(cx, cy), Offset(cx + w * 0.05f, h * 0.10f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
}

/** The shared black cat, seen from behind, as a plain silhouette with a rim of light. */
@Composable
fun ShadowCat(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) { onGrid { drawShadowCat(colors.lavender) } }
}

/** Cat silhouette on the 100 grid; feet along y = 94. */
internal fun DrawScope.drawShadowCat(rim: Color) {
    strokePath(MotifShapes.catTail, InkPalette.Ink, 6.5f)
    scratchEdge(MotifShapes.catTail, InkPalette.Bone.copy(alpha = 0.6f), strands = 26f, seed = 71)
    inkSilhouette(MotifShapes.catBack, seed = 72, strands = 38f)
}
