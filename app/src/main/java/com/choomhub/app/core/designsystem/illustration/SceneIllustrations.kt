package com.choomhub.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.theme.ChoomHubColors
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

private val DuskTop = Color(0xFF1F1A2E)
private val DuskMid = Color(0xFF3E3046)
private val DuskLow = Color(0xFF5B4250)
private val HillBack = Color(0xFF1A1424)
private val HillFront = Color(0xFF141019)
private val NightPane = Color(0xFF211B33)
private val Ink = Color(0xFF2A2530)

/**
 * Placeholder "photograph" for the latest snap: dusk, rain, and a few crows
 * waiting on a telephone wire. Authored on a 100 x 75 grid (4:3).
 */
@Composable
fun CrowsOnAWirePhoto(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val s = size.width / 100f
        drawRect(Brush.verticalGradient(0f to DuskTop, 0.6f to DuskMid, 1f to DuskLow))
        scale(s, s, pivot = Offset.Zero) { drawCrowsOnAWire(colors) }
    }
}

private fun DrawScope.drawCrowsOnAWire(colors: ChoomHubColors) {
    drawCrescentAt(Offset(77f, 21f), 8f, colors.lavender.copy(alpha = 0.85f))
    drawPath(
        Path().apply {
            moveTo(0f, 54f); cubicTo(25f, 46f, 45f, 54f, 62f, 50f); cubicTo(80f, 46f, 92f, 52f, 100f, 49f)
            lineTo(100f, 75f); lineTo(0f, 75f); close()
        },
        HillBack.copy(alpha = 0.75f),
    )
    drawPath(
        Path().apply {
            moveTo(0f, 64f); cubicTo(30f, 58f, 55f, 67f, 100f, 61f); lineTo(100f, 75f); lineTo(0f, 75f); close()
        },
        HillFront,
    )
    // Pole and two sagging wires.
    drawLine(HillFront, Offset(14f, 20f), Offset(14f, 64f), strokeWidth = 1.6f)
    drawLine(HillFront, Offset(8f, 24f), Offset(20f, 24f), strokeWidth = 1.6f)
    listOf(0f, 1.5f).forEach { dy ->
        drawPath(
            Path().apply { moveTo(14f, 24.5f + dy); quadraticTo(57f, 43.5f + dy, 100f, 27f + dy) },
            HillFront,
            style = Stroke(width = 0.5f),
        )
    }
    listOf(33f, 40f, 46f, 68f).forEachIndexed { i, x ->
        drawPerchedCrow(Offset(x, wireY(x) + 0.4f), height = if (i == 2) 5.2f else 5.8f)
    }
    drawRain(Rect(0f, 0f, 100f, 75f), colors.lavender.copy(alpha = 0.22f), drops = 22, seed = 7, strokeWidth = 0.35f)
}

/** Height of the upper wire at [x] (quadratic from (14, 24.5) via (57, 43.5) to (100, 27)). */
private fun wireY(x: Float): Float {
    val t = (x - 14f) / 86f
    return (1 - t) * (1 - t) * 24.5f + 2 * (1 - t) * t * 34f + t * t * 27f
}

/**
 * The shared cat, from behind, watching a crow through a rainy window.
 * Authored on a 100 x 88 grid.
 */
@Composable
fun WindowWatch(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val s = minOf(size.width / 100f, size.height / 88f)
        translate((size.width - 100f * s) / 2f, (size.height - 88f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) { drawWindowWatch(colors) }
        }
    }
}

private fun DrawScope.drawWindowWatch(colors: ChoomHubColors) {
    val pane = Rect(6f, 4f, 94f, 80f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(NightPane, colors.surfaceElevated), startY = pane.top, endY = pane.bottom),
        topLeft = pane.topLeft,
        size = pane.size,
        cornerRadius = CornerRadius(3f),
    )
    clipRect(pane.left, pane.top, pane.right, pane.bottom) {
        drawCrescentAt(Offset(66f, 26f), 8f, colors.lavender.copy(alpha = 0.9f))
        drawPath(
            Path().apply { moveTo(56f, 66f); cubicTo(70f, 64f, 82f, 67f, 96f, 63f) },
            Ink,
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
        drawPerchedCrow(Offset(75f, 65.2f), height = 11f, rim = colors.lavender.copy(alpha = 0.45f))
        drawRain(pane, colors.lavender.copy(alpha = 0.28f), drops = 20, seed = 11, strokeWidth = 0.5f)
    }
    drawRoundRect(colors.outline, pane.topLeft, pane.size, CornerRadius(3f), style = Stroke(width = 2.4f))
    drawLine(colors.outline, Offset(50f, pane.top), Offset(50f, pane.bottom), strokeWidth = 1.8f)
    drawLine(colors.outline, Offset(pane.left, 42f), Offset(pane.right, 42f), strokeWidth = 1.8f)
    drawRoundRect(colors.surfaceHighest, Offset(0f, 78f), Size(100f, 7f), CornerRadius(2f))
    // Cat sits on the sill: grid feet at y = 94 map onto y = 79.
    placed(Offset(10f, 32f), 0.5f) { drawShadowCat(colors.lavender) }
}

/** A sealed envelope with a wax seal stamped with a crescent. */
@Composable
fun SealedLetter(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Rect(w * 0.08f, h * 0.22f, w * 0.92f, h * 0.84f)
        rotate(-6f) {
            drawRoundRect(colors.bone.copy(alpha = 0.88f), body.topLeft, body.size, CornerRadius(w * 0.015f))
            val flap = Path().apply {
                moveTo(body.left, body.top)
                lineTo(body.center.x, body.top + body.height * 0.55f)
                lineTo(body.right, body.top)
            }
            drawPath(flap, Color(0xFFA89F92), style = Stroke(width = w * 0.012f))
            val seal = Offset(body.center.x, body.top + body.height * 0.56f)
            val r = w * 0.085f
            repeat(10) { i ->
                val a = Math.toRadians(i * 36.0)
                drawCircle(colors.wine, r * 0.28f, Offset(seal.x + (r * kotlin.math.cos(a)).toFloat(), seal.y + (r * kotlin.math.sin(a)).toFloat()))
            }
            drawCircle(colors.wine, r, seal)
            drawCircle(Color(0xFF3A1420), r * 0.72f, seal, style = Stroke(width = w * 0.008f))
            drawCrescentAt(seal, r * 0.5f, Color(0xFF3A1420), glow = false)
        }
    }
}

/** Three tarot-style cards fanned out: moon, eye, star. */
@Composable
fun TarotSpread(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val cardW = size.height * 0.52f
        val cardH = size.height * 0.86f
        val pivot = Offset(size.width / 2f, size.height * 1.05f)
        listOf(-16f to 0, 16f to 2, 0f to 1).forEach { (angle, symbol) ->
            rotate(angle, pivot) {
                val left = size.width / 2f - cardW / 2f
                val top = size.height * 0.06f
                drawTarotCard(Rect(left, top, left + cardW, top + cardH), symbol, colors)
            }
        }
    }
}

private fun DrawScope.drawTarotCard(card: Rect, symbol: Int, colors: ChoomHubColors) {
    val r = CornerRadius(card.width * 0.05f)
    drawRoundRect(colors.bone.copy(alpha = 0.9f), card.topLeft, card.size, r)
    val face = Rect(card.left + card.width * 0.06f, card.top + card.width * 0.06f, card.right - card.width * 0.06f, card.bottom - card.width * 0.06f)
    drawRoundRect(colors.surface, face.topLeft, face.size, CornerRadius(card.width * 0.03f))
    val inner = Rect(face.left + card.width * 0.05f, face.top + card.width * 0.05f, face.right - card.width * 0.05f, face.bottom - card.width * 0.05f)
    drawRect(colors.bone.copy(alpha = 0.35f), inner.topLeft, inner.size, style = Stroke(width = card.width * 0.012f))
    val c = inner.center
    val u = inner.width
    val line = colors.bone.copy(alpha = 0.85f)
    when (symbol) {
        0 -> drawCrescentAt(c, u * 0.26f, line, glow = false)
        1 -> {
            val eye = Path().apply {
                moveTo(c.x - u * 0.34f, c.y)
                quadraticTo(c.x, c.y - u * 0.30f, c.x + u * 0.34f, c.y)
                quadraticTo(c.x, c.y + u * 0.30f, c.x - u * 0.34f, c.y)
                close()
            }
            drawPath(eye, line, style = Stroke(width = u * 0.03f))
            drawCircle(line, u * 0.09f, c)
            listOf(-0.25f, 0f, 0.25f).forEach { dx ->
                drawLine(line, Offset(c.x + u * dx, c.y - u * 0.22f), Offset(c.x + u * dx * 1.3f, c.y - u * 0.34f), u * 0.025f, StrokeCap.Round)
            }
        }
        else -> {
            val star = Path().apply {
                moveTo(c.x, c.y - u * 0.32f)
                quadraticTo(c.x, c.y, c.x + u * 0.22f, c.y)
                quadraticTo(c.x, c.y, c.x, c.y + u * 0.32f)
                quadraticTo(c.x, c.y, c.x - u * 0.22f, c.y)
                quadraticTo(c.x, c.y, c.x, c.y - u * 0.32f)
                close()
            }
            drawPath(star, line, style = Stroke(width = u * 0.03f))
        }
    }
}

/** Three tilted polaroids pinned to an invisible wall: moon, crows, tomato. */
@Composable
fun PolaroidWall(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val frame = Size(w * 0.36f, h * 0.62f)
        data class Pin(val x: Float, val y: Float, val angle: Float, val scene: Int)
        listOf(
            Pin(0.05f, 0.22f, -9f, 0),
            Pin(0.58f, 0.18f, 8f, 2),
            Pin(0.32f, 0.08f, -2f, 1),
        ).forEach { pin ->
            translate(w * pin.x, h * pin.y) {
                rotate(pin.angle, pivot = Offset(frame.width / 2f, frame.height / 2f)) {
                    drawRect(colors.bone.copy(alpha = 0.9f), Offset.Zero, frame)
                    val inner = Rect(frame.width * 0.08f, frame.width * 0.08f, frame.width * 0.92f, frame.height * 0.74f)
                    drawRect(Brush.verticalGradient(listOf(DuskTop, DuskMid), inner.top, inner.bottom), inner.topLeft, inner.size)
                    val c = inner.center
                    when (pin.scene) {
                        0 -> drawCrescentAt(c, inner.width * 0.28f, colors.lavender)
                        1 -> {
                            drawFlyingCrow(Offset(c.x - inner.width * 0.12f, c.y - inner.width * 0.05f), inner.width * 0.42f)
                            drawFlyingCrow(Offset(c.x + inner.width * 0.22f, c.y + inner.width * 0.14f), inner.width * 0.28f)
                        }
                        else -> inset(
                            c.x - inner.width * 0.3f,
                            c.y - inner.width * 0.3f,
                            size.width - (c.x + inner.width * 0.3f),
                            size.height - (c.y + inner.width * 0.3f),
                        ) { drawTomato(colors.tomato, colors.leaf) }
                    }
                }
            }
        }
        rotate(-2f) {
            drawRect(colors.wine.copy(alpha = 0.6f), Offset(w * 0.44f, h * 0.05f), Size(w * 0.12f, h * 0.05f))
        }
    }
}
