package com.choomhub.app.feature.world.atlas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The part of Delhi on screen, in degrees. */
data class MapWindow(val west: Float, val east: Float, val south: Float, val north: Float) {
    companion object {
        val Delhi = MapWindow(DelhiMap.WEST, DelhiMap.EAST, DelhiMap.SOUTH, DelhiMap.NORTH)

        /** Just around the pins, with a margin, never tighter than about 6 km across. */
        fun around(pins: List<AtlasPin>, margin: Float = 0.025f): MapWindow {
            if (pins.isEmpty()) return Delhi
            val cx = (pins.minOf { it.lon } + pins.maxOf { it.lon }) / 2f
            val cy = (pins.minOf { it.lat } + pins.maxOf { it.lat }) / 2f
            val hw = maxOf((pins.maxOf { it.lon } - pins.minOf { it.lon }) / 2f + margin, 0.03f)
            val hh = maxOf((pins.maxOf { it.lat } - pins.minOf { it.lat }) / 2f + margin, 0.03f)
            return MapWindow(cx - hw, cx + hw, cy - hh, cy + hh)
        }
    }
}

/** A meetup pinned on the map. */
data class AtlasPin(val id: String, val lon: Float, val lat: Float, val count: Int = 1, val name: String? = null)

private val Paper = Color(0xFF16121C)
private val PaperLit = Color(0xFF1F1926)
private val Land = Color(0xFF241C2A)
private val InkLine = Color(0xFFCFC3AE)
private val River = Color(0xFF5E7E8C)
private val PinWine = Color(0xFF9A3B52)
private val PinHead = Color(0xFFC4566F)
private val Brass = Color(0xFFD9B45A)

/** Pixels per kilometre for [win] on a canvas of [size] (the whole window fits). */
fun pxPerKm(win: MapWindow, size: Size, pad: Float = 0.03f): Float {
    val wKm = (win.east - win.west) * 98f
    val hKm = (win.north - win.south) * 111f
    return min(size.width * (1 - pad * 2) / wKm, size.height * (1 - pad * 2) / hKm)
}

/** Longitude/latitude to a point on a canvas of [size], keeping Delhi's real proportions (about 98 km a degree east-west, 111 north-south). */
fun projectPoint(win: MapWindow, size: Size, lon: Float, lat: Float, pad: Float = 0.03f): Offset {
    val k = pxPerKm(win, size, pad)
    val cx = (win.west + win.east) / 2f
    val cy = (win.south + win.north) / 2f
    return Offset(size.width / 2f + (lon - cx) * 98f * k, size.height / 2f + (cy - lat) * 111f * k)
}

/** The other way: a point on the canvas to (longitude, latitude). */
fun unproject(win: MapWindow, size: Size, point: Offset, pad: Float = 0.03f): Pair<Float, Float> {
    val k = pxPerKm(win, size, pad)
    val cx = (win.west + win.east) / 2f
    val cy = (win.south + win.north) / 2f
    return (cx + (point.x - size.width / 2f) / (98f * k)) to (cy - (point.y - size.height / 2f) / (111f * k))
}

/**
 * The window after a pinch or drag: the spot under your fingers stays under them. Kept within
 * Delhi (with a margin), and from about 1.5 km across to the whole city.
 */
fun MapWindow.transformed(size: Size, centroid: Offset, pan: Offset, zoom: Float): MapWindow {
    val full = MapWindow.Delhi
    val width = ((east - west) / zoom).coerceIn(0.015f, (full.east - full.west) * 1.15f)
    val height = width * (north - south) / (east - west)
    val sized = MapWindow(0f, width, 0f, height)
    val (gLon, gLat) = unproject(this, size, centroid - pan)
    val k = pxPerKm(sized, size)
    var cx = gLon - (centroid.x - size.width / 2f) / (98f * k)
    var cy = gLat + (centroid.y - size.height / 2f) / (111f * k)
    cx = cx.coerceIn(full.west - 0.05f, full.east + 0.05f)
    cy = cy.coerceIn(full.south - 0.05f, full.north + 0.05f)
    return MapWindow(cx - width / 2f, cx + width / 2f, cy - height / 2f, cy + height / 2f)
}

/** Longitude/latitude to a point on this canvas. */
internal fun DrawScope.project(win: MapWindow, lon: Float, lat: Float, pad: Float = 0.03f): Offset = projectPoint(win, size, lon, lat, pad)

private fun DrawScope.polyline(win: MapWindow, points: FloatArray, close: Boolean) = Path().apply {
    var i = 0
    while (i < points.size) {
        val p = project(win, points[i], points[i + 1])
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        i += 2
    }
    if (close) close()
}

/**
 * Delhi, drawn like an old field map in the app's inks: the NCT border, the Yamuna, a few
 * landmarks, a compass and the pins. [selected] lifts one pin.
 */
@Composable
fun DelhiAtlas(pins: List<AtlasPin>, modifier: Modifier = Modifier, selected: String? = null, small: Boolean = false, window: MapWindow = MapWindow.Delhi) {
    val win = window
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds()) {
        // Close enough in that names fit: show them.
        val zoomed = (win.east - win.west) < (DelhiMap.EAST - DelhiMap.WEST) * 0.6f
        drawRect(Brush.radialGradient(listOf(PaperLit, Paper), center = center, radius = size.maxDimension * 0.7f))
        graticule(win)
        val border = polyline(win, DelhiMap.BORDER, close = true)
        drawPath(border, Land)
        clipPath(border) { hatch() }
        drawPath(border, InkLine.copy(alpha = 0.55f), style = Stroke(1.3f * density, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 3f * density))))
        val river = polyline(win, DelhiMap.YAMUNA, close = false)
        drawPath(river, River.copy(alpha = 0.35f), style = Stroke(7f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(river, River.copy(alpha = 0.9f), style = Stroke(2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (!small) {
            val y = DelhiMap.YAMUNA
            riverLabel(measurer, project(win, y[18], y[19]))
            DelhiMap.LANDMARKS.forEach { (name, lon, lat) -> landmark(measurer, name, project(win, lon, lat), labelled = zoomed) }
            compass(Offset(size.width * 0.88f, size.height * 0.12f), size.minDimension * 0.07f)
            scaleBar(measurer, win)
            drawText(
                measurer,
                "Map data © OpenStreetMap contributors",
                Offset(size.width * 0.03f, size.height - 16f * density),
                TextStyle(fontSize = 8.sp, color = InkLine.copy(alpha = 0.45f)),
            )
            if (!zoomed) drawText(
                measurer,
                "DELHI",
                Offset(size.width * 0.05f, size.height * 0.05f),
                TextStyle(fontSize = 22.sp, letterSpacing = 8.sp, color = InkLine.copy(alpha = 0.7f)),
            )
        }
        pins.forEach { pin ->
            if (pin.id != selected) pin(measurer, project(win, pin.lon, pin.lat), pin.count, false, small)
        }
        pins.firstOrNull { it.id == selected }?.let { pin(measurer, project(win, it.lon, it.lat), it.count, true, small) }
    }
}

private fun DrawScope.graticule(win: MapWindow) {
    val step = 0.05f
    var lon = DelhiMap.WEST - DelhiMap.WEST % step + step
    while (lon < DelhiMap.EAST) {
        drawLine(InkLine.copy(alpha = 0.05f), project(win, lon, DelhiMap.SOUTH - 0.1f), project(win, lon, DelhiMap.NORTH + 0.1f), 0.6f * density)
        lon += step
    }
    var lat = DelhiMap.SOUTH - DelhiMap.SOUTH % step + step
    while (lat < DelhiMap.NORTH) {
        drawLine(InkLine.copy(alpha = 0.05f), project(win, DelhiMap.WEST - 0.1f, lat), project(win, DelhiMap.EAST + 0.1f, lat), 0.6f * density)
        lat += step
    }
}

/** Fine diagonal hatching inside the city, like an engraved map. */
private fun DrawScope.hatch() {
    var x = -size.height
    while (x < size.width) {
        drawLine(InkLine.copy(alpha = 0.04f), Offset(x, size.height), Offset(x + size.height, 0f), 0.6f * density)
        x += 6f * density
    }
    // Speckle for texture.
    val r = Random(5)
    repeat(400) { drawCircle(InkLine.copy(alpha = 0.05f), 0.6f * density, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) }
}

private fun DrawScope.riverLabel(measurer: TextMeasurer, at: Offset) {
    rotate(-62f, at) {
        drawText(measurer, "Yamuna", at + Offset(6f * density, -4f * density), TextStyle(fontSize = 10.sp, fontStyle = FontStyle.Italic, letterSpacing = 2.sp, color = River.copy(alpha = 0.95f)))
    }
}

/** A tiny ink glyph per landmark, plus its name. */
private fun DrawScope.landmark(measurer: TextMeasurer, name: String, at: Offset, labelled: Boolean) {
    val k = 1f * density
    val ink = InkLine.copy(alpha = 0.75f)
    when (name) {
        "India Gate" -> {
            drawRect(ink, at + Offset(-5f * k, -9f * k), Size(10f * k, 2f * k))
            drawRect(ink, at + Offset(-5f * k, -7f * k), Size(3f * k, 7f * k))
            drawRect(ink, at + Offset(2f * k, -7f * k), Size(3f * k, 7f * k))
        }
        "Qutub Minar" -> {
            drawPath(Path().apply { moveTo(at.x - 3f * k, at.y); lineTo(at.x - 1f * k, at.y - 14f * k); lineTo(at.x + 1f * k, at.y - 14f * k); lineTo(at.x + 3f * k, at.y); close() }, ink)
            for (i in 1..3) drawLine(Paper, at + Offset(-3f * k, -i * 3.5f * k), at + Offset(3f * k, -i * 3.5f * k), 0.8f * k)
        }
        "Lotus Temple" -> for (i in -2..2) {
            rotate(i * 22f, at) { drawOval(ink, at + Offset(-1.6f * k, -8f * k), Size(3.2f * k, 8f * k)) }
        }
        "Humayun's Tomb", "Jama Masjid" -> {
            drawRect(ink, at + Offset(-6f * k, -4f * k), Size(12f * k, 4f * k))
            drawArc(ink, 180f, 180f, true, at + Offset(-3.5f * k, -8f * k), Size(7f * k, 8f * k))
            if (name == "Jama Masjid") {
                drawLine(ink, at + Offset(-7f * k, 0f), at + Offset(-7f * k, -10f * k), 1.4f * k)
                drawLine(ink, at + Offset(7f * k, 0f), at + Offset(7f * k, -10f * k), 1.4f * k)
            }
        }
        "Red Fort" -> {
            drawRect(ink, at + Offset(-7f * k, -5f * k), Size(14f * k, 5f * k))
            for (i in -3..3) drawRect(ink, at + Offset(i * 2f * k - 0.7f * k, -6.5f * k), Size(1.4f * k, 1.5f * k))
        }
        "Lodhi Garden", "Hauz Khas" -> {
            drawCircle(ink, 2.6f * k, at + Offset(-2.5f * k, -4f * k))
            drawCircle(ink, 3f * k, at + Offset(2.5f * k, -5f * k))
            drawLine(ink, at + Offset(0f, -2f * k), at, 1f * k)
        }
        "Connaught Place" -> {
            drawCircle(ink, 6f * k, at + Offset(0f, -5f * k), style = Stroke(1.2f * k))
            drawCircle(ink, 3f * k, at + Offset(0f, -5f * k), style = Stroke(1f * k))
        }
        "Akshardham" -> {
            drawRect(ink, at + Offset(-5f * k, -4f * k), Size(10f * k, 4f * k))
            for (i in -1..1) drawPath(Path().apply { moveTo(at.x + i * 3.5f * k - 1.6f * k, at.y - 4f * k); lineTo(at.x + i * 3.5f * k, at.y - (if (i == 0) 10f else 7.5f) * k); lineTo(at.x + i * 3.5f * k + 1.6f * k, at.y - 4f * k); close() }, ink)
        }
        "Airport" -> rotate(-35f, at) {
            drawLine(ink, at + Offset(0f, -9f * k), at + Offset(0f, 1f * k), 1.6f * k, StrokeCap.Round)
            drawLine(ink, at + Offset(-5f * k, -4f * k), at + Offset(5f * k, -4f * k), 1.4f * k, StrokeCap.Round)
            drawLine(ink, at + Offset(-2.4f * k, 0f), at + Offset(2.4f * k, 0f), 1.2f * k, StrokeCap.Round)
        }
        else -> drawCircle(ink, 2f * k, at)
    }
    if (labelled) {
        val laid = measurer.measure(name, TextStyle(fontSize = 8.sp, letterSpacing = 0.6.sp, color = InkLine.copy(alpha = 0.5f)))
        drawText(laid, topLeft = at + Offset(-laid.size.width / 2f, 3f * k))
    }
}

private fun DrawScope.compass(c: Offset, r: Float) {
    drawCircle(InkLine.copy(alpha = 0.25f), r, c, style = Stroke(0.8f * density))
    drawCircle(InkLine.copy(alpha = 0.15f), r * 0.7f, c, style = Stroke(0.6f * density))
    for (i in 0 until 4) {
        val a = i * Math.PI / 2
        val tip = c + Offset(sin(a).toFloat() * r * 1.05f, -cos(a).toFloat() * r * 1.05f)
        val l = c + Offset(sin(a - Math.PI / 2).toFloat() * r * 0.16f, -cos(a - Math.PI / 2).toFloat() * r * 0.16f)
        val rr = c + Offset(sin(a + Math.PI / 2).toFloat() * r * 0.16f, -cos(a + Math.PI / 2).toFloat() * r * 0.16f)
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(c.x, c.y); close() }, if (i == 0) Brass else InkLine.copy(alpha = 0.6f))
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(rr.x, rr.y); lineTo(c.x, c.y); close() }, if (i == 0) Brass.copy(alpha = 0.6f) else InkLine.copy(alpha = 0.3f))
    }
    drawCircle(Brass, r * 0.08f, c)
}

private fun DrawScope.scaleBar(measurer: TextMeasurer, win: MapWindow) {
    // 5 km, at the map's own scale.
    val km = when { win.east - win.west > 0.2f -> 5; win.east - win.west > 0.06f -> 2; else -> 1 }
    val span = project(win, win.west + km / 98f, win.south).x - project(win, win.west, win.south).x
    val start = Offset(size.width * 0.62f, size.height * 0.92f)
    val end = start + Offset(span, 0f)
    drawLine(InkLine.copy(alpha = 0.6f), start, end, 1.2f * density)
    drawLine(InkLine.copy(alpha = 0.6f), start, start + Offset(0f, -4f * density), 1.2f * density)
    drawLine(InkLine.copy(alpha = 0.6f), end, end + Offset(0f, -4f * density), 1.2f * density)
    drawText(measurer, "$km km", end + Offset(6f * density, -8f * density), TextStyle(fontSize = 8.sp, color = InkLine.copy(alpha = 0.55f)))
}

/** A wine push pin. More than one date there gets a small count. */
private fun DrawScope.pin(measurer: TextMeasurer, at: Offset, count: Int, selected: Boolean, small: Boolean) {
    val k = density * (if (small) 0.6f else 1f) * (if (selected) 1.35f else 1f)
    if (selected) drawCircle(PinHead.copy(alpha = 0.18f), 18f * k, at)
    drawOval(Color.Black.copy(alpha = 0.35f), at + Offset(-4f * k, -1.5f * k), Size(8f * k, 3f * k))
    drawLine(InkLine.copy(alpha = 0.8f), at, at + Offset(0f, -10f * k), 1.4f * k, StrokeCap.Round)
    val head = at + Offset(0f, -14f * k)
    drawCircle(PinWine, 6f * k, head)
    drawCircle(PinHead, 4.2f * k, head + Offset(-0.8f * k, -0.8f * k))
    drawCircle(Color.White.copy(alpha = 0.55f), 1.4f * k, head + Offset(-2f * k, -2.2f * k))
    if (count > 1 && !small) {
        val badge = head + Offset(7f * k, -6f * k)
        drawCircle(Brass, 6f * k, badge)
        val laid = measurer.measure(count.toString(), TextStyle(fontSize = (8f * k / density).sp, color = Paper))
        drawText(laid, topLeft = badge - Offset(laid.size.width / 2f, laid.size.height / 2f))
    }
}
