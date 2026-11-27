package com.choomhub.app.feature.world.cabinet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import kotlin.random.Random

internal object CabinetInk {
    val Wood = Color(0xFF2E211D)
    val WoodLit = Color(0xFF47332A)
    val WoodDark = Color(0xFF1A120F)
    val Brass = Color(0xFFD9B45A)
    val BrassDim = Color(0xFF9C7F3E)
    val Felt = Color(0xFF2B1E2A)
    val LabelPaper = Color(0xFFE9E1D0)
    val LabelInk = Color(0xFF2A2230)
    val LabelRule = Color(0xFFB7A98F)
    val Wine = Color(0xFF9A3B52)
    val Candle = Color(0xFFE6D6AE)
    val Bone = Color(0xFFE6DED0)
}

/**
 * The Our World card art: a museum cabinet of little drawers with brass label frames,
 * one drawer pulled out with polaroids standing in it, a glass dome on top with a rock.
 */
@Composable
fun SpecimenCabinetArt(modifier: Modifier = Modifier, labels: List<String> = emptyList()) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF15111B), Color(0xFF1E1620))))
        drawCircle(Brush.radialGradient(listOf(CabinetInk.Candle.copy(alpha = 0.14f), Color.Transparent), center = Offset(w * 0.2f, h * 0.2f), radius = w * 0.5f), w * 0.5f, Offset(w * 0.2f, h * 0.2f))
        // The cabinet body.
        val left = w * 0.12f
        val right = w * 0.88f
        val top = h * 0.4f
        drawRect(CabinetInk.WoodDark, Offset(left - 6.dp.toPx(), top - 8.dp.toPx()), Size(right - left + 12.dp.toPx(), 8.dp.toPx()))
        drawRect(CabinetInk.Wood, Offset(left, top), Size(right - left, h - top))
        // Glass dome with a geode under it.
        dome(Offset(w * 0.7f, top - 8.dp.toPx()), minOf(w * 0.07f, (top - 8.dp.toPx()) / 2.6f))
        // A candle on the other side.
        candle(Offset(w * 0.24f, top - 8.dp.toPx()))
        val cols = 4
        val rows = 3
        val gap = 6.dp.toPx()
        val dw = (right - left - gap * (cols + 1)) / cols
        val dh = (h - top - gap * (rows + 1)) / rows
        var n = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            val o = Offset(left + gap + c * (dw + gap), top + gap + r * (dh + gap))
            if (r == 1 && c == 2) {
                openDrawer(o, Size(dw, dh))
            } else {
                drawer(o, Size(dw, dh), n++)
            }
        }
    }
}

private fun DrawScope.drawer(o: Offset, s: Size, seed: Int) {
    drawRect(CabinetInk.WoodLit, o, s)
    drawRect(CabinetInk.WoodDark.copy(alpha = 0.7f), o, s, style = Stroke(1.dp.toPx()))
    // Brass label frame with a slip of paper.
    val lw = s.width * 0.5f
    val lh = s.height * 0.26f
    val lo = Offset(o.x + (s.width - lw) / 2f, o.y + s.height * 0.18f)
    drawRect(CabinetInk.Brass, lo - Offset(1.5f.dp.toPx(), 1.5f.dp.toPx()), Size(lw + 3.dp.toPx(), lh + 3.dp.toPx()))
    drawRect(CabinetInk.LabelPaper, lo, Size(lw, lh))
    val r = Random(seed)
    drawLine(CabinetInk.LabelInk.copy(alpha = 0.6f), lo + Offset(lw * 0.15f, lh * 0.5f), lo + Offset(lw * (0.4f + r.nextFloat() * 0.45f), lh * 0.5f), 1.dp.toPx())
    // Ring pull.
    val pull = Offset(o.x + s.width / 2f, o.y + s.height * 0.7f)
    drawCircle(CabinetInk.BrassDim, 2.dp.toPx(), pull - Offset(0f, 3.dp.toPx()))
    drawArc(CabinetInk.Brass, 0f, 180f, false, pull - Offset(5.dp.toPx(), 4.dp.toPx()), Size(10.dp.toPx(), 8.dp.toPx()), style = Stroke(1.6f.dp.toPx()))
}

/** A drawer pulled towards us, felt-lined, with polaroids standing up in it. */
private fun DrawScope.openDrawer(o: Offset, s: Size) {
    drawRect(Color.Black.copy(alpha = 0.6f), o, s)
    val out = 2.dp.toPx()
    // Polaroids standing in the drawer.
    listOf(-8f to 0.18f, 5f to 0.42f, -3f to 0.64f).forEachIndexed { i, (angle, x) ->
        val p = Offset(o.x + s.width * x, o.y - s.height * 0.12f + out)
        rotate(angle, p + Offset(s.width * 0.14f, s.height * 0.4f)) {
            drawRect(CabinetInk.Bone, p, Size(s.width * 0.3f, s.height * 0.62f))
            drawRect(listOf(Color(0xFF6B5A7A), Color(0xFF8C6A4E), Color(0xFF4E6A6B))[i], p + Offset(s.width * 0.03f, s.width * 0.03f), Size(s.width * 0.24f, s.width * 0.24f))
        }
    }
    // The drawer front, lower and slightly bigger: it's closer.
    val front = Offset(o.x - 3.dp.toPx(), o.y + s.height * 0.42f)
    val fs = Size(s.width + 6.dp.toPx(), s.height * 0.58f + 3.dp.toPx())
    drawRect(CabinetInk.WoodLit, front, fs)
    drawRect(CabinetInk.WoodDark, front, fs, style = Stroke(1.dp.toPx()))
    drawRect(CabinetInk.Felt, Offset(front.x + 3.dp.toPx(), front.y - 4.dp.toPx()), Size(fs.width - 6.dp.toPx(), 4.dp.toPx()))
    val pull = Offset(front.x + fs.width / 2f, front.y + fs.height * 0.55f)
    drawArc(CabinetInk.Brass, 0f, 180f, false, pull - Offset(5.dp.toPx(), 4.dp.toPx()), Size(10.dp.toPx(), 8.dp.toPx()), style = Stroke(1.6f.dp.toPx()))
}

private fun DrawScope.dome(base: Offset, r: Float) {
    drawRoundRect(CabinetInk.WoodLit, base - Offset(r * 1.1f, r * 0.2f), Size(r * 2.2f, r * 0.25f), CornerRadius(2f))
    // A half geode inside.
    drawArc(Color(0xFF6E6C76), 180f, 180f, true, base - Offset(r * 0.5f, r * 0.6f), Size(r, r * 0.8f))
    drawArc(Color(0xFF8B6FAE), 180f, 180f, true, base - Offset(r * 0.32f, r * 0.42f), Size(r * 0.64f, r * 0.48f))
    val glass = Path().apply {
        moveTo(base.x - r, base.y - r * 0.2f)
        lineTo(base.x - r, base.y - r * 1.4f)
        cubicTo(base.x - r, base.y - r * 2.5f, base.x + r, base.y - r * 2.5f, base.x + r, base.y - r * 1.4f)
        lineTo(base.x + r, base.y - r * 0.2f)
    }
    drawPath(glass, CabinetInk.Bone.copy(alpha = 0.06f))
    drawPath(glass, CabinetInk.Bone.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    drawLine(Color.White.copy(alpha = 0.35f), Offset(base.x - r * 0.7f, base.y - r * 1.8f), Offset(base.x - r * 0.75f, base.y - r * 0.6f), 2.dp.toPx())
}

private fun DrawScope.candle(base: Offset) {
    drawCircle(Brush.radialGradient(listOf(CabinetInk.Candle.copy(alpha = 0.35f), Color.Transparent), center = base - Offset(0f, 30.dp.toPx()), radius = 40.dp.toPx()), 40.dp.toPx(), base - Offset(0f, 30.dp.toPx()))
    drawRect(CabinetInk.BrassDim, base - Offset(9.dp.toPx(), 3.dp.toPx()), Size(18.dp.toPx(), 3.dp.toPx()))
    drawRect(CabinetInk.Bone, base - Offset(4.dp.toPx(), 22.dp.toPx()), Size(8.dp.toPx(), 19.dp.toPx()))
    drawOval(CabinetInk.Candle, base - Offset(2.5f.dp.toPx(), 31.dp.toPx()), Size(5.dp.toPx(), 9.dp.toPx()))
    drawOval(Color.White.copy(alpha = 0.8f), base - Offset(1.dp.toPx(), 27.dp.toPx()), Size(2.dp.toPx(), 4.dp.toPx()))
}

/**
 * A museum specimen label: double rule, a number, and typed fields. [fields] are
 * (label, value) pairs; blank values are left as an empty ruled line.
 */
@Composable
fun SpecimenLabel(number: Int, fields: List<Pair<String, String?>>, modifier: Modifier = Modifier, title: String = "SPECIMEN") {
    Column(
        modifier
            .background(CabinetInk.LabelPaper)
            .border(1.dp, CabinetInk.LabelInk.copy(alpha = 0.7f))
            .padding(3.dp)
            .border(0.6.dp, CabinetInk.LabelInk.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row {
            Text(title, style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 10.sp), color = CabinetInk.LabelInk, modifier = Modifier.weight(1f))
            Text("No. %03d".format(number), style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 10.sp), color = CabinetInk.Wine)
        }
        Spacer(Modifier.padding(top = 4.dp))
        fields.forEach { (label, value) -> LabelField(label, value) }
    }
}

@Composable
private fun ColumnScope.LabelField(label: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
        Text(label.uppercase(), style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 8.sp), color = CabinetInk.LabelInk.copy(alpha = 0.6f), modifier = Modifier.width(64.dp).padding(top = 3.dp))
        Column(Modifier.weight(1f)) {
            Text(value ?: " ", style = ChoomHubTextStyles.Handwritten.copy(fontSize = 15.sp, lineHeight = 16.sp), color = CabinetInk.LabelInk, maxLines = 2)
            Canvas(Modifier.fillMaxWidth().padding(top = 1.dp)) {
                drawLine(CabinetInk.LabelRule, Offset(0f, 0f), Offset(size.width, 0f), 0.8.dp.toPx())
            }
        }
    }
}

/** A brass pin head, for pinning labels and polaroids to the felt. */
@Composable
fun BrassPin(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        drawCircle(Color.Black.copy(alpha = 0.4f), size.minDimension * 0.42f, c + Offset(1.5f, 2f))
        drawCircle(CabinetInk.BrassDim, size.minDimension * 0.42f, c)
        drawCircle(CabinetInk.Brass, size.minDimension * 0.32f, c - Offset(1f, 1f))
        drawCircle(Color.White.copy(alpha = 0.6f), size.minDimension * 0.1f, c - Offset(size.minDimension * 0.12f, size.minDimension * 0.12f))
    }
}
