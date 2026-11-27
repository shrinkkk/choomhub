package com.choomhub.app.feature.world.shelf

import androidx.compose.foundation.border
import com.choomhub.app.domain.model.SongKind
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.feature.song.RatingStamp
import kotlin.random.Random

private val Vinyl = Color(0xFF0C0A0E)
private val Label = Color(0xFFE0453A)
private val Wood = Color(0xFF3A2A24)
private val WoodLit = Color(0xFF55402F)
private val WoodDark = Color(0xFF241915)
private val Brass = Color(0xFFD9B45A)
private val TagPaper = Color(0xFFE6DED0)
private val TagInk = Color(0xFF2A2230)

/** A record on the shelf, as the shelf needs to know it. */
data class ShelfRecord(val id: String, val title: String, val artist: String, val from: String, val rating: Int?, val cover: ImageBitmap?, val kind: SongKind = SongKind.Track)

/** A plank with a lit front edge, its shadow on the wall, and brass brackets. */
@Composable
fun ShelfPlank(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(26.dp)) {
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), startY = size.height * 0.55f, endY = size.height), Offset(0f, size.height * 0.55f), Size(size.width, size.height * 0.45f))
        drawRect(WoodLit, Offset.Zero, Size(size.width, size.height * 0.18f))
        drawRect(Wood, Offset(0f, size.height * 0.18f), Size(size.width, size.height * 0.37f))
        grain(Offset(0f, size.height * 0.2f), Size(size.width, size.height * 0.33f))
        listOf(0.12f, 0.88f).forEach { x ->
            val bx = size.width * x
            drawPath(Path().apply {
                moveTo(bx - 3.dp.toPx(), size.height * 0.55f)
                lineTo(bx + 3.dp.toPx(), size.height * 0.55f)
                lineTo(bx - 3.dp.toPx(), size.height)
                close()
            }, Brass.copy(alpha = 0.7f))
        }
    }
}

private fun DrawScope.grain(at: Offset, s: Size) {
    val r = Random(3)
    repeat(14) {
        val y = at.y + r.nextFloat() * s.height
        val x = at.x + r.nextFloat() * s.width * 0.8f
        drawLine(WoodDark.copy(alpha = 0.6f), Offset(x, y), Offset(x + s.width * (0.1f + r.nextFloat() * 0.3f), y + (r.nextFloat() - 0.5f) * 2f), 0.6.dp.toPx())
    }
}

/**
 * A record standing on the shelf: the sleeve facing out and leaning a little, the disc
 * peeking out of the top, the rating stamped on. The shelf itself is drawn under the row.
 */
@Composable
fun ShelvedRecord(record: ShelfRecord, width: Dp, tilt: Float, modifier: Modifier = Modifier) {
    Box(modifier.size(width + 12.dp, width * 1.18f), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.size(width).rotate(tilt)) {
            // An album has a second disc, further up and to the side (a double LP).
            if (record.kind == SongKind.Album) ShelfDisc(Modifier.size(width).offset(x = width * 0.16f, y = -width * 0.22f).padding(width * 0.08f), Brass.copy(alpha = 0.85f))
            // The disc, slid up out of the sleeve.
            ShelfDisc(Modifier.size(width).offset(y = -width * 0.18f).padding(width * 0.04f), Label.copy(alpha = 0.85f))
            // A playlist leans on two more sleeves, like a pile of mixtapes.
            if (record.kind == SongKind.Playlist) {
                listOf(-6f to -width * 0.05f, 4f to -width * 0.09f).forEach { (angle, dy) ->
                    Box(Modifier.size(width).offset(x = -width * 0.03f, y = dy).rotate(angle).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2B2433)).border(1.dp, TagPaper.copy(alpha = 0.18f), RoundedCornerShape(2.dp)))
                }
            }
            Box(
                Modifier
                    .size(width)
                    .shadow(8.dp, RoundedCornerShape(2.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2430)),
            ) {
                record.cover?.let { Image(it, contentDescription = "${record.title} cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                // Sleeve wear along the edge.
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent), endX = size.width * 0.08f))
                    drawRect(Color.Black.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
                }
            }
            record.rating?.let { RatingStamp(it, Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp), stampSize = width * 0.42f) }
        }
    }
}

@Composable
private fun ShelfDisc(modifier: Modifier, label: Color) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Vinyl, r, center)
        for (i in 1..4) drawCircle(Color.White.copy(alpha = 0.06f), r * (0.45f + i * 0.12f), center, style = Stroke(1f))
        drawArc(Color.White.copy(alpha = 0.1f), -150f, 60f, false, Offset(center.x - r * 0.8f, center.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(2f))
        drawCircle(label, r * 0.28f, center)
    }
}

/** The paper tag hung under the shelf on a thread: song, artist, who sent it, and play. */
@Composable
fun RecordTag(record: ShelfRecord, width: Dp, tilt: Float, modifier: Modifier = Modifier) {
    Column(modifier.width(width + 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(1.dp, 10.dp)) { drawLine(TagPaper.copy(alpha = 0.5f), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx()) }
        Column(
            Modifier
                .width(width)
                .rotate(tilt * -0.5f)
                .background(TagPaper, RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(record.title, color = TagInk, maxLines = 1, overflow = TextOverflow.Ellipsis, style = ChoomHubTextStyles.Handwritten.copy(fontSize = 17.sp, lineHeight = 18.sp))
            if (record.kind != SongKind.Track) {
                Text(record.kind.label.uppercase(), color = Color(0xFF8A6A2E), style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 8.sp), maxLines = 1)
            }
            Text(
                if (record.kind == SongKind.Playlist && record.artist.isNotEmpty()) "by ${record.artist}" else record.artist,
                color = TagInk.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FROM ${record.from.uppercase()}", color = Color(0xFF9A3B52), style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp))
                Canvas(Modifier.size(14.dp)) {
                    drawCircle(TagInk, size.minDimension / 2f)
                    drawPath(Path().apply { moveTo(size.width * 0.4f, size.height * 0.28f); lineTo(size.width * 0.74f, size.height * 0.5f); lineTo(size.width * 0.4f, size.height * 0.72f); close() }, TagPaper)
                }
            }
        }
    }
}

/** One shelf: the records standing on a single plank, their tags hanging below. */
@Composable
fun ShelfRow(records: List<ShelfRecord>, recordWidth: Dp, slots: Int, modifier: Modifier = Modifier, tags: Boolean = true, onTap: ((ShelfRecord) -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            for (i in 0 until slots) {
                val r = records.getOrNull(i)
                if (r != null) {
                    val tap = if (onTap == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = "Play ${r.title}") { onTap(r) }.testTag("record_${r.id}")
                    ShelvedRecord(r, recordWidth, if (i % 2 == 0) -3f else 2.5f, tap)
                } else Box(Modifier.size(recordWidth + 12.dp, 1.dp))
            }
        }
        ShelfPlank()
        if (tags) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (i in 0 until slots) {
                    val r = records.getOrNull(i)
                    if (r != null) RecordTag(r, recordWidth, if (i % 2 == 0) -3f else 2.5f) else Box(Modifier.width(recordWidth + 12.dp))
                }
            }
        }
    }
}

/** The Our World card art: a short shelf with spines and a couple of covers facing out. */
@Composable
fun ShelfPreviewArt(covers: List<ImageBitmap?>, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF1A1420), Color(0xFF241A22))))) {
        // Wall light.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Brass.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width * 0.7f, 0f), radius = size.width * 0.6f), size.width * 0.6f, Offset(size.width * 0.7f, 0f))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Spines, then the covers facing out.
                val spine = listOf(Color(0xFF5C2233), Color(0xFF2E3A4A), Color(0xFFB9A98A), Color(0xFF3B3349), Color(0xFF6B5A2E), Color(0xFF1C1A20), Color(0xFF7A3446))
                spine.forEachIndexed { i, c ->
                    Box(Modifier.width(if (i % 3 == 0) 9.dp else 7.dp).height((92 + (i * 7) % 12).dp).background(c, RoundedCornerShape(1.dp)))
                }
                covers.forEachIndexed { i, cover ->
                    ShelvedRecord(ShelfRecord("p$i", "", "", "", null, cover), 84.dp, if (i == 0) -4f else 3f, Modifier.padding(start = 8.dp))
                }
            }
            ShelfPlank()
        }
    }
}
