package com.choomhub.app.feature.focus

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.core.designsystem.theme.LocalSkyPhase
import com.choomhub.app.core.designsystem.theme.SkyPhase
import com.choomhub.app.domain.model.CatActivity
import com.choomhub.app.domain.model.CatOuting
import com.choomhub.app.domain.model.CatRules
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Where someone is in the focus room. */
enum class Spot { Away, Desk, Beanbag }

/** What the room shows for one person. */
@Immutable
data class RoomPerson(
    val name: String,
    val spot: Spot,
    /** The TV line: "1:02:33" focusing, "4:12" on a break, "+1:05" over it, "--:--" away. */
    val tvTime: String,
    val tvOverrun: Boolean = false,
    /** Scoreboard: hours this week and all time, already formatted ("6.5", "41"). */
    val weekHours: String,
    val totalHours: String,
    /** This week's focus, to decide whose lap the cat picks when you're both on a break. */
    val weekMinutes: Long = 0,
)

/**
 * The room's whole state. [him] has the left desk and beanbag, [her] the right ones. [jam] is
 * whether there's a jam link in the record player; [catOuting] where the cat went when someone
 * last tapped it (it's asleep in its bed otherwise).
 */
@Immutable
data class RoomScene(
    val him: RoomPerson,
    val her: RoomPerson,
    val together: Boolean,
    val jam: Boolean = false,
    val catOuting: CatOuting? = null,
)

/** Where the cat is. */
enum class CatSpot { Bed, HisLap, HerLap, Out }

/**
 * On whoever's on a break; if you both are, on the one with more focus this week (her, on a
 * tie). Otherwise out on its trip if someone sent it, else asleep in its bed by the fire.
 */
fun catSpot(scene: RoomScene, nowMs: Long): CatSpot {
    val his = scene.him.spot == Spot.Beanbag
    val hers = scene.her.spot == Spot.Beanbag
    return when {
        his && hers -> if (scene.him.weekMinutes > scene.her.weekMinutes) CatSpot.HisLap else CatSpot.HerLap
        his -> CatSpot.HisLap
        hers -> CatSpot.HerLap
        CatRules.isOut(scene.catOuting, nowMs) -> CatSpot.Out
        else -> CatSpot.Bed
    }
}

/** The room is drawn on this pixel grid (taller on taller screens), then scaled up with hard edges. */
internal const val ROOM_W = 128
internal const val BASE_H = 188
internal const val MAX_H = 232

/** The part of the grid that's shown: a few pixels are trimmed off each side so the room fills the screen. */
internal const val VIEW_X = 4
internal const val VIEW_W = 120

/** The bitmap is twice the room grid, so text can use half-size pixels. */
private const val FINE = 2

/** How many rows of room fit a space [width] × [height] (any unit), between [BASE_H] and [MAX_H]. */
fun roomRowsFor(width: Float, height: Float): Int {
    if (width <= 0f) return BASE_H
    return (height / (width / VIEW_W)).toInt().coerceIn(BASE_H, MAX_H)
}

/** The record player's spot in the room (in room pixels), for taps. */
internal val RECORD_PLAYER_BOUNDS = android.graphics.Rect(5, 50, 30, 84)

/** What a tap in the room landed on. */
internal sealed interface RoomTap {
    data object Cat : RoomTap
    data object RecordPlayer : RoomTap
    data class Egg(val id: String) : RoomTap
}

/**
 * The focus room as a little pixel-art scene (a cozy retro game, seen from above with the
 * back wall facing you). Fire, rain, lights and the cat move when ambient motion is on; tapping
 * things does small things (and tapping the sleeping cat sends it off for a minute).
 */
@Composable
fun FocusRoomArt(
    scene: RoomScene,
    modifier: Modifier = Modifier,
    rows: Int = BASE_H,
    onRecordPlayer: () -> Unit = {},
    onCat: () -> Unit = {},
) {
    val sky = LocalSkyPhase.current
    val animate = LocalAmbientMotionEnabled.current
    val currentScene by rememberUpdatedState(scene)
    var frame by remember { mutableIntStateOf(0) }
    val effects = remember { mutableStateMapOf<String, Long>() }
    var lampOff by remember { mutableStateOf(false) }
    val catOut = CatRules.isOut(scene.catOuting, System.currentTimeMillis())
    val busy = effects.isNotEmpty() || catOut
    LaunchedEffect(animate, busy) {
        while (animate || busy) {
            delay(FRAME_MS)
            frame++
            val now = System.currentTimeMillis()
            effects.entries.removeAll { now - it.value > EFFECT_MS }
            if (!animate && effects.isEmpty() && !CatRules.isOut(scene.catOuting, now)) break
        }
    }
    val bitmap = remember(rows) { createBitmap(ROOM_W * FINE, rows * FINE) }
    val painter = remember(bitmap) { PixelPainter(bitmap, rows) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(
        modifier
            .aspectRatio(VIEW_W.toFloat() / rows)
            .semantics { contentDescription = "The focus room" }
            .pointerInput(painter) {
                detectTapGestures { tap ->
                    val unit = size.width.toFloat() / VIEW_W
                    val x = (tap.x / unit).toInt() + VIEW_X
                    val y = (tap.y / unit).toInt()
                    when (val hit = painter.hit(x, y, currentScene, System.currentTimeMillis())) {
                        RoomTap.Cat -> onCat()
                        RoomTap.RecordPlayer -> onRecordPlayer()
                        is RoomTap.Egg -> if (hit.id == "lamp") lampOff = !lampOff else effects[hit.id] = System.currentTimeMillis()
                        null -> Unit
                    }
                }
            },
    ) {
        @Suppress("UNUSED_EXPRESSION")
        frame
        painter.paint(scene, sky, frame, System.currentTimeMillis(), effects.toMap(), lampOff)
        drawImage(
            image,
            srcOffset = IntOffset(VIEW_X * FINE, 0),
            srcSize = IntSize(VIEW_W * FINE, rows * FINE),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

private const val FRAME_MS = 140L
private const val EFFECT_MS = 2_000L

/** One frame of the room, the whole grid at twice its size (for previews and tests). */
internal fun renderRoomFrame(
    scene: RoomScene,
    sky: SkyPhase,
    frame: Int,
    rows: Int = BASE_H,
    nowMs: Long = 0,
    effects: Map<String, Long> = emptyMap(),
): Bitmap = createBitmap(ROOM_W * FINE, rows * FINE).also { PixelPainter(it, rows).paint(scene, sky, frame, nowMs, effects, false) }

// ---------------------------------------------------------------------------------------------
// Palette (ARGB ints).

private object P {
    const val CLEAR = 0x00000000
    const val INK = 0xFF07060A.toInt()
    const val WALL = 0xFF2A2233.toInt()
    const val WALL2 = 0xFF302739.toInt()
    const val TRIM = 0xFF17121C.toInt()
    const val SIDE = 0xFF1B1520.toInt()
    const val FLOOR = 0xFF2C2027.toInt()
    const val FLOOR_LIT = 0xFF33242A.toInt()
    const val FLOOR_LIT2 = 0xFF3B2A2B.toInt()
    const val PLANK = 0xFF20171D.toInt()
    const val WOOD = 0xFF4A372F.toInt()
    const val WOOD_MID = 0xFF3B2B25.toInt()
    const val WOOD_DARK = 0xFF261B18.toInt()
    const val STONE = 0xFF57505E.toInt()
    const val STONE_DARK = 0xFF3C3542.toInt()
    const val SOOT = 0xFF0B0809.toInt()
    const val BONE = 0xFFEDE6D6.toInt()
    const val CANDLE = 0xFFF2DFA8.toInt()
    const val AMBER = 0xFFE89A4A.toInt()
    const val ORANGE = 0xFFD46A3A.toInt()
    const val TOMATO = 0xFFC0504A.toInt()
    const val WINE = 0xFF5C2233.toInt()
    const val WINE_DARK = 0xFF3F1623.toInt()
    const val WINE_LIT = 0xFF74303F.toInt()
    const val PLUM = 0xFF4A3F52.toInt()
    const val PLUM_LIT = 0xFF5D5068.toInt()
    const val PLUM_DARK = 0xFF342C3B.toInt()
    const val VIOLET = 0xFF3B3349.toInt()
    const val LAVENDER = 0xFFB8B2BF.toInt()
    const val PINK = 0xFFC4A3A8.toInt()
    const val LEAF = 0xFF7D8C76.toInt()
    const val LEAF_DARK = 0xFF55604F.toInt()
    const val AMETHYST = 0xFF8B6FAE.toInt()
    const val AMETHYST_LIT = 0xFFB59CD6.toInt()
    const val SCREEN = 0xFF15111B.toInt()
    const val CHALK = 0xFF1F2620.toInt()
    const val METAL = 0xFF6B6872.toInt()
    const val LAPTOP = 0xFF2B2B33.toInt()
    const val LAPTOP_LIT = 0xFF3D3D48.toInt()
    const val FAINT = 0xFF5A5466.toInt()

    // People. Him: brown skin, black hair. Her: fair skin, dark brown hair.
    const val HIS_SKIN = 0xFF8D5A3B.toInt()
    const val HIS_SKIN_SHADE = 0xFF6E4429.toInt()
    const val HIS_HAIR = 0xFF0D0B0B.toInt()
    const val HIS_TOP = 0xFF4F5868.toInt()
    const val HIS_TOP_SHADE = 0xFF3C4351.toInt()
    const val HER_SKIN = 0xFFF1D3BE.toInt()
    const val HER_SKIN_SHADE = 0xFFDDB79E.toInt()
    const val HER_HAIR = 0xFF3B2418.toInt()
    const val HER_HAIR_LIT = 0xFF553424.toInt()
    const val HER_TOP = 0xFF6E3445.toInt()
    const val HER_TOP_SHADE = 0xFF552534.toInt()
    const val JEANS = 0xFF2E2F3D.toInt()
    const val BLUSH = 0xFFD99A98.toInt()
}

// ---------------------------------------------------------------------------------------------
// Sprites: one character per pixel, '.' is transparent.

private val SITTER_HIM = listOf(
    "...hhhhhh...",
    "..hhhhhhhh..",
    ".hhhhhhhhhh.",
    ".hhsssssshh.",
    ".hsseesseshh",
    ".hssssssssh.",
    "..sssmmsss..",
    "...ssssss...",
    "....dssd....",
    "..tttttttt..",
    ".tttttttttt.",
    "tttTttttTttt",
    "tttTttttTttt",
)

private val SITTER_HER = listOf(
    "...hhhhhh...",
    "..hhHhhhhh..",
    ".hhhhhhhhhh.",
    ".hhsssssshh.",
    "hhsseesseshh",
    "hhssssssssh.",
    "hhsbsmmsbshh",
    "hh.ssssss.hh",
    "hhh.dssd.hhh",
    "hhtttttttthh",
    "htttttttttth",
    "tttTttttTttt",
    "tttTttttTttt",
)

/** Lounging back on a beanbag, eyes shut, arms resting, legs out. */
private val LOUNGER_HIM = listOf(
    "....hhhhhh....",
    "...hhhhhhhh...",
    "...hhhhhhhh...",
    "...hsssssshh..",
    "...sddssddss..",
    "...ssssssss...",
    "....ssmmss....",
    "....dssssd....",
    "..tttttttttt..",
    ".tttttttttttt.",
    ".tTttttttttTt.",
    ".tTttttttttTt.",
    ".ss.jjjjjj.ss.",
    "...jjj..jjj...",
    "...jj....jj...",
    "..ss......ss..",
)

private val LOUNGER_HER = listOf(
    "....hhhhhh....",
    "...hhHhhhhh...",
    "..hhhhhhhhhh..",
    "..hhsssssshh..",
    "..hsddssddsh..",
    "..hsbssssbsh..",
    "..hh.smms.hh..",
    "..hhdssssdhh..",
    "..hhtttttthh..",
    ".hhtttttttthh.",
    ".tTttttttttTt.",
    ".tTttttttttTt.",
    ".ss.jjjjjj.ss.",
    "...jjj..jjj...",
    "...jj....jj...",
    "..ss......ss..",
)

private val GUITAR = listOf(
    "....pkkkp....",
    "....kkkkk....",
    "....pkkkp....",
    ".....nsn.....",
    ".....fsf.....",
    ".....nsn.....",
    ".....nsn.....",
    ".....fsf.....",
    ".....nsn.....",
    ".....nsn.....",
    "....eeeee....",
    "...ebbbbbe...",
    "..ebbllllbe..",
    "..ebllsllbe..",
    "..eblosolbe..",
    "..eblosolbe..",
    "...eblslbe...",
    "...eblslbe...",
    "..ebllsllbe..",
    ".ebbllsllbbe.",
    ".ebbllsllbbe.",
    ".ebbBBBBBbbe.",
    ".ebbllllllbe.",
    "..ebbbbbbbe..",
    "...eeeeeee...",
)

/** Sitting, seen from the side, facing right (towards the tank). */
private val CAT_SITTING = listOf(
    "......k.k",
    "......kkk",
    "......kwk",
    ".....kkkk",
    "....kkkk.",
    "...kkkkk.",
    "..kkkkkk.",
    "k.kkkkkk.",
    "kkkkkkkk.",
)

/** Up on its hind legs, facing left (the post), paw up and then down. */
private val CAT_PLAYING_A = listOf(
    "k.......",
    "kk.k.k..",
    ".kkkkk..",
    ".kwkkk..",
    "..kkkk..",
    "..kkkkk.",
    "..kkkkk.",
    "...kkkk.",
    "...kkkkk",
    "...k.kk.",
    "...k..k.",
)

private val CAT_PLAYING_B = listOf(
    "........",
    "...k.k..",
    "kkkkkk..",
    ".kwkkk..",
    "..kkkk..",
    "..kkkkk.",
    "..kkkkk.",
    "...kkkk.",
    "...kkkk.",
    "...k.kkk",
    "...k..k.",
)

private val CAT_COLORS = mapOf('k' to 0xFF07060A.toInt(), 'w' to 0xFFF2DFA8.toInt())

/** Walking, side view, facing right; two steps. */
private val CAT_WALK_A = listOf(
    "........k.k",
    "........kkk",
    "k.......kwk",
    ".kkkkkkkkkk",
    "..kkkkkkkk.",
    "..k.k..k.k.",
)

private val CAT_WALK_B = listOf(
    "........k.k",
    "........kkk",
    ".k......kwk",
    "k.kkkkkkkkk",
    "..kkkkkkkk.",
    "...k.k.k.k.",
)

/** On the windowsill, seen from behind, looking out. */
private val CAT_ON_SILL = listOf(
    "k...k",
    "kk.kk",
    "kkkkk",
    "kkkkk",
    ".kkk.",
    "kkkkk",
    "kkkkk",
    "kkkkk",
    "kkkkk",
)

private val CAT_CURLED = listOf(
    ".k.k......",
    ".kkk..kkk.",
    "kkkkkkkkkk",
    "kwkkkkkkkk",
    "kkkkkkkkkk",
    ".kkkkkkkk.",
)

private val CAT_ON_LAP = listOf(
    "k.k.....",
    "kkk.kkk.",
    "kwkkkkkk",
    "kkkkkkkk",
    ".kkkkkkk",
)

private val FLAMES = listOf(
    listOf(
        "....a.....",
        "...aa..a..",
        "..aoa.aa..",
        "..aooaoa.a",
        ".aocoooaaa",
        ".aocccooa.",
        "aaocccooaa",
    ),
    listOf(
        ".....a....",
        "..a..aa...",
        "..aa.aoa..",
        ".aoaaooa..",
        ".aoocooaa.",
        "aaocccooa.",
        "aaocccooaa",
    ),
    listOf(
        "...a......",
        "...aa..a..",
        "..aoa.aoa.",
        "..aooaoaa.",
        ".aooccoaa.",
        ".aocccooaa",
        "aaocccooaa",
    ),
)

// ---------------------------------------------------------------------------------------------
// A 3×5 pixel font for the TV and the scoreboard.

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
    '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
    '2' to listOf("##.", "..#", ".#.", "#..", "###"),
    '3' to listOf("##.", "..#", ".#.", "..#", "##."),
    '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
    '5' to listOf("###", "#..", "##.", "..#", "##."),
    '6' to listOf(".##", "#..", "###", "#.#", "###"),
    '7' to listOf("###", "..#", ".#.", ".#.", ".#."),
    '8' to listOf("###", "#.#", "###", "#.#", "###"),
    '9' to listOf("###", "#.#", "###", "..#", "##."),
    'A' to listOf(".#.", "#.#", "###", "#.#", "#.#"),
    'B' to listOf("##.", "#.#", "##.", "#.#", "##."),
    'C' to listOf(".##", "#..", "#..", "#..", ".##"),
    'D' to listOf("##.", "#.#", "#.#", "#.#", "##."),
    'E' to listOf("###", "#..", "##.", "#..", "###"),
    'F' to listOf("###", "#..", "##.", "#..", "#.."),
    'G' to listOf(".##", "#..", "#.#", "#.#", ".##"),
    'H' to listOf("#.#", "#.#", "###", "#.#", "#.#"),
    'I' to listOf("###", ".#.", ".#.", ".#.", "###"),
    'J' to listOf("..#", "..#", "..#", "#.#", ".#."),
    'K' to listOf("#.#", "#.#", "##.", "#.#", "#.#"),
    'L' to listOf("#..", "#..", "#..", "#..", "###"),
    'M' to listOf("#.#", "###", "###", "#.#", "#.#"),
    'N' to listOf("##.", "#.#", "#.#", "#.#", "#.#"),
    'O' to listOf(".#.", "#.#", "#.#", "#.#", ".#."),
    'P' to listOf("##.", "#.#", "##.", "#..", "#.."),
    'Q' to listOf(".#.", "#.#", "#.#", "##.", ".##"),
    'R' to listOf("##.", "#.#", "##.", "#.#", "#.#"),
    'S' to listOf(".##", "#..", ".#.", "..#", "##."),
    'T' to listOf("###", ".#.", ".#.", ".#.", ".#."),
    'U' to listOf("#.#", "#.#", "#.#", "#.#", "###"),
    'V' to listOf("#.#", "#.#", "#.#", "#.#", ".#."),
    'W' to listOf("#.#", "#.#", "###", "###", "#.#"),
    'X' to listOf("#.#", "#.#", ".#.", "#.#", "#.#"),
    'Y' to listOf("#.#", "#.#", ".#.", ".#.", ".#."),
    'Z' to listOf("###", "..#", ".#.", "#..", "###"),
    ':' to listOf(".", "#", ".", "#", "."),
    '.' to listOf(".", ".", ".", ".", "#"),
    '/' to listOf("..#", "..#", ".#.", "#..", "#.."),
    '+' to listOf("...", ".#.", "###", ".#.", "..."),
    '-' to listOf("...", "...", "###", "...", "..."),
    ' ' to listOf(".", ".", ".", ".", "."),
)

private fun glyphWidth(c: Char): Int = GLYPHS[c]?.first()?.length ?: 3

internal fun pixelTextWidth(s: String): Int = s.sumOf { glyphWidth(it) + 1 } - if (s.isEmpty()) 0 else 1

/** Keeps a string to what the pixel font can show, in at most [width] pixels. */
internal fun pixelFit(s: String, width: Int): String {
    val clean = s.uppercase().map { if (it in GLYPHS) it else ' ' }.joinToString("").trim()
    var out = clean
    while (out.isNotEmpty() && pixelTextWidth(out) > width) out = out.dropLast(1)
    return out.trimEnd()
}

// ---------------------------------------------------------------------------------------------

private class PixelPainter(private val bitmap: Bitmap, private val rows: Int) {
    private val canvas = android.graphics.Canvas(bitmap).apply { scale(FINE.toFloat(), FINE.toFloat()) }
    private val paint = Paint().apply { isAntiAlias = false }

    /** Extra rows on a taller screen: the bottom things move down by [low], the desks by [mid]. */
    private val low = rows - BASE_H
    private val mid = low / 2

    private var now = 0L
    private var fx: Map<String, Long> = emptyMap()

    /** How far through its little animation a tapped thing is (0..1), or null if it isn't playing. */
    private fun fx(id: String): Float? {
        val start = fx[id] ?: return null
        val t = (now - start).toFloat() / EFFECT_MS
        return t.takeIf { it in 0f..1f }
    }

    /** The colour already at a room pixel. */
    private fun at(x: Int, y: Int): Int = bitmap[x * FINE, y * FINE]

    /** One half-size pixel, in half-pixel coordinates (used for text). */
    private fun fine(hx: Int, hy: Int, color: Int) {
        paint.color = color
        val f = FINE.toFloat()
        canvas.drawRect(hx / f, hy / f, (hx + 1) / f, (hy + 1) / f, paint)
    }

    private fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) return
        paint.color = color
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    private fun px(x: Int, y: Int, color: Int) = rect(x, y, 1, 1, color)

    private fun hline(x: Int, y: Int, w: Int, color: Int) = rect(x, y, w, 1, color)

    private fun vline(x: Int, y: Int, h: Int, color: Int) = rect(x, y, 1, h, color)

    private fun sprite(x: Int, y: Int, rows: List<String>, colors: Map<Char, Int>) {
        rows.forEachIndexed { dy, row ->
            row.forEachIndexed { dx, c -> colors[c]?.let { px(x + dx, y + dy, it) } }
        }
    }

    /** Small pixel text at half-pixel coordinates [hx], [hy]. */
    private fun text(hx: Int, hy: Int, s: String, color: Int) {
        var cx = hx
        s.forEach { c ->
            val g = GLYPHS[c] ?: GLYPHS.getValue(' ')
            g.forEachIndexed { dy, row -> row.forEachIndexed { dx, ch -> if (ch == '#') fine(cx + dx, hy + dy, color) } }
            cx += g.first().length + 1
        }
    }

    /** A filled ellipse, pixel by pixel. */
    private fun oval(cx: Int, cy: Int, rx: Int, ry: Int, color: Int) {
        for (y in -ry..ry) for (x in -rx..rx) {
            val d = (x * x).toFloat() / (rx * rx) + (y * y).toFloat() / (ry * ry)
            if (d <= 1f) px(cx + x, cy + y, color)
        }
    }

    fun paint(scene: RoomScene, sky: SkyPhase, frame: Int, nowMs: Long, effects: Map<String, Long>, lampOff: Boolean) {
        now = nowMs
        fx = effects
        bitmap.eraseColor(P.CLEAR)
        floor(scene.together)
        backWall()
        fairyLights(frame)
        window(sky, frame)
        scoreboard(scene, frame)
        fireplace(scene.together, frame)
        tv(scene, frame)
        poster()
        floorLamp(lampOff)
        bookshelf(frame)
        recordPlayer(frame, scene.jam)
        val cat = catSpot(scene, nowMs)
        val trip = if (cat == CatSpot.Out) scene.catOuting else null
        guitar(frame, trip?.activity == CatActivity.PlayingGuitar && tripPhase(trip) == TripPhase.There)
        fishTank(frame)
        rug(frame)
        coffeeTable(frame)
        beanbag(BEAN_HIM_X, P.PLUM, P.PLUM_LIT, P.PLUM_DARK, fx("bean_him"))
        beanbag(BEAN_HER_X, P.WINE, P.WINE_LIT, P.WINE_DARK, fx("bean_her"))
        if (scene.him.spot == Spot.Beanbag) lounger(BEAN_HIM_X, his = true, catOnLap = cat == CatSpot.HisLap, frame = frame)
        if (scene.her.spot == Spot.Beanbag) lounger(BEAN_HER_X, his = false, catOnLap = cat == CatSpot.HerLap, frame = frame)
        if (scene.him.spot == Spot.Beanbag && scene.her.spot == Spot.Beanbag) holdHands()
        catBed(cat == CatSpot.Bed, frame)
        desk(left = true, here = scene.him.spot == Spot.Desk, frame = frame)
        desk(left = false, here = scene.her.spot == Spot.Desk, frame = frame)
        plant(frame)
        extras(frame)
        catCorner(frame)
        doormat(frame)
        umbrella(frame)
        monstera(frame)
        if (trip != null) catTrip(trip, frame)
    }

    /**
     * On taller screens there's more floor; fill it: a reading pile of cushions and books between the
     * rug and the desks, and a rock tumbler (it turns) and a cat toy below the desks.
     */
    private fun extras(frame: Int) {
        if (mid >= 6) {
            val y = 104 + mid / 2
            // Floor cushions and a book left open on one.
            oval(40, y + 1, 6, 3, P.PLUM_DARK)
            oval(40, y, 6, 3, P.PLUM)
            oval(50, y + 2, 5, 3, P.WINE)
            rect(37, y - 2, 6, 3, P.BONE); vline(40, y - 2, 3, P.FAINT)
            rect(56, y - 1, 6, 2, P.VIOLET); rect(57, y - 3, 5, 2, P.LEAF)
        }
        val gap = (146 + low) - (DESK_Y + mid + 16)
        if (gap >= 12) {
            val y = DESK_Y + mid + 16 + gap / 2 - 4
            // A rock tumbler on a crate: the drum turns (faster when tapped).
            rect(28, y + 4, 14, 6, P.WOOD_MID); hline(28, y + 4, 14, P.WOOD)
            rect(30, y, 10, 5, P.METAL)
            val spin = if (fx("tumbler") != null) frame else frame / 3
            for (i in 0 until 3) vline(31 + (spin + i * 3) % 9, y, 5, P.STONE_DARK)
            px(41, y + 2, P.TOMATO)
            // A toy mouse with a long tail.
            rect(86, y + 6, 3, 2, P.STONE); px(85, y + 6, P.PINK); px(87, y + 5, P.STONE)
            hline(89, y + 7, 3, P.FAINT); px(92, y + 8, P.FAINT)
        }
    }

    // --- The cat's trips ---

    private enum class TripPhase { Going, There, Coming }

    private fun tripPhase(trip: CatOuting): TripPhase {
        val t = now - trip.since.toEpochMilli()
        return when {
            t < CatRules.WALK_MS -> TripPhase.Going
            t < CatRules.WALK_MS + CatRules.STAY_MS -> TripPhase.There
            else -> TripPhase.Coming
        }
    }

    /** Where the cat stands (top-left of its sprite) for each trip, and where it walks to first. */
    private fun tripSpot(activity: CatActivity): Pair<Int, Int> = when (activity) {
        CatActivity.WatchingFish -> 90 to 92
        CatActivity.PlayingWithPost -> 13 to 161 + low
        CatActivity.PlayingGuitar -> 20 to 95
        CatActivity.Windowsill -> 15 to 40
    }

    private val bedSpot get() = CAT_BED_X - 5 to BEAN_Y - 5

    private fun catTrip(trip: CatOuting, frame: Int) {
        val (tx, ty) = tripSpot(trip.activity)
        val (bx, by) = bedSpot
        val t = now - trip.since.toEpochMilli()
        when (tripPhase(trip)) {
            TripPhase.Going -> walkCat(bx, by, tx, ty, t.toFloat() / CatRules.WALK_MS, frame)
            TripPhase.Coming -> walkCat(tx, ty, bx, by, (t - CatRules.WALK_MS - CatRules.STAY_MS).toFloat() / CatRules.WALK_MS, frame)
            TripPhase.There -> when (trip.activity) {
                CatActivity.WatchingFish -> {
                    val rows = CAT_SITTING.toMutableList()
                    if (frame % 6 < 3) rows[7] = ".kkkkkkk."
                    sprite(tx, ty, rows, CAT_COLORS)
                }
                CatActivity.PlayingWithPost -> sprite(tx, ty, if (frame % 4 < 2) CAT_PLAYING_A else CAT_PLAYING_B, CAT_COLORS)
                CatActivity.PlayingGuitar -> sprite(tx, ty, if (frame % 4 < 2) CAT_PLAYING_A else CAT_PLAYING_B, CAT_COLORS)
                CatActivity.Windowsill -> {
                    // Up on the sill, looking out at the rain, tail hanging down.
                    sprite(16, 24, CAT_ON_SILL, CAT_COLORS)
                    val sway = if (frame % 8 < 4) 0 else 1
                    vline(20 + sway, 33, 4, P.INK)
                }
            }
        }
    }

    private fun walkCat(fromX: Int, fromY: Int, toX: Int, toY: Int, progress: Float, frame: Int) {
        val p = progress.coerceIn(0f, 1f)
        val x = (fromX + (toX - fromX) * p).toInt()
        val y = (fromY + (toY - fromY) * p).toInt()
        val sprite = if (frame % 2 == 0) CAT_WALK_A else CAT_WALK_B
        val facingLeft = toX < fromX
        sprite(x, y, if (facingLeft) sprite.map { it.reversed() } else sprite, CAT_COLORS)
    }

    /** What's under a tap. The cat comes first (if it's asleep in its bed or on a lap). */
    fun hit(x: Int, y: Int, scene: RoomScene, nowMs: Long): RoomTap? {
        val cat = catSpot(scene, nowMs)
        fun inside(x0: Int, y0: Int, w: Int, h: Int) = x in x0 until x0 + w && y in y0 until y0 + h
        if (cat == CatSpot.Bed && inside(CAT_BED_X - 10, BEAN_Y - 8, 20, 14)) return RoomTap.Cat
        if (cat == CatSpot.HisLap && inside(BEAN_HIM_X - 6, BEAN_Y - 6, 12, 10)) return RoomTap.Egg("lap")
        if (cat == CatSpot.HerLap && inside(BEAN_HER_X - 6, BEAN_Y - 6, 12, 10)) return RoomTap.Egg("lap")
        if (RECORD_PLAYER_BOUNDS.contains(x, y)) return RoomTap.RecordPlayer
        val eggs = listOf(
            "window" to intArrayOf(6, 3, 25, 31),
            "board" to intArrayOf(32, 3, 23, 35),
            "tv" to intArrayOf(55, 2, 55, 19),
            "fire" to intArrayOf(66, 22, 32, 22),
            "poster" to intArrayOf(109, 7, 14, 20),
            "lights" to intArrayOf(4, 0, 120, 3),
            "lamp" to intArrayOf(4, 36, 10, 18),
            "books" to intArrayOf(104, 42, 17, 34),
            "tank" to intArrayOf(99, 84, 22, 22),
            "guitar" to intArrayOf(7, 84, 13, 25),
            "bean_him" to intArrayOf(BEAN_HIM_X - 9, BEAN_Y - 7, 18, 14),
            "bean_her" to intArrayOf(BEAN_HER_X - 9, BEAN_Y - 7, 18, 14),
            "tea" to intArrayOf(50, 78, 26, 11),
            "fossil" to intArrayOf(80, 90, 12, 10),
            "desk_him" to intArrayOf(12, DESK_Y + mid - 14, 44, 30),
            "desk_her" to intArrayOf(72, DESK_Y + mid - 14, 44, 30),
            "plant" to intArrayOf(58, 124 + mid, 13, 17),
            "tumbler" to intArrayOf(26, DESK_Y + mid + 16, 18, 30),
            "post" to intArrayOf(5, 146 + low, 18, 30),
            "bowls" to intArrayOf(23, 168 + low, 18, 10),
            "mat" to intArrayOf(50, 164 + low, 28, 20),
            "umbrella" to intArrayOf(87, 154 + low, 10, 28),
            "monstera" to intArrayOf(100, 148 + low, 22, 34),
        )
        return eggs.firstOrNull { (_, r) -> inside(r[0], r[1], r[2], r[3]) }?.let { RoomTap.Egg(it.first) }
    }

    // --- Room shell ---

    private fun floor(together: Boolean) {
        rect(0, WALL_H, ROOM_W, rows - WALL_H, P.FLOOR)
        val r = Random(4)
        var y = WALL_H + 1
        while (y < rows) {
            hline(0, y, ROOM_W, P.PLANK)
            var x = -r.nextInt(30)
            while (x < ROOM_W) {
                x += 22 + r.nextInt(18)
                if (x in 0 until ROOM_W) vline(x, y + 1, 6, P.PLANK)
            }
            y += 7
        }
        // Firelight on the floor, dithered like an old game.
        val cx = FIRE_X
        val cy = WALL_H + 4
        val rx = if (together) 40 else 32
        val ry = if (together) 22 else 16
        for (yy in WALL_H until minOf(rows, cy + ry)) for (xx in cx - rx..cx + rx) {
            if (xx !in 0 until ROOM_W) continue
            val d = ((xx - cx) * (xx - cx)).toFloat() / (rx * rx) + ((yy - cy) * (yy - cy)).toFloat() / (ry * ry)
            if (at(xx, yy) == P.PLANK) continue
            when {
                d < 0.3f -> px(xx, yy, P.FLOOR_LIT2)
                d < 0.6f -> px(xx, yy, P.FLOOR_LIT)
                d < 0.85f && (xx + yy) % 2 == 0 -> px(xx, yy, P.FLOOR_LIT)
            }
        }
        // Side walls, edge-on, at the edges of what's shown.
        rect(VIEW_X, WALL_H, 3, rows - WALL_H, P.SIDE)
        rect(VIEW_X + VIEW_W - 3, WALL_H, 3, rows - WALL_H, P.SIDE)
    }

    private fun backWall() {
        rect(0, 0, ROOM_W, WALL_H, P.WALL)
        var x = 2
        while (x < ROOM_W) {
            vline(x, 0, WALL_H, P.WALL2)
            vline(x + 1, 0, WALL_H, P.WALL2)
            x += 6
        }
        rect(0, WALL_H - 2, ROOM_W, 2, P.TRIM)
        hline(0, 0, ROOM_W, P.TRIM)
    }

    private fun window(sky: SkyPhase, frame: Int) {
        val x0 = 10
        val y0 = 7
        val w = 17
        val h = 22
        val (top, bottom, moon) = when (sky) {
            SkyPhase.Night -> Triple(0xFF07070A.toInt(), 0xFF1A1622.toInt(), true)
            SkyPhase.Dawn -> Triple(0xFF241A30.toInt(), 0xFF5A3A4A.toInt(), false)
            SkyPhase.Day -> Triple(0xFF4A4458.toInt(), 0xFF6E6578.toInt(), false)
            SkyPhase.Dusk -> Triple(0xFF2A1428.toInt(), 0xFF6B3040.toInt(), true)
        }
        rect(x0 - 2, y0 - 2, w + 4, h + 4, P.WOOD_DARK)
        rect(x0, y0, w, h / 2, top)
        rect(x0, y0 + h / 2, w, h - h / 2, bottom)
        if (moon) {
            // A crescent moon.
            vline(x0 + 11, y0 + 3, 4, P.BONE)
            px(x0 + 12, y0 + 2, P.BONE); px(x0 + 12, y0 + 7, P.BONE)
            px(x0 + 13, y0 + 2, P.BONE); px(x0 + 13, y0 + 7, P.BONE)
            px(x0 + 12, y0 + 3, P.LAVENDER); px(x0 + 12, y0 + 6, P.LAVENDER)
            px(x0 + 3, y0 + 2, P.BONE)
            px(x0 + 7, y0 + 5, P.LAVENDER)
        } else {
            rect(x0 + 11, y0 + 4, 3, 3, P.PINK)
        }
        // Rain: short streaks sliding down the glass.
        for (i in 0 until 7) {
            val dx = (i * 5 + 2) % w
            val speed = 1 + i % 2
            val dy = ((frame * speed) + i * 7) % (h + 3) - 3
            for (k in 0..2) {
                val yy = y0 + dy + k
                if (yy in y0 until y0 + h) px(x0 + dx, yy, if (k == 2) P.LAVENDER else P.FAINT)
            }
        }
        // Tapped: a flash of lightning.
        fx("window")?.let { t -> if (t < 0.25f && frame % 2 == 0) rect(x0, y0, w, h, P.LAVENDER) else if (t < 0.3f) rect(x0, y0, w, h, P.BONE) }
        vline(x0 + w / 2, y0, h, P.WOOD_DARK)
        hline(x0, y0 + h / 2, w, P.WOOD_DARK)
        // Sill, curtains and a tiny ammonite.
        rect(x0 - 3, y0 + h + 2, w + 6, 2, P.WOOD)
        rect(x0 - 4, y0 - 3, 4, h + 6, P.WINE)
        rect(x0 + w, y0 - 3, 4, h + 6, P.WINE)
        vline(x0 - 2, y0 - 3, h + 6, P.WINE_DARK)
        vline(x0 + w + 1, y0 - 3, h + 6, P.WINE_DARK)
        hline(x0 - 5, y0 - 4, w + 10, P.WOOD_DARK)
        px(x0 + 2, y0 + h + 1, P.PINK); px(x0 + 3, y0 + h + 1, P.PINK); px(x0 + 3, y0 + h, P.PINK)
    }

    private fun scoreboard(scene: RoomScene, frame: Int) {
        val x0 = 33
        val y0 = 4
        val w = 21
        val h = 33
        rect(x0 - 1, y0 - 1, w + 2, h + 2, P.WOOD)
        rect(x0, y0, w, h, P.CHALK)
        px(x0 + w / 2, y0 - 4, P.WOOD_DARK)
        px(x0 + w / 2 - 1, y0 - 3, P.FAINT); px(x0 + w / 2 + 1, y0 - 3, P.FAINT)
        px(x0 + w / 2 - 2, y0 - 2, P.FAINT); px(x0 + w / 2 + 2, y0 - 2, P.FAINT)
        val hx = x0 * FINE + 3
        val inner = w * FINE - 6
        text(hx, y0 * FINE + 4, "HRS", P.PINK)
        val label = "WK/ALL"
        text(hx + inner - pixelTextWidth(label), y0 * FINE + 4, label, P.FAINT)
        hline(x0 + 1, y0 + 6, w - 2, 0xFF2C362D.toInt())
        val leader = when {
            scene.him.weekMinutes > scene.her.weekMinutes -> 0
            scene.her.weekMinutes > scene.him.weekMinutes -> 1
            else -> -1
        }
        listOf(scene.him to P.LAVENDER, scene.her to P.PINK).forEachIndexed { i, (p, accent) ->
            val hy = y0 * FINE + 18 + i * 22
            val crown = if (leader == i) 6 else 0
            text(hx, hy, pixelFit(p.name, inner - crown), accent)
            if (crown > 0 && fx("board") != null && frame % 2 == 0) {
                // Tapped: the crown sparkles.
                fine(hx + inner - 8, hy - 1, P.CANDLE); fine(hx + inner + 1, hy + 5, P.CANDLE); fine(hx + inner - 7, hy + 6, P.AMBER)
            }
            if (crown > 0) {
                // A tiny crown for whoever's ahead this week.
                val cxh = hx + inner - 5
                for (d in 0 until 5) fine(cxh + d, hy + 4, P.CANDLE)
                fine(cxh, hy + 1, P.CANDLE); fine(cxh + 2, hy, P.CANDLE); fine(cxh + 4, hy + 1, P.CANDLE)
                for (d in 0 until 5) fine(cxh + d, hy + 3, P.CANDLE)
                fine(cxh, hy + 2, P.CANDLE); fine(cxh + 2, hy + 1, P.CANDLE); fine(cxh + 2, hy + 2, P.CANDLE); fine(cxh + 4, hy + 2, P.CANDLE)
            }
            val value = pixelFit("${p.weekHours} / ${p.totalHours}", inner)
            text(hx, hy + 8, value, P.BONE)
        }
        rect(x0 + w - 6, y0 + h, 4, 1, P.BONE)
    }

    private fun fireplace(together: Boolean, frame: Int) {
        val cx = FIRE_X
        // Chimney breast and stone surround.
        rect(cx - 16, 0, 32, WALL_H, P.STONE_DARK)
        rect(cx - 13, 25, 26, WALL_H - 25, P.STONE)
        for (row in 0 until 4) {
            val yy = 27 + row * 4
            hline(cx - 13, yy, 26, P.STONE_DARK)
            var xx = cx - 13 + if (row % 2 == 0) 3 else 0
            while (xx < cx + 13) {
                vline(xx, yy - 3, 3, P.STONE_DARK)
                xx += 6
            }
        }
        // Mantel, a candle and a crystal.
        rect(cx - 16, 23, 32, 2, P.WOOD)
        hline(cx - 16, 25, 32, P.WOOD_DARK)
        rect(cx - 13, 20, 1, 3, P.BONE)
        px(cx - 13, 19, if (frame % 4 < 2) P.AMBER else P.CANDLE)
        px(cx + 11, 22, P.AMETHYST); px(cx + 12, 22, P.AMETHYST); px(cx + 12, 21, P.AMETHYST_LIT); px(cx + 12, 20, P.AMETHYST)
        // Opening.
        rect(cx - 8, 30, 16, WALL_H - 30, P.SOOT)
        rect(cx - 7, 29, 14, 1, P.SOOT)
        // Flames (three frames) over two logs.
        val flame = FLAMES[(frame / 2) % FLAMES.size]
        val colors = mapOf('a' to P.TOMATO, 'o' to P.AMBER, 'c' to P.CANDLE)
        val flare = fx("fire")
        val tall = if (together || flare != null) 0 else 1
        sprite(cx - 5, WALL_H - 9 + tall, flame.drop(tall), colors)
        if (flare != null) {
            // Tapped: it roars up and throws sparks.
            sprite(cx - 5, WALL_H - 11, flame.take(3), colors)
            val r = Random((now / 140).toInt())
            repeat(5) { px(cx - 7 + r.nextInt(14), WALL_H - 12 - r.nextInt(6), if (it % 2 == 0) P.AMBER else P.CANDLE) }
        }
        rect(cx - 6, WALL_H - 3, 12, 1, P.WOOD_DARK)
        rect(cx - 5, WALL_H - 2, 10, 1, P.WOOD)
        // Hearth slab on the floor.
        rect(cx - 15, WALL_H, 30, 4, P.STONE_DARK)
        hline(cx - 15, WALL_H, 30, P.STONE)
        // Embers.
        if (frame % 3 == 0) px(cx - 2 + frame % 5, WALL_H - 10, P.AMBER)
    }

    private fun tv(scene: RoomScene, frame: Int) {
        val x0 = FIRE_X - 26
        val y0 = 3
        val w = 53
        val h = 17
        rect(x0 - 1, y0 - 1, w + 2, h + 2, P.INK)
        rect(x0, y0, w, h, P.SCREEN)
        hline(x0, y0 + h / 2, w, 0xFF1D1825.toInt())
        fx("tv")?.let { t ->
            // Tapped: a few hearts float up off the screen.
            for (i in 0 until 3) {
                val hx = x0 + 10 + i * 16
                val hy = y0 + h - 4 - (t * 18).toInt() - i * 2
                if (hy > y0 - 2) heart(hx, hy, if (i == 1) P.PINK else P.TOMATO)
            }
            return
        }
        listOf(scene.him to P.LAVENDER, scene.her to P.PINK).forEachIndexed { i, (p, accent) ->
            val yy = y0 + 2 + i * 8
            val hy = yy * FINE + 2
            val hx = x0 * FINE + 4
            val inner = w * FINE - 8
            val color = when {
                p.tvOverrun -> P.TOMATO
                p.spot == Spot.Away -> P.FAINT
                p.spot == Spot.Beanbag -> P.CANDLE
                else -> P.BONE
            }
            val status = when {
                p.tvOverrun -> "OVER"
                p.spot == Spot.Desk -> "FOCUS"
                p.spot == Spot.Beanbag -> "BREAK"
                else -> "AWAY"
            }
            val time = pixelFit(p.tvTime, 40)
            val timeX = hx + inner - 8 - pixelTextWidth(time)
            text(timeX, hy, time, color)
            text(hx, hy + 7, status, if (p.spot == Spot.Away) P.FAINT else 0xFF7A7286.toInt())
            text(hx, hy, pixelFit(p.name, timeX - hx - 4), accent)
            when (p.spot) {
                Spot.Desk -> if (frame % 8 < 6) rect(x0 + w - 4, yy + 1, 2, 2, P.TOMATO)
                Spot.Beanbag -> { rect(x0 + w - 4, yy + 1, 2, 3, P.BONE); px(x0 + w - 2, yy + 2, P.BONE) }
                Spot.Away -> Unit
            }
        }
    }

    private fun heart(x: Int, y: Int, c: Int) {
        px(x, y, c); px(x + 2, y, c); hline(x, y + 1, 3, c); px(x + 1, y + 2, c)
    }

    /** A geological cross-section poster: tilted strata and a fault. */
    private fun poster() {
        val x0 = 110
        val y0 = 8
        val w = 12
        val h = 18
        rect(x0 - 1, y0 - 1, w + 2, h + 2, P.WOOD_DARK)
        val bands = intArrayOf(0xFF6B5A4E.toInt(), 0xFF8C6F5E.toInt(), 0xFF4A3F52.toInt(), 0xFFB09A7E.toInt(), 0xFF5C3A3A.toInt(), 0xFF7D8C76.toInt())
        for (yy in 0 until h) for (xx in 0 until w) {
            // Layers tilt, then step down across a fault at the middle.
            // Tapped: the fault slips a little further.
            val shift = if (xx > w / 2) (if (fx("poster") != null) 5 else 3) else 0
            val band = ((yy + xx / 3 + shift) / 3) % bands.size
            px(x0 + xx, y0 + yy, bands[band])
        }
        for (yy in 0 until h) px(x0 + w / 2 + yy / 6, y0 + yy, P.INK)
        rect(x0, y0, w, 3, 0xFF4A4458.toInt())
        px(x0 + 3, y0 + 1, P.BONE)
    }

    /** Tapped, it switches off (and on again). */
    private fun floorLamp(off: Boolean) {
        val x = 9
        val y = 44
        // Light pool.
        if (!off) for (yy in -6..6) for (xx in -8..8) {
            if ((xx * xx) / 64f + (yy * yy) / 36f <= 1f && (xx + yy) % 2 == 0) {
                val c = at(x + xx, y + 8 + yy)
                if (c == P.FLOOR) px(x + xx, y + 8 + yy, P.FLOOR_LIT)
            }
        }
        vline(x, y - 2, 10, P.METAL)
        rect(x - 2, y + 8, 5, 1, P.METAL)
        rect(x - 3, y - 5, 7, 3, if (off) P.STONE else P.CANDLE)
        hline(x - 3, y - 6, 7, if (off) P.STONE_DARK else P.AMBER)
        // A short stack of books beside it.
        rect(x + 4, y + 5, 6, 2, P.WINE)
        rect(x + 5, y + 3, 5, 2, P.VIOLET)
        rect(x + 4, y + 1, 6, 2, P.LEAF)
    }

    /** Against the right wall: books, and one shelf of rocks (the geology corner). */
    private fun bookshelf(frame: Int) {
        val x0 = 104
        val y0 = WALL_H + 2
        val w = 17
        val h = 34
        rect(x0 + 1, y0 + h, w, 2, 0x55000000)
        rect(x0, y0, w, h, P.WOOD_DARK)
        rect(x0 + 1, y0 + 1, w - 2, h - 2, P.WOOD_MID)
        val books = intArrayOf(P.WINE, P.VIOLET, P.LEAF, P.PINK, P.LAVENDER, 0xFF4A1F1E.toInt(), P.BONE, P.PLUM)
        val r = Random(5)
        for (shelf in 0 until 4) {
            val base = y0 + 1 + (shelf + 1) * 8
            hline(x0 + 1, base, w - 2, P.WOOD_DARK)
            if (shelf == 2) {
                // Geode, tumbled stones and a quartz point.
                rect(x0 + 2, base - 4, 4, 4, P.STONE_DARK)
                rect(x0 + 3, base - 3, 2, 3, P.AMETHYST)
                px(x0 + 3, base - 2, P.AMETHYST_LIT)
                px(x0 + 7, base - 1, 0xFF8C6F5E.toInt()); px(x0 + 8, base - 1, 0xFF8C6F5E.toInt())
                px(x0 + 9, base - 1, P.LEAF); px(x0 + 10, base - 1, P.PINK); px(x0 + 10, base - 2, P.PINK)
                vline(x0 + 13, base - 4, 4, P.BONE); px(x0 + 12, base - 1, P.BONE)
                continue
            }
            var xx = x0 + 2
            while (xx < x0 + w - 2) {
                val bh = 5 + r.nextInt(3)
                val c = books[r.nextInt(books.size)]
                vline(xx, base - bh, bh, c)
                if (r.nextInt(3) == 0 && xx + 1 < x0 + w - 2) { xx++; vline(xx, base - bh, bh, c) }
                xx++
            }
            if (shelf == 0) {
                // Tapped: the little skull winks.
                val wink = fx("books") != null && frame % 4 < 2
                rect(x0 + w - 5, base - 3, 3, 3, P.BONE)
                px(x0 + w - 5, base - 2, if (wink) P.BONE else P.INK)
                px(x0 + w - 3, base - 2, P.INK)
            }
        }
        fx("books")?.let { t ->
            // ...and a book slides out of the bottom shelf and back.
            val out = if (t < 0.5f) (t * 8).toInt() else ((1f - t) * 8).toInt()
            vline(x0 + 6 - out.coerceAtMost(3), y0 + 1 + 4 * 8 - 6, 6, P.TOMATO)
            px(x0 + 3, y0 + 1 + 3 * 8 - 3, if (frame % 2 == 0) P.AMETHYST_LIT else P.BONE)
        }
    }

    /** A rug woven like rock strata: wavy sedimentary bands, with a fossil in one. */
    private fun rug(frame: Int) {
        val x0 = 30
        val y0 = 72
        val w = 66
        val h = 30
        val bands = intArrayOf(0xFF4A2B33.toInt(), 0xFF6B5A4E.toInt(), 0xFF3A2F45.toInt(), 0xFF5C3A3A.toInt(), 0xFF7A6A58.toInt(), 0xFF4A3F52.toInt())
        for (yy in 0 until h) for (xx in 0 until w) {
            if ((xx == 0 || xx == w - 1) && (yy == 0 || yy == h - 1)) continue
            val wave = if ((xx / 8) % 2 == 0) 0 else 1
            px(x0 + xx, y0 + yy, bands[((yy + wave) / 4) % bands.size])
        }
        for (xx in 1 until w - 1) {
            if (xx % 2 == 0) { px(x0 + xx, y0 - 1, P.FAINT); px(x0 + xx, y0 + h, P.FAINT) }
        }
        // Ammonite.
        val fx = x0 + w - 10
        val fy = y0 + h - 8
        hline(fx + 1, fy, 3, P.BONE); vline(fx, fy + 1, 3, P.BONE); vline(fx + 4, fy + 1, 2, P.BONE)
        hline(fx + 1, fy + 4, 3, P.BONE); px(fx + 2, fy + 2, P.BONE)
        if (fx("fossil") != null && frame % 2 == 0) {
            // Tapped: the ammonite glints.
            px(fx - 1, fy - 1, P.CANDLE); px(fx + 5, fy + 5, P.CANDLE); px(fx + 5, fy - 1, P.AMBER)
        }
    }

    /** Tapped, it squashes down and puffs back up. */
    private fun beanbag(cx: Int, base: Int, lit: Int, dark: Int, squish: Float?) {
        val s = if (squish != null && squish < 0.4f) 2 else 0
        oval(cx, BEAN_Y + 2, 9 + s / 2, 6, 0x55000000)
        oval(cx, BEAN_Y + s / 2, 9 + s / 2, 7 - s, base)
        oval(cx - 2, BEAN_Y - 3 + s, 4, 2, lit)
        hline(cx - 6, BEAN_Y + 4, 12, dark)
    }

    private fun lounger(cx: Int, his: Boolean, catOnLap: Boolean, frame: Int) {
        val colors = personColors(his)
        sprite(cx - 7, BEAN_Y - 12, if (his) LOUNGER_HIM else LOUNGER_HER, colors)
        // A mug on the floor beside them.
        val mx = if (his) cx - 11 else cx + 9
        rect(mx, BEAN_Y + 4, 3, 3, P.BONE)
        px(mx + 1, BEAN_Y + 4, 0xFF5A3A2A.toInt())
        px(mx + 3, BEAN_Y + 5, P.BONE)
        if (catOnLap) {
            sprite(cx - 4, BEAN_Y - 3, CAT_ON_LAP, mapOf('k' to P.INK, 'w' to P.FAINT))
            val pet = fx("lap")
            if (pet != null) heart(cx + 3, BEAN_Y - 8 - (pet * 8).toInt(), P.PINK) else zzz(cx + 4, BEAN_Y - 6, frame)
        }
    }

    private fun catBed(catHere: Boolean, frame: Int) {
        val cx = CAT_BED_X
        val cy = BEAN_Y + 1
        oval(cx, cy + 1, 8, 4, 0x55000000)
        oval(cx, cy, 8, 4, P.VIOLET)
        oval(cx, cy, 6, 3, P.PLUM)
        if (catHere) {
            sprite(cx - 5, cy - 4, CAT_CURLED, mapOf('k' to P.INK, 'w' to P.FAINT))
            zzz(cx + 3, cy - 8, frame)
        }
    }

    /** Little z's drifting up from a sleeping cat. */
    private fun zzz(x: Int, y: Int, frame: Int) {
        val t = frame % 12
        val zs = listOf(0, 4, 8)
        zs.forEachIndexed { i, start ->
            val age = (t - start + 12) % 12
            if (age > 7) return@forEachIndexed
            val zx = x + i + age / 3
            val zy = y - age
            val c = if (age < 5) P.LAVENDER else P.FAINT
            if (i == 2) {
                hline(zx, zy, 4, c); px(zx + 2, zy + 1, c); px(zx + 1, zy + 2, c); hline(zx, zy + 3, 4, c)
            } else {
                hline(zx, zy, 2, c); px(zx + 1, zy + 1, c); hline(zx + 1, zy + 2, 2, c)
            }
        }
    }

    private fun personColors(his: Boolean): Map<Char, Int> = if (his) {
        mapOf(
            'h' to P.HIS_HAIR, 'H' to P.HIS_HAIR, 's' to P.HIS_SKIN, 'd' to P.HIS_SKIN_SHADE, 'e' to P.INK, 'm' to P.HIS_SKIN_SHADE,
            'b' to P.HIS_SKIN, 't' to P.HIS_TOP, 'T' to P.HIS_TOP_SHADE, 'j' to P.JEANS,
        )
    } else {
        mapOf(
            'h' to P.HER_HAIR, 'H' to P.HER_HAIR_LIT, 's' to P.HER_SKIN, 'd' to P.HER_SKIN_SHADE, 'e' to P.INK, 'm' to P.BLUSH,
            'b' to P.BLUSH, 't' to P.HER_TOP, 'T' to P.HER_TOP_SHADE, 'j' to P.JEANS,
        )
    }

    /** Desk with its chair behind it; whoever sits there faces you over their laptop. */
    private fun desk(left: Boolean, here: Boolean, frame: Int) {
        val x0 = if (left) 12 else 72
        val y0 = DESK_Y + mid
        val w = 44
        val h = 14
        val cx = x0 + w / 2
        // Chair (pushed in when empty).
        val chairTop = if (here) y0 - 13 else y0 - 7
        rect(cx - 6, chairTop, 12, 8, P.WOOD_DARK)
        hline(cx - 6, chairTop, 12, P.WOOD)
        if (here) sprite(cx - 6, y0 - 12, if (left) SITTER_HIM else SITTER_HER, personColors(left))
        // Top, edge and shadow.
        rect(x0 + 1, y0 + h, w, 2, 0x55000000)
        rect(x0, y0, w, h, P.WOOD)
        hline(x0, y0 + 5, w, P.WOOD_MID)
        rect(x0, y0 + h - 2, w, 2, P.WOOD_DARK)
        // Laptop: lid up (its back to you, a little moon on it) or shut.
        if (here) {
            rect(cx - 7, y0 - 1, 14, 8, P.LAPTOP)
            hline(cx - 7, y0 - 1, 14, P.LAPTOP_LIT)
            px(cx, y0 + 3, P.LAVENDER); px(cx - 1, y0 + 2, P.LAVENDER); px(cx - 1, y0 + 4, P.LAVENDER)
        } else {
            rect(cx - 7, y0 + 3, 14, 6, P.LAPTOP)
            hline(cx - 7, y0 + 3, 14, P.LAPTOP_LIT)
        }
        // Lamp: warm when someone's here.
        val lx = if (left) x0 + 3 else x0 + w - 6
        rect(lx, y0 + 1, 3, 3, if (here) P.CANDLE else P.STONE_DARK)
        px(lx + 1, y0 + 4, P.METAL)
        if (here) {
            px(lx - 1, y0 + 2, P.AMBER); px(lx + 3, y0 + 2, P.AMBER); px(lx + 1, y0, P.AMBER)
        }
        // Open book.
        val bx = if (left) x0 + w - 13 else x0 + 6
        rect(bx, y0 + 7, 9, 5, P.BONE)
        vline(bx + 4, y0 + 7, 5, P.FAINT)
        hline(bx + 1, y0 + 9, 2, P.FAINT); hline(bx + 6, y0 + 9, 2, P.FAINT)
        // Coffee.
        val mx = if (left) x0 + w - 3 else x0 + 2
        rect(mx - 1, y0 + 1, 3, 3, if (left) P.VIOLET else P.WINE)
        px(mx, y0 + 2, 0xFF2A1A14.toInt())
        if (fx(if (left) "desk_him" else "desk_her") != null) {
            // Tapped: the coffee steams.
            for (i in 0 until 3) px(mx + ((frame + i) % 3) - 1, y0 - 1 - i * 2, if (i == 0) P.LAVENDER else P.FAINT)
        }
        if (left) {
            // His headphones.
            hline(x0 + 4, y0 + 7, 5, P.INK); px(x0 + 3, y0 + 8, P.INK); px(x0 + 9, y0 + 8, P.INK)
            rect(x0 + 2, y0 + 9, 2, 3, P.INK); rect(x0 + 9, y0 + 9, 2, 3, P.INK)
        } else {
            // Her rock hammer, hand lens and a sample.
            for (i in 0 until 7) px(x0 + w - 12 + i, y0 + 11 - i / 2, P.WOOD_MID)
            rect(x0 + w - 6, y0 + 6, 2, 4, P.METAL)
            rect(x0 + w - 15, y0 + 2, 3, 3, P.LAVENDER); px(x0 + w - 14, y0 + 3, P.WOOD)
            px(x0 + w - 12, y0 + 5, P.INK)
            rect(x0 + w - 20, y0 + 2, 3, 2, 0xFF8C6F5E.toInt())
        }
    }

    private fun plant(frame: Int) {
        val x = 64
        val y = 135 + mid
        val ruffle = fx("plant") != null
        rect(x - 3, y, 7, 5, 0xFF4A1F1E.toInt())
        hline(x - 3, y, 7, 0xFF5E2A28.toInt())
        val leaves = listOf(0 to -2, -2 to -3, 2 to -3, -3 to -5, 3 to -5, 0 to -6, -1 to -8, 1 to -9, -4 to -7, 4 to -7)
        leaves.forEachIndexed { i, (dx, dy) ->
            // Tapped: the leaves rustle.
            val sway = if (ruffle) (if ((frame + i) % 2 == 0) 1 else -1) * (if (dy < -4) 1 else 0) else 0
            px(x + dx + sway, y + dy, if (i % 2 == 0) P.LEAF else P.LEAF_DARK)
            px(x + dx + sway, y + dy + 1, P.LEAF_DARK)
        }
    }

    /** Warm string lights sagging along the top of the wall; a few twinkle. */
    private fun fairyLights(frame: Int) {
        val colors = intArrayOf(P.CANDLE, P.PINK, P.AMETHYST_LIT, P.AMBER)
        for (x in 0 until ROOM_W) {
            val sag = if ((x / 16) % 2 == 0) (x % 16) / 6 else (15 - x % 16) / 6
            px(x, 1 + sag, P.TRIM)
            if (x % 6 == 3) {
                val i = x / 6
                // Tapped: they chase along the wall.
                val on = if (fx("lights") != null) (frame + i) % 3 == 0 else (frame + i * 3) % 11 != 0
                px(x, 2 + sag, if (on) colors[i % colors.size] else P.FAINT)
            }
        }
    }

    /** A record player on a little cabinet; the record turns. */
    private fun recordPlayer(frame: Int, jam: Boolean) {
        val x0 = 7
        val y0 = 60
        rect(x0 + 1, y0 + 20, 20, 2, 0x55000000)
        rect(x0, y0 + 6, 20, 14, P.WOOD_MID)
        rect(x0, y0, 20, 7, P.WOOD)
        hline(x0, y0 + 6, 20, P.WOOD_DARK)
        // Records stood up inside.
        for (i in 0 until 6) vline(x0 + 2 + i * 3, y0 + 9, 9, intArrayOf(P.INK, P.WINE, P.INK, P.LEAF, P.INK, P.PINK)[i])
        // Turntable.
        rect(x0 + 2, y0 + 1, 13, 5, P.STONE_DARK)
        oval(x0 + 8, y0 + 3, 4, 2, P.INK)
        px(x0 + 8, y0 + 3, P.WINE)
        val spin = listOf(-3 to 0, -2 to -1, 0 to -2, 2 to -1, 3 to 0, 2 to 1, 0 to 2, -2 to 1)[frame % 8]
        px(x0 + 8 + spin.first, y0 + 3 + spin.second, P.FAINT)
        vline(x0 + 16, y0 + 1, 3, P.METAL); px(x0 + 15, y0 + 4, P.METAL)
        if (jam) {
            // There's a jam link inside: a glowing note bobs over the record player.
            val bob = if (frame % 8 < 4) 0 else 1
            val nx = x0 + 9
            val ny = y0 - 7 + bob
            vline(nx + 2, ny, 4, P.CANDLE)
            px(nx + 3, ny, P.CANDLE); px(nx + 4, ny + 1, P.CANDLE)
            rect(nx, ny + 3, 2, 2, P.CANDLE)
            if (frame % 6 < 3) { px(nx - 2, ny + 1, P.AMBER); px(nx + 6, ny + 3, P.AMBER) } else { px(nx - 1, ny - 1, P.PINK); px(nx + 5, ny + 5, P.PINK) }
            // And the record spins on the platter, lit.
            px(x0 + 8, y0 + 3, P.CANDLE)
        }
    }

    /** An acoustic guitar standing against the left wall; its strings shake when it's played. */
    private fun guitar(frame: Int, catPlaying: Boolean) {
        val x = 7
        val y = 84
        sprite(
            x,
            y,
            GUITAR,
            mapOf(
                'e' to 0xFF2A1A12.toInt(), 'b' to 0xFF8A5A34.toInt(), 'l' to 0xFFB98A55.toInt(), 'o' to P.INK,
                's' to 0xFFCFC6B4.toInt(), 'n' to P.WOOD_DARK, 'f' to P.METAL, 'k' to P.WOOD_DARK, 'p' to P.METAL,
                'B' to 0xFF1A120E.toInt(),
            ),
        )
        if (fx("guitar") != null || catPlaying) {
            // The string blurs, and notes drift up.
            val wob = if (frame % 2 == 0) -1 else 1
            for (yy in 13..20) px(x + 6 + wob, y + yy, 0xFFCFC6B4.toInt())
            val ny = y + 8 - (frame % 6)
            px(x + 10, ny, P.CANDLE); vline(x + 11, ny - 3, 3, P.CANDLE); px(x + 12, ny - 3, P.CANDLE)
        }
    }

    /** A little tank: murky water, a rock arch, sand, one fish and bubbles. */
    private fun fishTank(frame: Int) {
        val x0 = 100
        val y0 = 86
        val w = 20
        val h = 13
        rect(x0 - 1, y0 + h + 1, w + 2, 6, P.WOOD_MID)
        hline(x0 - 1, y0 + h + 1, w + 2, P.WOOD)
        rect(x0 - 1, y0 - 1, w + 2, h + 2, P.METAL)
        rect(x0, y0, w, h, 0xFF2D3A36.toInt())
        hline(x0, y0, w, 0xFF3E4F49.toInt())
        rect(x0, y0 + h - 2, w, 2, 0xFF8C7A62.toInt())
        // Rock arch.
        rect(x0 + 11, y0 + h - 6, 2, 4, P.STONE); rect(x0 + 16, y0 + h - 6, 2, 4, P.STONE); hline(x0 + 11, y0 + h - 7, 7, P.STONE)
        px(x0 + 3, y0 + h - 3, P.LEAF); px(x0 + 3, y0 + h - 4, P.LEAF); px(x0 + 4, y0 + h - 5, P.LEAF)
        // The fish swims back and forth.
        val span = w - 6
        val t = frame % (span * 2)
        val fishX = x0 + 2 + if (t < span) t else span * 2 - t
        var right = t < span
        // Tapped: the fish leaps out of the water, flips, and splashes back in.
        val flip = fx("tank")
        val fy = if (flip != null) {
            val arc = kotlin.math.sin(Math.PI * (flip / 0.6f).coerceAtMost(1f)).toFloat()
            if (flip > 0.3f) right = !right
            if (flip in 0.55f..0.75f) { px(fishX - 1, y0 - 1, P.LAVENDER); px(fishX + 3, y0 - 1, P.LAVENDER); px(fishX + 1, y0 - 2, P.LAVENDER) }
            y0 + 5 - (arc * 9).toInt()
        } else {
            y0 + 5
        }
        rect(fishX, fy, 3, 2, P.AMBER)
        px(if (right) fishX - 1 else fishX + 3, fy, P.TOMATO); px(if (right) fishX - 1 else fishX + 3, fy + 1, P.TOMATO)
        px(if (right) fishX + 2 else fishX, fy, P.INK)
        val by = y0 + h - 3 - frame % (h - 3)
        px(x0 + 6, by, P.LAVENDER)
        if (by - 3 > y0) px(x0 + 7, by - 3, P.FAINT)
    }

    /** A low table on the rug: a teapot, two cups and a deck of cards. */
    private fun coffeeTable(frame: Int) {
        val x0 = 50
        val y0 = 80
        rect(x0 + 1, y0 + 9, 26, 2, 0x55000000)
        rect(x0, y0, 26, 9, P.WOOD)
        hline(x0, y0 + 8, 26, P.WOOD_DARK)
        // Teapot.
        oval(x0 + 6, y0 + 4, 3, 2, P.PLUM_LIT)
        px(x0 + 6, y0 + 2, P.BONE)
        hline(x0 + 9, y0 + 3, 2, P.PLUM_LIT)
        px(x0 + 2, y0 + 4, P.PLUM_LIT)
        fx("tea")?.let {
            // Tapped: steam curls up from the spout.
            for (i in 0 until 4) px(x0 + 10 + ((frame + i) % 2), y0 + 1 - i * 2, if (i < 2) P.LAVENDER else P.FAINT)
        }
        // Cups.
        rect(x0 + 13, y0 + 3, 2, 2, P.BONE); px(x0 + 13, y0 + 3, 0xFF5A3A2A.toInt())
        rect(x0 + 17, y0 + 5, 2, 2, P.BONE); px(x0 + 17, y0 + 5, 0xFF5A3A2A.toInt())
        // Cards.
        rect(x0 + 21, y0 + 2, 3, 4, P.BONE); px(x0 + 22, y0 + 3, P.TOMATO)
        rect(x0 + 20, y0 + 4, 3, 4, P.WINE)
    }

    /** Both on a break: their hands meet between the beanbags. */
    private fun holdHands() {
        val y = BEAN_Y - 2
        val mid = (BEAN_HIM_X + BEAN_HER_X) / 2
        hline(BEAN_HIM_X + 6, y, mid - BEAN_HIM_X - 7, P.HIS_TOP)
        px(mid - 1, y, P.HIS_SKIN)
        px(mid, y, P.HER_SKIN)
        hline(mid + 1, y, BEAN_HER_X - 6 - mid - 1, P.HER_TOP)
        px(mid - 1, y + 1, P.HIS_SKIN_SHADE)
        px(mid, y + 1, P.HER_SKIN_SHADE)
        // A little heart above.
        val hy = y - 6
        px(mid - 1, hy, P.PINK); px(mid + 1, hy, P.PINK)
        hline(mid - 1, hy + 1, 3, P.PINK); px(mid, hy + 2, P.PINK)
    }

    /** The cat's things: a scratching post, a yarn ball and two bowls (with a fish-bone mat). */
    private fun catCorner(frame: Int) {
        val x0 = 8
        val y0 = 150 + low
        rect(x0 - 2, y0 + 22, 10, 3, P.WOOD_MID)
        rect(x0 + 1, y0, 4, 22, 0xFFB09A7E.toInt())
        for (i in 0 until 11) hline(x0 + 1, y0 + i * 2, 4, 0xFF8C7A62.toInt())
        rect(x0 - 1, y0 - 2, 8, 3, P.PLUM)
        // The yarn ball (tapped: it rolls off and back).
        val roll = fx("post")?.let { t -> (kotlin.math.sin(Math.PI * t) * 7).toInt() } ?: 0
        oval(x0 + 12 + roll, y0 + 22, 2, 2, P.PINK)
        px(x0 + 11 + roll, y0 + 21 + (frame % 2), 0xFFE2C4C8.toInt()); px(x0 + 14 + roll, y0 + 23, 0xFFE2C4C8.toInt())
        // Mat and bowls.
        rect(x0 + 16, y0 + 24, 16, 6, P.VIOLET)
        hline(x0 + 18, y0 + 27, 12, P.FAINT)
        for (i in 0 until 4) { px(x0 + 20 + i * 3, y0 + 26, P.FAINT); px(x0 + 20 + i * 3, y0 + 28, P.FAINT) }
        oval(x0 + 20, y0 + 22, 3, 2, P.TOMATO); hline(x0 + 19, y0 + 22, 3, 0xFF8C6F5E.toInt())
        oval(x0 + 28, y0 + 22, 3, 2, P.LAVENDER); hline(x0 + 27, y0 + 22, 3, 0xFF3E4F49.toInt())
        if (fx("bowls") != null) {
            // Tapped: a fresh heap of kibble, and a fish treat.
            hline(x0 + 18, y0 + 21, 5, 0xFFB08A5E.toInt()); px(x0 + 20, y0 + 20, 0xFFB08A5E.toInt())
            hline(x0 + 23, y0 + 19, 3, P.AMBER); px(x0 + 22, y0 + 19, P.TOMATO)
        }
    }

    /** A door mat with both your shoes kicked off beside it. */
    private fun doormat(frame: Int) {
        val x0 = 50
        val y0 = 172 + low
        // Tapped: the shoes do a little hop.
        val hop = if (fx("mat") != null && frame % 2 == 0) 1 else 0
        rect(x0, y0, 28, 10, P.WINE)
        rect(x0 + 1, y0 + 1, 26, 8, P.WINE_DARK)
        for (x in x0 + 2 until x0 + 26 step 2) px(x, y0 + 4, P.WINE)
        // His (dark) and hers (pink), a bit askew.
        rect(x0 + 3, y0 - 6 - hop, 3, 5, P.INK); rect(x0 + 7, y0 - 7 + hop, 3, 5, P.INK)
        px(x0 + 4, y0 - 6 - hop, P.BONE); px(x0 + 8, y0 - 7 + hop, P.BONE)
        rect(x0 + 18, y0 - 6 + hop, 3, 4, P.PINK); rect(x0 + 22, y0 - 5 - hop, 3, 4, P.PINK)
        px(x0 + 19, y0 - 6 + hop, P.WINE); px(x0 + 23, y0 - 5 - hop, P.WINE)
    }

    /** A wet umbrella in its stand, dripping. */
    private fun umbrella(frame: Int) {
        val shake = fx("umbrella")
        val x = 92 + if (shake != null) (if (frame % 2 == 0) 1 else -1) else 0
        val y = 158 + low
        rect(x - 3, y + 14, 7, 9, P.STONE_DARK)
        hline(x - 3, y + 14, 7, P.STONE)
        vline(x, y - 2, 16, P.INK)
        px(x - 1, y - 3, P.INK); px(x - 2, y - 2, P.INK)
        for (i in 0 until 8) hline(x - 2 + (if (i > 3) 1 else 0), y + 2 + i, 4 - (if (i > 5) 1 else 0), P.WINE)
        vline(x + 1, y + 3, 6, P.WINE_LIT)
        val drop = frame % 10
        if (drop < 6) px(x + 2, y + 10 + drop, P.LAVENDER)
        if (shake != null) {
            // Tapped: it shakes off a spray of drops.
            px(x - 4, y + 4 + frame % 3, P.LAVENDER); px(x + 5, y + 2 + frame % 4, P.LAVENDER); px(x + 4, y + 8, P.FAINT)
        }
    }

    /** A big monstera in the corner. */
    private fun monstera(frame: Int) {
        val x = 110
        val y = 172 + low
        val ruffle = fx("monstera") != null
        rect(x - 5, y, 11, 9, 0xFF5E2A28.toInt())
        hline(x - 5, y, 11, 0xFF74363A.toInt())
        rect(x - 4, y + 1, 9, 1, 0xFF3A1A18.toInt())
        val leaves = listOf(-6 to -8, 5 to -9, -1 to -14, -8 to -16, 7 to -17, 0 to -21)
        leaves.forEachIndexed { i, (dx0, dy) ->
            // Tapped: the leaves sway.
            val dx = dx0 + if (ruffle) (if ((frame + i) % 2 == 0) 1 else -1) else 0
            oval(x + dx, y + dy, 4, 3, if (i % 2 == 0) P.LEAF else P.LEAF_DARK)
            px(x + dx, y + dy, P.LEAF_DARK)
            px(x + dx + 2, y + dy - 1, P.FLOOR)
        }
        vline(x, y - 12, 12, P.LEAF_DARK)
        vline(x - 3, y - 6, 6, P.LEAF_DARK)
        vline(x + 3, y - 7, 7, P.LEAF_DARK)
    }

    private companion object {
        const val WALL_H = 40
        const val FIRE_X = 82
        const val BEAN_Y = 54
        const val BEAN_HIM_X = 70
        const val BEAN_HER_X = 88
        const val CAT_BED_X = 53
        const val DESK_Y = 124
    }
}
