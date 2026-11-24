package com.choomhub.app.core.designsystem.illustration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser

/** Near-black used for silhouettes (raven, cat, moth). */
internal val Silhouette = Color(0xFF0B0A0F)

/** Parses SVG path data into a Compose [Path]. */
internal fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** Parsed motif paths, created once. */
internal object MotifShapes {
    val ravenBranch by lazy { svgPath(MotifPaths.RAVEN_BRANCH) }
    val ravenTwig by lazy { svgPath(MotifPaths.RAVEN_TWIG) }
    val ravenLegs by lazy { svgPath(MotifPaths.RAVEN_LEGS) }
    val ravenBody by lazy { svgPath(MotifPaths.RAVEN_BODY) }
    val ravenWingLines by lazy { svgPath(MotifPaths.RAVEN_WING_LINES) }
    val mothForewings by lazy { svgPath(MotifPaths.MOTH_FOREWINGS) }
    val mothHindwings by lazy { svgPath(MotifPaths.MOTH_HINDWINGS) }
    val mothVeins by lazy { svgPath(MotifPaths.MOTH_VEINS) }
    val mothAntennae by lazy { svgPath(MotifPaths.MOTH_ANTENNAE) }
    val candleDish by lazy { svgPath(MotifPaths.CANDLE_DISH) }
    val candleCup by lazy { svgPath(MotifPaths.CANDLE_CUP) }
    val candleBody by lazy { svgPath(MotifPaths.CANDLE_BODY) }
    val candleDrips by lazy { svgPath(MotifPaths.CANDLE_DRIPS) }
    val candleFlame by lazy { svgPath(MotifPaths.CANDLE_FLAME) }
    val candleFlameCore by lazy { svgPath(MotifPaths.CANDLE_FLAME_CORE) }
    val keyBit by lazy { svgPath(MotifPaths.KEY_BIT) }
    val flyingCrow by lazy { svgPath(MotifPaths.FLYING_CROW) }
    val webThreads by lazy { svgPath(MotifPaths.WEB_THREADS) }
    val spiderLegs by lazy { svgPath(MotifPaths.SPIDER_LEGS) }
    val catBack by lazy { svgPath(MotifPaths.CAT_BACK) }
    val catTail by lazy { svgPath(MotifPaths.CAT_TAIL) }
    val moonCrescent by lazy { svgPath(MotifPaths.MOON_CRESCENT) }
}

/**
 * Draws [block] in a centred 100 x 100 grid scaled to fit the canvas, so the
 * motifs authored on that grid can be drawn at any size.
 */
internal inline fun DrawScope.onGrid(crossinline block: DrawScope.() -> Unit) {
    val s = size.minDimension / 100f
    translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
        scale(s, s, pivot = Offset.Zero) { block() }
    }
}

/** Draws [path] (authored on the 100 grid) at [topLeft] with a uniform [scale], in the current coordinates. */
internal inline fun DrawScope.placed(topLeft: Offset, scale: Float, crossinline block: DrawScope.() -> Unit) {
    translate(topLeft.x, topLeft.y) { scale(scale, scale, pivot = Offset.Zero) { block() } }
}

internal fun DrawScope.fillWithRim(path: Path, fill: Color, rim: Color, rimAlpha: Float, rimWidth: Float) {
    drawPath(path, fill)
    drawPath(path, rim.copy(alpha = rimAlpha), style = Stroke(width = rimWidth, join = StrokeJoin.Round))
}

internal fun DrawScope.strokePath(path: Path, color: Color, width: Float) {
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Crescent moon with a halo, centred on [center] with outer radius [radius], in current coordinates. */
internal fun DrawScope.drawCrescentAt(center: Offset, radius: Float, color: Color, glow: Boolean = true) {
    if (glow) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), center, radius * 1.5f),
            radius = radius * 1.5f,
            center = center,
        )
    }
    // The crescent path's disc is centred at (62, 38) with radius 20 on the grid.
    val k = radius / 20f
    placed(Offset(center.x - 62f * k, center.y - 38f * k), k) { drawPath(MotifShapes.moonCrescent, color) }
}

/** A small raven/crow body (no branch), standing with its feet at [feet], [height] tall. */
internal fun DrawScope.drawPerchedCrow(feet: Offset, height: Float, color: Color = Silhouette, rim: Color? = null) {
    // Body spans roughly y 17..80 on the grid; feet sit near (54, 80).
    val k = height / 63f
    placed(Offset(feet.x - 54f * k, feet.y - 80f * k), k) {
        drawPath(MotifShapes.ravenBody, color)
        if (rim != null) drawPath(MotifShapes.ravenBody, rim, style = Stroke(width = 2.5f))
    }
}

/** A crow in flight, centred on [center]; [span] is the wingspan. */
internal fun DrawScope.drawFlyingCrow(center: Offset, span: Float, color: Color = Silhouette) {
    val k = span / 24f
    translate(center.x, center.y) { scale(k, k, pivot = Offset.Zero) { drawPath(MotifShapes.flyingCrow, color) } }
}
