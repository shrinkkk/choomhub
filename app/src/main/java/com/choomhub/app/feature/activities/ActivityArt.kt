package com.choomhub.app.feature.activities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.feature.focus.FocusRoomArt
import com.choomhub.app.feature.focus.RoomScene
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose
import kotlin.random.Random

/** The shared ink palette for the Activities art: night wood, wine, candlelight, bone. */
internal object Ink {
    val Night = Color(0xFF0E0B12)
    val Stone = Color(0xFF1E1924)
    val StoneLine = Color(0xFF2A2331)
    val Wood = Color(0xFF3A2A26)
    val WoodLit = Color(0xFF4D3830)
    val WoodDark = Color(0xFF241A18)
    val Plum = Color(0xFF3B2F45)
    val PlumLit = Color(0xFF4C3D58)
    val Wine = Color(0xFF5C2233)
    val WineLit = Color(0xFF7A3446)
    val Bone = Color(0xFFE6DED0)
    val BoneDim = Color(0xFFB9B2A6)
    val Paper = Color(0xFFD9CDB4)
    val PaperDark = Color(0xFFB8A988)
    val Candle = Color(0xFFE6C36A)
    val Amber = Color(0xFFE89A4A)
    val Iron = Color(0xFF15111A)
    val Brass = Color(0xFFB08A4A)
}

// --- Focus room: double doors, left ajar ---

/**
 * A pointed gothic doorway with its two doors pushed slightly open; through the gap, the real
 * focus room (fire, the cat, whoever's at their desk) and a wedge of warm light on the floor.
 */
@Composable
fun FocusDoors(scene: RoomScene, inside: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.6f)) {
        val w = maxWidth
        val h = maxHeight
        // The room behind, sized so its middle (the hearth and desks) shows through the gap.
        Box(
            Modifier
                .fillMaxSize()
                .clip(GenericShape { size, _ ->
                    val gx = size.width * 0.5f
                    moveTo(gx - size.width * 0.075f, size.height * 0.22f)
                    lineTo(gx + size.width * 0.075f, size.height * 0.22f)
                    lineTo(gx + size.width * 0.075f, size.height * 0.88f)
                    lineTo(gx - size.width * 0.075f, size.height * 0.88f)
                    close()
                }),
        ) {
            FocusRoomArt(scene, Modifier.requiredSize(w * 0.62f, w * 0.62f * 188f / 120f).offset(x = w * 0.19f, y = -h * 0.2f))
        }
        Canvas(Modifier.fillMaxSize()) { doorway(inside) }
    }
}

private fun DrawScope.doorway(inside: Boolean) {
    val w = size.width
    val h = size.height
    val floor = h * 0.88f
    // Stone wall.
    val wall = Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, floor)) }
    val arch = archPath(w * 0.26f, w * 0.74f, h * 0.1f, floor)
    val wallOnly = Path().apply { op(wall, arch, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(wallOnly, Ink.Stone)
    val r = Random(3)
    var y = 0f
    var row = 0
    while (y < floor) {
        drawLine(Ink.StoneLine, Offset(0f, y), Offset(w, y), 1.5f)
        var x = if (row % 2 == 0) 0f else w * 0.06f
        while (x < w) {
            drawLine(Ink.StoneLine, Offset(x, y), Offset(x, y + h * 0.07f), 1.5f)
            x += w * (0.1f + r.nextFloat() * 0.05f)
        }
        y += h * 0.07f
        row++
    }
    // Floor.
    drawRect(Brush.verticalGradient(listOf(Ink.WoodDark, Ink.Night), startY = floor, endY = h), Offset(0f, floor), Size(w, h - floor))
    // The light spilling out through the gap.
    val spill = Path().apply {
        moveTo(w * 0.425f, floor)
        lineTo(w * 0.575f, floor)
        lineTo(w * 0.68f, h)
        lineTo(w * 0.32f, h)
        close()
    }
    drawPath(spill, Brush.verticalGradient(listOf(Ink.Amber.copy(alpha = if (inside) 0.45f else 0.25f), Ink.Amber.copy(alpha = 0f)), startY = floor, endY = h))
    // The doors: each swung a little inward, so narrower than the half it fills.
    val left = Path().apply {
        moveTo(w * 0.26f, floor)
        lineTo(w * 0.26f, h * 0.36f)
        quadraticTo(w * 0.27f, h * 0.16f, w * 0.425f, h * 0.15f)
        lineTo(w * 0.425f, floor - h * 0.02f)
        close()
    }
    val right = Path().apply {
        moveTo(w * 0.74f, floor)
        lineTo(w * 0.74f, h * 0.36f)
        quadraticTo(w * 0.73f, h * 0.16f, w * 0.575f, h * 0.15f)
        lineTo(w * 0.575f, floor - h * 0.02f)
        close()
    }
    listOf(left to true, right to false).forEach { (door, isLeft) ->
        drawPath(door, Brush.horizontalGradient(if (isLeft) listOf(Ink.Plum, Ink.PlumLit) else listOf(Ink.PlumLit, Ink.Plum), startX = w * 0.26f, endX = w * 0.74f))
        // Planks.
        for (i in 1..3) {
            val x = if (isLeft) w * (0.26f + i * 0.041f) else w * (0.74f - i * 0.041f)
            drawLine(Ink.Iron.copy(alpha = 0.45f), Offset(x, h * 0.2f), Offset(x, floor - h * 0.02f), 2f)
        }
        // Iron straps with studs.
        listOf(0.34f, 0.7f).forEach { fy ->
            val y1 = h * fy
            val x0 = if (isLeft) w * 0.26f else w * 0.575f
            val x1 = if (isLeft) w * 0.425f else w * 0.74f
            drawLine(Ink.Iron, Offset(x0, y1), Offset(x1, y1), h * 0.025f)
            var sx = x0 + w * 0.02f
            while (sx < x1) {
                drawCircle(Ink.Brass.copy(alpha = 0.7f), h * 0.007f, Offset(sx, y1))
                sx += w * 0.04f
            }
        }
        drawPath(door, Ink.Iron, style = Stroke(3f))
    }
    // Ring handles by the gap.
    listOf(w * 0.405f, w * 0.595f).forEach { x ->
        drawCircle(Ink.Brass, h * 0.035f, Offset(x, h * 0.55f), style = Stroke(h * 0.01f))
        drawCircle(Ink.Brass, h * 0.012f, Offset(x, h * 0.515f))
    }
    // A tag on the right handle: someone's in.
    if (inside) {
        val tag = Offset(w * 0.6f, h * 0.6f)
        drawLine(Ink.BoneDim, Offset(w * 0.595f, h * 0.585f), tag + Offset(w * 0.01f, 0f), 1.5f)
        rotate(8f, tag) {
            drawRoundRect(Ink.Paper, tag + Offset(-w * 0.005f, 0f), Size(w * 0.07f, h * 0.09f), CornerRadius(4f))
            drawCircle(Ink.Wine, h * 0.008f, tag + Offset(w * 0.03f, h * 0.012f))
            drawLine(Ink.Wine, tag + Offset(w * 0.01f, h * 0.045f), tag + Offset(w * 0.055f, h * 0.045f), 2f)
            drawLine(Ink.Wine, tag + Offset(w * 0.01f, h * 0.065f), tag + Offset(w * 0.045f, h * 0.065f), 2f)
        }
    }
    // The pointed arch frame and its keystone.
    drawPath(arch, Ink.WoodDark, style = Stroke(w * 0.022f))
    drawPath(archPath(w * 0.245f, w * 0.755f, h * 0.075f, floor), Ink.Wood, style = Stroke(w * 0.01f))
    drawRect(Ink.WoodLit, Offset(w * 0.485f, h * 0.05f), Size(w * 0.03f, h * 0.07f))
    // Wall sconces either side.
    listOf(w * 0.15f, w * 0.85f).forEach { x ->
        glow(Offset(x, h * 0.38f), h * 0.2f, 0.22f)
        drawRect(Ink.Iron, Offset(x - h * 0.02f, h * 0.44f), Size(h * 0.04f, h * 0.05f))
        drawRect(Ink.Bone, Offset(x - h * 0.01f, h * 0.38f), Size(h * 0.02f, h * 0.06f))
        drawOval(Ink.Candle, Offset(x - h * 0.01f, h * 0.345f), Size(h * 0.02f, h * 0.035f))
    }
}

/** Candlelight: bright at the flame, fading out. */
private fun DrawScope.glow(at: Offset, radius: Float, strength: Float) {
    drawCircle(Brush.radialGradient(listOf(Ink.Candle.copy(alpha = strength), Ink.Amber.copy(alpha = strength * 0.35f), Ink.Amber.copy(alpha = 0f)), center = at, radius = radius), radius, at)
}

private fun archPath(l: Float, r: Float, top: Float, bottom: Float) = Path().apply {
    val mid = (l + r) / 2f
    val spring = top + (bottom - top) * 0.3f
    moveTo(l, bottom)
    lineTo(l, spring)
    quadraticTo(l, top + (spring - top) * 0.2f, mid, top)
    quadraticTo(r, top + (spring - top) * 0.2f, r, spring)
    lineTo(r, bottom)
}

// --- Points shop: a market tent with the cat behind the counter ---

/** A striped market tent, lantern lit, with the cat minding the counter. */
@Composable
fun ShopTent(modifier: Modifier = Modifier) {
    val text = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.6f)) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) { tentBack() }
        // The shopkeeper: leaning on the counter, bow tie on.
        PetCat(
            PetPose.Sit,
            Modifier.size(w * 0.28f).offset(x = w * 0.36f, y = h * 0.37f),
            expression = CatExpression.Hopeful,
            outfit = PetOutfit.BowTie,
        )
        Canvas(Modifier.fillMaxSize()) { tentFront(text) }
    }
}

private val stripeA = Ink.Wine
private val stripeB = Color(0xFFCBBFA8)

private fun DrawScope.tentBack() {
    val w = size.width
    val h = size.height
    drawRect(Ink.Night)
    // Stars and a moon behind.
    val r = Random(9)
    repeat(26) { drawCircle(Ink.Bone.copy(alpha = 0.3f + r.nextFloat() * 0.4f), 1.2f + r.nextFloat(), Offset(r.nextFloat() * w, r.nextFloat() * h * 0.5f)) }
    drawCircle(Ink.Bone.copy(alpha = 0.85f), h * 0.06f, Offset(w * 0.86f, h * 0.14f))
    drawCircle(Ink.Night, h * 0.055f, Offset(w * 0.875f, h * 0.125f))
    // The inside of the tent: warm and dim.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2129), Color(0xFF1E1418)), startY = h * 0.25f, endY = h * 0.75f), Offset(w * 0.16f, h * 0.25f), Size(w * 0.68f, h * 0.5f))
    // Shelves at the back with little things on them.
    listOf(0.42f, 0.56f).forEach { fy ->
        drawRect(Ink.Wood, Offset(w * 0.2f, h * fy), Size(w * 0.6f, h * 0.015f))
    }
    val things = listOf(Ink.Candle, Ink.WineLit, Ink.PlumLit, Ink.Paper, Ink.Brass, Color(0xFF5E9C98))
    for (i in 0 until 9) {
        val x = w * (0.22f + i * 0.065f)
        val c = things[i % things.size]
        if (i % 3 == 0) drawRect(c, Offset(x, h * 0.37f), Size(w * 0.035f, h * 0.05f))
        else drawCircle(c, h * 0.022f, Offset(x + w * 0.015f, h * 0.4f))
        if (i % 2 == 0) drawRoundRect(things[(i + 2) % things.size], Offset(x, h * 0.505f), Size(w * 0.03f, h * 0.055f), CornerRadius(3f))
    }
}

private fun DrawScope.tentFront(text: TextMeasurer) {
    val w = size.width
    val h = size.height
    // Poles.
    listOf(w * 0.15f, w * 0.85f).forEach { x -> drawRect(Ink.WoodDark, Offset(x - w * 0.012f, h * 0.18f), Size(w * 0.024f, h * 0.72f)) }
    // Ground.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF17111A), Ink.Night), startY = h * 0.88f, endY = h), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
    // Counter with its sign.
    drawRect(Ink.WoodLit, Offset(w * 0.18f, h * 0.62f), Size(w * 0.64f, h * 0.04f))
    drawRect(Ink.Wood, Offset(w * 0.2f, h * 0.66f), Size(w * 0.6f, h * 0.24f))
    for (i in 1..5) drawLine(Ink.WoodDark, Offset(w * (0.2f + i * 0.1f), h * 0.66f), Offset(w * (0.2f + i * 0.1f), h * 0.9f), 2f)
    drawRoundRect(Ink.Night.copy(alpha = 0.85f), Offset(w * 0.33f, h * 0.7f), Size(w * 0.34f, h * 0.13f), CornerRadius(6f))
    drawRoundRect(Ink.Candle.copy(alpha = 0.7f), Offset(w * 0.33f, h * 0.7f), Size(w * 0.34f, h * 0.13f), CornerRadius(6f), style = Stroke(2f))
    val label = text.measure("POINTS SHOP", TextStyle(color = Ink.Candle, fontSize = (h * 0.055f / density).sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
    drawText(label, topLeft = Offset(w * 0.5f - label.size.width / 2f, h * 0.765f - label.size.height / 2f))
    // Things on the counter: a letter, a little gift, a coin pile.
    drawRect(Ink.Paper, Offset(w * 0.24f, h * 0.575f), Size(w * 0.07f, h * 0.045f))
    drawLine(Ink.PaperDark, Offset(w * 0.24f, h * 0.575f), Offset(w * 0.275f, h * 0.6f), 1.5f)
    drawLine(Ink.PaperDark, Offset(w * 0.31f, h * 0.575f), Offset(w * 0.275f, h * 0.6f), 1.5f)
    drawRect(Ink.WineLit, Offset(w * 0.69f, h * 0.565f), Size(w * 0.05f, h * 0.055f))
    drawRect(Ink.Candle, Offset(w * 0.712f, h * 0.565f), Size(w * 0.008f, h * 0.055f))
    for (i in 0..3) drawOval(Ink.Candle, Offset(w * 0.62f + i * 2f, h * (0.605f - i * 0.012f)), Size(w * 0.03f, h * 0.015f))
    // The canopy: stripes with a scalloped edge.
    val top = h * 0.06f
    val edge = h * 0.26f
    val stripes = 10
    for (i in 0 until stripes) {
        val x0 = w * 0.08f + i * w * 0.84f / stripes
        val x1 = x0 + w * 0.84f / stripes
        val apex = w * 0.5f
        val p = Path().apply {
            moveTo(apex + (x0 - apex) * 0.15f, top)
            lineTo(apex + (x1 - apex) * 0.15f, top)
            lineTo(x1, edge)
            quadraticTo((x0 + x1) / 2f, edge + h * 0.06f, x0, edge)
            close()
        }
        drawPath(p, if (i % 2 == 0) stripeA else stripeB)
    }
    drawLine(Ink.WoodDark, Offset(w * 0.08f, edge), Offset(w * 0.92f, edge), 2f)
    // Bunting across the front.
    for (i in 0 until 9) {
        val x = w * (0.18f + i * 0.08f)
        val sag = (kotlin.math.sin(i / 8f * Math.PI).toFloat()) * h * 0.015f - h * 0.03f
        val flag = Path().apply {
            moveTo(x, edge + h * 0.05f + sag)
            lineTo(x + w * 0.05f, edge + h * 0.05f + sag)
            lineTo(x + w * 0.025f, edge + h * 0.11f + sag)
            close()
        }
        drawPath(flag, listOf(Ink.Candle, Ink.PlumLit, Ink.Bone)[i % 3].copy(alpha = 0.85f))
    }
    // A lantern hanging from the left pole.
    val lx = w * 0.2f
    drawLine(Ink.Iron, Offset(w * 0.15f, h * 0.3f), Offset(lx, h * 0.3f), 2f)
    drawLine(Ink.Iron, Offset(lx, h * 0.3f), Offset(lx, h * 0.34f), 2f)
    glow(Offset(lx, h * 0.39f), h * 0.18f, 0.3f)
    drawRoundRect(Ink.Iron, Offset(lx - h * 0.03f, h * 0.34f), Size(h * 0.06f, h * 0.1f), CornerRadius(4f))
    drawRoundRect(Ink.Candle, Offset(lx - h * 0.02f, h * 0.355f), Size(h * 0.04f, h * 0.07f), CornerRadius(3f))
}

// --- Bounty board ---

/** A weathered notice board under a little roof, notices pinned up, a raven on top, a lantern on a hook. */
@Composable
fun BountyBoardArt(open: Int, modifier: Modifier = Modifier) {
    val text = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(1.6f)) { board(open, text) }
}

private fun DrawScope.board(open: Int, text: TextMeasurer) {
    val w = size.width
    val h = size.height
    drawRect(Ink.Night)
    val r = Random(5)
    repeat(20) { drawCircle(Ink.Bone.copy(alpha = 0.25f + r.nextFloat() * 0.3f), 1.2f, Offset(r.nextFloat() * w, r.nextFloat() * h * 0.4f)) }
    // Ground and grass tufts.
    drawRect(Color(0xFF15111A), Offset(0f, h * 0.9f), Size(w, h * 0.1f))
    repeat(16) {
        val x = r.nextFloat() * w
        drawLine(Color(0xFF3E4A3B), Offset(x, h * 0.92f), Offset(x + 3f, h * 0.88f), 2f)
    }
    // Posts.
    listOf(w * 0.2f, w * 0.8f).forEach { x -> drawRect(Ink.WoodDark, Offset(x - w * 0.015f, h * 0.2f), Size(w * 0.03f, h * 0.72f)) }
    // Roof.
    val roof = Path().apply {
        moveTo(w * 0.12f, h * 0.24f)
        lineTo(w * 0.5f, h * 0.06f)
        lineTo(w * 0.88f, h * 0.24f)
        lineTo(w * 0.84f, h * 0.27f)
        lineTo(w * 0.5f, h * 0.11f)
        lineTo(w * 0.16f, h * 0.27f)
        close()
    }
    drawPath(roof, Ink.Wood)
    drawPath(roof, Ink.WoodDark, style = Stroke(2f))
    // The board.
    drawRect(Ink.Wood, Offset(w * 0.22f, h * 0.27f), Size(w * 0.56f, h * 0.5f))
    for (i in 1..4) drawLine(Ink.WoodDark, Offset(w * 0.22f, h * (0.27f + i * 0.1f)), Offset(w * 0.78f, h * (0.27f + i * 0.1f)), 2f)
    drawRect(Ink.WoodDark, Offset(w * 0.22f, h * 0.27f), Size(w * 0.56f, h * 0.5f), style = Stroke(5f))
    // Header plank.
    drawRect(Ink.WoodLit, Offset(w * 0.3f, h * 0.16f), Size(w * 0.4f, h * 0.085f))
    drawRect(Ink.WoodDark, Offset(w * 0.3f, h * 0.16f), Size(w * 0.4f, h * 0.085f), style = Stroke(2f))
    val title = text.measure("BOUNTIES", TextStyle(color = Ink.Bone, fontSize = (h * 0.05f / density).sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
    drawText(title, topLeft = Offset(w * 0.5f - title.size.width / 2f, h * 0.2f - title.size.height / 2f))
    // Notices.
    val notices = listOf(Triple(0.3f, 0.34f, -6f), Triple(0.45f, 0.31f, 3f), Triple(0.6f, 0.35f, -2f), Triple(0.37f, 0.53f, 5f), Triple(0.55f, 0.54f, -4f))
    notices.forEachIndexed { i, (fx, fy, angle) ->
        val tl = Offset(w * fx, h * fy)
        val sz = Size(w * 0.12f, h * 0.18f)
        rotate(angle, tl + Offset(sz.width / 2f, 0f)) {
            drawRect(Ink.Night.copy(alpha = 0.35f), tl + Offset(3f, 4f), sz)
            drawRect(if (i < open || i == 4) Ink.Paper else Ink.PaperDark, tl, sz)
            for (l in 0..3) drawLine(Ink.PaperDark, tl + Offset(sz.width * 0.15f, sz.height * (0.35f + l * 0.14f)), tl + Offset(sz.width * (0.85f - (l % 2) * 0.2f), sz.height * (0.35f + l * 0.14f)), 1.5f)
            drawLine(Ink.Wine, tl + Offset(sz.width * 0.2f, sz.height * 0.18f), tl + Offset(sz.width * 0.8f, sz.height * 0.18f), 3f)
            drawCircle(Ink.WineLit, 4f, tl + Offset(sz.width / 2f, 6f))
            if (i == 4) {
                // One's been paid: stamped.
                rotate(-18f, tl + Offset(sz.width / 2f, sz.height / 2f)) {
                    drawRoundRect(Color(0xFFE0453A).copy(alpha = 0.8f), tl + Offset(sz.width * 0.05f, sz.height * 0.4f), Size(sz.width * 0.9f, sz.height * 0.22f), CornerRadius(3f), style = Stroke(2.5f))
                }
            }
        }
    }
    // Lantern on the right post.
    val lx = w * 0.84f
    drawLine(Ink.Iron, Offset(w * 0.8f, h * 0.42f), Offset(lx, h * 0.42f), 2f)
    glow(Offset(lx, h * 0.49f), h * 0.2f, 0.28f)
    drawRoundRect(Ink.Iron, Offset(lx - h * 0.03f, h * 0.44f), Size(h * 0.06f, h * 0.1f), CornerRadius(4f))
    drawRoundRect(Ink.Candle, Offset(lx - h * 0.02f, h * 0.455f), Size(h * 0.04f, h * 0.07f), CornerRadius(3f))
    // A raven on the roof's peak.
    val rv = Offset(w * 0.56f, h * 0.085f)
    drawOval(Ink.Iron, rv + Offset(-w * 0.02f, -h * 0.05f), Size(w * 0.045f, h * 0.06f))
    drawCircle(Ink.Iron, h * 0.022f, rv + Offset(w * 0.022f, -h * 0.06f))
    drawPath(Path().apply { moveTo(rv.x + w * 0.035f, rv.y - h * 0.062f); lineTo(rv.x + w * 0.05f, rv.y - h * 0.055f); lineTo(rv.x + w * 0.035f, rv.y - h * 0.05f); close() }, Ink.BoneDim)
    drawPath(Path().apply { moveTo(rv.x - w * 0.02f, rv.y - h * 0.02f); lineTo(rv.x - w * 0.045f, rv.y + h * 0.005f); lineTo(rv.x - w * 0.012f, rv.y - h * 0.005f); close() }, Ink.Iron)
    drawCircle(Ink.Candle, 1.8f, rv + Offset(w * 0.026f, -h * 0.065f))
    drawLine(Ink.Iron, rv + Offset(0f, 0f), rv + Offset(0f, h * 0.012f), 2f, StrokeCap.Round)
}
