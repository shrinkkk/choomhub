package com.choomhub.app.feature.world.constellation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.sin
import kotlin.random.Random

/** One meetup in the sky. [minutes] together sets how bright it burns. */
data class StarUi(val id: String, val date: LocalDate, val minutes: Int?, val place: String?)

private val SkyTop = Color(0xFF07060B)
private val SkyMid = Color(0xFF120E1C)
private val SkyLow = Color(0xFF1E1426)
private val StarBone = Color(0xFFEDE4D3)
private val StarWarm = Color(0xFFE6D6AE)
private val Dust = Color(0xFFB7A6D6)

/**
 * Where each meetup sits, in a unit square: rows that wind back and forth across the sky
 * (six dates a row), nudged off the grid so it reads as one constellation that grows with
 * every date rather than a chart.
 */
fun starPositions(count: Int): List<Offset> {
    val r = Random(7)
    val perRow = 6
    val rows = ((count + perRow - 1) / perRow).coerceAtLeast(1)
    return List(count) { i ->
        val row = i / perRow
        val col = i % perRow
        val x = if (row % 2 == 0) col else perRow - 1 - col
        val fx = 0.1f + x * (0.8f / (perRow - 1)) + (r.nextFloat() - 0.5f) * 0.08f
        val fy = 0.2f + row * (0.62f / rows.coerceAtLeast(2)) + (r.nextFloat() - 0.5f) * 0.12f + sin(x * 1.1f) * 0.04f
        Offset(fx, fy)
    }
}

/**
 * The night sky with the meetup stars joined in date order.
 * [time] (seconds) drives the twinkle, drifting dust and the odd shooting star; [preview]
 * is the small, quiet version that sits behind the Our World title.
 */
@Composable
fun ConstellationSky(
    stars: List<StarUi>,
    modifier: Modifier = Modifier,
    time: Float = 0f,
    preview: Boolean = false,
    focus: Int? = null,
    /** Pinch zoom (1 = the whole sky) and how far it's been dragged, in pixels. */
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
) {
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 9.sp, letterSpacing = 1.5.sp, color = Dust.copy(alpha = 0.6f))
    Canvas(modifier) {
        if (!preview) {
            drawRect(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyLow)))
            milkyWay(time)
        }
        // Far-off stars move a little with the pinch, for depth.
        backgroundStars(if (preview) 40 else 170, time, if (preview) 0.5f else 1f, 1f + (zoom - 1f) * 0.15f, pan * 0.15f)
        if (!preview) {
            moon(Offset(size.width * 0.84f, size.height * 0.1f), size.minDimension * 0.045f)
            shootingStar(time)
        }
        val pts = starPoints(stars.size, size, zoom, pan)
        // The lines, faint, in the order things happened.
        for (i in 1 until pts.size) {
            drawLine(StarBone.copy(alpha = if (preview) 0.22f else 0.3f), pts[i - 1], pts[i], (if (preview) 0.7f else 1.1f) * density, StrokeCap.Round)
        }
        val radii = stars.mapIndexed { _, s -> starRadius(s, preview) * density * zoom.coerceAtMost(2.5f).let { 1f + (it - 1f) * 0.35f } }
        stars.forEachIndexed { i, s ->
            sparkle(pts[i], radii[i] * twinkle(s.id, time), (if (focus == i) StarWarm else StarBone).copy(alpha = if (preview) 0.7f else 1f), focused = focus == i)
        }
        if (!preview) {
            // Month markers at the first date of each month, placed where they don't cover a star,
            // a line or another label; left out if there's no room (zoom in to see them).
            val placed = mutableListOf<Rect>()
            stars.forEachIndexed { i, s ->
                if (i > 0 && stars[i - 1].date.month == s.date.month && stars[i - 1].date.year == s.date.year) return@forEachIndexed
                val laid = measurer.measure(s.date.month.name.take(3) + " ’" + (s.date.year % 100), label)
                val w = laid.size.width.toFloat()
                val h = laid.size.height.toFloat()
                val gap = radii[i] * 2.2f + 3f * density
                val candidates = listOf(
                    Offset(gap, -h / 2f), Offset(-gap - w, -h / 2f), Offset(-w / 2f, -gap - h), Offset(-w / 2f, gap),
                    Offset(gap * 0.75f, -gap * 0.75f - h), Offset(-gap * 0.75f - w, -gap * 0.75f - h),
                    Offset(gap * 0.75f, gap * 0.75f), Offset(-gap * 0.75f - w, gap * 0.75f),
                ).map { Rect(pts[i] + it, Size(w, h)) }
                val spot = candidates.firstOrNull { r ->
                    r.left >= 4f && r.top >= 4f && r.right <= size.width - 4f && r.bottom <= size.height - 4f &&
                        placed.none { it.overlaps(r.inflate(2f * density)) } &&
                        pts.indices.none { j -> circleHits(pts[j], radii[j] * 2f, r) } &&
                        (1 until pts.size).none { j -> segmentHits(pts[j - 1], pts[j], r) }
                } ?: return@forEachIndexed
                placed += spot
                drawText(laid, topLeft = spot.topLeft)
            }
        }
    }
}

/** How big a star is: longer together, brighter. */
private fun starRadius(s: StarUi, preview: Boolean): Float {
    val hours = (s.minutes ?: 90) / 60f
    return (2.2f + hours.coerceAtMost(8f) * 0.55f) * (if (preview) 0.42f else 1f)
}

/**
 * Each star's own twinkle: its own speed, depth and rhythm (from its id), so they never pulse
 * together. Kept subtle: never more than about a fifth brighter or dimmer.
 */
fun twinkle(id: String, time: Float): Float {
    val r = Random(id.hashCode())
    val speed = 0.35f + r.nextFloat() * 1.3f
    val depth = 0.04f + r.nextFloat() * 0.14f
    val phase = r.nextFloat() * 6.28f
    val wobble = 0.15f + r.nextFloat() * 0.5f
    // Two waves at odd ratios: a slow swell with a little flicker on top.
    val v = sin(time * speed + phase) * 0.75f + sin(time * speed * 2.73f + phase * 1.9f) * 0.25f * wobble
    return 1f + depth * v
}

/** The stars on screen at [zoom] and [pan] (zooming about the middle). */
fun starPoints(count: Int, size: Size, zoom: Float = 1f, pan: Offset = Offset.Zero): List<Offset> {
    val c = Offset(size.width / 2f, size.height / 2f)
    return starPositions(count).map { p -> c + (Offset(p.x * size.width, p.y * size.height) - c) * zoom + pan }
}

/** The star under a tap, if one is within a finger's reach ([reach] pixels), nearest first. */
fun starAt(tap: Offset, count: Int, size: Size, zoom: Float, pan: Offset, reach: Float): Int? =
    starPoints(count, size, zoom, pan).withIndex()
        .map { (i, p) -> i to (p - tap).getDistance() }
        .filter { it.second <= reach }
        .minByOrNull { it.second }?.first

private fun circleHits(c: Offset, r: Float, rect: Rect): Boolean {
    val x = c.x.coerceIn(rect.left, rect.right)
    val y = c.y.coerceIn(rect.top, rect.bottom)
    return (Offset(x, y) - c).getDistance() < r
}

/** Whether the segment [a]-[b] passes through [rect] (sampled; labels are small). */
private fun segmentHits(a: Offset, b: Offset, rect: Rect): Boolean {
    val steps = 24
    for (k in 0..steps) {
        val p = a + (b - a) * (k / steps.toFloat())
        if (rect.contains(p)) return true
    }
    return false
}

private fun DrawScope.backgroundStars(count: Int, time: Float, strength: Float, zoom: Float = 1f, pan: Offset = Offset.Zero) {
    val r = Random(31)
    val c = Offset(size.width / 2f, size.height / 2f)
    repeat(count) { i ->
        val p = c + (Offset(r.nextFloat() * size.width, r.nextFloat() * size.height) - c) * zoom + pan
        val tw = 0.5f + 0.5f * sin(time * (0.6f + r.nextFloat() * 1.4f) + i)
        val big = r.nextFloat() < 0.08f
        val a = (0.15f + 0.45f * tw) * strength * (if (big) 1.3f else 1f)
        drawCircle(StarBone.copy(alpha = a.coerceAtMost(1f)), (if (big) 1.2f else 0.6f) * density, p)
    }
}

private fun DrawScope.milkyWay(time: Float) {
    val r = Random(99)
    // A soft diagonal band: a few wide hazy glows, then fine star dust along it. It drifts very slowly.
    val drift = (time * 0.004f) % 1f
    fun along(t: Float, across: Float) = Offset(
        ((t + drift) % 1.2f - 0.1f) * size.width,
        size.height * (0.95f - t * 0.85f) + across * size.height,
    )
    repeat(14) {
        val c = along(r.nextFloat(), (r.nextFloat() - 0.5f) * 0.12f)
        val rad = size.minDimension * (0.12f + r.nextFloat() * 0.12f)
        drawCircle(Brush.radialGradient(listOf(Dust.copy(alpha = 0.07f), Color.Transparent), center = c, radius = rad), rad, c)
    }
    repeat(420) {
        val t = r.nextFloat()
        val spread = (r.nextFloat() - 0.5f) * (r.nextFloat() * 0.26f)
        drawCircle(StarBone.copy(alpha = 0.08f + r.nextFloat() * 0.22f), (0.35f + r.nextFloat() * 0.5f) * density, along(t, spread))
    }
}

private fun DrawScope.moon(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(StarWarm.copy(alpha = 0.14f), Color.Transparent), center = c, radius = r * 4f), r * 4f, c)
    drawCircle(StarWarm.copy(alpha = 0.9f), r, c)
    drawCircle(SkyTop, r * 0.92f, c + Offset(r * 0.42f, -r * 0.18f))
}

private fun DrawScope.shootingStar(time: Float) {
    // One every 9 seconds, for about a second.
    val cycle = time % 9f
    if (cycle > 1.1f) return
    val k = cycle / 1.1f
    val start = Offset(size.width * 0.25f, size.height * 0.08f)
    val head = start + Offset(size.width * 0.35f * k, size.height * 0.16f * k)
    val tail = head - Offset(size.width * 0.12f, size.height * 0.055f)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, StarBone.copy(alpha = 0.8f * (1f - k * 0.6f))), tail, head), tail, head, 1.4f * density, StrokeCap.Round)
}

/** A four-pointed star with a glow. */
fun DrawScope.sparkle(c: Offset, r: Float, color: Color, focused: Boolean = false) {
    drawCircle(color.copy(alpha = if (focused) 0.25f else 0.12f), r * (if (focused) 5f else 3.4f), c)
    drawCircle(color.copy(alpha = 0.22f), r * 1.8f, c)
    val ray = Path().apply {
        moveTo(c.x, c.y - r * 2.6f)
        quadraticTo(c.x + r * 0.22f, c.y - r * 0.22f, c.x + r * 2.6f, c.y)
        quadraticTo(c.x + r * 0.22f, c.y + r * 0.22f, c.x, c.y + r * 2.6f)
        quadraticTo(c.x - r * 0.22f, c.y + r * 0.22f, c.x - r * 2.6f, c.y)
        quadraticTo(c.x - r * 0.22f, c.y - r * 0.22f, c.x, c.y - r * 2.6f)
        close()
    }
    drawPath(ray, color)
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.45f, c)
    if (focused) drawCircle(color.copy(alpha = 0.6f), r * 3.6f, c, style = Stroke(0.8f * density))
}
