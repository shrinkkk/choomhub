// Moon and sun, in the ink-and-bone hand of the rest of the art.
package com.choomhub.app.core.designsystem.illustration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.choomhub.app.core.designsystem.theme.SkyColors

internal fun DrawScope.drawMoon(c: Offset, r: Float, sky: SkyColors) {
    val bone = InkPalette.Bone
    drawCircle(Brush.radialGradient(listOf(bone.copy(alpha = 0.10f), Color.Transparent), c, r * 2.8f), r * 2.8f, c)
    drawCircle(
        Brush.radialGradient(listOf(bone.copy(alpha = 0.34f), bone.copy(alpha = 0.20f)), Offset(c.x - r * 0.3f, c.y - r * 0.35f), r * 1.3f),
        r,
        c,
    )
    // Soft craters.
    listOf(Triple(-0.30f, -0.10f, 0.22f), Triple(0.25f, 0.28f, 0.16f), Triple(0.18f, -0.38f, 0.10f), Triple(-0.05f, 0.45f, 0.08f))
        .forEach { (dx, dy, cr) -> drawCircle(InkPalette.Ash.copy(alpha = 0.30f), r * cr, Offset(c.x + dx * r, c.y + dy * r)) }
    scratchEdge(circlePath(c, r), bone.copy(alpha = 0.30f), strands = 30f, length = 1.5f..5f, width = 0.8f, seed = 81)
    cloudBands(c, r, sky, seed = 0)
}

internal fun DrawScope.drawSun(c: Offset, r: Float, tone: Color, sky: SkyColors, haze: Float) {
    // Overcast: a wide, weak glow and a flat pale disc, no rays.
    drawCircle(Brush.radialGradient(listOf(tone.copy(alpha = haze * 0.55f), Color.Transparent), c, r * 3.4f), r * 3.4f, c)
    // Faint, uneven hand-drawn rays: what makes it read as a sun and not a second moon.
    val random = kotlin.random.Random(84)
    repeat(40) { i ->
        val angle = (i / 40f) * 2f * Math.PI.toFloat() + random.nextFloat() * 0.12f
        val inner = r * (1.25f + random.nextFloat() * 0.15f)
        val outer = r * (1.6f + random.nextFloat() * 0.9f)
        val dir = Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
        drawLine(
            tone.copy(alpha = haze * (0.8f + random.nextFloat() * 0.9f)),
            c + dir * inner,
            c + dir * outer,
            strokeWidth = 0.35f + random.nextFloat() * 0.55f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
    drawCircle(
        Brush.radialGradient(listOf(tone.copy(alpha = haze * 2.2f), tone.copy(alpha = haze * 1.3f)), c, r),
        r,
        c,
    )
    scratchEdge(circlePath(c, r), tone.copy(alpha = haze * 2f), strands = 26f, length = 1.5f..5f, width = 0.8f, seed = 83)
    cloudBands(c, r, sky, seed = 1)
}

/** Soft wisps of cloud in the sky's darkest tone: stretched, feathered ovals with no hard edges. */
private fun DrawScope.cloudBands(c: Offset, r: Float, sky: SkyColors, seed: Int) {
    val wisps = if (seed == 0) {
        listOf(Triple(-0.9f, 0.62f, 2.6f), Triple(0.7f, 0.86f, 2.0f), Triple(-2.2f, 0.40f, 1.6f))
    } else {
        listOf(Triple(-1.1f, 0.42f, 2.8f), Triple(0.6f, 0.78f, 2.2f), Triple(0.9f, -0.55f, 1.5f), Triple(-2.4f, 0.70f, 1.4f))
    }
    val h = r * 0.30f
    wisps.forEach { (dx, dy, halfWidth) ->
        val center = Offset(c.x + dx * r, c.y + dy * r)
        val stretch = halfWidth * r / h
        withTransform({ scale(stretch, 1f, pivot = center) }) {
            drawCircle(
                Brush.radialGradient(
                    0f to sky.top.copy(alpha = 0.95f),
                    0.55f to sky.top.copy(alpha = 0.8f),
                    1f to Color.Transparent,
                    center = center,
                    radius = h,
                ),
                radius = h,
                center = center,
            )
        }
    }
}

private fun circlePath(c: Offset, r: Float) = Path().apply {
    addOval(androidx.compose.ui.geometry.Rect(c, r))
}
