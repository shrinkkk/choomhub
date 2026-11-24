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
import androidx.compose.ui.graphics.Path
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

private val Bone = InkPalette.Bone
private val Ink = InkPalette.Ink

private fun DrawScope.drawRavenOnBranch(colors: ChoomHubColors, withMoon: Boolean = false) {
    if (withMoon) {
        drawCircle(Bone.copy(alpha = 0.9f), radius = 20f, center = Offset(64f, 32f))
        scratchEdge(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(64f, 32f), 20f)) }, Ink, strands = 30f, seed = 4, coverage = 0.5f)
    }
    splatter(Offset(14f, 70f), 1.6f, InkPalette.Ash, seed = 9)
    drawPath(MotifShapes.ravenBranch, Ink, style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    scratchEdge(MotifShapes.ravenBranch, Bone.copy(alpha = 0.7f), strands = 18f, seed = 2)
    strokePath(MotifShapes.ravenTwig, Ink, 2f)
    strokePath(MotifShapes.ravenLegs, Ink, 2.2f)
    inkSilhouette(MotifShapes.ravenBody, seed = 13, strands = 36f)
    strokePath(MotifShapes.ravenWingLines, Bone.copy(alpha = 0.22f), 0.6f)
    drawCircle(Bone, radius = 1.9f, center = Offset(29.3f, 25.6f))
    drawCircle(Ink, radius = 0.9f, center = Offset(29.8f, 25.8f))
}

private fun DrawScope.drawMoth(colors: ChoomHubColors) {
    splatter(Offset(86f, 84f), 1.8f, InkPalette.Ash, seed = 12)
    inkSilhouette(MotifShapes.mothHindwings, seed = 31)
    inkSilhouette(MotifShapes.mothForewings, seed = 32)
    strokePath(MotifShapes.mothVeins, Bone.copy(alpha = 0.3f), 0.6f)
    listOf(Offset(79f, 35f), Offset(21f, 35f)).forEach { eye ->
        drawCircle(Bone.copy(alpha = 0.9f), radius = 4.2f, center = eye, style = Stroke(width = 1f))
        drawCircle(Bone.copy(alpha = 0.9f), radius = 1.4f, center = eye)
    }
    val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(45.7f, 34f, 54.3f, 72f)) }
    inkSilhouette(body, seed = 33, strands = 52f)
    inkSilhouette(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(50f, 32f), 4f)) }, seed = 34, strands = 40f)
    strokePath(MotifShapes.mothAntennae, Bone.copy(alpha = 0.75f), 0.7f)
}

private fun DrawScope.drawCandle(colors: ChoomHubColors, flicker: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(colors.candle.copy(alpha = 0.16f * flicker), Color.Transparent), Offset(50f, 28f), 18f),
        radius = 18f,
        center = Offset(50f, 28f),
    )
    splatter(Offset(20f, 78f), 1.7f, InkPalette.Ash, seed = 15)
    inkSilhouette(MotifShapes.candleDish, seed = 41)
    inkSilhouette(MotifShapes.candleCup, seed = 42)
    inkSilhouette(MotifShapes.candleBody, seed = 43, strands = 42f)
    // Pale wax running down the black candle.
    drawPath(MotifShapes.candleDrips, Bone.copy(alpha = 0.85f))
    drip(Offset(54f, 46f), 10f, 1.1f, Bone.copy(alpha = 0.7f))
    drawLine(Bone.copy(alpha = 0.5f), Offset(50f, 41f), Offset(50f, 35.5f), strokeWidth = 0.9f)
    scale(scaleX = 1f, scaleY = flicker, pivot = Offset(50f, 36f)) {
        drawPath(MotifShapes.candleFlame, colors.candle)
        drawPath(MotifShapes.candleFlameCore, Bone)
    }
}

private fun DrawScope.drawSkeletonKey() {
    splatter(Offset(80f, 76f), 1.8f, InkPalette.Ash, seed = 17)
    rotate(-38f, pivot = Offset(50f, 50f)) {
        // Drawn twice, slightly offset, like a pen going over it.
        listOf(Offset.Zero to 1f, Offset(0.7f, -0.5f) to 0.4f).forEach { (o, a) ->
            val c = Bone.copy(alpha = 0.92f * a)
            drawCircle(c, radius = 12f, center = Offset(28f, 50f) + o, style = Stroke(3f))
            listOf(Offset(28f, 44.6f), Offset(28f, 55.4f), Offset(22.6f, 50f), Offset(33.4f, 50f)).forEach {
                drawCircle(c, radius = 3f, center = it + o, style = Stroke(1.2f))
            }
            drawLine(c, Offset(40f, 50f) + o, Offset(88f, 50f) + o, strokeWidth = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(c, Offset(45f, 45.5f) + o, Offset(45f, 54.5f) + o, strokeWidth = 1.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(c, Offset(49f, 46.5f) + o, Offset(49f, 53.5f) + o, strokeWidth = 1.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        strokePath(MotifShapes.keyBit, Bone.copy(alpha = 0.92f), 2.6f)
    }
}

private fun DrawScope.drawMoonAndCrows(colors: ChoomHubColors) {
    drawCrescentAt(Offset(62f, 38f), 20f, Bone.copy(alpha = 0.92f), glow = false)
    splatter(Offset(84f, 76f), 1.6f, InkPalette.Ash, seed = 19)
    drawFlyingCrow(Offset(24f, 62f), 31f, Ink)
    drawFlyingCrow(Offset(40f, 74f), 23f, Ink)
    drawFlyingCrow(Offset(15f, 80f), 17f, Ink)
}

private fun DrawScope.drawCobweb(colors: ChoomHubColors) {
    clipRect(0f, 0f, 100f, 100f) {
        strokePath(MotifShapes.webThreads, Bone.copy(alpha = 0.42f), 0.6f)
    }
    drawLine(Bone.copy(alpha = 0.55f), Offset(64f, 52f), Offset(64f, 66f), strokeWidth = 0.5f)
    strokePath(MotifShapes.spiderLegs, Ink, 1.1f)
    inkSilhouette(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(64f, 68f), 2.6f)) }, seed = 51, strands = 80f)
    inkSilhouette(Path().apply { addOval(androidx.compose.ui.geometry.Rect(60.2f, 69.4f, 67.8f, 78.6f)) }, seed = 52, strands = 60f)
    drawCircle(Bone, 0.6f, Offset(63f, 67.4f))
    drawCircle(Bone, 0.6f, Offset(65f, 67.4f))
}
