package com.choomhub.app.feature.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.illustration.InkPalette
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.core.designsystem.theme.LocalSkyPhase
import com.choomhub.app.core.designsystem.theme.SkyPhase
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetBuild
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.random.Random

/** Where in the apartment the cat is. */
enum class CatSpot { Bed, Window, Tree, Box, Rug, Food, Water, Litter, Hiding, Center }

/** Where the cat is and what it's doing. */
data class CatPlacement(val spot: CatSpot, val pose: PetPose, val expression: CatExpression, val facingLeft: Boolean = false)

/** The apartment's size in drawing units. */
private const val W = 100f
private const val H = 92f
private const val FLOOR = 60f

/** How it's feeling, as a face. */
fun petExpression(state: PetState, now: Instant): CatExpression {
    val w = PetRules.wellbeing(state, now)
    return when {
        w > 75 -> CatExpression.Happy
        w > 55 -> CatExpression.Hopeful
        w > 35 -> CatExpression.Bored
        w > 18 -> CatExpression.Sad
        else -> CatExpression.Crying
    }
}

/**
 * What the cat does when you look: whatever it needs most shows (sat by the empty bowl, asleep
 * when tired, hiding when unloved, poorly in bed); otherwise it potters between its favourite
 * spots, a new one every 40 seconds (the same on both phones).
 */
fun catPlacement(state: PetState, now: Instant): CatPlacement {
    val l = PetRules.levels(state, now)
    val face = petExpression(state, now)
    return when {
        state.sick -> CatPlacement(CatSpot.Bed, PetPose.Sick, CatExpression.Sad)
        l.getValue(PetNeed.Affection) < 12 -> CatPlacement(CatSpot.Hiding, PetPose.Sit, CatExpression.Sad)
        l.getValue(PetNeed.Energy) < 25 -> CatPlacement(CatSpot.Bed, PetPose.Asleep, face)
        l.getValue(PetNeed.Hunger) < 20 -> CatPlacement(CatSpot.Food, PetPose.Sit, CatExpression.Sad)
        l.getValue(PetNeed.Thirst) < 20 -> CatPlacement(CatSpot.Water, PetPose.Sit, CatExpression.Hopeful)
        l.getValue(PetNeed.Litter) < 20 -> CatPlacement(CatSpot.Litter, PetPose.Sit, CatExpression.Bored)
        l.getValue(PetNeed.Fun) < 20 -> CatPlacement(CatSpot.Rug, PetPose.Pounce, CatExpression.Bored)
        else -> {
            val options = buildList {
                add(CatPlacement(CatSpot.Bed, PetPose.Loaf, face))
                add(CatPlacement(CatSpot.Window, if (state.has("perch")) PetPose.Loaf else PetPose.Sit, face))
                add(CatPlacement(CatSpot.Tree, if (state.has("tree")) PetPose.Loaf else PetPose.Sit, face))
                add(CatPlacement(CatSpot.Rug, PetPose.BellyUp, face))
                add(CatPlacement(CatSpot.Rug, PetPose.Grooming, face))
                add(CatPlacement(CatSpot.Center, PetPose.Sit, face))
                if (state.has("box")) add(CatPlacement(CatSpot.Box, PetPose.Loaf, face))
                // Dressed up? It sits so you can see.
                if (state.outfit != null) {
                    add(CatPlacement(CatSpot.Center, PetPose.Sit, face))
                    add(CatPlacement(CatSpot.Rug, PetPose.Sit, face))
                }
            }
            options[Random(now.epochSecond / 40).nextInt(options.size)]
        }
    }
}

/** Floor x of each spot (where the cat walks to), and where it sits: bottom centre and size, in units. */
private fun spotFloorX(spot: CatSpot) = when (spot) {
    CatSpot.Bed, CatSpot.Hiding -> 43f
    CatSpot.Window -> 17f
    CatSpot.Tree -> 88f
    CatSpot.Box -> 62f
    CatSpot.Rug -> 50f
    CatSpot.Food -> 58f
    CatSpot.Water -> 87f
    CatSpot.Litter -> 26f
    CatSpot.Center -> 56f
}

private fun spotAnchor(spot: CatSpot, state: PetState): Pair<Offset, Float> = when (spot) {
    CatSpot.Bed -> Offset(43f, 78f) to 23f
    CatSpot.Hiding -> Offset(43f, 80f) to 23f
    CatSpot.Window -> Offset(17f, 41f) to 17f
    CatSpot.Tree -> (if (state.has("tree")) Offset(89f, 16f) else Offset(89f, 46f)) to 17f
    CatSpot.Box -> Offset(62f, 76f) to 18f
    CatSpot.Rug -> Offset(50f, 90f) to 25f
    CatSpot.Food -> Offset(58f, 89f) to 24f
    CatSpot.Water -> Offset(87f, 90f) to 22f
    CatSpot.Litter -> Offset(28f, 87f) to 21f
    CatSpot.Center -> Offset(54f, 90f) to 25f
}

/**
 * The cat's apartment, drawn in the app's ink style with whatever you've bought, and the cat in
 * it doing its thing. Tapping the cat calls [onCatTap].
 */
@Composable
fun PetApartment(state: PetState, now: Instant, modifier: Modifier = Modifier, onCatTap: () -> Unit = {}) {
    val sky = LocalSkyPhase.current
    val animate = LocalAmbientMotionEnabled.current
    var frame by remember { mutableIntStateOf(0) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(180)
                frame++
            }
        }
    }
    val placement = catPlacement(state, now)
    // When it changes spot, it walks there along the floor first.
    var shown by remember { mutableStateOf(placement) }
    val walk = remember { Animatable(1f) }
    var walkFrom by remember { mutableFloatStateOf(spotFloorX(placement.spot)) }
    LaunchedEffect(placement.spot, placement.pose) {
        if (placement.spot != shown.spot && animate) {
            walkFrom = spotFloorX(shown.spot)
            walk.snapTo(0f)
            shown = placement
            walk.animateTo(1f, tween(2_200, easing = LinearEasing))
        } else {
            shown = placement
        }
    }
    BoxWithConstraints(modifier.aspectRatio(W / H).testTag("pet_apartment")) {
        val unit = maxWidth / W
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width / W
            scale(s, s, pivot = Offset.Zero) { drawRoom(state, now, sky, frame) }
        }
        val build = when {
            PetRules.isKitten(state, now) -> PetBuild.Kitten
            PetRules.isChonky(state) -> PetBuild.Chonky
            else -> PetBuild.Cat
        }
        val walking = walk.value < 1f
        val (anchor, catSize) = if (walking) {
            val x = walkFrom + (spotFloorX(shown.spot) - walkFrom) * walk.value
            Offset(x, 90f) to 21f
        } else {
            spotAnchor(shown.spot, state)
        }
        val pose = if (walking) PetPose.Walk else shown.pose
        val facingLeft = if (walking) spotFloorX(shown.spot) < walkFrom else shown.spot == CatSpot.Litter || shown.spot == CatSpot.Window
        if (shown.spot == CatSpot.Hiding && !walking) {
            // Under the bed: only its eyes show, blinking now and then.
            Box(
                Modifier
                    .offset(unit * 34f, unit * 72f)
                    .size(unit * 18f, unit * 10f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "Pet the cat") { onCatTap() }
                    .testTag("pet_cat"),
            )
        } else {
            PetCat(
                pose = pose,
                modifier = Modifier
                    .offset(unit * (anchor.x - catSize / 2f), unit * (anchor.y - catSize))
                    .size(unit * catSize)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "Pet the cat") { onCatTap() }
                    .testTag("pet_cat"),
                expression = shown.expression,
                build = build,
                outfits = PetOutfit.lookOf(state.wearing()),
                frame = frame,
                facingLeft = facingLeft,
            )
        }
        // In front of the cat: the box's front and the bed's rim, so it sits in them.
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width / W
            scale(s, s, pivot = Offset.Zero) { drawForeground(state, shown.spot, walking, frame) }
        }
    }
}

// --- The room ---

private object Room {
    val Wall = Color(0xFF241D2C)
    val WallLow = Color(0xFF1B1621)
    val Floor = Color(0xFF2A1F25)
    val Plank = Color(0xFF1F171C)
    val Wood = Color(0xFF4A372F)
    val WoodDark = Color(0xFF2E221D)
    val Wine = Color(0xFF5C2233)
    val WineDark = Color(0xFF3F1623)
    val Plum = Color(0xFF4A3F52)
    val PlumLit = Color(0xFF5D5068)
    val Violet = Color(0xFF3B3349)
    val Bone = InkPalette.Bone
    val Candle = Color(0xFFE6D6AE)
    val Amber = Color(0xFFE89A4A)
    val Leaf = Color(0xFF7D8C76)
    val LeafDark = Color(0xFF55604F)
    val Stone = Color(0xFF57505E)
    val Water = Color(0xFF3E5A5C)
    val Litter = Color(0xFF8C7A62)
    val Sisal = Color(0xFFB09A7E)
}

private fun DrawScope.drawRoom(state: PetState, now: Instant, sky: SkyPhase, frame: Int) {
    // Wall and floor.
    drawRect(Brush.verticalGradient(listOf(Room.Wall, Room.WallLow), endY = FLOOR), Offset.Zero, Size(W, FLOOR))
    repeat(40) {
        // Faint little crescents in the wallpaper.
        val x = (it % 8) * 13f + 4f + (it / 8 % 2) * 6f
        val y = (it / 8) * 12f + 6f
        if (y < FLOOR - 4) drawArc(Room.Plum.copy(alpha = 0.35f), 40f, 220f, false, Offset(x, y), Size(2.4f, 2.4f), style = Stroke(0.4f))
    }
    drawRect(Room.Floor, Offset(0f, FLOOR), Size(W, H - FLOOR))
    var y = FLOOR + 2f
    var gap = 3f
    while (y < H) {
        drawLine(Room.Plank, Offset(0f, y), Offset(W, y), 0.4f)
        gap *= 1.25f
        y += gap
    }
    drawRect(Room.WoodDark, Offset(0f, FLOOR - 1.5f), Size(W, 2.5f))
    if (state.has("fairy_lights")) fairyLights(frame)
    window(sky, frame, state.has("perch"))
    clock(now)
    shelf(state, frame)
    if (state.has("poster")) poster()
    if (state.has("moon_lamp")) moonLamp()
    catTree(state.has("tree"))
    if (state.has("hammock")) hammock()
    if (state.has("rug")) drawOval(Room.Wine.copy(alpha = 0.85f), Offset(33f, 84.5f), Size(29f, 6.5f))
    if (state.has("rug")) drawOval(Room.WineDark, Offset(35.5f, 85.6f), Size(24f, 4.3f), style = Stroke(0.5f))
    bed(state.has("heated_bed"), frame)
    litterBox(PetRules.level(state, PetNeed.Litter, now))
    bowls(state, now)
    if (state.has("plants")) plants()
    if (state.has("yarn")) {
        drawCircle(Color(0xFFC4A3A8), 2.2f, Offset(70f, 89.5f))
        drawLine(Color(0xFFC4A3A8), Offset(68f, 90.5f), Offset(60f, 91.5f), 0.4f)
    }
    if (state.has("mouse")) {
        drawOval(Room.Stone, Offset(90f, 89f), Size(4f, 2.2f))
        drawCircle(Color(0xFFC4A3A8), 0.5f, Offset(94f, 89.6f))
        drawLine(Room.Stone, Offset(90f, 90f), Offset(87f, 91f), 0.3f)
    }
    if (state.muddy) {
        // Muddy paw prints across the floor.
        for (i in 0..5) {
            val px = 20f + i * 9f
            val py = 88f - (i % 2) * 2.5f
            drawCircle(Color(0xFF4A3A2A), 0.9f, Offset(px, py))
            for (t in 0..2) drawCircle(Color(0xFF4A3A2A), 0.35f, Offset(px - 0.8f + t * 0.8f, py - 1.2f))
        }
    }
}

private fun DrawScope.fairyLights(frame: Int) {
    val colors = listOf(Room.Candle, Color(0xFFC4A3A8), Color(0xFFB59CD6), Room.Amber)
    var x = 2f
    var i = 0
    while (x < W) {
        val sag = kotlin.math.sin(x / 12f).toFloat() * 1.4f
        drawLine(Color(0xFF15111A), Offset(x, 3f + sag), Offset(x + 4f, 3f + kotlin.math.sin((x + 4f) / 12f).toFloat() * 1.4f), 0.25f)
        val on = (frame / 3 + i) % 7 != 0
        val c = colors[i % colors.size]
        if (on) drawCircle(c.copy(alpha = 0.25f), 1.6f, Offset(x, 4f + sag))
        drawCircle(if (on) c else c.copy(alpha = 0.3f), 0.6f, Offset(x, 4f + sag))
        x += 4f
        i++
    }
}

private fun DrawScope.window(sky: SkyPhase, frame: Int, perch: Boolean) {
    val l = 6f
    val t = 8f
    val w = 22f
    val h = 30f
    val (top, bottom, moon) = when (sky) {
        SkyPhase.Night -> Triple(Color(0xFF07070A), Color(0xFF1A1622), true)
        SkyPhase.Dawn -> Triple(Color(0xFF241A30), Color(0xFF5A3A4A), false)
        SkyPhase.Day -> Triple(Color(0xFF4A4458), Color(0xFF6E6578), false)
        SkyPhase.Dusk -> Triple(Color(0xFF2A1428), Color(0xFF6B3040), true)
    }
    drawRect(Room.WoodDark, Offset(l - 1.2f, t - 1.2f), Size(w + 2.4f, h + 2.4f))
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = t, endY = t + h), Offset(l, t), Size(w, h))
    if (moon) {
        drawCircle(Room.Bone, 3f, Offset(l + 15f, t + 7f))
        drawCircle(top, 2.6f, Offset(l + 16.2f, t + 6.2f))
        listOf(Offset(l + 4f, t + 4f), Offset(l + 9f, t + 11f), Offset(l + 19f, t + 14f)).forEach { drawCircle(Room.Bone.copy(alpha = 0.7f), 0.3f, it) }
    } else {
        drawCircle(Room.Candle.copy(alpha = 0.25f), 3.4f, Offset(l + 15f, t + 8f))
    }
    // Rooftops and rain.
    drawRect(Color(0xFF0E0B12), Offset(l, t + h - 6f), Size(w, 6f))
    drawRect(Color(0xFF0E0B12), Offset(l + 3f, t + h - 9f), Size(5f, 3f))
    drawRect(Color(0xFF0E0B12), Offset(l + 13f, t + h - 11f), Size(4f, 5f))
    val rain = Random(2)
    repeat(12) { i ->
        val x = l + rain.nextFloat() * w
        val yy = t + ((rain.nextFloat() * h + frame * (1.5f + i % 3)) % (h - 2f))
        drawLine(Color(0xFFB8B2BF).copy(alpha = 0.3f), Offset(x, yy), Offset(x - 0.6f, yy + 2.2f), 0.25f)
    }
    drawLine(Room.WoodDark, Offset(l + w / 2f, t), Offset(l + w / 2f, t + h), 0.9f)
    drawLine(Room.WoodDark, Offset(l, t + h / 2f), Offset(l + w, t + h / 2f), 0.9f)
    // Curtains.
    listOf(l - 3f to 1f, l + w + 3f to -1f).forEach { (x, dir) ->
        val p = Path().apply {
            moveTo(x, t - 3f)
            lineTo(x + dir * 5f, t - 3f)
            quadraticTo(x + dir * 2.5f, t + h * 0.5f, x + dir * 4f, t + h + 4f)
            lineTo(x, t + h + 4f)
            close()
        }
        drawPath(p, Room.Wine)
        drawLine(Room.WineDark, Offset(x + dir * 2f, t - 2f), Offset(x + dir * 2.4f, t + h + 3f), 0.4f)
    }
    drawRect(Room.WoodDark, Offset(l - 5f, t - 4f), Size(w + 10f, 1f))
    // Sill (or a cushioned perch).
    drawRect(Room.Wood, Offset(l - 2f, t + h + 1f), Size(w + 4f, 2f))
    if (perch) {
        drawRoundRect(Room.Plum, Offset(l - 1f, t + h + 0.2f), Size(w + 2f, 2.6f), CornerRadius(1.3f))
        drawLine(Room.Wood, Offset(l + 2f, t + h + 3f), Offset(l + 4f, t + h + 8f), 0.8f)
        drawLine(Room.Wood, Offset(l + w - 2f, t + h + 3f), Offset(l + w - 4f, t + h + 8f), 0.8f)
    }
}

/** The gift shelf: whatever it's brought you, in a row. */
private fun DrawScope.shelf(state: PetState, frame: Int) {
    val l = 36f
    val y = 24f
    drawRect(Room.Wood, Offset(l, y), Size(26f, 1.4f))
    drawLine(Room.WoodDark, Offset(l + 3f, y + 1.4f), Offset(l + 3f, y + 4f), 0.6f)
    drawLine(Room.WoodDark, Offset(l + 23f, y + 1.4f), Offset(l + 23f, y + 4f), 0.6f)
    state.gifts.takeLast(6).forEachIndexed { i, g -> giftIcon(g.id, Offset(l + 2.5f + i * 3.4f, y - 0.2f), 1.35f) }
    if (state.has("candles")) {
        listOf(0f, 1.8f).forEachIndexed { i, dx ->
            val x = l + 22.5f + dx
            drawRect(Room.Bone, Offset(x - 0.5f, y - 3f - i), Size(1f, 3f + i))
            val flick = if ((frame + i) % 4 < 2) 0.2f else 0f
            drawCircle(Room.Amber.copy(alpha = 0.25f), 1.4f, Offset(x, y - 3.8f - i))
            drawOval(Room.Candle, Offset(x - 0.35f, y - 4.6f - i - flick), Size(0.7f, 1.2f))
        }
    }
}

/** A small picture of a gift, sitting on [base] (bottom centre). */
internal fun DrawScope.giftIcon(id: String, base: Offset, k: Float) {
    val x = base.x
    val y = base.y
    when (id) {
        "leaf" -> drawOval(Color(0xFF8C6F3E), Offset(x - 1.2f * k, y - 1.4f * k), Size(2.4f * k, 1.4f * k))
        "sock" -> {
            drawRect(Room.Wine, Offset(x - 0.5f * k, y - 2.4f * k), Size(1f * k, 2f * k))
            drawRect(Room.Wine, Offset(x - 0.5f * k, y - 0.6f * k), Size(1.8f * k, 0.6f * k))
        }
        "feather" -> drawLine(Color(0xFF15111A), Offset(x - 1f * k, y), Offset(x + 0.8f * k, y - 2.6f * k), 0.6f * k, StrokeCap.Round)
        "bottle_cap" -> drawOval(Room.Amber, Offset(x - 0.9f * k, y - 0.7f * k), Size(1.8f * k, 0.7f * k))
        "hair_tie" -> drawCircle(Color(0xFF15111A), 0.8f * k, Offset(x, y - 0.9f * k), style = Stroke(0.35f * k))
        "button" -> drawCircle(Room.Bone, 0.7f * k, Offset(x, y - 0.7f * k))
        "moth_wing" -> drawOval(Color(0xFFB8A88E), Offset(x - 1f * k, y - 1.6f * k), Size(2f * k, 1.6f * k))
        "pebble" -> drawOval(Room.Stone, Offset(x - 1f * k, y - 1f * k), Size(2f * k, 1f * k))
        "quartz" -> drawPath(Path().apply { moveTo(x - 0.6f * k, y); lineTo(x, y - 2.4f * k); lineTo(x + 0.6f * k, y); close() }, Room.Bone.copy(alpha = 0.85f))
        "obsidian" -> drawPath(Path().apply { moveTo(x - 1f * k, y); lineTo(x - 0.2f * k, y - 1.8f * k); lineTo(x + 1f * k, y - 0.6f * k); lineTo(x + 0.6f * k, y); close() }, Color(0xFF15111A))
        "geode" -> {
            drawArc(Room.Stone, 180f, 180f, true, Offset(x - 1.2f * k, y - 1.2f * k), Size(2.4f * k, 2.4f * k))
            drawArc(Color(0xFF8B6FAE), 180f, 180f, true, Offset(x - 0.8f * k, y - 0.8f * k), Size(1.6f * k, 1.6f * k))
        }
        "ammonite" -> {
            drawCircle(Color(0xFFB09A7E), 1.1f * k, Offset(x, y - 1.1f * k))
            drawCircle(Color(0xFF6B5A4E), 0.6f * k, Offset(x, y - 1.1f * k), style = Stroke(0.25f * k))
        }
        "trilobite" -> {
            drawOval(Color(0xFF6B5A4E), Offset(x - 0.8f * k, y - 2f * k), Size(1.6f * k, 2f * k))
            drawLine(Room.Bone.copy(alpha = 0.4f), Offset(x, y - 1.9f * k), Offset(x, y - 0.1f * k), 0.2f * k)
        }
        "ribbon" -> drawLine(Room.Wine, Offset(x - 1f * k, y - 0.4f * k), Offset(x + 1f * k, y - 1.6f * k), 0.6f * k, StrokeCap.Round)
        "key" -> {
            drawCircle(Color(0xFFD9B45A), 0.5f * k, Offset(x - 0.8f * k, y - 1f * k), style = Stroke(0.25f * k))
            drawLine(Color(0xFFD9B45A), Offset(x - 0.3f * k, y - 1f * k), Offset(x + 1f * k, y - 1f * k), 0.25f * k)
        }
        else -> moreGiftIcon(id, x, y, k)
    }
}

private fun poly(x: Float, y: Float, k: Float, vararg pts: Float) = Path().apply {
    moveTo(x + pts[0] * k, y - pts[1] * k)
    var i = 2
    while (i < pts.size) { lineTo(x + pts[i] * k, y - pts[i + 1] * k); i += 2 }
    close()
}

/** The rarer rocks and finds. Coordinates are in gift units: x across from the centre, y up from the base. */
private fun DrawScope.moreGiftIcon(id: String, x: Float, y: Float, k: Float) {
    val ink = Color(0xFF15111A)
    when (id) {
        "pyrite" -> {
            // Stacked brassy cubes.
            drawRect(Color(0xFFB8963E), Offset(x - 1.1f * k, y - 1.2f * k), Size(1.3f * k, 1.2f * k))
            drawRect(Color(0xFFD9B45A), Offset(x - 0.1f * k, y - 1.6f * k), Size(1.2f * k, 1.6f * k))
            drawRect(Color(0xFFE8CF85), Offset(x - 0.1f * k, y - 1.6f * k), Size(1.2f * k, 0.3f * k))
            drawLine(Color(0xFF8E6E2E), Offset(x + 0.1f * k, y - 1.1f * k), Offset(x + 0.9f * k, y - 1.1f * k), 0.08f * k)
        }
        "halite" -> {
            drawRect(Color(0xFFE9E2DA), Offset(x - 0.9f * k, y - 1.8f * k), Size(1.8f * k, 1.8f * k))
            drawPath(poly(x, y, k, -0.9f, 1.8f, -0.4f, 2.2f, 1.3f, 2.2f, 0.9f, 1.8f), Color(0xFFF6F1EA))
            drawPath(poly(x, y, k, 0.9f, 1.8f, 1.3f, 2.2f, 1.3f, 0.4f, 0.9f, 0f), Color(0xFFC9C0B6))
            drawLine(Color(0xFFD9A3B0).copy(alpha = 0.5f), Offset(x - 0.6f * k, y - 1.3f * k), Offset(x + 0.4f * k, y - 0.4f * k), 0.12f * k)
        }
        "pumice" -> {
            drawOval(Color(0xFFB9B2A6), Offset(x - 1.2f * k, y - 1.3f * k), Size(2.4f * k, 1.3f * k))
            listOf(-0.6f to 0.8f, 0.1f to 0.5f, 0.6f to 0.9f, -0.2f to 1f, 0.4f to 0.3f, -0.8f to 0.4f).forEach { (dx, dy) ->
                drawCircle(Color(0xFF8C8578), 0.12f * k, Offset(x + dx * k, y - dy * k))
            }
        }
        "rose_quartz" -> {
            drawPath(poly(x, y, k, -1.1f, 0f, -0.9f, 1.3f, -0.2f, 2.1f, 0.7f, 1.6f, 1.1f, 0f), Color(0xFFE3B3BE))
            drawPath(poly(x, y, k, -0.2f, 2.1f, 0.7f, 1.6f, 0.2f, 0.6f), Color(0xFFF2D1D8))
        }
        "galena" -> {
            // Lead-grey cubes, very heavy for their size.
            drawRect(Color(0xFF6E6C76), Offset(x - 1.2f * k, y - 1.4f * k), Size(1.4f * k, 1.4f * k))
            drawRect(Color(0xFF8C8A95), Offset(x - 0.3f * k, y - 2f * k), Size(1.3f * k, 1.3f * k))
            drawRect(Color(0xFFA9A7B2), Offset(x - 0.3f * k, y - 2f * k), Size(1.3f * k, 0.25f * k))
            drawRect(Color(0xFF55535C), Offset(x + 0.2f * k, y - 0.8f * k), Size(0.9f * k, 0.8f * k))
        }
        "magnetite" -> {
            drawPath(poly(x, y, k, -1f, 0f, -0.6f, 1.6f, 0.6f, 1.6f, 1f, 0f), Color(0xFF2B2830))
            drawPath(poly(x, y, k, -0.6f, 1.6f, 0f, 2.2f, 0.6f, 1.6f), Color(0xFF3E3A45))
            // A paperclip stuck to it.
            drawRoundRect(Color(0xFFB9B2A6), Offset(x + 0.5f * k, y - 2.1f * k), Size(0.5f * k, 1.2f * k), CornerRadius(0.25f * k), style = Stroke(0.1f * k))
        }
        "malachite" -> {
            drawOval(Color(0xFF2F6B4F), Offset(x - 1.2f * k, y - 1.6f * k), Size(2.4f * k, 1.6f * k))
            for (i in 1..3) drawArc(Color(0xFF6FB08A), 200f, 140f, false, Offset(x - (1.2f - i * 0.25f) * k, y - (1.6f - i * 0.2f) * k), Size((2.4f - i * 0.5f) * k, (1.4f - i * 0.3f) * k), style = Stroke(0.12f * k))
        }
        "tigers_eye" -> {
            drawOval(Color(0xFF7A4E22), Offset(x - 1.1f * k, y - 1.5f * k), Size(2.2f * k, 1.5f * k))
            for (i in 0..3) drawLine(Color(0xFFD9A84E).copy(alpha = 0.7f), Offset(x - 0.9f * k, y - (0.4f + i * 0.28f) * k), Offset(x + 0.9f * k, y - (0.6f + i * 0.28f) * k), 0.14f * k)
        }
        "fluorite" -> {
            // An octahedron, purple into green.
            drawPath(poly(x, y, k, 0f, 0f, -1f, 1.2f, 0f, 2.5f), Color(0xFF7B5EA7))
            drawPath(poly(x, y, k, 0f, 0f, 1f, 1.2f, 0f, 2.5f), Color(0xFF5E9C8A))
            drawLine(Color(0xFFE6DED0).copy(alpha = 0.5f), Offset(x - 1f * k, y - 1.2f * k), Offset(x + 1f * k, y - 1.2f * k), 0.08f * k)
        }
        "desert_rose" -> {
            for (i in 0..4) {
                val a = Math.toRadians(-160.0 + i * 35.0)
                val c = Offset(x + kotlin.math.cos(a).toFloat() * 0.6f * k, y - 0.9f * k + kotlin.math.sin(a).toFloat() * 0.5f * k)
                drawOval(Color(0xFFC9A07A), Offset(c.x - 0.5f * k, c.y - 0.25f * k), Size(1f * k, 0.55f * k))
                drawOval(Color(0xFF9E7653), Offset(c.x - 0.5f * k, c.y - 0.25f * k), Size(1f * k, 0.55f * k), style = Stroke(0.07f * k))
            }
            drawOval(Color(0xFFB88C64), Offset(x - 1.1f * k, y - 0.6f * k), Size(2.2f * k, 0.6f * k))
        }
        "labradorite" -> {
            drawPath(poly(x, y, k, -1.2f, 0f, -0.9f, 1.4f, 0.4f, 1.7f, 1.2f, 0.6f, 1f, 0f), Color(0xFF3A3C48))
            drawPath(poly(x, y, k, -0.6f, 0.4f, -0.4f, 1.2f, 0.4f, 1.3f, 0.8f, 0.5f), Color(0xFF4F8FB3).copy(alpha = 0.85f))
            drawPath(poly(x, y, k, -0.1f, 0.5f, 0.1f, 1.1f, 0.6f, 0.6f), Color(0xFF7FC6A4).copy(alpha = 0.7f))
        }
        "bismuth" -> {
            // Stepped hopper squares in rainbow oxide.
            listOf(Color(0xFFC77DBA), Color(0xFF6FA8DC), Color(0xFFD9B45A), Color(0xFF7FC6A4)).forEachIndexed { i, c ->
                val w = (2.4f - i * 0.55f) * k
                drawRect(c, Offset(x - w / 2, y - (0.5f + i * 0.5f) * k), Size(w, 0.5f * k), style = Stroke(0.22f * k))
            }
        }
        "petrified_wood" -> {
            drawRoundRect(Color(0xFF8C5E3C), Offset(x - 1.2f * k, y - 1.3f * k), Size(2.4f * k, 1.3f * k), CornerRadius(0.2f * k))
            drawOval(Color(0xFFC9A07A), Offset(x + 0.7f * k, y - 1.3f * k), Size(0.9f * k, 1.3f * k))
            for (r in 1..3) drawOval(Color(0xFF8C5E3C), Offset(x + (0.7f + r * 0.1f) * k, y - (1.3f - r * 0.15f) * k), Size((0.9f - r * 0.2f) * k, (1.3f - r * 0.3f) * k), style = Stroke(0.06f * k))
            drawLine(Color(0xFF6B4A36), Offset(x - 1f * k, y - 0.5f * k), Offset(x + 0.6f * k, y - 0.4f * k), 0.08f * k)
        }
        "amber" -> {
            drawOval(Color(0xFFD08A2E).copy(alpha = 0.9f), Offset(x - 1.1f * k, y - 1.8f * k), Size(2.2f * k, 1.8f * k))
            drawOval(Color(0xFFF0B85A).copy(alpha = 0.6f), Offset(x - 0.7f * k, y - 1.6f * k), Size(0.9f * k, 0.5f * k))
            // A tiny trapped bug.
            drawOval(ink, Offset(x - 0.2f * k, y - 1f * k), Size(0.45f * k, 0.25f * k))
            drawLine(ink, Offset(x + 0f, y - 0.9f * k), Offset(x + 0.4f * k, y - 1.15f * k), 0.05f * k)
            drawLine(ink, Offset(x + 0f, y - 0.9f * k), Offset(x - 0.4f * k, y - 1.15f * k), 0.05f * k)
        }
        "megalodon" -> {
            drawPath(poly(x, y, k, -1.1f, 0.4f, 0f, 2.6f, 1.1f, 0.4f), Color(0xFF4A4550))
            drawPath(Path().apply {
                moveTo(x - 1.1f * k, y - 0.4f * k); quadraticTo(x, y + 0.3f * k, x + 1.1f * k, y - 0.4f * k); lineTo(x + 0.9f * k, y - 0.7f * k); lineTo(x - 0.9f * k, y - 0.7f * k); close()
            }, Color(0xFF8C7A62))
            for (i in 0..5) drawLine(Color(0xFF8C8794), Offset(x + (-0.95f + i * 0.17f) * k, y - (0.7f + i * 0.33f) * k), Offset(x + (-0.85f + i * 0.17f) * k, y - (0.85f + i * 0.33f) * k), 0.06f * k)
        }
        "moldavite" -> {
            drawPath(poly(x, y, k, -1.1f, 0f, -0.8f, 1.2f, -0.3f, 0.9f, 0f, 2.2f, 0.5f, 1.1f, 1.1f, 1.4f, 0.9f, 0f), Color(0xFF4E7A3A).copy(alpha = 0.9f))
            drawPath(poly(x, y, k, -0.3f, 0.3f, 0f, 1.6f, 0.4f, 0.4f), Color(0xFF8DBA6A).copy(alpha = 0.6f))
        }
        "fulgurite" -> {
            // Glassy sand fused by lightning: a branching tube.
            drawLine(Color(0xFFBFA98A), Offset(x - 0.2f * k, y), Offset(x + 0.1f * k, y - 2.5f * k), 0.45f * k, StrokeCap.Round)
            drawLine(Color(0xFFBFA98A), Offset(x, y - 1.2f * k), Offset(x + 0.8f * k, y - 1.9f * k), 0.3f * k, StrokeCap.Round)
            drawLine(Color(0xFFBFA98A), Offset(x - 0.1f * k, y - 0.6f * k), Offset(x - 0.8f * k, y - 1.3f * k), 0.25f * k, StrokeCap.Round)
            drawLine(Color(0xFFEDE3C8), Offset(x - 0.05f * k, y - 0.3f * k), Offset(x + 0.1f * k, y - 2.2f * k), 0.1f * k)
        }
        "meteorite" -> {
            drawPath(poly(x, y, k, -1.2f, 0.1f, -1f, 1.3f, -0.2f, 1.8f, 0.9f, 1.4f, 1.2f, 0.3f, 0.4f, 0f), Color(0xFF3E3A40))
            // A cut face showing the crosshatch (Widmanstätten) pattern.
            drawPath(poly(x, y, k, -0.6f, 0.4f, -0.4f, 1.3f, 0.6f, 1.2f, 0.7f, 0.4f), Color(0xFF9A98A2))
            for (i in 0..2) {
                drawLine(Color(0xFF6E6C76), Offset(x + (-0.5f + i * 0.4f) * k, y - 0.45f * k), Offset(x + (-0.2f + i * 0.4f) * k, y - 1.25f * k), 0.05f * k)
                drawLine(Color(0xFF6E6C76), Offset(x - 0.5f * k, y - (0.6f + i * 0.25f) * k), Offset(x + 0.65f * k, y - (0.75f + i * 0.2f) * k), 0.05f * k)
            }
        }
        "uraninite" -> {
            // A small lead box, lid ajar, the stone inside. Safely boxed.
            drawRect(Color(0xFF55535C), Offset(x - 1.2f * k, y - 1.2f * k), Size(2.4f * k, 1.2f * k))
            drawRect(Color(0xFF6E6C76), Offset(x - 1.2f * k, y - 1.2f * k), Size(2.4f * k, 0.2f * k))
            drawCircle(Color(0xFF9FD46A).copy(alpha = 0.18f), 0.9f * k, Offset(x - 0.1f * k, y - 1.3f * k))
            drawOval(Color(0xFF26232A), Offset(x - 0.6f * k, y - 1.55f * k), Size(1f * k, 0.55f * k))
            drawPath(poly(x, y, k, -1.2f, 1.2f, -1.3f, 2.1f, 1.1f, 2.4f, 1.2f, 1.5f), Color(0xFF6E6C76))
            // The hazard trefoil on the front, small.
            val c = Offset(x + 0.6f * k, y - 0.6f * k)
            drawCircle(Color(0xFFD9B45A), 0.32f * k, c)
            for (i in 0..2) drawArc(ink, -90f + i * 120f - 30f, 60f, true, Offset(c.x - 0.26f * k, c.y - 0.26f * k), Size(0.52f * k, 0.52f * k))
            drawCircle(Color(0xFFD9B45A), 0.07f * k, c)
            drawCircle(ink, 0.05f * k, c)
        }
        else -> drawCircle(Room.Bone, 0.6f * k, Offset(x, y - 0.6f * k))
    }
}

/** A little round clock that keeps real time. */
private fun DrawScope.clock(now: Instant) {
    val t = now.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    val c = Offset(49f, 40f)
    drawCircle(Room.WoodDark, 4.6f, c)
    drawCircle(Room.Bone.copy(alpha = 0.85f), 3.9f, c)
    for (i in 0 until 12) {
        val a = Math.toRadians(i * 30.0)
        drawCircle(Room.WoodDark, if (i % 3 == 0) 0.3f else 0.18f, Offset(c.x + 3.2f * kotlin.math.sin(a).toFloat(), c.y - 3.2f * kotlin.math.cos(a).toFloat()))
    }
    val h = Math.toRadians((t.hour % 12 + t.minute / 60.0) * 30.0)
    val m = Math.toRadians(t.minute * 6.0)
    drawLine(Color(0xFF15111A), c, Offset(c.x + 1.9f * kotlin.math.sin(h).toFloat(), c.y - 1.9f * kotlin.math.cos(h).toFloat()), 0.45f, StrokeCap.Round)
    drawLine(Color(0xFF15111A), c, Offset(c.x + 2.9f * kotlin.math.sin(m).toFloat(), c.y - 2.9f * kotlin.math.cos(m).toFloat()), 0.3f, StrokeCap.Round)
    drawCircle(Room.Wine, 0.35f, c)
}

private fun DrawScope.poster() {
    drawRect(Room.WoodDark, Offset(65f, 9f), Size(10f, 14f))
    drawRect(Room.Bone.copy(alpha = 0.85f), Offset(65.7f, 9.7f), Size(8.6f, 12.6f))
    // A moth.
    val c = Offset(70f, 15.5f)
    drawOval(Color(0xFF15111A), Offset(c.x - 3.4f, c.y - 2.4f), Size(3.4f, 3.6f))
    drawOval(Color(0xFF15111A), Offset(c.x, c.y - 2.4f), Size(3.4f, 3.6f))
    drawOval(Color(0xFF15111A), Offset(c.x - 2.4f, c.y + 0.6f), Size(2.4f, 2.4f))
    drawOval(Color(0xFF15111A), Offset(c.x, c.y + 0.6f), Size(2.4f, 2.4f))
    drawCircle(Room.Bone, 0.4f, Offset(c.x - 1.7f, c.y - 0.8f))
    drawCircle(Room.Bone, 0.4f, Offset(c.x + 1.7f, c.y - 0.8f))
}

private fun DrawScope.moonLamp() {
    drawLine(Room.WoodDark, Offset(30f, 60f), Offset(30f, 76f), 0.6f)
    drawOval(Room.WoodDark, Offset(28f, 75.6f), Size(4f, 1.2f))
    drawCircle(Room.Candle.copy(alpha = 0.18f), 6f, Offset(30f, 58f))
    drawCircle(Room.Candle, 2.6f, Offset(30f, 58f))
    drawCircle(Color(0xFFCFC2A0), 0.6f, Offset(29f, 57.4f))
    drawCircle(Color(0xFFCFC2A0), 0.4f, Offset(31f, 59f))
}

private fun DrawScope.catTree(tall: Boolean) {
    if (tall) {
        drawRect(Room.Sisal, Offset(87f, 16f), Size(3f, 60f))
        for (i in 0 until 20) drawLine(Color(0xFF8C7A62), Offset(87f, 18f + i * 3f), Offset(90f, 18f + i * 3f), 0.3f)
        listOf(16f to 12f, 34f to 10f, 52f to 12f).forEach { (y, w) ->
            drawRoundRect(Room.Plum, Offset(88.5f - w / 2f, y), Size(w, 2.4f), CornerRadius(1f))
            drawRoundRect(Room.PlumLit, Offset(88.5f - w / 2f, y), Size(w, 0.8f), CornerRadius(0.4f))
        }
        // A cubby.
        drawRoundRect(Room.Plum, Offset(82f, 62f), Size(13f, 12f), CornerRadius(1.5f))
        drawOval(Color(0xFF15111A), Offset(85f, 65f), Size(7f, 7f))
        drawRoundRect(Room.Plum, Offset(80f, 76f), Size(17f, 2.4f), CornerRadius(1f))
    } else {
        drawRect(Room.Sisal, Offset(87f, 46f), Size(3f, 30f))
        for (i in 0 until 10) drawLine(Color(0xFF8C7A62), Offset(87f, 48f + i * 3f), Offset(90f, 48f + i * 3f), 0.3f)
        drawRoundRect(Room.Plum, Offset(83f, 45f), Size(11f, 2.4f), CornerRadius(1f))
        drawRoundRect(Room.Plum, Offset(82f, 76f), Size(13f, 2.4f), CornerRadius(1f))
    }
}

private fun DrawScope.hammock() {
    // A little wooden stand with a sling.
    drawLine(Room.Wood, Offset(71f, 82f), Offset(73f, 66f), 0.8f, StrokeCap.Round)
    drawLine(Room.Wood, Offset(83f, 82f), Offset(81f, 66f), 0.8f, StrokeCap.Round)
    drawLine(Room.Wood, Offset(70f, 82f), Offset(84f, 82f), 0.8f, StrokeCap.Round)
    drawArc(Room.Wine, 0f, 180f, true, Offset(72.6f, 64f), Size(8.8f, 8f))
    drawArc(Room.WineDark, 0f, 180f, false, Offset(73.6f, 65f), Size(6.8f, 5.6f), style = Stroke(0.4f))
}

private fun DrawScope.bed(heated: Boolean, frame: Int) {
    if (heated) drawOval(Room.Amber.copy(alpha = if (frame % 10 < 5) 0.14f else 0.1f), Offset(30f, 70f), Size(26f, 12f))
    drawOval(if (heated) Color(0xFF6E3445) else Room.Plum, Offset(33f, 74f), Size(20f, 7f))
    drawOval(if (heated) Room.Wine else Room.Violet, Offset(35f, 74.6f), Size(16f, 4.6f))
}

private fun DrawScope.litterBox(level: Float) {
    drawRoundRect(Room.Violet, Offset(16f, 80f), Size(16f, 6f), CornerRadius(1f))
    drawRect(Room.Litter, Offset(17f, 80f), Size(14f, 1.2f))
    val clumps = ((100f - level) / 20f).toInt().coerceIn(0, 5)
    for (i in 0 until clumps) drawOval(Color(0xFF6B5A4E), Offset(18f + i * 2.6f, 79.4f), Size(1.8f, 1.2f))
    if (level < 20) {
        // Stink lines.
        for (i in 0..2) drawPath(Path().apply { moveTo(20f + i * 4f, 78.5f); quadraticTo(22f + i * 4f, 76f, 20.5f + i * 4f, 74f); quadraticTo(19f + i * 4f, 72f, 21f + i * 4f, 70f) }, Room.Leaf.copy(alpha = 0.7f), style = Stroke(0.45f, cap = StrokeCap.Round))
    }
    // The scoop.
    drawLine(Room.Stone, Offset(13f, 86f), Offset(15f, 78f), 0.6f)
    drawRoundRect(Room.Stone, Offset(12f, 85.5f), Size(3f, 1.6f), CornerRadius(0.4f))
}

private fun DrawScope.bowls(state: PetState, now: Instant) {
    val food = PetRules.level(state, PetNeed.Hunger, now)
    val water = PetRules.level(state, PetNeed.Thirst, now)
    drawArc(Room.Wine, 0f, 180f, true, Offset(64f, 84f), Size(8f, 5f))
    val n = (food / 20f).toInt().coerceIn(0, 5)
    for (i in 0 until n) drawCircle(Color(0xFFB08A5E), 0.55f, Offset(65.4f + i * 1.3f, 86.2f - (i % 2) * 0.4f))
    drawArc(Room.Violet, 0f, 180f, true, Offset(73f, 84f), Size(8f, 5f))
    if (water > 10) drawRect(Room.Water, Offset(73.8f, 86.2f - water / 100f * 1.2f), Size(6.4f, 0.8f))
    if (state.has("fountain")) {
        drawRoundRect(Room.Stone, Offset(81f, 80f), Size(4f, 7f), CornerRadius(1f))
        drawArc(Room.Water.copy(alpha = 0.8f), 180f, 160f, false, Offset(78f, 81f), Size(5f, 4f), style = Stroke(0.4f))
    }
}

private fun DrawScope.plants() {
    // A pot by the tree and a hanging plant.
    drawRoundRect(Color(0xFF5E2A28), Offset(93f, 80f), Size(5f, 6f), CornerRadius(0.8f))
    listOf(-2f to -6f, 0f to -9f, 2f to -7f, -1f to -11f, 1.5f to -10f).forEachIndexed { i, (dx, dy) ->
        drawOval(if (i % 2 == 0) Room.Leaf else Room.LeafDark, Offset(95.5f + dx - 1.4f, 80f + dy), Size(2.8f, 4.4f))
    }
    drawLine(Room.WoodDark, Offset(79f, 0f), Offset(79f, 28f), 0.25f)
    drawArc(Color(0xFF5E2A28), 0f, 180f, true, Offset(76.5f, 27f), Size(5f, 3.4f))
    for (i in 0..4) {
        val x = 77f + i * 1f
        val end = Offset(76f + i * 1.5f, 33f + (i % 3) * 2.5f)
        drawLine(Room.LeafDark, Offset(x, 29f), end, 0.35f)
        drawOval(if (i % 2 == 0) Room.Leaf else Room.LeafDark, Offset(end.x - 0.8f, end.y - 0.4f), Size(1.6f, 2.2f))
    }
}

private fun DrawScope.drawForeground(state: PetState, spot: CatSpot, walking: Boolean, frame: Int) {
    if (!walking && (spot == CatSpot.Bed || spot == CatSpot.Hiding)) {
        // The bed's front rim (and, when it's hiding, a dark gap under a blanket).
        if (spot == CatSpot.Hiding) {
            drawRoundRect(Room.Plum, Offset(32f, 71f), Size(22f, 6f), CornerRadius(2f))
            drawRect(Color(0xFF0B080D), Offset(34f, 76.5f), Size(18f, 4f))
            // Only its eyes show, blinking now and then.
            if (frame % 25 != 24) {
                drawOval(Color(0xFFD9C27A), Offset(39.2f, 77.6f), Size(2.6f, 1.7f))
                drawOval(Color(0xFFD9C27A), Offset(44.2f, 77.6f), Size(2.6f, 1.7f))
                drawOval(Color(0xFF0B080D), Offset(40.2f, 77.7f), Size(0.6f, 1.5f))
                drawOval(Color(0xFF0B080D), Offset(45.2f, 77.7f), Size(0.6f, 1.5f))
            }
        } else {
            drawArc(if (state.has("heated_bed")) Color(0xFF6E3445) else Room.Plum, 0f, 180f, true, Offset(33f, 75f), Size(20f, 6.4f))
        }
    }
    if (state.has("box")) {
        drawRect(Color(0xFF8C6F4E), Offset(55f, 72f), Size(14f, 9f))
        drawRect(Color(0xFF7A5E40), Offset(55f, 72f), Size(14f, 1.2f))
        drawPath(Path().apply { moveTo(55f, 72f); lineTo(52f, 69f); lineTo(53f, 72f); close() }, Color(0xFF7A5E40))
        drawPath(Path().apply { moveTo(69f, 72f); lineTo(72f, 69f); lineTo(71f, 72f); close() }, Color(0xFF7A5E40))
    }
}
