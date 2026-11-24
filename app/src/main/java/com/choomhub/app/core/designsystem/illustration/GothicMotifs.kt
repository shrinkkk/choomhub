package com.choomhub.app.core.designsystem.illustration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.choomhub.app.core.designsystem.theme.ChoomHubColors
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import java.time.LocalDate

/** The rotating set of header motifs. */
enum class GothicMotif {
    Raven,
    Moth,
    Candle,
    SkeletonKey,
    MoonAndCrows,
    Cobweb,
    ;

    companion object {
        /** How many days each motif stays before the next one takes over. */
        const val DAYS_PER_MOTIF = 3

        /** Deterministic: both phones show the same motif on the same day. */
        fun forDate(date: LocalDate): GothicMotif =
            entries[Math.floorMod(Math.floorDiv(date.toEpochDay(), DAYS_PER_MOTIF.toLong()), entries.size.toLong()).toInt()]
    }
}

/** Draws a [GothicMotif] filling the given bounds. The candle flame flickers when ambient motion is on. */
@Composable
fun GothicMotifArt(motif: GothicMotif, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val flicker = if (motif == GothicMotif.Candle) flickerState() else remember { mutableFloatStateOf(1f) }
    Canvas(modifier) {
        onGrid {
            when (motif) {
                GothicMotif.Raven -> drawRavenOnBranch(colors, withMoon = true)
                GothicMotif.Moth -> drawMoth(colors)
                GothicMotif.Candle -> drawCandle(colors, flicker.value)
                GothicMotif.SkeletonKey -> drawSkeletonKey()
                GothicMotif.MoonAndCrows -> drawMoonAndCrows(colors)
                GothicMotif.Cobweb -> drawCobweb(colors)
            }
        }
    }
}

/** The perched raven on its own, used as a small signature mark. */
@Composable
fun RavenMark(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) { onGrid { drawRavenOnBranch(colors) } }
}

@Composable
private fun flickerState(): State<Float> {
    if (!LocalAmbientMotionEnabled.current) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "flame").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flameScale",
    )
}

private val Branch = Color(0xFF3A3443)
private val Tarnish = Color(0xFFA097AD)

private fun DrawScope.drawRavenOnBranch(colors: ChoomHubColors, withMoon: Boolean = false) {
    if (withMoon) {
        // A pale moon behind the bird so the silhouette reads on dark backgrounds.
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.lavender.copy(alpha = 0.22f), Color.Transparent), Offset(64f, 32f), 32f),
            radius = 32f,
            center = Offset(64f, 32f),
        )
        drawCircle(colors.lavender.copy(alpha = 0.88f), radius = 20f, center = Offset(64f, 32f))
    }
    strokePath(MotifShapes.ravenBranch, Branch, 3.5f)
    strokePath(MotifShapes.ravenTwig, Branch, 2f)
    strokePath(MotifShapes.ravenLegs, Silhouette, 2.2f)
    fillWithRim(MotifShapes.ravenBody, Silhouette, colors.lavender, 0.38f, 0.9f)
    strokePath(MotifShapes.ravenWingLines, colors.lavender.copy(alpha = 0.16f), 0.9f)
    drawCircle(colors.candle, radius = 1.35f, center = Offset(29.3f, 25.6f))
}

private fun DrawScope.drawMoth(colors: ChoomHubColors) {
    fillWithRim(MotifShapes.mothForewings, Color(0xFF15131A), colors.lavender, 0.4f, 0.9f)
    fillWithRim(MotifShapes.mothHindwings, Color(0xFF121016), colors.lavender, 0.35f, 0.9f)
    strokePath(MotifShapes.mothVeins, colors.lavender.copy(alpha = 0.15f), 0.8f)
    listOf(Offset(79f, 35f), Offset(21f, 35f)).forEach { eye ->
        drawCircle(colors.candle.copy(alpha = 0.7f), radius = 4.2f, center = eye, style = Stroke(width = 1f))
        drawCircle(colors.candle.copy(alpha = 0.7f), radius = 1.4f, center = eye)
    }
    // Body and head.
    drawOval(Silhouette, topLeft = Offset(45.7f, 34f), size = androidx.compose.ui.geometry.Size(8.6f, 38f))
    drawOval(colors.lavender.copy(alpha = 0.4f), topLeft = Offset(45.7f, 34f), size = androidx.compose.ui.geometry.Size(8.6f, 38f), style = Stroke(0.9f))
    drawCircle(Silhouette, radius = 4f, center = Offset(50f, 32f))
    drawCircle(colors.lavender.copy(alpha = 0.4f), radius = 4f, center = Offset(50f, 32f), style = Stroke(0.9f))
    strokePath(MotifShapes.mothAntennae, colors.lavender.copy(alpha = 0.55f), 0.9f)
}

private fun DrawScope.drawCandle(colors: ChoomHubColors, flicker: Float) {
    val holder = Color(0xFF26222D)
    val wax = Color(0xFF141218)
    drawCircle(
        brush = Brush.radialGradient(listOf(colors.candle.copy(alpha = 0.42f * flicker), Color.Transparent), Offset(50f, 28f), 22f),
        radius = 22f,
        center = Offset(50f, 28f),
    )
    fillWithRim(MotifShapes.candleDish, holder, colors.lavender, 0.3f, 0.8f)
    fillWithRim(MotifShapes.candleCup, holder, colors.lavender, 0.3f, 0.8f)
    fillWithRim(MotifShapes.candleBody, wax, colors.lavender, 0.4f, 0.9f)
    fillWithRim(MotifShapes.candleDrips, wax, colors.lavender, 0.3f, 0.7f)
    drawLine(Branch, Offset(50f, 41f), Offset(50f, 35.5f), strokeWidth = 1.2f)
    // Flame scales from its base so it flickers upward.
    scale(scaleX = 1f, scaleY = flicker, pivot = Offset(50f, 36f)) {
        drawPath(MotifShapes.candleFlame, colors.candle)
        drawPath(MotifShapes.candleFlameCore, Color(0xFFF4E3C4))
    }
}

private fun DrawScope.drawSkeletonKey() {
    rotate(-38f, pivot = Offset(50f, 50f)) {
        drawCircle(Tarnish, radius = 12f, center = Offset(28f, 50f), style = Stroke(3.4f))
        listOf(Offset(28f, 44.6f), Offset(28f, 55.4f), Offset(22.6f, 50f), Offset(33.4f, 50f)).forEach {
            drawCircle(Tarnish, radius = 3f, center = it, style = Stroke(1.4f))
        }
        drawLine(Tarnish, Offset(40f, 50f), Offset(88f, 50f), strokeWidth = 3.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Tarnish, Offset(45f, 45.5f), Offset(45f, 54.5f), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Tarnish, Offset(49f, 46.5f), Offset(49f, 53.5f), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        strokePath(MotifShapes.keyBit, Tarnish, 2.8f)
    }
}

private fun DrawScope.drawMoonAndCrows(colors: ChoomHubColors) {
    drawCrescentAt(Offset(62f, 38f), 20f, colors.lavender)
    drawFlyingCrow(Offset(24f, 62f), 31f)
    drawFlyingCrow(Offset(40f, 74f), 23f)
    drawFlyingCrow(Offset(15f, 80f), 17f)
}

private fun DrawScope.drawCobweb(colors: ChoomHubColors) {
    clipRect(0f, 0f, 100f, 100f) {
        strokePath(MotifShapes.webThreads, colors.lavender.copy(alpha = 0.42f), 0.8f)
    }
    drawLine(colors.lavender.copy(alpha = 0.5f), Offset(64f, 52f), Offset(64f, 66f), strokeWidth = 0.6f)
    strokePath(MotifShapes.spiderLegs, Silhouette, 1.1f)
    drawCircle(Silhouette, radius = 2.6f, center = Offset(64f, 68f))
    drawCircle(colors.lavender.copy(alpha = 0.4f), radius = 2.6f, center = Offset(64f, 68f), style = Stroke(0.5f))
    drawOval(Silhouette, topLeft = Offset(60.2f, 69.4f), size = androidx.compose.ui.geometry.Size(7.6f, 9.2f))
    drawOval(colors.lavender.copy(alpha = 0.4f), topLeft = Offset(60.2f, 69.4f), size = androidx.compose.ui.geometry.Size(7.6f, 9.2f), style = Stroke(0.5f))
}
