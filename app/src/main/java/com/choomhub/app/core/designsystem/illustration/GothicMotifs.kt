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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
        // Pale moon with a few soft craters.
        val c = Offset(64f, 32f)
        drawCircle(Bone.copy(alpha = 0.88f), 20f, c)
        listOf(Offset(70f, 24f) to 2.6f, Offset(58f, 40f) to 1.8f, Offset(74f, 38f) to 1.3f, Offset(55f, 25f) to 1f).forEach { (p, r) ->
            drawCircle(Ink.copy(alpha = 0.12f), r, p)
        }
    }
    // Gnarled branch, moonlit along its top.
    val branch = svgPath("M0 85 C14 83 26 86 40 84 C56 82 70 87 86 83 C92 82 96 83 100 82 L100 88 C92 89 80 91 66 89 C50 87 34 92 18 90 C10 89 4 90 0 91 Z")
    drawPath(branch, Ink)
    strokePath(svgPath("M0 85 C14 83 26 86 40 84 C56 82 70 87 86 83 C92 82 96 83 100 82"), Bone.copy(alpha = 0.22f), 0.4f)
    strokePath(svgPath("M74 86 C79 89 83 92 88 97"), Ink, 1.8f)
    strokePath(MotifShapes.ravenLegs, Ink, 2.2f)
    // Talons hooking the branch.
    strokePath(svgPath("M50.5 84.5 L47.8 85.6 M50.5 84.5 L52.8 86 M57.5 84.5 L55 85.8 M57.5 84.5 L60 85.8"), Ink, 1f)

    drawPath(MotifShapes.ravenBody, Ink)
    // Moonlight on the back and head only; shaggy hackles at the throat.
    furEdge(MotifShapes.ravenBody, seed = 13, density = 26f, length = 0.8f..2.4f, light = Offset(0.6f, -0.8f), ambient = 0f, width = 0.28f)
    furEdge(svgPath("M24.8 37.2 L26 40.2 L22.4 37 L21.2 35.4 C26 41 29 44 30.6 45.5"), seed = 14, color = Bone.copy(alpha = 0.45f), density = 90f, length = 0.8f..2f, light = Offset(-1f, 0.4f), ambient = 0.2f, width = 0.25f)
    // Layered wing feathers, kept inside the body.
    clipPath(MotifShapes.ravenBody) { listOf(
        "M40 38 C50 43 60 50 70 60",
        "M44 44 C53 49 62 56 72 66",
        "M48 51 C56 56 64 62 74 71",
        "M56 61 C63 65 70 69 80 74",
        "M66 68 C74 71 82 74 92 80",
    ).forEachIndexed { i, d -> strokePath(svgPath(d), Bone.copy(alpha = 0.14f + i * 0.02f), 0.45f) }
    // Primary feathers fanning at the wing tip.
    listOf("M70 60 L84 66", "M72 64 L87 71", "M74 68 L89 76").forEach { strokePath(svgPath(it), Bone.copy(alpha = 0.2f), 0.4f) } }
    // Beak highlight and eye.
    strokePath(svgPath("M10.5 33.4 C14.5 30.6 18.5 28.9 23 28"), Bone.copy(alpha = 0.45f), 0.5f)
    drawCircle(Bone.copy(alpha = 0.9f), radius = 1.5f, center = Offset(29.3f, 25.6f))
    drawCircle(Ink, radius = 0.85f, center = Offset(29.6f, 25.8f))
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
        strokePath(MotifShapes.webThreads, Bone.copy(alpha = 0.38f), 0.5f)
    }
    // Spider resting where the middle ring crosses the centre spoke, head toward the hub.
    val perch = Offset(4f + 43f * 0.7071f, 4f + 43f * 0.7071f + 2f)
    rotate(-45f, pivot = perch) { drawSpider(perch, 1.3f) }
}

/** A hanging spider: eight jointed, tapering legs around a small head and a rounded abdomen. */
private fun DrawScope.drawSpider(at: Offset, k: Float) {
    // Knee and foot offsets per leg (right side; mirrored for the left). Knees bend up and out;
    // front legs reach up toward the thread, back legs hang down.
    val legs = listOf(
        Offset(4.5f, -6f) to Offset(7.5f, -12f),
        Offset(6.5f, -4f) to Offset(12.5f, -5f),
        Offset(6.5f, -2f) to Offset(12.5f, 4f),
        Offset(5f, 0.5f) to Offset(8.5f, 9.5f),
    )
    listOf(-1f, 1f).forEach { side ->
        legs.forEachIndexed { i, (kneeOff, footOff) ->
            val hip = Offset(at.x + side * 1.2f * k, at.y - 1.6f * k + i * 0.7f * k)
            val knee = Offset(hip.x + side * kneeOff.x * k, hip.y + kneeOff.y * k)
            val foot = Offset(hip.x + side * footOff.x * k, hip.y + footOff.y * k)
            drawLine(Ink, hip, knee, 1.15f * k, StrokeCap.Round)
            drawLine(Ink, knee, foot, 0.7f * k, StrokeCap.Round)
            // Moonlight on the upper leg segment.
            drawLine(Bone.copy(alpha = 0.3f), Offset(hip.x + (knee.x - hip.x) * 0.3f, hip.y + (knee.y - hip.y) * 0.3f), knee, 0.25f * k, StrokeCap.Round)
        }
    }
    // Head (cephalothorax) and abdomen.
    val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(at.x, at.y - 1.5f * k), 2.6f * k)) }
    val abdomen = svgPath(
        "M${at.x} ${at.y + 0.6f * k} C${at.x + 4.5f * k} ${at.y + 1.5f * k} ${at.x + 5f * k} ${at.y + 9f * k} ${at.x} ${at.y + 11f * k} " +
            "C${at.x - 5f * k} ${at.y + 9f * k} ${at.x - 4.5f * k} ${at.y + 1.5f * k} ${at.x} ${at.y + 0.6f * k} Z",
    )
    drawPath(abdomen, Ink)
    drawPath(head, Ink)
    furEdge(abdomen, seed = 52, density = 60f, length = 0.4f..1.2f, ambient = 0.1f, width = 0.25f)
    // Faint pale markings on the abdomen.
    drawLine(Bone.copy(alpha = 0.35f), Offset(at.x - 1.6f * k, at.y + 4.5f * k), Offset(at.x, at.y + 5.8f * k), 0.4f * k, StrokeCap.Round)
    drawLine(Bone.copy(alpha = 0.35f), Offset(at.x + 1.6f * k, at.y + 4.5f * k), Offset(at.x, at.y + 5.8f * k), 0.4f * k, StrokeCap.Round)
    drawLine(Bone.copy(alpha = 0.25f), Offset(at.x - 1.2f * k, at.y + 7.2f * k), Offset(at.x, at.y + 8.2f * k), 0.35f * k, StrokeCap.Round)
    drawLine(Bone.copy(alpha = 0.25f), Offset(at.x + 1.2f * k, at.y + 7.2f * k), Offset(at.x, at.y + 8.2f * k), 0.35f * k, StrokeCap.Round)
    drawCircle(Bone, 0.45f * k, Offset(at.x - 0.9f * k, at.y - 2.3f * k))
    drawCircle(Bone, 0.45f * k, Offset(at.x + 0.9f * k, at.y - 2.3f * k))
}
