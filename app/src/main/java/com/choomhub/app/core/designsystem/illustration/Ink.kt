package com.choomhub.app.core.designsystem.illustration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The ink-illustration toolkit behind all of ChoomHub's art: solid ink
 * silhouettes whose edges are made of scratchy, bone-white hairlines, plus
 * splatters and drips. Deterministic (seeded) so art doesn't shimmer between
 * frames. Coordinates are whatever the caller draws in (usually the 100 grid).
 */
object InkPalette {
    /** The ink itself: a hair off pure black so it still reads on the darkest backgrounds. */
    val Ink = Color(0xFF050506)

    /** Off-white paper / bone. */
    val Bone = Color(0xFFEDE6D6)

    /** Mid grey for splatter and texture on dark surfaces. */
    val Ash = Color(0xFF3A3740)
}

/**
 * Hairline strokes that follow the edge of [path], like fur caught in light.
 * [strands] is per 100 units of outline length.
 */
internal fun DrawScope.scratchEdge(
    path: Path,
    color: Color = InkPalette.Bone,
    strands: Float = 34f,
    length: ClosedFloatingPointRange<Float> = 1.4f..4.5f,
    width: Float = 0.3f,
    seed: Int = 7,
    coverage: Float = 0.75f,
) {
    val random = Random(seed)
    val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    do {
        val total = measure.length
        val count = (total / 100f * strands).toInt().coerceAtLeast(1)
        repeat(count) {
            if (random.nextFloat() > coverage) return@repeat
            val d = random.nextFloat() * total
            measure.getPosTan(d, pos, tan)
            val len = length.start + random.nextFloat() * (length.endInclusive - length.start)
            // Mostly along the edge, slightly fanned off it, like stray fur.
            val fan = (random.nextFloat() - 0.5f) * 0.9f
            val nx = -tan[1]
            val ny = tan[0]
            val dx = tan[0] + nx * fan
            val dy = tan[1] + ny * fan
            val start = Offset(pos[0] + nx * (random.nextFloat() - 0.5f) * 1.2f, pos[1] + ny * (random.nextFloat() - 0.5f) * 1.2f)
            drawLine(
                color = color.copy(alpha = color.alpha * (0.35f + random.nextFloat() * 0.6f)),
                start = start,
                end = Offset(start.x + dx * len, start.y + dy * len),
                strokeWidth = width * (0.6f + random.nextFloat() * 0.8f),
                cap = StrokeCap.Round,
            )
        }
    } while (measure.nextContour())
}

/** An ink silhouette: solid fill plus a scratchy bone edge. */
internal fun DrawScope.inkSilhouette(path: Path, seed: Int, edge: Color = InkPalette.Bone, strands: Float = 34f) {
    drawPath(path, InkPalette.Ink)
    scratchEdge(path, edge, strands = strands, seed = seed)
}

/** A splash of ink: a blob with satellite droplets and specks. */
internal fun DrawScope.splatter(center: Offset, radius: Float, color: Color = InkPalette.Ash, seed: Int = 3) {
    val random = Random(seed)
    drawCircle(color, radius, center)
    repeat(5) {
        val a = random.nextFloat() * 6.283f
        val r = radius * (0.7f + random.nextFloat() * 0.6f)
        drawCircle(color, radius * (0.3f + random.nextFloat() * 0.3f), Offset(center.x + cos(a) * r, center.y + sin(a) * r))
    }
    repeat(9) {
        val a = random.nextFloat() * 6.283f
        val r = radius * (1.5f + random.nextFloat() * 2.2f)
        drawCircle(color, radius * (0.06f + random.nextFloat() * 0.14f), Offset(center.x + cos(a) * r, center.y + sin(a) * r))
    }
}

/** A run of ink: a thin line ending in a bead. */
internal fun DrawScope.drip(top: Offset, length: Float, width: Float, color: Color = InkPalette.Ink) {
    val end = Offset(top.x, top.y + length)
    drawLine(color, top, end, width, StrokeCap.Round)
    drawCircle(color, width * 0.9f, end)
}

/** A long wiry whisker from [from] towards [to], bowed slightly. */
internal fun DrawScope.whisker(from: Offset, to: Offset, bow: Float, color: Color = InkPalette.Bone, width: Float = 0.5f) {
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f + bow)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(mid.x, mid.y, to.x, to.y)
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = width, cap = StrokeCap.Round))
}

/** A sketchy line: drawn two or three times with slight offsets, like a pen going over it. */
internal fun DrawScope.sketchLine(from: Offset, to: Offset, color: Color, width: Float, seed: Int) {
    val random = Random(seed)
    repeat(3) { i ->
        val j = { (random.nextFloat() - 0.5f) * width * 1.4f }
        drawLine(
            color.copy(alpha = color.alpha * if (i == 0) 1f else 0.45f),
            Offset(from.x + j(), from.y + j()),
            Offset(to.x + j(), to.y + j()),
            width * if (i == 0) 1f else 0.55f,
            StrokeCap.Round,
        )
    }
}

/**
 * Natural-looking fur along the edge of [path]: curved strands in uneven
 * tufts that emerge from just inside the silhouette, catch light mostly from
 * [light] (a direction), and drift with [flow] (gravity / grain). A sparse
 * layer of faint strands inside the edge suggests texture without outlining.
 */
internal fun DrawScope.furEdge(
    path: Path,
    seed: Int,
    color: Color = InkPalette.Bone,
    density: Float = 45f,
    length: ClosedFloatingPointRange<Float> = 2f..7f,
    light: Offset = Offset(0.55f, -0.85f),
    flow: Offset = Offset(0f, 1f),
    width: Float = 0.32f,
    ambient: Float = 0.18f,
) {
    val random = Random(seed)
    val inside = insideTester(path)
    val lightLen = kotlin.math.sqrt(light.x * light.x + light.y * light.y)
    val lx = light.x / lightLen
    val ly = light.y / lightLen
    val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    val strand = Path()
    do {
        val total = measure.length
        val count = (total / 100f * density).toInt().coerceAtLeast(1)
        // Tufts: strands cluster around a few centres instead of being spread evenly.
        repeat(count) {
            val d = (random.nextFloat() * total)
            measure.getPosTan(d, pos, tan)
            var nx = -tan[1]
            var ny = tan[0]
            if (inside(pos[0] + nx * 1.2f, pos[1] + ny * 1.2f)) { nx = -nx; ny = -ny }
            val lit = (nx * lx + ny * ly).coerceAtLeast(0f)
            if (random.nextFloat() > ambient + (1f - ambient) * lit) return@repeat
            val tuft = 1 + random.nextInt(3)
            repeat(tuft) {
                val len = (length.start + random.nextFloat() * (length.endInclusive - length.start)) * (0.55f + 0.75f * lit)
                val jitter = (random.nextFloat() - 0.5f) * 0.7f
                var dx = nx * 0.6f + flow.x * 0.45f + tan[0] * jitter
                var dy = ny * 0.6f + flow.y * 0.45f + tan[1] * jitter
                val dl = kotlin.math.sqrt(dx * dx + dy * dy)
                dx /= dl; dy /= dl
                val back = 0.5f + random.nextFloat() * 1.8f
                val sx = pos[0] - nx * back + tan[0] * (random.nextFloat() - 0.5f) * 1.6f
                val sy = pos[1] - ny * back + tan[1] * (random.nextFloat() - 0.5f) * 1.6f
                val bend = (random.nextFloat() - 0.5f) * len * 0.5f
                strand.reset()
                strand.moveTo(sx, sy)
                strand.quadraticTo(sx + dx * len * 0.5f - dy * bend, sy + dy * len * 0.5f + dx * bend, sx + dx * len, sy + dy * len)
                drawPath(
                    strand,
                    color.copy(alpha = color.alpha * (0.25f + 0.7f * lit) * (0.5f + random.nextFloat() * 0.5f)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = width * (0.5f + random.nextFloat() * 0.8f), cap = StrokeCap.Round),
                )
            }
            // Faint texture strands just inside the edge.
            if (random.nextFloat() < 0.35f) {
                val depth = 1.5f + random.nextFloat() * 4f
                val sx = pos[0] - nx * depth
                val sy = pos[1] - ny * depth
                val len = 2f + random.nextFloat() * 4f
                drawLine(
                    color.copy(alpha = color.alpha * 0.12f * (0.4f + lit)),
                    Offset(sx, sy),
                    Offset(sx + (flow.x * 0.8f + tan[0] * 0.3f) * len, sy + (flow.y * 0.8f + tan[1] * 0.3f) * len),
                    width * 0.6f,
                    StrokeCap.Round,
                )
            }
        }
    } while (measure.nextContour())
}

/** Point-in-shape test for a path, at 1/10-unit resolution. */
private fun insideTester(path: Path): (Float, Float) -> Boolean {
    val scaled = android.graphics.Path(path.asAndroidPath()).apply {
        transform(android.graphics.Matrix().apply { setScale(10f, 10f) })
    }
    val bounds = android.graphics.RectF()
    scaled.computeBounds(bounds, true)
    val clip = android.graphics.Region(bounds.left.toInt() - 2, bounds.top.toInt() - 2, bounds.right.toInt() + 2, bounds.bottom.toInt() + 2)
    val region = android.graphics.Region().apply { setPath(scaled, clip) }
    return { x, y -> region.contains((x * 10f).toInt(), (y * 10f).toInt()) }
}
