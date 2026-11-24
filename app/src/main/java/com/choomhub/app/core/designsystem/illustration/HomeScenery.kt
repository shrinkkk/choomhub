package com.choomhub.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.SkyColors
import com.choomhub.app.core.designsystem.theme.SkyPhase

/**
 * The fixed scene in Home's top-right corner: a dead branch reaching in from
 * the edge with the moon or a clouded sun behind it. Two ravens sit on it
 * facing each other at dawn and during the day; at dusk and at night two bats
 * hang from it asleep.
 */
@Composable
fun HomeScenery(phase: SkyPhase, sky: SkyColors, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // Authored in a 176 x 130 dp box anchored to the right edge; it shrinks on narrow
        // phones so it never runs into the app name.
        val k = ((size.width - 205.dp.toPx()) / 176.dp.toPx()).coerceIn(0.62f, 1f) * 1.dp.toPx()
        val origin = Offset(size.width - 176f * k, 0f)
        withTransform({ translate(origin.x, origin.y); scale(k, k, pivot = Offset.Zero) }) {
            drawScene(phase, sky)
        }
    }
}

private fun DrawScope.drawScene(phase: SkyPhase, sky: SkyColors) {
    // Up behind the ravens in daylight; low behind the hanging bats at dusk and night,
    // so they show as silhouettes against it.
    val high = Offset(90f, 44f)
    val low = Offset(92f, 94f)
    when (phase) {
        SkyPhase.Night -> drawMoon(low, 27f, sky)
        SkyPhase.Dawn -> drawSun(high, 23f, androidx.compose.ui.graphics.Color(0xFFD9B7B2), sky, haze = 0.20f)
        SkyPhase.Day -> drawSun(high, 23f, androidx.compose.ui.graphics.Color(0xFFEADFCF), sky, haze = 0.17f)
        SkyPhase.Dusk -> drawSun(low, 25f, androidx.compose.ui.graphics.Color(0xFFC98A72), sky, haze = 0.24f)
    }

    drawBranch(moonlit = phase == SkyPhase.Night)

    when (phase) {
        SkyPhase.Dawn, SkyPhase.Day -> {
            raven(feet = Offset(64f, 71.4f), height = 34f, facingRight = true)
            raven(feet = Offset(118f, 68.2f), height = 32f, facingRight = false)
        }
        SkyPhase.Dusk, SkyPhase.Night -> {
            hangingBat(top = Offset(84f, 75.2f), height = 33f, mirrored = false)
            hangingBat(top = Offset(104.5f, 74.2f), height = 29f, mirrored = true)
        }
    }
}

private val BRANCH = "M184 55 C162 57 141 63.5 120 66.8 C100 70 80 71.6 60 70.8 C40 70 22 64 6 58 " +
    "C4.8 57.6 4.6 59.2 5.8 59.8 C22 66.6 42 74.6 60 75.8 C80 77 100 75.2 121 72.6 C141 70 162 64.6 184 64 Z"
private const val BRANCH_TOP = "M184 55 C162 57 141 63.5 120 66.8 C100 70 80 71.6 60 70.8 C40 70 22 64 6 58"
private const val TWIG_UP = "M58.5 71.5 C52 63.5 46 55.5 40 46.5 C39.4 45.8 40.9 45.3 41.5 46.1 C47.5 54.5 54.5 62.5 62 71 Z"
private const val TWIG_DOWN = "M138 69.2 C143 75 147.5 80.5 152 86 C152.6 86.8 151.3 87.3 150.7 86.5 C146 81 141 75.6 135.4 70.2 Z"
private const val SPUR = "M101.5 71.2 C103.6 66.2 106.8 63 111 61 C111.7 60.7 112 61.8 111.4 62.1 C108 64.2 106 67.2 105.2 70.8 Z"

private fun DrawScope.drawBranch(moonlit: Boolean) {
    listOf(BRANCH, TWIG_UP, TWIG_DOWN, SPUR).forEachIndexed { i, d ->
        val path = svgPath(d)
        drawPath(path, InkPalette.Ink)
        // Bark: short scratchy strands along the edges.
        scratchEdge(path, InkPalette.Bone.copy(alpha = if (moonlit) 0.3f else 0.18f), strands = 22f, length = 0.6f..2.2f, width = 0.25f, seed = 90 + i)
    }
    strokePath(svgPath(BRANCH_TOP), InkPalette.Bone.copy(alpha = if (moonlit) 0.42f else 0.2f), 0.5f)
    // A couple of knots.
    drawCircle(InkPalette.Bone.copy(alpha = 0.12f), 0.9f, Offset(92f, 72.6f))
    drawCircle(InkPalette.Bone.copy(alpha = 0.10f), 0.7f, Offset(150f, 63.4f))
}

/** The motif raven, scaled to [height] with its talons on [feet]; mirrored to face right. */
private fun DrawScope.raven(feet: Offset, height: Float, facingRight: Boolean) {
    val s = height / 67.5f
    withTransform({
        translate(feet.x, feet.y)
        scale(if (facingRight) -s else s, s, pivot = Offset.Zero)
        translate(-54f, -84.5f)
    }) { drawRavenFigure() }
}

/**
 * A bat asleep upside down, after the owner's reference: one foot gripping the
 * branch, a thin leg, then a long angular body of leathery wings folded around
 * it, one wing's elbow jutting out to the side, and the head tucked at the
 * bottom with its ears pointing down and eyes shut. Ink black with engraved
 * bone hatching. Authored on a 20 x 40 grid, foot at (10.8, 0).
 */
private fun DrawScope.hangingBat(top: Offset, height: Float, mirrored: Boolean) {
    val s = height / 40f
    withTransform({
        translate(top.x, top.y)
        scale(if (mirrored) -s else s, s, pivot = Offset.Zero)
        translate(-10.8f, 0f)
    }) {
        val ink = InkPalette.Ink
        val bone = InkPalette.Bone
        drawPath(BatShapes.body, ink)
        drawPath(BatShapes.head, ink)
        // Toes curled over the branch.
        strokePath(svgPath("M10 3.6 C9.6 1.8 9.8 0.2 10.8 -0.6 M11 3.4 C10.8 1.6 11.2 0 12.2 -0.5 M11.8 3.4 C11.8 1.8 12.4 0.6 13.2 0.2"), ink, 0.75f)
        // Leathery edges catch a little light; the head is furry.
        scratchEdge(BatShapes.body, bone.copy(alpha = 0.34f), strands = 30f, length = 0.4f..1.3f, width = 0.16f, seed = 71)
        furEdge(BatShapes.head, seed = 72, density = 70f, length = 0.3f..0.9f, light = Offset(0f, 1f), ambient = 0.5f, width = 0.12f, color = bone.copy(alpha = 0.75f))
        // Folds: the wing's front edge crossing over, and the jutting elbow.
        strokePath(svgPath("M2 31.8 C6.5 30 12 29.6 17.6 31.2"), bone.copy(alpha = 0.34f), 0.32f)
        strokePath(svgPath("M4.4 18 C7.6 19.6 10.6 22 12.4 25.4"), bone.copy(alpha = 0.22f), 0.28f)
        strokePath(svgPath("M12 7 C14 14 15.6 21 16.2 29.4"), bone.copy(alpha = 0.16f), 0.26f)
        // Engraved hatching along the folds, clipped to the wings.
        clipPath(BatShapes.body) {
            BatShapes.hatching.forEach { (from, to) -> drawLine(bone.copy(alpha = 0.13f), from, to, strokeWidth = 0.22f) }
        }
        // Eyes shut, a small nose.
        strokePath(svgPath("M7.9 35.6 C8.5 36.3 9.3 36.3 9.8 35.6 M11.8 35.6 C12.3 36.3 13.1 36.3 13.7 35.6"), bone.copy(alpha = 0.7f), 0.28f)
        drawCircle(bone.copy(alpha = 0.35f), 0.35f, Offset(10.8f, 34.4f))
    }
}

private object BatShapes {
    /** Leg and folded wings: narrow at the top, angular, the left wing's elbow jutting out. */
    val body by lazy {
        svgPath(
            "M9.9 3.2 L9.9 5.2 C9.4 7 8.8 8.6 8.1 10.2 C7.3 11.6 6.6 12.6 6.1 13.8 C5.4 15.3 4.9 16.6 4.4 18 " +
                "C5.2 19.2 5.9 20.4 6.1 21.6 C5.4 22.8 4.6 23.6 3.8 24.4 L0.1 22.5 C0.4 24 0.7 25.4 1 26.9 " +
                "C1.4 28.6 1.7 30.2 2 31.8 L1.8 33.1 C2.8 34.4 3.8 35.4 5 36.1 L15.6 36.2 L17.2 36.4 " +
                "C18 36 18.8 35.8 19.6 35.6 C19.8 33 20 30.6 20 28.2 C20 25.6 19.9 22.9 19.8 20.3 " +
                "C19.4 19 18.9 17.8 18.4 16.7 C16.8 14 15 11.2 13.3 8.5 C12.8 7.4 12.3 6.3 12.1 5.2 L11.9 3 Z",
        )
    }

    /** Head tucked under the wings, ears pointing down. */
    val head by lazy {
        svgPath(
            "M5.6 34.8 C6.6 33.4 8.6 32.6 10.8 32.6 C13 32.6 15 33.4 16 34.8 C16.6 35.8 16.9 37.4 16.8 39.5 " +
                "L14.1 38 C13.2 38.8 12 39.2 10.9 39.2 C9.7 39.2 8.6 38.8 7.7 38 L5.1 40 C4.9 37.8 5.1 36 5.6 34.8 Z",
        )
    }

    /** Short diagonal strokes following the folds, like an engraving. */
    val hatching: List<Pair<Offset, Offset>> by lazy {
        val random = kotlin.random.Random(73)
        List(46) {
            val x = 2f + random.nextFloat() * 17f
            val y = 9f + random.nextFloat() * 26f
            val len = 1.2f + random.nextFloat() * 1.8f
            val slope = if (y > 27f) -0.15f else 0.55f
            Offset(x, y) to Offset(x + len, y + len * slope)
        }
    }
}
