package com.choomhub.app.feature.pet.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

// Wardrobe colours: muted to sit with the night palette.
private val W_Ink = Color(0xFF0C0A0E)
private val W_Bone = Color(0xFFE6DED0)
private val W_BoneDim = Color(0xFFB9B2A6)
private val W_Wine = Color(0xFF5C2233)
private val W_WineLit = Color(0xFF7A3446)
private val W_Plum = Color(0xFF4A3F5C)
private val W_PlumLit = Color(0xFF5F5174)
private val W_Moss = Color(0xFF55604F)
private val W_Sage = Color(0xFF7D9C76)
private val W_Mustard = Color(0xFFC9A04A)
private val W_MustardDark = Color(0xFF8E6E2E)
private val W_Teal = Color(0xFF3E6B68)
private val W_TealLit = Color(0xFF5E9C98)
private val W_Rose = Color(0xFFD27A9A)
private val W_Blush = Color(0xFFEDB3C6)
private val W_Khaki = Color(0xFF6F6A4C)
private val W_KhakiLit = Color(0xFF8A845E)
private val W_Rust = Color(0xFFA8502A)
private val W_Steel = Color(0xFF8C8794)

private fun f(headAt: Offset, size: Float, x: Float, y: Float) = Offset(headAt.x + size * x / 100f, headAt.y + size * y / 100f)

// --- Body ---

/** Clothes over the body and the tops of the front legs (so they read as sleeves). */
internal fun DrawScope.garment(o: PetOutfit, body: Path, legXs: List<Float>, half: Float, legTop: Float, headAt: Offset, headSize: Float) {
    val bounds = body.getBounds()
    val neckY = headAt.y + headSize * 0.78f
    val torso = Path().apply {
        op(body, Path().apply { legXs.forEach { addRect(Rect(it - half - 0.6f, legTop - 2f, it + half + 0.6f, 97f)) } }, PathOperation.Union)
    }
    fun band(top: Float, bottom: Float, block: DrawScope.() -> Unit) {
        val clip = Path().apply { op(torso, Path().apply { addRect(Rect(0f, top, 100f, bottom)) }, PathOperation.Intersect) }
        clipPath(clip) { block() }
        drawPath(clip, W_Ink.copy(alpha = 0.6f), style = Stroke(0.6f))
    }
    when (o) {
        PetOutfit.Jumper -> {
            val bottom = legTop + 14f
            band(neckY - 2f, bottom) {
                drawRect(W_Wine, Offset(0f, neckY - 2f), Size(100f, bottom - neckY + 2f))
                var y = neckY + 4f
                while (y < bottom) {
                    drawRect(W_Bone.copy(alpha = 0.85f), Offset(0f, y), Size(100f, 1.8f))
                    y += 5f
                }
            }
            // Ribbed collar and cuffs.
            drawRoundRect(W_WineLit, Offset(bounds.center.x - 7f, neckY - 3f), Size(14f, 3.2f), CornerRadius(1.5f))
            legXs.forEach { drawRect(W_WineLit, Offset(it - half - 0.6f, bottom - 2.5f), Size(half * 2 + 1.2f, 2.5f)) }
        }
        PetOutfit.Hoodie -> {
            val bottom = legTop + 16f
            band(neckY - 2f, bottom) { drawRect(W_Plum, Offset(0f, neckY - 2f), Size(100f, bottom - neckY + 2f)) }
            legXs.forEach { drawRect(W_PlumLit, Offset(it - half - 0.6f, bottom - 2.5f), Size(half * 2 + 1.2f, 2.5f)) }
            // Drawstrings and the front pocket.
            listOf(-2.2f, 2.2f).forEach { dx ->
                drawLine(W_Bone, Offset(bounds.center.x + dx, neckY), Offset(bounds.center.x + dx * 1.3f, neckY + 9f), 0.6f, StrokeCap.Round)
                drawCircle(W_Bone, 0.7f, Offset(bounds.center.x + dx * 1.3f, neckY + 9.4f))
            }
            drawRoundRect(W_PlumLit, Offset(bounds.center.x - 8f, legTop + 4f), Size(16f, 7f), CornerRadius(2f))
            drawRoundRect(W_Ink.copy(alpha = 0.5f), Offset(bounds.center.x - 8f, legTop + 4f), Size(16f, 7f), CornerRadius(2f), style = Stroke(0.5f))
        }
        PetOutfit.Raincoat -> {
            val bottom = 93f
            band(neckY - 2f, bottom) {
                drawRect(W_Mustard, Offset(0f, neckY - 2f), Size(100f, bottom - neckY + 2f))
                drawLine(W_Bone.copy(alpha = 0.3f), Offset(bounds.left + 4f, neckY + 6f), Offset(bounds.left + 2f, bottom - 6f), 1.2f)
            }
            drawLine(W_MustardDark, Offset(bounds.center.x, neckY), Offset(bounds.center.x, bottom), 0.7f)
            for (i in 0..3) drawCircle(W_MustardDark, 0.9f, Offset(bounds.center.x + 1.8f, neckY + 5f + i * 7f))
            // Collar points.
            listOf(-1f, 1f).forEach { side ->
                drawPath(Path().apply {
                    moveTo(bounds.center.x, neckY - 1f)
                    lineTo(bounds.center.x + side * 7f, neckY - 2f)
                    lineTo(bounds.center.x + side * 3f, neckY + 5f)
                    close()
                }, W_MustardDark)
            }
        }
        PetOutfit.Tutu -> {
            val y = legTop + 20f
            for (layer in 0..1) {
                val p = Path().apply {
                    val left = bounds.left - 6f + layer * 2f
                    val right = bounds.right + 6f - layer * 2f
                    moveTo(left + 4f, y - 4f)
                    lineTo(right - 4f, y - 4f)
                    var x = right
                    lineTo(x, y + 4f - layer * 2f)
                    while (x > left) {
                        quadraticTo(x - 2.5f, y + 8f - layer * 2f, x - 5f, y + 4f - layer * 2f)
                        x -= 5f
                    }
                    close()
                }
                drawPath(p, (if (layer == 0) W_Blush else W_Rose).copy(alpha = 0.75f))
            }
            drawRect(W_Rose, Offset(bounds.left + 2f, y - 5f), Size(bounds.width - 4f, 2f))
        }
        PetOutfit.FieldVest -> {
            val bottom = legTop + 12f
            band(neckY - 1f, bottom) {
                // Two front panels, open in the middle.
                drawRect(W_Khaki, Offset(0f, neckY - 1f), Size(bounds.center.x - 2.5f, bottom - neckY + 1f))
                drawRect(W_Khaki, Offset(bounds.center.x + 2.5f, neckY - 1f), Size(100f, bottom - neckY + 1f))
            }
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(W_KhakiLit, Offset(bounds.center.x + side * 6f - 3f, legTop - 2f), Size(6f, 5f), CornerRadius(1f))
                drawLine(W_Ink.copy(alpha = 0.6f), Offset(bounds.center.x + side * 6f - 3f, legTop - 0.5f), Offset(bounds.center.x + side * 6f + 3f, legTop - 0.5f), 0.4f)
            }
            // A rock hammer hanging off the side.
            val hx = bounds.right - 1f
            drawLine(Color(0xFF6B4A36), Offset(hx, legTop), Offset(hx + 2f, legTop + 16f), 1.4f, StrokeCap.Round)
            drawRoundRect(W_Steel, Offset(hx - 3f, legTop - 1.5f), Size(7f, 2.6f), CornerRadius(0.6f))
        }
        else -> Unit
    }
}

/** Clothes on the loaf: the tucked body under a blanket of whatever it's wearing. */
internal fun DrawScope.loafGarment(o: PetOutfit, body: Path) {
    val b = body.getBounds()
    val (fill, accent) = when (o) {
        PetOutfit.Jumper -> W_Wine to W_Bone
        PetOutfit.Hoodie -> W_Plum to W_PlumLit
        PetOutfit.Raincoat -> W_Mustard to W_MustardDark
        PetOutfit.FieldVest -> W_Khaki to W_KhakiLit
        PetOutfit.Tutu -> W_Blush to W_Rose
        else -> return
    }
    val top = if (o == PetOutfit.Tutu) b.bottom - 12f else b.top + 10f
    clipPath(body) {
        drawRect(fill, Offset(b.left - 2f, top), Size(b.width + 4f, b.bottom - top - 2f))
        when (o) {
            PetOutfit.Jumper -> { var y = top + 5f; while (y < b.bottom - 3f) { drawRect(accent.copy(alpha = 0.85f), Offset(b.left - 2f, y), Size(b.width + 4f, 1.8f)); y += 5f } }
            PetOutfit.Raincoat -> for (i in 0..3) drawCircle(accent, 0.9f, Offset(b.center.x, top + 5f + i * 7f))
            PetOutfit.Hoodie -> drawRoundRect(accent, Offset(b.center.x - 8f, b.bottom - 16f), Size(16f, 7f), CornerRadius(2f))
            PetOutfit.FieldVest -> drawRect(W_Ink.copy(alpha = 0.9f), Offset(b.center.x - 2.5f, top), Size(5f, b.bottom - top))
            else -> Unit
        }
    }
    if (o == PetOutfit.Tutu) {
        var x = b.left - 4f
        while (x < b.right + 4f) { drawOval(accent.copy(alpha = 0.7f), Offset(x, b.bottom - 6f), Size(5f, 4f)); x += 4f }
    }
}

/** The hoodie's hood, bunched behind the neck (drawn before the head). */
internal fun DrawScope.hood(headAt: Offset, size: Float) {
    val c = f(headAt, size, 50f, 80f)
    drawOval(W_Plum, Offset(c.x - size * 0.3f, c.y - size * 0.1f), Size(size * 0.6f, size * 0.22f))
    drawOval(W_PlumLit, Offset(c.x - size * 0.24f, c.y - size * 0.07f), Size(size * 0.48f, size * 0.08f))
}

// --- Feet ---

internal fun DrawScope.footwear(o: PetOutfit, legXs: List<Float>, half: Float) {
    legXs.forEach { x ->
        when (o) {
            PetOutfit.StripedSocks -> {
                for (i in 0..5) drawRect(if (i % 2 == 0) W_Wine else W_Bone, Offset(x - half - 0.4f, 82f + i * 2.6f), Size(half * 2 + 0.8f, 2.6f))
                drawOval(W_Wine, Offset(x - half - 1.5f, 94f), Size(half * 2 + 3f, 4.5f))
                drawRect(W_WineLit, Offset(x - half - 0.6f, 81f), Size(half * 2 + 1.2f, 1.6f))
            }
            PetOutfit.RainBoots -> {
                drawRoundRect(W_Mustard, Offset(x - half - 1f, 83f), Size(half * 2 + 2f, 13f), CornerRadius(1.2f))
                drawRoundRect(W_Mustard, Offset(x - half - 1.5f, 92f), Size(half * 2 + 5f, 5f), CornerRadius(2f))
                drawRect(W_MustardDark, Offset(x - half - 1.5f, 96f), Size(half * 2 + 5f, 1.4f))
                drawLine(W_Bone.copy(alpha = 0.45f), Offset(x - half + 0.3f, 85f), Offset(x - half + 0.3f, 91f), 0.6f)
            }
            PetOutfit.BunnySlippers -> {
                drawOval(W_Bone, Offset(x - half - 2.5f, 91.5f), Size(half * 2 + 5f, 6.5f))
                listOf(-1.4f, 1.4f).forEach { dx -> drawOval(W_Bone, Offset(x + dx - 1f, 85.5f), Size(2f, 7f)) }
                listOf(-1.4f, 1.4f).forEach { dx -> drawOval(W_Blush, Offset(x + dx - 0.45f, 86.5f), Size(0.9f, 4.5f)) }
                drawCircle(W_Ink, 0.45f, Offset(x - 1.2f, 93.2f))
                drawCircle(W_Ink, 0.45f, Offset(x + 1.2f, 93.2f))
                drawCircle(W_Rose, 0.5f, Offset(x, 94.6f))
            }
            PetOutfit.Sneakers -> {
                drawRoundRect(W_Bone, Offset(x - half - 1.5f, 90f), Size(half * 2 + 4.5f, 6.5f), CornerRadius(2.5f))
                drawRect(W_Ink, Offset(x - half - 1.5f, 95.5f), Size(half * 2 + 4.5f, 1.6f))
                drawLine(W_TealLit, Offset(x - half - 0.5f, 94f), Offset(x + half + 2f, 91.5f), 0.9f)
                for (i in 0..1) drawLine(W_Ink.copy(alpha = 0.6f), Offset(x - 1.2f, 91f + i * 1.4f), Offset(x + 1.2f, 91f + i * 1.4f), 0.4f)
            }
            PetOutfit.ChunkyBoots -> {
                drawRoundRect(Color(0xFF16121A), Offset(x - half - 1f, 84f), Size(half * 2 + 2f, 12f), CornerRadius(1f))
                drawRoundRect(Color(0xFF16121A), Offset(x - half - 1.8f, 92f), Size(half * 2 + 5f, 5.5f), CornerRadius(1.6f))
                drawRect(Color(0xFF2A2430), Offset(x - half - 1.8f, 96f), Size(half * 2 + 5f, 2f))
                drawLine(W_Mustard, Offset(x - half - 1.8f, 95.6f), Offset(x + half + 3.2f, 95.6f), 0.35f)
                for (i in 0..2) drawLine(W_BoneDim, Offset(x - 1.4f, 85.5f + i * 2f), Offset(x + 1.4f, 86.5f + i * 2f), 0.4f)
            }
            PetOutfit.KnitBooties -> {
                drawRoundRect(W_Rose, Offset(x - half - 1.2f, 88f), Size(half * 2 + 2.4f, 9f), CornerRadius(3f))
                for (r in 0..2) for (k in 0..2) drawArc(W_Blush, 0f, 180f, false, Offset(x - half - 0.6f + k * (half * 2 + 1.2f) / 3f, 89.5f + r * 2.4f), Size((half * 2 + 1.2f) / 3f, 1.6f), style = Stroke(0.35f))
                drawCircle(W_Bone, 1.3f, Offset(x, 87.6f))
            }
            PetOutfit.LegWarmers -> {
                drawRoundRect(W_Plum, Offset(x - half - 1f, 76f), Size(half * 2 + 2f, 16f), CornerRadius(1.2f))
                for (i in 0..6) drawLine(W_PlumLit, Offset(x - half - 0.6f + i * (half * 2 + 1.2f) / 6f, 76.5f), Offset(x - half - 0.6f + i * (half * 2 + 1.2f) / 6f, 91.5f), 0.5f)
            }
            PetOutfit.GardenClogs -> {
                drawRoundRect(W_Teal, Offset(x - half - 2f, 89.5f), Size(half * 2 + 5f, 7.5f), CornerRadius(3.5f))
                for (i in 0..2) drawCircle(W_Ink.copy(alpha = 0.55f), 0.5f, Offset(x - half + 0.5f + i * 2.2f, 91.6f))
                drawLine(W_TealLit, Offset(x - half - 1f, 96.4f), Offset(x + half + 2.5f, 96.4f), 0.8f)
            }
            else -> Unit
        }
    }
}

// --- Head, face, neck ---

/** The newer head, face and neck items. */
internal fun DrawScope.wearMore(o: PetOutfit, headAt: Offset, size: Float) {
    when (o) {
        PetOutfit.Beanie -> {
            val c = f(headAt, size, 50f, 30f)
            val dome = Path().apply {
                moveTo(c.x - size * 0.3f, c.y + 2f)
                cubicTo(c.x - size * 0.28f, c.y - size * 0.3f, c.x + size * 0.28f, c.y - size * 0.3f, c.x + size * 0.3f, c.y + 2f)
                close()
            }
            drawPath(dome, W_Teal)
            for (i in -4..4) drawLine(W_TealLit, Offset(c.x + i * size * 0.06f, c.y + 1f), Offset(c.x + i * size * 0.045f, c.y - size * 0.19f), 0.6f)
            drawRoundRect(W_TealLit, Offset(c.x - size * 0.31f, c.y - 1.5f), Size(size * 0.62f, 5.5f), CornerRadius(2.5f))
            drawCircle(W_Bone, 3.4f, Offset(c.x, c.y - size * 0.25f))
        }
        PetOutfit.PartyHat -> {
            val b = f(headAt, size, 56f, 22f)
            rotate(14f, b) {
                val cone = Path().apply { moveTo(b.x - size * 0.11f, b.y); lineTo(b.x, b.y - size * 0.27f); lineTo(b.x + size * 0.11f, b.y); close() }
                drawPath(cone, W_Plum)
                clipPath(cone) { for (i in 0..4) drawLine(W_Mustard, Offset(b.x - size * 0.2f, b.y - i * size * 0.08f), Offset(b.x + size * 0.2f, b.y - i * size * 0.08f - size * 0.05f), 1.6f) }
                drawCircle(W_Rose, 2.2f, Offset(b.x, b.y - size * 0.27f))
                drawLine(W_Bone.copy(alpha = 0.6f), Offset(b.x - size * 0.11f, b.y), Offset(b.x - size * 0.16f, b.y + size * 0.3f), 0.4f)
            }
        }
        PetOutfit.BunnyEars -> {
            val band = Path().apply {
                val l = f(headAt, size, 22f, 30f)
                val r = f(headAt, size, 78f, 30f)
                moveTo(l.x, l.y)
                quadraticTo(f(headAt, size, 50f, 2f).x, f(headAt, size, 50f, 2f).y, r.x, r.y)
            }
            drawPath(band, W_Rose, style = Stroke(1.6f, cap = StrokeCap.Round))
            listOf(40f to -12f, 60f to 12f).forEach { (x, a) ->
                val base = f(headAt, size, x, 10f)
                rotate(a, base) {
                    drawOval(W_Bone, Offset(base.x - size * 0.055f, base.y - size * 0.3f), Size(size * 0.11f, size * 0.32f))
                    drawOval(W_Blush, Offset(base.x - size * 0.028f, base.y - size * 0.25f), Size(size * 0.056f, size * 0.22f))
                }
            }
        }
        PetOutfit.FrogHat -> {
            val c = f(headAt, size, 50f, 24f)
            drawArc(W_Sage, 180f, 180f, true, Offset(c.x - size * 0.24f, c.y - size * 0.2f), Size(size * 0.48f, size * 0.36f))
            drawRect(W_Moss, Offset(c.x - size * 0.24f, c.y - 1.5f), Size(size * 0.48f, 3f))
            listOf(-1f, 1f).forEach { side ->
                val e = Offset(c.x + side * size * 0.13f, c.y - size * 0.2f)
                drawCircle(W_Sage, size * 0.075f, e)
                drawCircle(W_Bone, size * 0.05f, e)
                drawCircle(W_Ink, size * 0.025f, e + Offset(side * 0.6f, 0.4f))
            }
            drawArc(W_Moss, 20f, 140f, false, Offset(c.x - 4f, c.y - size * 0.12f), Size(8f, 4f), style = Stroke(0.6f))
        }
        PetOutfit.MushroomCap -> {
            val c = f(headAt, size, 50f, 18f)
            drawArc(W_Wine, 180f, 180f, true, Offset(c.x - size * 0.3f, c.y - size * 0.22f), Size(size * 0.6f, size * 0.44f))
            drawRect(W_BoneDim, Offset(c.x - size * 0.28f, c.y - 1f), Size(size * 0.56f, 2.2f))
            listOf(-0.16f to -0.08f, 0.04f to -0.14f, 0.18f to -0.05f, -0.03f to -0.04f).forEach { (dx, dy) ->
                drawCircle(W_Bone, size * 0.03f, Offset(c.x + size * dx, c.y + size * dy))
            }
        }
        PetOutfit.HardHat -> {
            val c = f(headAt, size, 50f, 22f)
            drawArc(W_Mustard, 180f, 180f, true, Offset(c.x - size * 0.22f, c.y - size * 0.22f), Size(size * 0.44f, size * 0.42f))
            drawRoundRect(W_Mustard, Offset(c.x - size * 0.3f, c.y - 1.5f), Size(size * 0.6f, 3.5f), CornerRadius(1.6f))
            drawLine(W_MustardDark, Offset(c.x, c.y - size * 0.21f), Offset(c.x, c.y - 1.5f), 1.2f)
            // A headlamp.
            drawRoundRect(W_Ink, Offset(c.x - 3f, c.y - size * 0.1f), Size(6f, 4f), CornerRadius(1f))
            drawCircle(Color(0xFFF1E0A6), 1.4f, Offset(c.x, c.y - size * 0.1f + 2f))
        }
        PetOutfit.RoundGlasses -> {
            listOf(39f, 61f).forEach { x ->
                val p = f(headAt, size, x, 44f)
                drawCircle(W_Mustard, size * 0.095f, p, style = Stroke(0.9f))
                drawCircle(W_Bone.copy(alpha = 0.12f), size * 0.09f, p)
            }
            drawArc(W_Mustard, 200f, 140f, false, f(headAt, size, 47f, 40f), Size(size * 0.06f, size * 0.05f), style = Stroke(0.8f))
        }
        PetOutfit.Monocle -> {
            val p = f(headAt, size, 61f, 44f)
            drawCircle(W_Mustard, size * 0.1f, p, style = Stroke(1f))
            drawCircle(W_Bone.copy(alpha = 0.14f), size * 0.095f, p)
            val chain = Path().apply {
                moveTo(p.x + size * 0.06f, p.y + size * 0.08f)
                quadraticTo(p.x + size * 0.14f, p.y + size * 0.3f, p.x + size * 0.04f, p.y + size * 0.4f)
            }
            drawPath(chain, W_Mustard, style = Stroke(0.45f))
        }
        PetOutfit.HeartGlasses -> {
            listOf(39f, 61f).forEach { x ->
                val p = f(headAt, size, x, 44f)
                val r = size * 0.055f
                val heart = Path().apply {
                    moveTo(p.x, p.y + r * 1.4f)
                    cubicTo(p.x - r * 2.2f, p.y + r * 0.2f, p.x - r * 1.2f, p.y - r * 1.5f, p.x, p.y - r * 0.4f)
                    cubicTo(p.x + r * 1.2f, p.y - r * 1.5f, p.x + r * 2.2f, p.y + r * 0.2f, p.x, p.y + r * 1.4f)
                    close()
                }
                drawPath(heart, W_Rose.copy(alpha = 0.9f))
                drawPath(heart, W_Wine, style = Stroke(0.6f))
            }
            drawLine(W_Wine, f(headAt, size, 47f, 42f), f(headAt, size, 53f, 42f), 0.7f)
        }
        PetOutfit.PearlNecklace -> {
            val c = f(headAt, size, 50f, 74f)
            for (i in 0..10) {
                val a = Math.toRadians(20.0 + i * 14.0)
                val p = Offset(c.x + cos(a).toFloat() * size * 0.2f, c.y + sin(a).toFloat() * size * 0.08f)
                drawCircle(W_Bone, 1.25f, p)
                drawCircle(Color.White.copy(alpha = 0.6f), 0.4f, p - Offset(0.4f, 0.4f))
            }
        }
        PetOutfit.FlowerLei -> {
            val c = f(headAt, size, 50f, 76f)
            for (i in 0..8) {
                val a = Math.toRadians(10.0 + i * 20.0)
                val p = Offset(c.x + cos(a).toFloat() * size * 0.23f, c.y + sin(a).toFloat() * size * 0.1f)
                val col = listOf(W_Rose, W_Mustard, W_Blush)[i % 3]
                for (k in 0..4) {
                    val pa = Math.toRadians(k * 72.0)
                    drawCircle(col, 1.1f, p + Offset(cos(pa).toFloat() * 1.3f, sin(pa).toFloat() * 1.3f))
                }
                drawCircle(W_Bone, 0.6f, p)
            }
        }
        PetOutfit.TinyCamera -> {
            val c = f(headAt, size, 50f, 78f)
            drawLine(W_Wine, Offset(c.x - size * 0.16f, c.y - 2f), Offset(c.x - 3f, c.y + 7f), 0.9f)
            drawLine(W_Wine, Offset(c.x + size * 0.16f, c.y - 2f), Offset(c.x + 3f, c.y + 7f), 0.9f)
            drawRoundRect(Color(0xFF1A161E), Offset(c.x - 5f, c.y + 6f), Size(10f, 6.5f), CornerRadius(1.2f))
            drawRect(W_BoneDim, Offset(c.x - 5f, c.y + 6f), Size(10f, 1.6f))
            drawCircle(W_Steel, 2.1f, Offset(c.x, c.y + 9.6f))
            drawCircle(W_Ink, 1.2f, Offset(c.x, c.y + 9.6f))
            drawCircle(W_Bone, 0.4f, Offset(c.x - 0.4f, c.y + 9.2f))
        }
        else -> Unit
    }
}
