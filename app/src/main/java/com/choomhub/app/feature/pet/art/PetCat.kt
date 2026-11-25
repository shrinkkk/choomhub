package com.choomhub.app.feature.pet.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.choomhub.app.core.designsystem.illustration.InkPalette
import com.choomhub.app.core.designsystem.illustration.furEdge
import com.choomhub.app.core.designsystem.illustration.svgPath
import com.choomhub.app.core.designsystem.illustration.whisker
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.drawCatFace

/** What the cat is doing. */
enum class PetPose { Sit, Loaf, Asleep, Walk, Eat, Pounce, Petted, BellyUp, Grooming, Sick }

/** How it's built: it grows from a kitten, and can get chonky if it's overfed. */
enum class PetBuild { Kitten, Cat, Chonky }

/** Things it can wear (from the shop); [id] is the shop item. */
enum class PetOutfit(val id: String?) {
    None(null),
    BowTie("bow_tie"),
    BellCollar("bell_collar"),
    SpikedCollar("spiked_collar"),
    Bandana("bandana"),
    Scarf("scarf"),
    MoonPendant("moon_pendant"),
    WitchHat("witch_hat"),
    PumpkinHat("pumpkin_hat"),
    FlowerCrown("flower_crown"),
    Beret("beret"),
    Sunglasses("sunglasses"),
    BatWings("bat_wings"),
    Cape("cape"),
    Crown("crown"),
    ;

    companion object {
        fun fromId(id: String?): PetOutfit = entries.firstOrNull { it.id != null && it.id == id } ?: None
    }
}

private val Ink = InkPalette.Ink
private val Bone = InkPalette.Bone
private val Wine = Color(0xFF5C2233)
private val WineLit = Color(0xFF7A3446)
private val Plum = Color(0xFF3B3349)
private val Candle = Color(0xFFE6D6AE)
private val Tongue = Color(0xFF7A2E3E)
private val Bean = Color(0xFF6E3A44)

/**
 * The pet: the same black ink cat as the meetup countdown, now with a whole body. Drawn in a
 * 100×100 box; [frame] moves its legs and tail where the pose has motion.
 */
@Composable
fun PetCat(
    pose: PetPose,
    modifier: Modifier = Modifier,
    expression: CatExpression = CatExpression.Happy,
    build: PetBuild = PetBuild.Cat,
    outfit: PetOutfit = PetOutfit.None,
    frame: Int = 0,
    facingLeft: Boolean = false,
) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                scale(if (facingLeft) -1f else 1f, 1f, pivot = Offset(50f, 50f)) {
                    drawPetCat(pose, expression, build, outfit, frame)
                }
            }
        }
    }
}

internal fun DrawScope.drawPetCat(pose: PetPose, expression: CatExpression, build: PetBuild, outfit: PetOutfit, frame: Int) {
    when (pose) {
        PetPose.Sit -> sitting(expression, build, outfit)
        PetPose.Petted -> sitting(CatExpression.Ecstatic, build, outfit, purr = true)
        PetPose.Grooming -> sitting(CatExpression.Ecstatic, build, outfit, grooming = true)
        PetPose.Sick -> sitting(CatExpression.Sad, build, outfit, sick = true)
        PetPose.Loaf -> loaf(expression, build, outfit)
        PetPose.Asleep -> asleep(build)
        PetPose.Walk -> side(build, frame, SideMode.Walk)
        PetPose.Eat -> side(build, frame, SideMode.Eat)
        PetPose.Pounce -> side(build, frame, SideMode.Pounce)
        PetPose.BellyUp -> bellyUp(build)
    }
}

private fun union(vararg paths: Path): Path {
    var out = paths.first()
    for (p in paths.drop(1)) out = Path().apply { op(out, p, PathOperation.Union) }
    return out
}

private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = Path().apply {
    addOval(androidx.compose.ui.geometry.Rect(cx - rx, cy - ry, cx + rx, cy + ry))
}

private fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float) = Path().apply {
    addRoundRect(RoundRect(l, t, r, b, CornerRadius(radius)))
}

/** The ink body with fur catching the light from the upper right. */
private fun DrawScope.inkFur(path: Path, seed: Int, density: Float = 36f) {
    drawPath(path, Ink)
    furEdge(path, seed = seed, density = density, length = 1.2f..4.2f, light = Offset(0.9f, -0.35f), ambient = 0.1f)
}

/** The tail, as a thick furry stroke. */
private fun DrawScope.tail(d: String, width: Float, seed: Int) {
    val path = svgPath(d)
    drawPath(path, Ink, style = Stroke(width, cap = StrokeCap.Round))
    furEdge(
        Path().apply { addPath(path) },
        seed = seed,
        density = 30f,
        length = 1f..3.5f,
        light = Offset(0.9f, -0.35f),
        ambient = 0.2f,
        width = 0.3f,
    )
}

/** Front-facing head: the countdown cat's face, scaled and placed with its top-left at [at]. */
private fun DrawScope.head(expression: CatExpression, at: Offset, size: Float) {
    translate(at.x, at.y) {
        scale(size / 100f, size / 100f, pivot = Offset.Zero) {
            clipRect(-10f, -10f, 110f, 76f) { drawCatFace(expression, splatters = false) }
        }
    }
}

private fun DrawScope.paws(xs: List<Float>, y: Float, w: Float) {
    xs.forEach { x ->
        drawOval(Ink, Offset(x - w / 2f, y - 3f), Size(w, 6f))
        // Toe marks catching the light.
        for (i in 0..1) {
            val tx = x - w / 6f + i * w / 3f
            drawLine(Bone.copy(alpha = 0.45f), Offset(tx, y + 0.5f), Offset(tx, y + 2.6f), 0.5f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.sitting(
    expression: CatExpression,
    build: PetBuild,
    outfit: PetOutfit,
    purr: Boolean = false,
    grooming: Boolean = false,
    sick: Boolean = false,
) {
    val (headSize, headAt, body) = when (build) {
        PetBuild.Kitten -> Triple(
            62f,
            Offset(19f, 10f),
            svgPath("M38 50 C33 60 28 70 28 82 C28 92 34 96 43 96 L57 96 C66 96 72 92 72 82 C72 70 67 60 62 50 Z"),
        )
        PetBuild.Cat -> Triple(
            58f,
            Offset(21f, 0f),
            // Slim, like the countdown cat: a long thin neck into a narrow body.
            svgPath("M44 36 C42 50 36 64 35 80 C34 92 39 97 45 97 L55 97 C61 97 66 92 65 80 C64 64 58 50 56 36 Z"),
        )
        PetBuild.Chonky -> Triple(
            50f,
            Offset(25f, 4f),
            svgPath("M36 35 C22 46 8 62 8 78 C8 92 22 97 38 97 L62 97 C78 97 92 92 92 78 C92 62 78 46 64 35 Z"),
        )
    }
    if (outfit == PetOutfit.BatWings) batWings(build)
    if (outfit == PetOutfit.Cape) cape(build)
    // Tail curling round from behind.
    val tailD = when (build) {
        PetBuild.Kitten -> "M68 92 C84 93 88 84 85 74 C83 68 86 63 89 61"
        PetBuild.Cat -> "M62 93 C80 95 88 84 86 70 C85 62 88 56 92 53"
        PetBuild.Chonky -> "M86 90 C97 88 98 78 95 68 C93 61 96 56 98 53"
    }
    tail(tailD, when (build) { PetBuild.Kitten -> 5f; PetBuild.Cat -> 4.5f; PetBuild.Chonky -> 6.5f }, seed = 31)
    inkFur(body, seed = 17)
    // Front legs, a little paler at the edges so they read against the chest.
    val legTop = when (build) {
        PetBuild.Kitten -> 70f
        PetBuild.Cat -> 62f
        PetBuild.Chonky -> 66f
    }
    val legXs = when (build) {
        PetBuild.Kitten -> listOf(44f, 56f)
        PetBuild.Cat -> listOf(45.5f, 54.5f)
        PetBuild.Chonky -> listOf(41f, 59f)
    }
    legXs.forEachIndexed { i, x ->
        if (grooming && i == 1) return@forEachIndexed
        val half = if (build == PetBuild.Cat) 3f else 4f
        val leg = roundRect(x - half, legTop, x + half, 96f, 3f)
        drawPath(leg, Ink)
        // Light along the outer edge of each leg, so they stand out from the chest.
        val edge = if (i == 0) x - half else x + half
        drawLine(Bone.copy(alpha = 0.32f), Offset(edge, legTop + 6f), Offset(edge, 92f), 0.6f, StrokeCap.Round)
        drawLine(Bone.copy(alpha = 0.12f), Offset(if (i == 0) x + half else x - half, legTop + 10f), Offset(if (i == 0) x + half else x - half, 92f), 0.4f)
    }
    paws(legXs.filterIndexed { i, _ -> !(grooming && i == 1) }, 95f, if (build == PetBuild.Cat) 8f else 10f)
    head(expression, headAt, headSize)
    if (grooming) {
        // A front paw up at the mouth (its edge catching the light), and a little pink tongue.
        val mouth = Offset(headAt.x + headSize * 0.5f, headAt.y + headSize * 0.62f)
        val paw = svgPath(
            "M${mouth.x + 10} 80 C${mouth.x + 12} 66 ${mouth.x + 9} 52 ${mouth.x + 4} ${mouth.y + 5} " +
                "C${mouth.x + 2} ${mouth.y + 1} ${mouth.x - 3} ${mouth.y + 1} ${mouth.x - 4} ${mouth.y + 5} " +
                "C${mouth.x - 3} ${mouth.y + 10} ${mouth.x + 1} ${mouth.y + 14} ${mouth.x + 3} 66 C${mouth.x + 4} 72 ${mouth.x + 4} 76 ${mouth.x + 3} 80 Z",
        )
        drawPath(paw, Ink)
        drawPath(paw, Bone.copy(alpha = 0.35f), style = Stroke(0.6f))
        drawArc(Tongue, 0f, 180f, true, Offset(mouth.x - 2.5f, mouth.y - 1f), Size(5f, 5f))
    }
    wear(outfit, headAt, headSize)
    if (purr) {
        // Purr lines either side.
        val c = Bone.copy(alpha = 0.6f)
        listOf(8f to 50f, 92f to 50f).forEach { (x, y) ->
            for (i in 0..2) drawArc(c, if (x < 50) 120f else -60f, 120f, false, Offset(x - 3f - i * 3f * (if (x < 50) 1 else -1), y - 4f - i * 2f), Size(6f + i * 3f, 8f + i * 4f), style = Stroke(0.7f, cap = StrokeCap.Round))
        }
    }
    if (sick) {
        // An ice pack on its head, and a sneeze.
        val top = headAt.y + headSize * 0.08f
        drawRoundRect(Color(0xFFB8C4C8), Offset(headAt.x + headSize * 0.32f, top), Size(headSize * 0.36f, headSize * 0.16f), CornerRadius(4f))
        drawRoundRect(Bone.copy(alpha = 0.5f), Offset(headAt.x + headSize * 0.32f, top), Size(headSize * 0.36f, headSize * 0.16f), CornerRadius(4f), style = Stroke(0.6f))
        drawLine(Wine, Offset(headAt.x + headSize * 0.5f, top - 2f), Offset(headAt.x + headSize * 0.5f, top + 2f), 1.4f, StrokeCap.Round)
        val c = Bone.copy(alpha = 0.55f)
        listOf(Offset(12f, 30f), Offset(8f, 36f), Offset(14f, 38f)).forEachIndexed { i, p -> drawCircle(c, 1.4f - i * 0.3f, p) }
        drawLine(c, Offset(18f, 32f), Offset(10f, 26f), 0.6f, StrokeCap.Round)
    }
}

/** Whatever it's wearing that sits on its head or neck. */
private fun DrawScope.wear(outfit: PetOutfit, headAt: Offset, headSize: Float) {
    when (outfit) {
        PetOutfit.BowTie -> bowTie(headAt, headSize)
        PetOutfit.WitchHat -> witchHat(headAt, headSize)
        PetOutfit.Scarf -> scarf(headAt, headSize)
        PetOutfit.Crown -> crown(headAt, headSize)
        PetOutfit.BellCollar -> collar(headAt, headSize, spiked = false)
        PetOutfit.SpikedCollar -> collar(headAt, headSize, spiked = true)
        PetOutfit.Bandana -> bandana(headAt, headSize)
        PetOutfit.MoonPendant -> moonPendant(headAt, headSize)
        PetOutfit.PumpkinHat -> pumpkinHat(headAt, headSize)
        PetOutfit.FlowerCrown -> flowerCrown(headAt, headSize)
        PetOutfit.Beret -> beret(headAt, headSize)
        PetOutfit.Sunglasses -> sunglasses(headAt, headSize)
        PetOutfit.Cape -> capeCollar(headAt, headSize)
        else -> Unit
    }
}

private fun DrawScope.bowTie(headAt: Offset, size: Float) {
    val cx = headAt.x + size * 0.5f
    val cy = headAt.y + size * 0.74f
    val left = Path().apply { moveTo(cx, cy); lineTo(cx - 9f, cy - 5f); lineTo(cx - 9f, cy + 5f); close() }
    val right = Path().apply { moveTo(cx, cy); lineTo(cx + 9f, cy - 5f); lineTo(cx + 9f, cy + 5f); close() }
    drawPath(left, Wine)
    drawPath(right, Wine)
    drawLine(WineLit, Offset(cx + 2f, cy - 2f), Offset(cx + 8f, cy - 4f), 0.7f)
    drawCircle(WineLit, 2.2f, Offset(cx, cy))
}

private fun DrawScope.witchHat(headAt: Offset, size: Float) {
    val cx = headAt.x + size * 0.5f
    val brimY = headAt.y + size * 0.2f
    drawOval(Plum, Offset(cx - size * 0.32f, brimY - 2.5f), Size(size * 0.64f, 6f))
    val cone = Path().apply {
        moveTo(cx - size * 0.2f, brimY)
        quadraticTo(cx - size * 0.08f, brimY - size * 0.36f, cx + size * 0.18f, brimY - size * 0.5f)
        quadraticTo(cx + size * 0.06f, brimY - size * 0.3f, cx + size * 0.2f, brimY)
        close()
    }
    drawPath(cone, Plum)
    drawPath(cone, Bone.copy(alpha = 0.25f), style = Stroke(0.6f))
    drawRect(Wine, Offset(cx - size * 0.19f, brimY - 5f), Size(size * 0.38f, 3.2f))
    drawRect(Candle, Offset(cx - 2f, brimY - 5.3f), Size(4f, 3.8f), style = Stroke(0.8f))
}

private fun DrawScope.scarf(headAt: Offset, size: Float) {
    val cx = headAt.x + size * 0.5f
    val cy = headAt.y + size * 0.72f
    drawRoundRect(Wine, Offset(cx - 15f, cy - 3.5f), Size(30f, 7f), CornerRadius(3.5f))
    for (i in 0..4) drawLine(WineLit, Offset(cx - 12f + i * 6f, cy - 3f), Offset(cx - 12f + i * 6f, cy + 3f), 1f)
    // The hanging end.
    drawRoundRect(Wine, Offset(cx + 5f, cy), Size(7f, 18f), CornerRadius(2f))
    drawLine(WineLit, Offset(cx + 5f, cy + 8f), Offset(cx + 12f, cy + 8f), 1f)
    for (i in 0..3) drawLine(Wine, Offset(cx + 6f + i * 1.8f, cy + 18f), Offset(cx + 6f + i * 1.8f, cy + 21f), 0.6f)
}

private fun DrawScope.crown(headAt: Offset, size: Float) {
    val cx = headAt.x + size * 0.5f
    val y = headAt.y + size * 0.2f
    val w = size * 0.3f
    val crown = Path().apply {
        moveTo(cx - w / 2, y)
        lineTo(cx - w / 2, y - 7f); lineTo(cx - w / 4, y - 3f); lineTo(cx, y - 9f)
        lineTo(cx + w / 4, y - 3f); lineTo(cx + w / 2, y - 7f); lineTo(cx + w / 2, y)
        close()
    }
    drawPath(crown, Candle)
    drawCircle(Wine, 1.2f, Offset(cx, y - 2.5f))
}

private fun DrawScope.batWings(build: PetBuild) {
    val y = if (build == PetBuild.Kitten) 58f else 48f
    listOf(-1f, 1f).forEach { side ->
        val x = 50f + side * 14f
        val wing = Path().apply {
            moveTo(x, y)
            lineTo(x + side * 30f, y - 16f)
            quadraticTo(x + side * 26f, y - 6f, x + side * 30f, y + 2f)
            quadraticTo(x + side * 22f, y + 0f, x + side * 20f, y + 8f)
            quadraticTo(x + side * 14f, y + 4f, x + side * 10f, y + 12f)
            close()
        }
        drawPath(wing, Plum)
        drawLine(Bone.copy(alpha = 0.35f), Offset(x, y), Offset(x + side * 30f, y - 16f), 0.7f)
        drawLine(Bone.copy(alpha = 0.25f), Offset(x + side * 6f, y + 2f), Offset(x + side * 20f, y + 8f), 0.5f)
    }
}

private val Pumpkin = Color(0xFFC46A3A)
private val PumpkinDark = Color(0xFF8E4426)
private val Gold = Color(0xFFD9B45A)
private val Leaf = Color(0xFF7D8C76)
private val Pink = Color(0xFFC4A3A8)

/** A point on the face, in the countdown face's own 100-unit coordinates. */
private fun face(headAt: Offset, size: Float, x: Float, y: Float) = Offset(headAt.x + size * x / 100f, headAt.y + size * y / 100f)

private fun DrawScope.collar(headAt: Offset, size: Float, spiked: Boolean) {
    val c = face(headAt, size, 50f, 76f)
    val w = size * 0.36f
    drawRoundRect(if (spiked) Color(0xFF1A161E) else Wine, Offset(c.x - w / 2f, c.y - 2.5f), Size(w, 5f), CornerRadius(2.5f))
    if (spiked) {
        drawRoundRect(Bone.copy(alpha = 0.35f), Offset(c.x - w / 2f, c.y - 2.5f), Size(w, 5f), CornerRadius(2.5f), style = Stroke(0.5f))
        for (i in 0..4) {
            val x = c.x - w / 2f + w * (i + 0.5f) / 5f
            drawPath(Path().apply { moveTo(x - 1.6f, c.y + 2.4f); lineTo(x, c.y + 6.5f); lineTo(x + 1.6f, c.y + 2.4f); close() }, Bone.copy(alpha = 0.9f))
        }
    } else {
        drawCircle(Gold, 3.2f, Offset(c.x, c.y + 5f))
        drawLine(Color(0xFF8A6A2A), Offset(c.x - 3f, c.y + 5f), Offset(c.x + 3f, c.y + 5f), 0.6f)
        drawCircle(Color(0xFF8A6A2A), 0.8f, Offset(c.x, c.y + 6.6f))
        drawCircle(Bone.copy(alpha = 0.7f), 0.8f, Offset(c.x - 1.2f, c.y + 3.8f))
    }
}

private fun DrawScope.bandana(headAt: Offset, size: Float) {
    val c = face(headAt, size, 50f, 74f)
    val w = size * 0.42f
    val tri = Path().apply { moveTo(c.x - w / 2f, c.y - 2f); lineTo(c.x + w / 2f, c.y - 2f); lineTo(c.x, c.y + w * 0.55f); close() }
    drawPath(tri, Color(0xFFC0504A))
    val r = kotlin.random.Random(3)
    repeat(7) { drawCircle(Bone.copy(alpha = 0.75f), 0.7f, Offset(c.x - w * 0.3f + r.nextFloat() * w * 0.6f, c.y + r.nextFloat() * w * 0.3f)) }
}

private fun DrawScope.moonPendant(headAt: Offset, size: Float) {
    val c = face(headAt, size, 50f, 72f)
    drawArc(Bone.copy(alpha = 0.6f), 20f, 140f, false, Offset(c.x - size * 0.16f, c.y - 6f), Size(size * 0.32f, 14f), style = Stroke(0.5f))
    val m = Offset(c.x, c.y + 9f)
    drawCircle(Candle, 3.4f, m)
    drawCircle(Ink, 2.9f, Offset(m.x + 1.6f, m.y - 1f))
}

private fun DrawScope.pumpkinHat(headAt: Offset, size: Float) {
    val c = face(headAt, size, 50f, 16f)
    val w = size * 0.34f
    drawOval(PumpkinDark, Offset(c.x - w / 2f, c.y - w * 0.36f), Size(w, w * 0.6f))
    drawOval(Pumpkin, Offset(c.x - w * 0.36f, c.y - w * 0.36f), Size(w * 0.72f, w * 0.6f))
    drawOval(Pumpkin, Offset(c.x - w * 0.16f, c.y - w * 0.38f), Size(w * 0.32f, w * 0.62f))
    drawLine(PumpkinDark, Offset(c.x - w * 0.18f, c.y - w * 0.3f), Offset(c.x - w * 0.18f, c.y + w * 0.18f), 0.6f)
    drawLine(PumpkinDark, Offset(c.x + w * 0.18f, c.y - w * 0.3f), Offset(c.x + w * 0.18f, c.y + w * 0.18f), 0.6f)
    drawLine(Color(0xFF4A3A2A), Offset(c.x, c.y - w * 0.36f), Offset(c.x + 1.5f, c.y - w * 0.52f), 1.6f, StrokeCap.Round)
    drawOval(Leaf, Offset(c.x + 1.5f, c.y - w * 0.56f), Size(5f, 2.6f))
}

private fun DrawScope.flowerCrown(headAt: Offset, size: Float) {
    val colors = listOf(Pink, Bone, Color(0xFFB8B2BF), Pink, Candle, Bone, Pink)
    for (i in colors.indices) {
        val t = i / (colors.size - 1f)
        val p = face(headAt, size, 26f + t * 48f, 22f - kotlin.math.sin(Math.PI * t).toFloat() * 7f)
        if (i % 2 == 1) drawOval(Leaf, Offset(p.x - 2.6f, p.y - 0.6f), Size(5.2f, 2.2f))
        drawCircle(colors[i], 2.1f, p)
        drawCircle(Wine, 0.6f, p)
    }
}

private fun DrawScope.beret(headAt: Offset, size: Float) {
    val c = face(headAt, size, 44f, 18f)
    rotate(-14f, c) {
        drawOval(Wine, Offset(c.x - size * 0.2f, c.y - 4.5f), Size(size * 0.4f, 9f))
        drawOval(WineLit, Offset(c.x - size * 0.14f, c.y - 4f), Size(size * 0.22f, 3f))
        drawLine(Wine, Offset(c.x, c.y - 4.5f), Offset(c.x + 0.6f, c.y - 7f), 1.2f, StrokeCap.Round)
    }
}

private fun DrawScope.sunglasses(headAt: Offset, size: Float) {
    listOf(39f, 61f).forEach { x ->
        val p = face(headAt, size, x, 44f)
        val w = size * 0.2f
        drawRoundRect(Color(0xFF0C0A0E), Offset(p.x - w / 2f, p.y - w * 0.36f), Size(w, w * 0.72f), CornerRadius(2.5f))
        drawRoundRect(Bone.copy(alpha = 0.6f), Offset(p.x - w / 2f, p.y - w * 0.36f), Size(w, w * 0.72f), CornerRadius(2.5f), style = Stroke(0.6f))
        drawLine(Bone.copy(alpha = 0.35f), Offset(p.x - w * 0.3f, p.y - w * 0.18f), Offset(p.x - w * 0.05f, p.y - w * 0.18f), 0.6f, StrokeCap.Round)
    }
    drawLine(Bone.copy(alpha = 0.6f), face(headAt, size, 49f, 42f), face(headAt, size, 51f, 42f), 0.7f)
}

private fun DrawScope.cape(build: PetBuild) {
    val top = if (build == PetBuild.Kitten) 56f else 40f
    val cape = Path().apply {
        moveTo(40f, top)
        quadraticTo(22f, top + 30f, 18f, 97f)
        lineTo(82f, 97f)
        quadraticTo(78f, top + 30f, 60f, top)
        close()
    }
    drawPath(cape, Wine)
    drawLine(WineLit.copy(alpha = 0.6f), Offset(30f, top + 24f), Offset(24f, 94f), 0.8f)
    drawLine(WineLit.copy(alpha = 0.6f), Offset(70f, top + 24f), Offset(76f, 94f), 0.8f)
}

private fun DrawScope.capeCollar(headAt: Offset, size: Float) {
    val c = face(headAt, size, 50f, 76f)
    listOf(-1f, 1f).forEach { side ->
        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x + side * size * 0.22f, c.y - 5f); lineTo(c.x + side * size * 0.12f, c.y + 4f); close() }, WineLit)
    }
    drawCircle(Gold, 1.6f, c)
}

private fun DrawScope.loaf(expression: CatExpression, build: PetBuild, outfit: PetOutfit = PetOutfit.None) {
    val w = when (build) {
        PetBuild.Kitten -> 24f
        PetBuild.Cat -> 24f
        PetBuild.Chonky -> 42f
    }
    tail("M${50 + w - 4} 90 C${50 + w + 2} 92 ${50 + w - 6} 96 ${50 - w / 2} 95", 5.5f, seed = 52)
    val body = svgPath("M${50 - w} 92 C${50 - w - 4} 70 ${50 - w / 2} 52 50 52 C${50 + w / 2} 52 ${50 + w + 4} 70 ${50 + w} 92 Z")
    inkFur(body, seed = 53)
    head(if (expression == CatExpression.Happy) CatExpression.Bored else expression, Offset(25f, 18f), 50f)
    wear(outfit, Offset(25f, 18f), 50f)
}

private fun DrawScope.asleep(build: PetBuild) {
    val k = when (build) {
        PetBuild.Kitten -> 0.78f
        PetBuild.Cat -> 1f
        PetBuild.Chonky -> 1.2f
    }
    scale(k, k, pivot = Offset(50f, 80f)) {
        val body = union(oval(52f, 68f, 30f, 19f), oval(30f, 62f, 14f, 13f))
        inkFur(body, seed = 61)
        // Ears of the tucked head.
        val ears = Path().apply {
            moveTo(20f, 55f); lineTo(19f, 44f); lineTo(27f, 51f); close()
            moveTo(31f, 50f); lineTo(36f, 41f); lineTo(39f, 51f); close()
        }
        drawPath(ears, Ink)
        furEdge(ears, seed = 62, density = 60f, length = 1f..2.5f, light = Offset(0.9f, -0.35f), ambient = 0.2f)
        // Closed eyes.
        val lid = Stroke(1.1f, cap = StrokeCap.Round)
        drawArc(Bone.copy(alpha = 0.8f), 20f, 140f, false, Offset(21f, 60f), Size(6f, 3f), style = lid)
        drawArc(Bone.copy(alpha = 0.8f), 20f, 140f, false, Offset(30f, 60f), Size(6f, 3f), style = lid)
        // Tail wrapped round the front, over the nose.
        tail("M80 78 C78 90 58 90 40 86 C30 84 22 78 18 70", 6f, seed = 63)
    }
}

private enum class SideMode { Walk, Eat, Pounce }

/** A tapered leg from [top] down to a paw on the ground at [footX]. */
private fun leg(topX: Float, topY: Float, footX: Float, groundY: Float, thigh: Float, hind: Boolean): Path {
    val w = thigh
    val kneeX = (topX + footX) / 2f + if (hind) -3f else 0f
    val kneeY = topY + (groundY - topY) * 0.55f
    return Path().apply {
        moveTo(topX - w / 2f, topY)
        quadraticTo(kneeX - w * 0.45f, kneeY, footX - 2.6f, groundY - 2f)
        quadraticTo(footX - 2.6f, groundY + 0.6f, footX + 0.5f, groundY + 0.6f)
        quadraticTo(footX + 4.4f, groundY + 0.6f, footX + 3.4f, groundY - 2.5f)
        quadraticTo(kneeX + w * 0.35f, kneeY, topX + w / 2f, topY)
        close()
    }
}

/** Seen from the side, facing right. */
private fun DrawScope.side(build: PetBuild, frame: Int, mode: SideMode) {
    val k = when (build) {
        PetBuild.Kitten -> 0.8f
        else -> 1f
    }
    val belly = if (build == PetBuild.Chonky) 6f else 0f
    scale(k, k, pivot = Offset(50f, 90f)) {
        // Stride: legs swing in pairs.
        val step = when (frame % 4) {
            0 -> -4f
            1 -> 0f
            2 -> 4f
            else -> 0f
        }
        val ground = 90f
        val headC = when (mode) {
            SideMode.Walk -> Offset(79f, 37f)
            SideMode.Eat -> Offset(80f, 70f)
            SideMode.Pounce -> Offset(80f, 60f)
        }
        val body = when (mode) {
            SideMode.Pounce -> svgPath("M18 48 C18 40 28 40 40 46 C52 52 64 56 72 58 C78 60 78 68 72 72 C58 76 36 72 24 64 C19 60 17 54 18 48 Z")
            SideMode.Eat -> svgPath("M22 50 C22 44 30 42 42 43 C54 44 62 48 68 54 C72 58 72 63 68 66 C58 ${69 + belly} 40 ${69 + belly} 30 65 C24 62 22 56 22 50 Z")
            SideMode.Walk -> svgPath("M22 52 C22 46 30 44 42 45 C54 46 62 44 68 43 C72 43 74 47 74 52 C74 58 72 62 68 64 C58 ${67 + belly} 40 ${67 + belly} 30 64 C24 62 22 57 22 52 Z")
        }
        val legs = when (mode) {
            SideMode.Pounce -> listOf(
                leg(26f, 56f, 22f, 82f, 13f, true), leg(34f, 58f, 30f, 82f, 13f, true),
                leg(66f, 66f, 72f, 82f, 7f, false), leg(72f, 66f, 78f, 82f, 7f, false),
            )
            SideMode.Eat -> listOf(
                leg(29f, 56f, 27f - step, ground, 10f, true), leg(36f, 56f, 34f + step, ground, 9f, true),
                leg(62f, 56f, 64f + step, ground, 6f, false), leg(67f, 56f, 69f - step, ground, 6f, false),
            )
            SideMode.Walk -> listOf(
                leg(29f, 55f, 27f + step, ground, 10f, true), leg(36f, 55f, 34f - step, ground, 9f, true),
                leg(63f, 55f, 65f - step, ground, 6f, false), leg(68f, 55f, 70f + step, ground, 6f, false),
            )
        }
        when (mode) {
            SideMode.Walk -> tail("M22 50 C12 46 8 34 12 24 C13 20 17 18 20 20", 5f, seed = 71)
            SideMode.Eat -> tail("M22 52 C10 56 6 66 10 78", 5f, seed = 72)
            SideMode.Pounce -> tail("M19 48 C16 36 ${18 + step} 26 ${14 + step} 18", 5f, seed = 73)
        }
        val headPath = union(
            oval(headC.x, headC.y, 11.5f, 10f),
            oval(headC.x + 8.5f, headC.y + 3.5f, 6.5f, 5f),
            Path().apply {
                moveTo(headC.x - 9f, headC.y - 3f)
                quadraticTo(headC.x - 9f, headC.y - 12f, headC.x - 6f, headC.y - 18f)
                quadraticTo(headC.x - 1f, headC.y - 12f, headC.x + 1f, headC.y - 8f)
                close()
                moveTo(headC.x - 1f, headC.y - 9f)
                quadraticTo(headC.x + 3f, headC.y - 15f, headC.x + 5f, headC.y - 20f)
                quadraticTo(headC.x + 8f, headC.y - 13f, headC.x + 8.5f, headC.y - 6f)
                close()
            },
        )
        val neck = when (mode) {
            SideMode.Eat -> svgPath("M62 46 C68 50 74 58 78 62 L74 76 C68 70 62 62 58 56 Z")
            SideMode.Pounce -> svgPath("M66 56 L78 54 L80 68 L66 70 Z")
            SideMode.Walk -> svgPath("M62 44 C66 38 70 33 74 31 L84 44 C80 50 74 54 70 56 Z")
        }
        // The far legs sit a shade back.
        listOf(legs[1], legs[3]).forEach { drawPath(it, Ink) }
        val near = union(body, neck, headPath, legs[0], legs[2])
        inkFur(near, seed = 70, density = 42f)
        listOf(legs[1], legs[3]).forEach { drawPath(it, Bone.copy(alpha = 0.14f), style = Stroke(0.5f)) }
        // Inner ear.
        drawPath(
            Path().apply { moveTo(headC.x + 0.5f, headC.y - 9f); lineTo(headC.x + 4.2f, headC.y - 16.5f); lineTo(headC.x + 6.8f, headC.y - 7f); close() },
            Bone.copy(alpha = 0.7f),
        )
        val eye = Offset(headC.x + 4.5f, headC.y - 1f)
        when (mode) {
            SideMode.Pounce -> {
                drawCircle(Bone, 3.8f, eye)
                drawCircle(Ink, 2.8f, Offset(eye.x + 0.6f, eye.y))
                drawCircle(Bone, 0.7f, Offset(eye.x + 1.4f, eye.y - 1f))
            }
            SideMode.Eat -> drawArc(Bone.copy(alpha = 0.85f), 20f, 140f, false, Offset(eye.x - 3f, eye.y - 1f), Size(6f, 3f), style = Stroke(1f, cap = StrokeCap.Round))
            SideMode.Walk -> {
                drawOval(Bone, Offset(eye.x - 3.6f, eye.y - 2.6f), Size(7.2f, 5.2f))
                drawOval(Ink, Offset(eye.x - 0.2f, eye.y - 2.4f), Size(2.2f, 4.8f))
            }
        }
        drawCircle(Bone.copy(alpha = 0.7f), 0.9f, Offset(headC.x + 14.5f, headC.y + 1.5f))
        listOf(-1.5f, 0.5f, 2.5f).forEachIndexed { i, dy ->
            whisker(Offset(headC.x + 11f, headC.y + 4.5f + dy * 0.3f), Offset(headC.x + 25f, headC.y + 2f + dy * 2f), (i - 1) * 0.8f, Bone.copy(alpha = 0.6f), 0.4f)
        }
        if (mode == SideMode.Eat) {
            drawArc(Wine, 0f, 180f, true, Offset(72f, 80f), Size(26f, 14f))
            drawLine(WineLit, Offset(74f, 87f), Offset(96f, 87f), 0.8f)
            for (i in 0..4) drawCircle(Color(0xFFB08A5E), 1.3f, Offset(78f + i * 4f, 86f))
        }
    }
}

private fun DrawScope.bellyUp(build: PetBuild) {
    val k = if (build == PetBuild.Kitten) 0.8f else 1f
    val wide = if (build == PetBuild.Chonky) 6f else 0f
    scale(k, k, pivot = Offset(50f, 80f)) {
        // Lying on its back, head on the left tipped towards you, paws curled up in the air.
        tail("M80 76 C90 78 94 86 88 92", 5f, seed = 82)
        val body = union(
            svgPath("M28 72 C28 60 40 56 54 56 C${70 + wide} 56 ${82 + wide} 62 ${82 + wide} 72 C${82 + wide} 82 70 ${86 + wide / 2} 54 ${86 + wide / 2} C40 ${86 + wide / 2} 28 82 28 72 Z"),
            // Front paws, curled at the chest.
            svgPath("M38 60 C36 52 38 46 42 44 C46 44 47 48 45 52 C44 55 44 58 44 60 Z"),
            svgPath("M48 58 C47 50 50 45 54 44 C58 45 58 49 55 52 C53 55 53 57 53 59 Z"),
            // Hind legs splayed up.
            svgPath("M68 60 C70 50 74 44 79 44 C82 46 80 52 77 56 C75 59 74 61 74 63 Z"),
            svgPath("M76 64 C82 58 88 56 91 58 C92 62 87 65 83 67 C80 68 78 69 77 70 Z"),
        )
        inkFur(body, seed = 81, density = 40f)
        // Pale fluffy belly.
        val r = kotlin.random.Random(9)
        repeat(40) {
            val x = 46f + r.nextFloat() * 26f
            val y = 64f + r.nextFloat() * 14f
            drawLine(Bone.copy(alpha = 0.18f + r.nextFloat() * 0.12f), Offset(x, y), Offset(x + 1.2f, y + 2.4f), 0.5f, StrokeCap.Round)
        }
        // Toe beans.
        listOf(Offset(42f, 45f), Offset(54f, 45f), Offset(79f, 46f), Offset(90f, 59f)).forEach { drawCircle(Bean, 1.6f, it) }
        // Its head, upside down: face the countdown cat's, rotated.
        rotate(170f, Offset(22f, 70f)) { head(CatExpression.Ecstatic, Offset(4f, 52f), 36f) }
    }
}
