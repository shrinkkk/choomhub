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
import com.choomhub.app.core.designsystem.illustration.furEdge
import com.choomhub.app.core.designsystem.illustration.splatter
import com.choomhub.app.core.designsystem.illustration.svgPath
import com.choomhub.app.core.designsystem.illustration.whisker

/** The cat's moods, from "nothing planned" to "it's today". */
enum class CatExpression { Crying, Sad, Bored, Hopeful, Happy, Excited, Ecstatic }

private val Ink = InkPalette.Ink
private val Bone = InkPalette.Bone
private val Tongue = androidx.compose.ui.graphics.Color(0xFF7A2E3E)

/**
 * One continuous shape from ear tips through fluffy cheeks into the chest,
 * which runs off the bottom of the frame, so head and body read as one cat.
 */
private fun catShape(ears: Ears): Path {
    val (lTip, lBase, rTip, rBase) = when (ears) {
        Ears.Up -> listOf("24 7", "27 37", "76 7", "73 37")
        Ears.Tilted -> listOf("15 15", "26 38", "85 15", "74 38")
        Ears.Flat -> listOf("8 27", "25 40", "92 27", "75 40")
    }
    return svgPath(
        // Slender build: a head wider than the long, thin neck below it.
        "M39 120 C39 106 39 93 40 84 C40.5 78 37.5 74 31 72 C25 70 21.5 65 21.5 58 C21 51 23 45 $lBase " +
            "L$lTip C31 15 36 19 41 23 C46 21.5 54 21.5 59 23 C64 19 69 15 $rTip L$rBase " +
            "C77 45 79 51 78.5 58 C78.5 65 75 70 69 72 C62.5 74 59.5 78 60 84 C61 93 61 106 61 120 Z",
    )
}

private enum class Ears { Up, Tilted, Flat }

/**
 * The shared black cat (the one on the windowsill that will become your pet),
 * drawn in ink after the reference: big pale eyes, wiry whiskers, fur that
 * catches the light from the upper right.
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

internal fun DrawScope.drawCatFace(e: CatExpression, splatters: Boolean = true) {
    val ears = when (e) {
        CatExpression.Crying, CatExpression.Sad -> Ears.Flat
        CatExpression.Bored -> Ears.Tilted
        else -> Ears.Up
    }
    val body = catShape(ears)

    if (splatters) {
        splatter(Offset(9f, 12f), 1.8f, InkPalette.Ash.copy(alpha = 0.8f), seed = 11)
        splatter(Offset(93f, 64f), 1.3f, InkPalette.Ash.copy(alpha = 0.7f), seed = 5)
    }

    drawPath(body, Ink)
    // Light from the right: one side catches fur, the other melts into the dark.
    furEdge(body, seed = 21, density = 64f, length = 1.6f..6.5f, light = Offset(0.9f, -0.3f), ambient = 0.1f)

    drawInnerEars(ears)
    drawEyes(e)
    drawMouth(e)

    // Long wiry whiskers; they sag when low and lift when thrilled.
    val lift = when (e) {
        CatExpression.Crying, CatExpression.Sad -> 5f
        CatExpression.Bored -> 2f
        CatExpression.Excited, CatExpression.Ecstatic -> -3f
        else -> 0f
    }
    listOf(-2.5f, 0f, 2.5f).forEachIndexed { i, dy ->
        val bow = (i - 1) * 1.4f + lift * 0.5f
        whisker(Offset(39f, 57.5f + dy * 0.4f), Offset(-2f, 53f + dy * 2.4f + lift * 2f), bow, Bone.copy(alpha = 0.7f), 0.45f)
        whisker(Offset(61f, 57.5f + dy * 0.4f), Offset(102f, 53f + dy * 2.4f + lift * 2f), bow, Bone.copy(alpha = 0.7f), 0.45f)
    }

    if (e == CatExpression.Excited || e == CatExpression.Ecstatic) {
        val c = Bone.copy(alpha = 0.8f)
        listOf(Offset(90f, 16f) to 3.5f, Offset(10f, 30f) to 2.5f, Offset(91f, 40f) to 2f).forEach { (p, r) ->
            drawLine(c, Offset(p.x, p.y - r), Offset(p.x, p.y + r), 0.8f, StrokeCap.Round)
            drawLine(c, Offset(p.x - r, p.y), Offset(p.x + r, p.y), 0.8f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawInnerEars(ears: Ears) {
    val pair = when (ears) {
        Ears.Up -> tri(28.5f, 33f, 25.8f, 12f, 36.5f, 23f) to tri(71.5f, 33f, 74.2f, 12f, 63.5f, 23f)
        Ears.Tilted -> tri(27f, 35f, 18f, 19f, 35f, 25f) to tri(73f, 35f, 82f, 19f, 65f, 25f)
        Ears.Flat -> tri(26f, 37f, 13f, 29f, 35f, 28f) to tri(74f, 37f, 87f, 29f, 65f, 28f)
    }
    listOf(pair.first, pair.second).forEachIndexed { i, ear ->
        drawPath(ear, Bone.copy(alpha = 0.82f))
        // Ink fur reaching into the pale ear.
        furEdge(ear, seed = 40 + i, color = Ink, density = 120f, length = 1.2f..3.5f, light = Offset(0f, 1f), ambient = 0.9f, width = 0.45f)
    }
}

private fun DrawScope.drawEyes(e: CatExpression) {
    val xs = listOf(39f, 61f)
    val y = 44f
    when (e) {
        CatExpression.Ecstatic -> xs.forEach { x ->
            // Squeezed shut with joy: thick pale arcs.
            drawArc(Bone, 200f, 140f, false, Offset(x - 7f, y - 2f), Size(14f, 9f), style = Stroke(2.4f, cap = StrokeCap.Round))
        }
        CatExpression.Excited -> xs.forEachIndexed { i, x ->
            drawCircle(Bone, 10f, Offset(x, y))
            drawCircle(Ink, 2.1f, Offset(x + if (i == 0) 1.2f else -1.2f, y))
        }
        CatExpression.Happy -> xs.forEachIndexed { i, x ->
            drawCircle(Bone, 9.5f, Offset(x, y))
            drawCircle(Ink, 3.6f, Offset(x + if (i == 0) 1.5f else -1f, y - 1f))
            // A soft lower lid, smiling eyes.
            drawArc(Ink, 20f, 140f, true, Offset(x - 10f, y + 3.5f), Size(20f, 10f))
        }
        CatExpression.Hopeful -> xs.forEach { x ->
            // Looking up, hoping.
            drawCircle(Bone, 9.5f, Offset(x, y))
            drawCircle(Ink, 3.4f, Offset(x + 2f, y - 4.2f))
        }
        CatExpression.Bored -> xs.forEach { x ->
            // Deadpan: flat lids halfway down, eyes slid to the side.
            drawCircle(Bone, 9.5f, Offset(x, y))
            drawCircle(Ink, 3.2f, Offset(x - 4.5f, y + 2f))
            drawRect(Ink, Offset(x - 11f, y - 11f), Size(22f, 9.8f))
            drawLine(Bone.copy(alpha = 0.5f), Offset(x - 9f, y - 1.2f), Offset(x + 9f, y - 1.2f), 0.6f)
        }
        CatExpression.Sad, CatExpression.Crying -> {
            xs.forEachIndexed { i, x ->
                drawCircle(Bone, 9.5f, Offset(x, y + 1f))
                drawCircle(Ink, 3.4f, Offset(x + if (i == 0) -1f else 1f, y + 5.5f))
                val outer = if (i == 0) x - 11f else x + 11f
                val inner = if (i == 0) x + 11f else x - 11f
                drawPath(
                    Path().apply { moveTo(outer, y - 12f); lineTo(inner, y - 12f); lineTo(inner, y - 4.5f); lineTo(outer, y + 1.5f); close() },
                    Ink,
                )
            }
            if (e == CatExpression.Crying) drip(Offset(34f, 54f), 13f, 1.3f, Bone.copy(alpha = 0.85f))
        }
    }
}

private fun DrawScope.drawMouth(e: CatExpression) {
    drawRoundRect(Bone.copy(alpha = 0.9f), Offset(47.3f, 56.5f), Size(5.4f, 1.8f), CornerRadius(0.9f))
    val pale = Bone.copy(alpha = 0.7f)
    val line = Stroke(width = 0.8f, cap = StrokeCap.Round)
    when (e) {
        CatExpression.Ecstatic -> {
            drawArc(Ink, 0f, 180f, true, Offset(45.5f, 60f), Size(9f, 8f))
            drawArc(Tongue, 0f, 180f, true, Offset(47.5f, 63.5f), Size(5f, 4f))
            drawArc(pale, 0f, 180f, false, Offset(45.5f, 60f), Size(9f, 8f), style = line)
        }
        CatExpression.Excited -> drawArc(pale, 0f, 180f, false, Offset(47.5f, 60.5f), Size(5f, 4.5f), style = line)
        CatExpression.Happy, CatExpression.Hopeful -> {
            drawArc(pale, 0f, 160f, false, Offset(46f, 60.5f), Size(4f, 3f), style = line)
            drawArc(pale, 20f, 160f, false, Offset(50f, 60.5f), Size(4f, 3f), style = line)
        }
        CatExpression.Bored -> drawLine(pale, Offset(47.5f, 62f), Offset(52.5f, 62f), 0.8f, StrokeCap.Round)
        CatExpression.Sad, CatExpression.Crying -> {
            drawLine(pale, Offset(47.5f, 63f), Offset(50f, 61.5f), 0.8f, StrokeCap.Round)
            drawLine(pale, Offset(52.5f, 63f), Offset(50f, 61.5f), 0.8f, StrokeCap.Round)
        }
    }
}

private fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Path().apply {
    moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
}
