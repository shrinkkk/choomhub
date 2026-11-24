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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.theme.ChoomHubColors
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

private val PaperCream = Color(0xFFEDE4EE)
private val PaperShade = Color(0xFFD7CBDB)
private val DuskTop = Color(0xFF2A2145)
private val DuskMid = Color(0xFF5B3D66)
private val DuskLow = Color(0xFF9A6478)
private val Hills = Color(0xFF1A1424)

/**
 * Placeholder "photograph" for the latest-snap card: a rainy dusk over
 * sleepy hills with a single warm streetlamp. Stands in until real images
 * arrive from storage.
 */
@Composable
fun RainyDuskPhoto(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) { drawRainyDusk(colors) }
}

internal fun DrawScope.drawRainyDusk(colors: ChoomHubColors) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(DuskTop, DuskMid, DuskLow), startY = 0f, endY = h))
    drawCrescentMoon(Offset(w * 0.76f, h * 0.24f), h * 0.20f, colors.lavender.copy(alpha = 0.95f))
    // Back hills.
    val back = Path().apply {
        moveTo(0f, h * 0.70f)
        cubicTo(w * 0.25f, h * 0.55f, w * 0.45f, h * 0.68f, w * 0.62f, h * 0.62f)
        cubicTo(w * 0.80f, h * 0.56f, w * 0.92f, h * 0.64f, w, h * 0.60f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(back, Hills.copy(alpha = 0.7f))
    val front = Path().apply {
        moveTo(0f, h * 0.82f)
        cubicTo(w * 0.30f, h * 0.74f, w * 0.55f, h * 0.86f, w, h * 0.78f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(front, Hills)
    // Streetlamp with a warm halo.
    val lampTop = Offset(w * 0.24f, h * 0.56f)
    drawLine(Hills, Offset(lampTop.x, h * 0.86f), lampTop, strokeWidth = w * 0.012f)
    drawCircle(
        brush = Brush.radialGradient(listOf(colors.candle.copy(alpha = 0.55f), Color.Transparent), center = lampTop, radius = h * 0.16f),
        radius = h * 0.16f,
        center = lampTop,
    )
    drawCircle(colors.candle, radius = w * 0.014f, center = lampTop)
    drawRain(Rect(0f, 0f, w, h), colors.lavender.copy(alpha = 0.35f), drops = 34, seed = 7)
}

/**
 * A rainy window at night with the shared cat sitting on the sill.
 */
@Composable
fun RainyWindowWithCat(
    modifier: Modifier = Modifier,
    catEyesOpen: Boolean = false,
) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val pane = Rect(w * 0.06f, h * 0.04f, w * 0.94f, h * 0.80f)
        val corner = CornerRadius(w * 0.06f)
        // Night sky inside the window.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(DuskTop, colors.surfaceElevated), startY = pane.top, endY = pane.bottom),
            topLeft = pane.topLeft,
            size = pane.size,
            cornerRadius = corner,
        )
        clipRect(pane.left, pane.top, pane.right, pane.bottom) {
            drawCrescentMoon(Offset(w * 0.74f, h * 0.22f), h * 0.14f, colors.lavender)
            drawSparkle(Offset(w * 0.22f, h * 0.16f), w * 0.025f, colors.pink.copy(alpha = 0.8f))
            drawSparkle(Offset(w * 0.44f, h * 0.30f), w * 0.018f, colors.lavender.copy(alpha = 0.6f))
            drawRain(pane, colors.lavender.copy(alpha = 0.30f), drops = 24, seed = 11)
        }
        // Window frame and mullions.
        val frame = colors.outline
        drawRoundRect(frame, pane.topLeft, pane.size, corner, style = Stroke(width = w * 0.025f))
        drawLine(frame, Offset(pane.center.x, pane.top), Offset(pane.center.x, pane.bottom), strokeWidth = w * 0.018f)
        drawLine(frame, Offset(pane.left, pane.center.y), Offset(pane.right, pane.center.y), strokeWidth = w * 0.018f)
        // Sill.
        drawRoundRect(
            color = colors.surfaceHighest,
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.03f),
        )
        // Cat on the sill, drawn into a sub-region.
        val catHeight = h * 0.50f
        val catWidth = catHeight * 0.8f
        val catTop = h * 0.785f - catHeight
        drawInRect(Rect(w * 0.16f, catTop, w * 0.16f + catWidth, catTop + catHeight)) {
            drawBlackCat(rim = colors.lavender, eyes = colors.candle, innerEar = colors.pink, eyesOpen = catEyesOpen)
        }
    }
}

/** A sealed envelope with a heart-shaped wax seal. */
@Composable
fun LoveLetter(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Rect(w * 0.06f, h * 0.22f, w * 0.94f, h * 0.86f)
        rotate(-6f) {
            drawRoundRect(PaperCream, body.topLeft, body.size, CornerRadius(w * 0.04f))
            val flap = Path().apply {
                moveTo(body.left, body.top)
                lineTo(body.center.x, body.top + body.height * 0.55f)
                lineTo(body.right, body.top)
            }
            drawPath(flap, PaperShade, style = Stroke(width = w * 0.02f))
            val sealTop = body.top + body.height * 0.44f
            drawInRect(Rect(body.center.x - w * 0.09f, sealTop, body.center.x + w * 0.09f, sealTop + w * 0.16f)) {
                drawHeart(colors.tomato)
            }
        }
    }
}

/** Three tilted polaroids pinned to an invisible wall. */
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
                    drawRoundRect(PaperCream, Offset.Zero, frame, CornerRadius(w * 0.012f))
                    val inner = Rect(frame.width * 0.08f, frame.width * 0.08f, frame.width * 0.92f, frame.height * 0.74f)
                    drawRect(Brush.verticalGradient(listOf(DuskTop, DuskMid), inner.top, inner.bottom), inner.topLeft, inner.size)
                    val c = inner.center
                    when (pin.scene) {
                        0 -> drawCrescentMoon(c, inner.width * 0.42f, colors.lavender)
                        1 -> drawInRect(
                            Rect(c.x - inner.width * 0.28f, c.y - inner.width * 0.24f, c.x + inner.width * 0.28f, c.y + inner.width * 0.26f),
                        ) { drawHeart(colors.pink) }
                        else -> drawInRect(
                            Rect(c.x - inner.width * 0.3f, c.y - inner.width * 0.3f, c.x + inner.width * 0.3f, c.y + inner.width * 0.3f),
                        ) { drawTomato(colors.tomato, colors.leaf, colors.pink, withFace = true) }
                    }
                }
            }
        }
        // Washi tape on the middle photo.
        rotate(-2f) {
            drawRect(colors.pink.copy(alpha = 0.55f), Offset(w * 0.44f, h * 0.05f), Size(w * 0.12f, h * 0.06f))
        }
    }
}

/** A loose cluster of sparkles around a small moon. */
@Composable
fun SparkleCluster(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCrescentMoon(Offset(w * 0.30f, h * 0.36f), h * 0.26f, colors.lavender)
        drawSparkle(Offset(w * 0.68f, h * 0.30f), h * 0.16f, colors.pink)
        drawSparkle(Offset(w * 0.56f, h * 0.70f), h * 0.11f, colors.purple)
        drawSparkle(Offset(w * 0.82f, h * 0.62f), h * 0.07f, colors.candle)
        drawSparkle(Offset(w * 0.20f, h * 0.78f), h * 0.06f, colors.lavender.copy(alpha = 0.7f))
    }
}

/**
 * Runs [block] with the canvas temporarily narrowed to [rect], so
 * illustrations written against "the whole canvas" can be reused as parts of
 * a bigger scene without distortion.
 */
internal inline fun DrawScope.drawInRect(rect: Rect, crossinline block: DrawScope.() -> Unit) {
    inset(rect.left, rect.top, size.width - rect.right, size.height - rect.bottom) { block() }
}
