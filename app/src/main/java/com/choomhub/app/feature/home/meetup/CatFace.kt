package com.choomhub.app.feature.home.meetup

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.illustration.InkPalette
import com.choomhub.app.core.designsystem.illustration.drip
import com.choomhub.app.core.designsystem.illustration.inkSilhouette
import com.choomhub.app.core.designsystem.illustration.scratchEdge
import com.choomhub.app.core.designsystem.illustration.splatter
import com.choomhub.app.core.designsystem.illustration.svgPath
import com.choomhub.app.core.designsystem.illustration.whisker

/** Expressions for the countdown's cat. */
enum class CatExpression { Crying, Sad, Happy, Excited }

private val Ink = InkPalette.Ink
private val Bone = InkPalette.Bone

/** Head and shoulders, ears up; the body runs off the bottom of the frame. */
private val UprightCat by lazy {
    svgPath(
        "M20 118 C18 86 15 74 17 62 C18 52 20 43 24 36 L21 5 L39 23 C45 20.5 55 20.5 61 23 L79 5 " +
            "L76 36 C80 43 82 52 83 62 C85 74 82 86 80 118 Z",
    )
}

/** Same cat with ears flattened out to the sides. */
private val DroopyCat by lazy {
    svgPath(
        "M20 118 C18 86 15 74 17 62 C18 52 20 44 23 38 L6 24 L36 26 C44 23 56 23 64 26 L94 24 " +
            "L77 38 C80 44 82 52 83 62 C85 74 82 86 80 118 Z",
    )
}

/**
 * The shared black cat (the one on the windowsill that will become your pet),
 * drawn in ink: huge cream eyes, wiry whiskers, fur scratched into the edges.
 * Ears flatten and lids drop when nothing is planned; pupils shrink to pinpricks
 * when the meetup is close.
 */
@Composable
fun CatFace(expression: CatExpression, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                clipRect(0f, 0f, 100f, 100f) { drawCatFace(expression) }
            }
        }
    }
}

private fun DrawScope.drawCatFace(expression: CatExpression) {
    val droopy = expression == CatExpression.Crying || expression == CatExpression.Sad

    splatter(Offset(12f, 14f), 2.2f, InkPalette.Ash.copy(alpha = 0.8f), seed = 11)
    splatter(Offset(90f, 70f), 1.6f, InkPalette.Ash.copy(alpha = 0.7f), seed = 5)

    inkSilhouette(if (droopy) DroopyCat else UprightCat, seed = 21, strands = 38f)

    // Pale inner ears with a little hatching.
    val ears = if (droopy) {
        listOf(tri(23f, 37f, 10f, 26f, 32f, 28f), tri(77f, 37f, 90f, 26f, 68f, 28f))
    } else {
        listOf(tri(26f, 32f, 23.5f, 11f, 36f, 23.5f), tri(74f, 32f, 76.5f, 11f, 64f, 23.5f))
    }
    ears.forEach { ear ->
        drawPath(ear, Bone.copy(alpha = 0.88f))
        scratchEdge(ear, Ink, strands = 60f, length = 1.5f..4f, width = 0.5f, seed = 3, coverage = 0.6f)
    }

    drawEyes(expression)

    // Nose: a small pale sliver; mouth: a tiny chevron.
    drawRoundRect(Bone.copy(alpha = 0.9f), Offset(47.2f, 57f), Size(5.6f, 1.9f), CornerRadius(0.9f))
    val mouth = Stroke(width = 0.8f, cap = StrokeCap.Round)
    when (expression) {
        CatExpression.Excited -> drawArc(Bone.copy(alpha = 0.85f), 0f, 180f, false, Offset(47.5f, 61f), Size(5f, 4.5f), style = mouth)
        CatExpression.Happy -> {
            drawArc(Bone.copy(alpha = 0.7f), 0f, 160f, false, Offset(46f, 61f), Size(4f, 3f), style = mouth)
            drawArc(Bone.copy(alpha = 0.7f), 20f, 160f, false, Offset(50f, 61f), Size(4f, 3f), style = mouth)
        }
        else -> {
            drawLine(Bone.copy(alpha = 0.6f), Offset(47.5f, 63.5f), Offset(50f, 62f), 0.8f, StrokeCap.Round)
            drawLine(Bone.copy(alpha = 0.6f), Offset(52.5f, 63.5f), Offset(50f, 62f), 0.8f, StrokeCap.Round)
        }
    }

    // Long wiry whiskers running past the face; they sag when sad.
    val sag = if (droopy) 4f else 0f
    listOf(-2.5f, 0f, 2.5f).forEachIndexed { i, dy ->
        val bow = (i - 1) * 1.5f + sag
        whisker(Offset(36f, 57f + dy * 0.4f), Offset(-2f, 52f + dy * 2.4f + sag * 2f), bow, Bone.copy(alpha = 0.75f))
        whisker(Offset(64f, 57f + dy * 0.4f), Offset(102f, 52f + dy * 2.4f + sag * 2f), bow, Bone.copy(alpha = 0.75f))
    }
}

private fun DrawScope.drawEyes(expression: CatExpression) {
    val eyes = listOf(37f, 63f)
    val eyeY = 44f
    when (expression) {
        CatExpression.Excited -> eyes.forEach { x ->
            // Wide open, pupils shrunk to pinpricks, staring straight at you.
            drawCircle(Bone, 10.5f, Offset(x, eyeY))
            drawCircle(Ink, 2.2f, Offset(x + (if (x < 50) 1.5f else -1.5f), eyeY))
        }
        CatExpression.Happy -> eyes.forEach { x ->
            // Big and soft, looking up, a little off-kilter.
            drawCircle(Bone, 9.5f, Offset(x, eyeY))
            drawCircle(Ink, 3.4f, Offset(x + (if (x < 50) 2f else 2.5f), eyeY - 3.6f))
        }
        CatExpression.Sad, CatExpression.Crying -> {
            eyes.forEachIndexed { i, x ->
                drawCircle(Bone, 9.5f, Offset(x, eyeY + 1f))
                drawCircle(Ink, 3.4f, Offset(x + (if (i == 0) -1f else 1f), eyeY + 5.5f))
                // Heavy lids, lower at the outer corner.
                val outer = if (i == 0) x - 11f else x + 11f
                val inner = if (i == 0) x + 11f else x - 11f
                drawPath(
                    Path().apply { moveTo(outer, eyeY - 12f); lineTo(inner, eyeY - 12f); lineTo(inner, eyeY - 4.5f); lineTo(outer, eyeY + 1.5f); close() },
                    Ink,
                )
            }
            if (expression == CatExpression.Crying) {
                // A tear running down like spilled ink, but pale.
                drip(Offset(32f, 54f), 14f, 1.4f, Bone.copy(alpha = 0.85f))
            }
        }
    }
}

private fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Path().apply {
    moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
}
