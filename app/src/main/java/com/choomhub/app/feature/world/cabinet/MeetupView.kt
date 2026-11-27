package com.choomhub.app.feature.world.cabinet

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.choomhub.app.domain.model.MeetupActivity
import kotlin.random.Random
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.SnapPhoto
import com.choomhub.app.core.designsystem.component.PolaroidFilter
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPlace
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.repository.MeetupLogRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.feature.pet.PetIcons
import com.choomhub.app.feature.world.atlas.AtlasPin
import com.choomhub.app.feature.world.atlas.DelhiAtlas
import com.choomhub.app.feature.world.atlas.MapWindow
import com.choomhub.app.feature.world.atlas.projectPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import javax.inject.Inject

/** One date in the cabinet, to look at (the editor is a tap away). */
@Serializable
data class MeetupViewRoute(val id: String)

/** A logged date, with its place in the cabinet's numbering. */
data class MeetupViewUi(val meetup: MeetupLog, val number: Int, val partner: String)

@HiltViewModel
class MeetupViewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val meetups: MeetupLogRepository,
    profile: ProfileRepository,
    ticker: SecondTicker,
    clock: Clock,
) : ViewModel() {
    val id: String = savedState.toRoute<MeetupViewRoute>().id

    val ui: StateFlow<MeetupViewUi?> = combine(meetups.observe(), profile.observeNames()) { all, names ->
        val ordered = MeetupRules.inOrder(all)
        val i = ordered.indexOfFirst { it.id == id }
        if (i < 0) null else MeetupViewUi(ordered[i], i + 1, names.partnerName ?: "They")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val now: StateFlow<Instant> = ticker.ticks().onStart { emit(clock.instant()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())

    fun photo(photoId: String): Flow<ByteArray?> = meetups.observePhoto(id, photoId)
}

private val weekday = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
private val fullDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/**
 * The date as it was: the day, how long, the photos large, the route between the places on the
 * map, what you did, a first, the note. Nothing to edit here; EDIT at the top right for that.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeetupViewScreen(contentPadding: PaddingValues, onEdit: (String) -> Unit, onBack: () -> Unit, viewModel: MeetupViewViewModel = hiltViewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val colors = ChoomHubTheme.colors
    var bigPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val ui = state
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .padding(16.dp)
            .widthIn(max = 600.dp)
            .testTag("screen_meetup_view"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).testTag("world_back"),
                contentAlignment = Alignment.Center,
            ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textSecondary) }
            Text("THE ARCHIVE", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.weight(1f))
            if (ui != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).border(1.dp, colors.lavender.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClickLabel = "Edit this date") { onEdit(ui.meetup.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("meetup_edit"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        // A little nib.
                        val w = size.width
                        drawLine(colors.lavender, Offset(w * 0.15f, w * 0.85f), Offset(w * 0.85f, w * 0.15f), w * 0.18f, StrokeCap.Round)
                        drawLine(colors.lavender, Offset(w * 0.05f, w * 0.95f), Offset(w * 0.22f, w * 0.78f), w * 0.1f, StrokeCap.Round)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("EDIT", style = ChoomHubTextStyles.Eyebrow, color = colors.lavender)
                }
            }
        }
        if (ui == null) return@Column
        val m = ui.meetup
        Spacer(Modifier.height(14.dp))
        Text("ARCHIVE %03d".format(ui.number), style = ChoomHubTextStyles.Eyebrow, color = colors.pink)
        Text(m.date.format(weekday).uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.candle, modifier = Modifier.padding(top = 8.dp))
        Text(m.date.format(fullDate), style = MaterialTheme.typography.displaySmall, color = colors.textMain, modifier = Modifier.testTag("view_date"))
        val minutes = m.minutesAt(now)
        if (m.live) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Box(Modifier.size(8.dp).background(colors.tomato, CircleShape))
                Text("  STILL GOING · ${MeetupRules.duration(minutes ?: 0)} SO FAR", style = ChoomHubTextStyles.Eyebrow, color = colors.tomato)
            }
        } else if (minutes != null) {
            Text("${MeetupRules.duration(minutes)} together", style = ChoomHubTextStyles.Handwritten, color = colors.pink, modifier = Modifier.testTag("view_together"))
        }

        // One place: just where, up here. Several get the sketched route further down.
        if (m.places.size == 1) {
            val only = m.places.first()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).testTag("view_place")) {
                Canvas(Modifier.size(width = 18.dp, height = 24.dp)) { pushPin(Offset(size.width * 0.6f, size.height), 7.dp.toPx(), null, null, colors.bone, false) }
                Spacer(Modifier.width(8.dp))
                Text(only.name.ifBlank { "A pin on the map" }, style = MaterialTheme.typography.titleMedium, color = colors.textMain)
            }
        }

        // The numbers, as little ink tags.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            listOfNotNull(
                minutes?.let { "TIME" to MeetupRules.duration(it) },
                m.places.size.takeIf { it > 1 }?.let { "PLACES" to "$it" },
                m.photos.size.takeIf { it > 0 }?.let { "PHOTOS" to "$it" },
            ).forEach { (label, value) -> InkTag(label, value) }
        }

        m.note?.let { note ->
            Box(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                Column(
                    Modifier.fillMaxWidth().rotate(-0.8f).background(Color(0xFFE6DED0), RoundedCornerShape(3.dp)).padding(horizontal = 18.dp, vertical = 20.dp)
                        .testTag("view_note"),
                ) {
                    Text(note, style = ChoomHubTextStyles.Handwritten.copy(fontSize = 20.sp, lineHeight = 26.sp), color = Color(0xFF2A2230))
                }
                BrassPin(Modifier.align(Alignment.TopCenter))
            }
        }

        if (m.photos.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            PhotoStrip(m, viewModel, onOpen = { bigPhoto = it })
        }

        if (m.places.size > 1) {
            Section("THE ROUTE")
            RouteSketch(m.places, Modifier.fillMaxWidth().testTag("view_route"))
        }

        val did = activityLabels(m)
        if (did.isNotEmpty()) {
            Section("WHAT WE DID")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                did.forEach { PillBadge(it, color = colors.lavender) }
            }
        }

        if (m.firsts.isNotEmpty()) {
            Section(if (m.firsts.size == 1) "A FIRST" else "FIRSTS")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(colors.candle.copy(alpha = 0.12f))
                    .border(1.dp, colors.candle.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("view_firsts"),
            ) {
                m.firsts.forEach { first ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("★", color = colors.candle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(12.dp))
                        Text(first, style = ChoomHubTextStyles.Handwritten, color = colors.bone)
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            if (m.byMe) "Pinned by you" else "Pinned by ${ui.partner}",
            style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp),
            color = colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
    bigPhoto?.let { pid ->
        val bytes by remember(pid) { viewModel.photo(pid) }.collectAsStateWithLifecycle(null)
        val thumb = ui?.meetup?.photos?.firstOrNull { it.id == pid }?.thumb
        Box(
            Modifier.fillMaxSize().background(Color(0xF00B090F)).clickable(role = Role.Button, onClickLabel = "Close") { bigPhoto = null }.testTag("view_photo_big"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                SnapPhoto(bytes ?: thumb, aspect = 1f, contentDescription = "Photo", colorFilter = PolaroidFilter.colorFilter, modifier = Modifier.fillMaxWidth())
                ui?.meetup?.stamps?.get(pid)?.let { FilmStamp(it, 24.sp, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp)) }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
    Spacer(Modifier.height(10.dp))
}

/** A small label, in the cabinet's ink: "TIME 5h 40m". */
@Composable
private fun InkTag(label: String, value: String) {
    val colors = ChoomHubTheme.colors
    Row(
        Modifier.border(1.dp, colors.candle.copy(alpha = 0.35f), RoundedCornerShape(3.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = colors.textFaint)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "lnum"), color = colors.bone)
    }
}

/** The photos, large, one at a time: swipe through; tap one to see it bigger. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoStrip(m: MeetupLog, viewModel: MeetupViewViewModel, onOpen: (String) -> Unit) {
    val colors = ChoomHubTheme.colors
    val pager = rememberPagerState { m.photos.size }
    HorizontalPager(
        pager,
        contentPadding = PaddingValues(horizontal = 36.dp),
        pageSpacing = 14.dp,
        modifier = Modifier.fillMaxWidth().testTag("view_photos"),
    ) { page ->
        val p = m.photos[page]
        val full by remember(p.id) { viewModel.photo(p.id) }.collectAsStateWithLifecycle(null)
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.rotate(listOf(-2f, 1.5f, -1f, 2f)[page % 4]).clickable(role = Role.Button, onClickLabel = "See it bigger") { onOpen(p.id) }
                    .testTag("view_photo_$page"),
            ) {
                Column(Modifier.background(colors.bone).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 34.dp)) {
                    Box {
                        SnapPhoto(full ?: p.thumb, aspect = 1f, contentDescription = "Photo ${page + 1}", colorFilter = PolaroidFilter.colorFilter, modifier = Modifier.fillMaxWidth())
                        m.stamps[p.id]?.let { FilmStamp(it, 20.sp, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp)) }
                    }
                }
                if (page == 0) Text("COVER", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = CabinetInk.Wine, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
            }
        }
    }
    if (m.photos.size > 1) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            m.photos.indices.forEach { i ->
                Box(
                    Modifier.padding(horizontal = 3.dp).size(if (i == pager.currentPage) 8.dp else 5.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) colors.pink else colors.textFaint.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

/** What you did, as the chips say it: the list's own, and your own words in place of "Other". */
internal fun activityLabels(m: MeetupLog): List<String> =
    m.did.sortedBy { it.ordinal }.filterNot { it == MeetupActivity.Other && m.extras.isNotEmpty() }.map { it.label } + m.extras

/**
 * A push-pin stuck in the page, drawn in ink: a short needle into the paper at [at], a round head
 * up and to the left with a shine, [number] on the head. Hollow and dashed when [unknown] (a
 * place that isn't on the map).
 */
internal fun DrawScope.pushPin(at: Offset, r: Float, number: String?, measurer: TextMeasurer?, ink: Color, unknown: Boolean, seed: Int = 0) {
    val rnd = Random(seed)
    val head = at + Offset(-r * 0.58f, -r * 1.6f)
    drawLine(Color.Black.copy(alpha = 0.35f), at, at + Offset(r * 0.75f, -r * 0.16f), r * 0.16f, StrokeCap.Round)
    drawCircle(Color.Black.copy(alpha = 0.22f), r * 0.9f, head + Offset(r * 0.85f, r * 1.25f))
    drawCircle(ink.copy(alpha = 0.6f), r * 0.13f, at)
    drawLine(ink, at, head + Offset(r * 0.3f, r * 0.55f), r * 0.13f, StrokeCap.Round)
    if (unknown) {
        drawCircle(Color(0xFF17131D), r, head)
        drawCircle(ink, r, head, style = Stroke(r * 0.13f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.33f, r * 0.25f))))
    } else {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC0506E), PinRed, Color(0xFF4E1C2C)), center = head - Offset(r * 0.35f, r * 0.4f), radius = r * 1.6f), r, head)
        // Gone over twice with the pen, not quite in the same place.
        repeat(2) {
            val o = Offset((rnd.nextFloat() - 0.5f) * r * 0.18f, (rnd.nextFloat() - 0.5f) * r * 0.18f)
            drawCircle(ink, r + o.x * 0.3f, head + o, style = Stroke(if (it == 0) r * 0.14f else r * 0.075f))
        }
        drawArc(Color.White.copy(alpha = 0.55f), 200f, 60f, false, head - Offset(r * 0.62f, r * 0.62f), Size(r * 1.24f, r * 1.24f), style = Stroke(r * 0.13f, cap = StrokeCap.Round))
    }
    if (number != null && measurer != null) {
        val label = measurer.measure(number, TextStyle(fontSize = (r / density * 1.1f).sp, color = ink, fontFamily = ChoomHubTextStyles.Handwritten.fontFamily, fontWeight = FontWeight.Bold))
        drawText(label, topLeft = head - Offset(label.size.width / 2f, label.size.height / 2f))
    }
}

private val PinRed = Color(0xFF8E3350)

/**
 * Brush dashes along [path]: short, chunky, a little uneven, like a felt-tip line on a hand-drawn
 * map.
 */
internal fun DrawScope.brushDashes(path: Path, color: Color, seed: Int) {
    val r = Random(seed)
    val measure = PathMeasure().apply { setPath(path, false) }
    val total = measure.length
    val dash = 9.dp.toPx()
    val gap = 6.5.dp.toPx()
    var at = 3.dp.toPx()
    while (at < total - 3.dp.toPx()) {
        val l = dash * (0.75f + r.nextFloat() * 0.5f)
        val seg = Path()
        measure.getSegment(at, (at + l).coerceAtMost(total), seg, true)
        drawPath(seg, color, style = Stroke(3.4.dp.toPx() * (0.85f + r.nextFloat() * 0.3f), cap = StrokeCap.Round))
        drawPath(seg, color.copy(alpha = 0.45f), style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Butt))
        at += l + gap
    }
}

/** Where each stop sits on the sketch: down the left, swaying side to side, in order. */
internal fun sketchSpots(count: Int, width: Float, top: Float, step: Float): List<Offset> =
    List(count) { i -> Offset(width * (if (i % 2 == 0) 0.13f else 0.33f), top + i * step) }

/**
 * The day's places, sketched like a hand-drawn map: numbered push-pins down the left, joined in
 * order by curved brush-dash lines, each name on the right. A place that isn't on the map gets a
 * hollow pin and a fainter line.
 */
@Composable
internal fun RouteSketch(places: List<MeetupPlace>, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val measurer = rememberTextMeasurer()
    val step = 84.dp
    Canvas(
        modifier
            .height(step * (places.size - 1) + 92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF17131D))
            .border(1.dp, colors.lavender.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = "Route: " + places.mapIndexed { i, p -> "${i + 1} ${p.name.ifBlank { "a pin on the map" }}${if (p.hasPin) "" else " (not on the map)"}" }.joinToString(", ")
            },
    ) {
        val spots = sketchSpots(places.size, size.width, 58.dp.toPx(), step.toPx())
        // The lines first, so the pins sit on top of them.
        spots.zipWithNext().forEachIndexed { k, (a, b) ->
            val mid = (a + b) / 2f
            val bow = (if (k % 2 == 0) -1f else 1f) * 26.dp.toPx()
            val ctrl = Offset((mid.x + bow).coerceIn(10.dp.toPx(), size.width * 0.45f), mid.y)
            val path = Path().apply {
                moveTo(a.x, a.y)
                quadraticTo(ctrl.x, ctrl.y, b.x, b.y)
            }
            val faint = !places[k + 1].hasPin
            brushDashes(path, if (faint) colors.bone.copy(alpha = 0.4f) else colors.bone.copy(alpha = 0.82f), seed = 10 + k)
        }
        places.forEachIndexed { i, p ->
            val at = spots[i]
            val name = measurer.measure(
                p.name.ifBlank { "A pin on the map" },
                TextStyle(fontSize = 19.sp, color = colors.bone, fontFamily = ChoomHubTextStyles.Handwritten.fontFamily, fontStyle = FontStyle.Italic),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = (size.width * 0.46f).toInt()),
            )
            val tl = Offset(size.width * 0.5f, at.y - 30.dp.toPx() - name.size.height / 2f)
            // A faint pencil leader from the pin's head to its name.
            drawLine(
                colors.textSecondary.copy(alpha = 0.35f),
                at + Offset(6.dp.toPx(), -19.dp.toPx()),
                Offset(tl.x - 8.dp.toPx(), tl.y + name.size.height / 2f),
                1.dp.toPx(),
                StrokeCap.Round,
                PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
            )
            drawText(name, topLeft = tl)
            if (!p.hasPin) {
                val note = measurer.measure("not on the map", TextStyle(fontSize = 11.sp, color = colors.textFaint, fontFamily = ChoomHubTextStyles.Handwritten.fontFamily, fontStyle = FontStyle.Italic))
                drawText(note, topLeft = tl + Offset(2f, name.size.height - 2f))
            }
            pushPin(at, 12.dp.toPx(), if (p.hasPin) "${i + 1}" else "?", measurer, colors.bone, unknown = !p.hasPin, seed = i)
        }
    }
}
