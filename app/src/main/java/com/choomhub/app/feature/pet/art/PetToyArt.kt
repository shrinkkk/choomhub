package com.choomhub.app.feature.pet.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

private val T_Ink = Color(0xFF15111A)
private val T_Bone = Color(0xFFE6DED0)
private val T_Wood = Color(0xFF8C6F4E)
private val T_WoodDark = Color(0xFF5E4632)
private val T_Plum = Color(0xFF3B3349)
private val T_Lavender = Color(0xFFB7A6D6)
private val T_Pink = Color(0xFFC4A3A8)
private val T_PinkDark = Color(0xFF9C7F84)
private val T_Mouse = Color(0xFF57505E)
private val T_Sage = Color(0xFF7D9C76)
private val T_Moss = Color(0xFF55604F)
private val T_Card = Color(0xFFB08A5E)
private val T_CardDark = Color(0xFF8A6A45)
private val T_Red = Color(0xFFFF6A5C)

/**
 * A toy drawn around [c]. One unit is [u] pixels; each toy fits in about 40 x 40 units.
 * Used in the shop, the play picker and the mini-games.
 */
internal fun DrawScope.toyArt(id: String, c: Offset, u: Float) {
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    when (id) {
        "feather" -> {
            // A wand, a string, and a real feather: quill, split vanes, barbs.
            drawLine(T_Wood, p(-18f, 18f), p(-4f, -14f), 2.2f * u, StrokeCap.Round)
            drawLine(T_WoodDark, p(-17f, 16f), p(-15.5f, 12.5f), 2.4f * u, StrokeCap.Round)
            drawLine(T_Bone.copy(alpha = 0.7f), p(-4f, -14f), p(6f, -12f), 0.35f * u)
            featherAt(p(7f, -12f), 26f * u, 12f)
        }
        "laser" -> {
            rotate(-30f, p(-6f, 6f)) {
                drawRoundRect(Color(0xFF2A2430), p(-16f, 3f), Size(22f * u, 6f * u), CornerRadius(3f * u))
                drawRoundRect(Color(0xFF8C8794), p(4f, 3.6f), Size(2.6f * u, 4.8f * u), CornerRadius(1f * u))
                drawCircle(T_Red, 0.9f * u, p(-8f, 6f))
            }
            drawCircle(T_Red.copy(alpha = 0.18f), 7f * u, p(12f, -10f))
            drawCircle(T_Red.copy(alpha = 0.35f), 3.6f * u, p(12f, -10f))
            drawCircle(T_Red, 2f * u, p(12f, -10f))
        }
        "yarn" -> {
            val o = p(-2f, 0f)
            drawCircle(T_Pink, 13f * u, o)
            for (i in 0..4) drawArc(T_PinkDark, 150f + i * 30f, 110f, false, Offset(o.x - (12f - i * 2f) * u, o.y - (12f - i * 2f) * u), Size((24f - i * 4f) * u, (24f - i * 4f) * u), style = Stroke(1f * u))
            for (i in 0..3) drawArc(T_PinkDark, -40f + i * 25f, 80f, false, Offset(o.x - (11f - i * 2.6f) * u, o.y - (9f - i * 1.8f) * u), Size((22f - i * 5f) * u, (18f - i * 3.6f) * u), style = Stroke(0.8f * u))
            val tail = Path().apply {
                moveTo(o.x + 10f * u, o.y + 8f * u)
                cubicTo(o.x + 16f * u, o.y + 14f * u, o.x + 12f * u, o.y + 18f * u, o.x + 19f * u, o.y + 17f * u)
            }
            drawPath(tail, T_Pink, style = Stroke(1.1f * u, cap = StrokeCap.Round))
        }
        "mouse" -> {
            drawOval(T_Mouse, p(-11f, -5f), Size(22f * u, 12f * u))
            drawCircle(T_Mouse, 3.6f * u, p(7f, -6f))
            drawCircle(T_Pink, 2.2f * u, p(7f, -6f))
            drawCircle(T_Ink, 0.8f * u, p(10.5f, -1.5f))
            drawCircle(T_Pink, 0.9f * u, p(12f, 1f))
            listOf(-1f, 1f).forEach { s -> drawLine(T_Bone.copy(alpha = 0.5f), p(11.5f, 1f), p(16f, 1f + s * 1.6f), 0.25f * u) }
            val tail = Path().apply { moveTo(c.x - 11f * u, c.y + 2f * u); cubicTo(c.x - 17f * u, c.y + 4f * u, c.x - 15f * u, c.y + 12f * u, c.x - 20f * u, c.y + 11f * u) }
            drawPath(tail, T_Mouse, style = Stroke(1f * u, cap = StrokeCap.Round))
            drawLine(T_Ink.copy(alpha = 0.4f), p(-6f, 7f), p(4f, 7f), 0.5f * u)
        }
        "kicker" -> {
            // A long stuffed pillow with stitched leaves and a tassel.
            rotate(-20f, c) {
                drawRoundRect(T_Sage, p(-18f, -6f), Size(34f * u, 12f * u), CornerRadius(6f * u))
                drawRoundRect(T_Moss, p(-18f, -6f), Size(34f * u, 12f * u), CornerRadius(6f * u), style = Stroke(0.6f * u))
                for (i in 0..3) {
                    val l = p(-12f + i * 8f, 0f)
                    drawOval(T_Moss, Offset(l.x - 2.4f * u, l.y - 1.3f * u), Size(4.8f * u, 2.6f * u))
                    drawLine(T_Sage, Offset(l.x - 2.2f * u, l.y), Offset(l.x + 2.2f * u, l.y), 0.3f * u)
                }
                // Stitching down the seam.
                var x = -16f
                while (x < 14f) { drawLine(T_Bone.copy(alpha = 0.5f), p(x, 5f), p(x + 1.2f, 5f), 0.3f * u); x += 2.4f }
                drawLine(T_Bone, p(16f, 0f), p(19f, 0f), 0.6f * u)
                for (i in -2..2) drawLine(T_Bone, p(19f, 0f), p(23f, i * 1.3f), 0.35f * u)
            }
        }
        "bubble_wand" -> {
            drawLine(T_Plum, p(-14f, 18f), p(0f, -2f), 1.8f * u, StrokeCap.Round)
            drawCircle(T_Lavender, 5f * u, p(3f, -7f), style = Stroke(1.2f * u))
            bubble(p(12f, -14f), 4.5f * u)
            bubble(p(16f, -2f), 3f * u)
            bubble(p(7f, -20f), 2.4f * u)
            bubble(p(-5f, -16f), 1.8f * u)
        }
        "box" -> {
            // An open cardboard box, flaps up.
            drawRect(T_Card, p(-14f, -4f), Size(28f * u, 18f * u))
            drawRect(T_CardDark, p(-14f, -4f), Size(28f * u, 3f * u))
            drawPath(Path().apply { moveTo(c.x - 14f * u, c.y - 4f * u); lineTo(c.x - 19f * u, c.y - 12f * u); lineTo(c.x - 8f * u, c.y - 12f * u); lineTo(c.x - 4f * u, c.y - 4f * u); close() }, Color(0xFFC49C6C))
            drawPath(Path().apply { moveTo(c.x + 14f * u, c.y - 4f * u); lineTo(c.x + 18f * u, c.y - 13f * u); lineTo(c.x + 7f * u, c.y - 13f * u); lineTo(c.x + 4f * u, c.y - 4f * u); close() }, Color(0xFFC49C6C))
            drawLine(Color(0xFFD9C29A), p(-4f, 0f), p(-4f, 14f), 1.8f * u)
            drawCircle(T_Ink.copy(alpha = 0.75f), 2.2f * u, p(6f, 6f))
            drawCircle(T_Ink.copy(alpha = 0.75f), 2.2f * u, p(-9f, 8f))
            // "This side up", in tiny arrows.
            drawLine(T_WoodDark, p(8f, 12f), p(8f, 9.5f), 0.4f * u)
            drawLine(T_WoodDark, p(10f, 12f), p(10f, 9.5f), 0.4f * u)
        }
    }
}

/** A dark feather hanging from its quill at [top]; [len] is its length in pixels, tilted by [tilt] degrees. */
internal fun DrawScope.featherAt(top: Offset, len: Float, tilt: Float = 0f) {
    rotate(tilt, top) {
        val tip = Offset(top.x, top.y + len)
        // Quill and shaft.
        drawLine(T_Bone.copy(alpha = 0.85f), top, Offset(top.x, top.y + len * 0.18f), len * 0.035f, StrokeCap.Round)
        val vane = Path().apply {
            moveTo(top.x, top.y + len * 0.16f)
            cubicTo(top.x - len * 0.26f, top.y + len * 0.3f, top.x - len * 0.24f, top.y + len * 0.75f, tip.x, tip.y)
            cubicTo(top.x + len * 0.2f, top.y + len * 0.78f, top.x + len * 0.22f, top.y + len * 0.32f, top.x, top.y + len * 0.16f)
            close()
        }
        drawPath(vane, Color(0xFF221C29))
        // Barbs: fine lines angled down from the shaft, with a split on one side.
        for (i in 0..13) {
            val y = top.y + len * (0.2f + i * 0.055f)
            val w = len * 0.2f * sin(Math.PI * (0.15 + i / 15.0)).toFloat()
            val gap = i == 6 || i == 7
            drawLine(T_Lavender.copy(alpha = 0.35f), Offset(top.x, y), Offset(top.x - w, y + len * 0.06f), len * 0.008f)
            if (!gap) drawLine(T_Lavender.copy(alpha = 0.35f), Offset(top.x, y), Offset(top.x + w * 0.9f, y + len * 0.06f), len * 0.008f)
        }
        // The split in the vane.
        drawLine(Color(0xFF15111A), Offset(top.x + len * 0.02f, top.y + len * 0.53f), Offset(top.x + len * 0.18f, top.y + len * 0.6f), len * 0.018f)
        drawLine(T_Bone.copy(alpha = 0.6f), Offset(top.x, top.y + len * 0.16f), tip, len * 0.014f)
        // A little fluff at the base.
        for (i in 0..4) {
            val a = Math.toRadians(200.0 + i * 35.0)
            drawLine(Color(0xFF3B3349), Offset(top.x, top.y + len * 0.2f), Offset(top.x + cos(a).toFloat() * len * 0.1f, top.y + len * 0.2f - sin(a).toFloat() * len * 0.06f + len * 0.04f), len * 0.012f)
        }
    }
}

/** A soap bubble with a little rainbow sheen. */
internal fun DrawScope.bubble(c: Offset, r: Float, alpha: Float = 1f) {
    drawCircle(T_Lavender.copy(alpha = 0.08f * alpha), r, c)
    drawCircle(T_Bone.copy(alpha = 0.55f * alpha), r, c, style = Stroke(r * 0.08f))
    drawArc(Color(0xFF7FC6C2).copy(alpha = 0.6f * alpha), 200f, 70f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.12f))
    drawArc(Color(0xFFD9A3C8).copy(alpha = 0.6f * alpha), 20f, 60f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.12f))
    drawCircle(Color.White.copy(alpha = 0.7f * alpha), r * 0.18f, Offset(c.x - r * 0.4f, c.y - r * 0.4f))
}
