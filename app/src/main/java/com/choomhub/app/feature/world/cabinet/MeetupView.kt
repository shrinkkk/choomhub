package com.choomhub.app.feature.world.cabinet

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
            Text("THE CABINET", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.weight(1f))
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
        Text("SPECIMEN No. %03d".format(ui.number), style = ChoomHubTextStyles.Eyebrow, color = colors.pink)
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

        // The numbers, as little ink tags.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            listOfNotNull(
                minutes?.let { "TIME" to MeetupRules.duration(it) },
                m.places.size.takeIf { it > 0 }?.let { "PLACES" to "$it" },
                m.photos.size.takeIf { it > 0 }?.let { "PHOTOS" to "$it" },
            ).forEach { (label, value) -> InkTag(label, value) }
        }

        if (m.photos.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            PhotoStrip(m, viewModel, onOpen = { bigPhoto = it })
        }

        if (m.places.isNotEmpty()) {
            Section(if (m.places.size > 1) "THE ROUTE" else "WHERE")
            if (m.pins.isNotEmpty()) {
                RouteMap(m.places, Modifier.fillMaxWidth().height(if (m.pins.size > 1) 280.dp else 220.dp).clip(RoundedCornerShape(16.dp)).testTag("view_route"))
                Spacer(Modifier.height(12.dp))
            }
            Itinerary(m.places)
        }

        if (m.did.isNotEmpty()) {
            Section("WHAT WE DID")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.did.sortedBy { it.ordinal }.forEach { PillBadge(it.label, color = colors.lavender) }
            }
        }

        m.first?.let { first ->
            Section("A FIRST")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(colors.candle.copy(alpha = 0.14f))
                    .border(1.dp, colors.candle.copy(alpha = 0.45f), RoundedCornerShape(4.dp)).padding(14.dp)
                    .testTag("view_first"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("★", color = colors.candle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text("First $first", style = ChoomHubTextStyles.Handwritten, color = colors.bone)
            }
        }

        m.note?.let { note ->
            Section("THE NOTE")
            Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Column(
                    Modifier.fillMaxWidth().rotate(-0.8f).background(Color(0xFFE6DED0), RoundedCornerShape(3.dp)).padding(horizontal = 18.dp, vertical = 20.dp)
                        .testTag("view_note"),
                ) {
                    Text(note, style = ChoomHubTextStyles.Handwritten.copy(fontSize = 20.sp, lineHeight = 26.sp), color = Color(0xFF2A2230))
                }
                BrassPin(Modifier.align(Alignment.TopCenter).padding(top = 0.dp))
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
            SnapPhoto(bytes ?: thumb, aspect = 1f, contentDescription = "Photo", colorFilter = PolaroidFilter.colorFilter, modifier = Modifier.fillMaxWidth().padding(16.dp))
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
                    SnapPhoto(full ?: p.thumb, aspect = 1f, contentDescription = "Photo ${page + 1}", colorFilter = PolaroidFilter.colorFilter, modifier = Modifier.fillMaxWidth())
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

/**
 * The map of the day: every pinned place, numbered in order, joined by a dashed path drawn from
 * one to the next.
 */
@Composable
internal fun RouteMap(places: List<MeetupPlace>, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val pins = places.filter { it.hasPin }
    val window = remember(pins) { MapWindow.around(pins.map { AtlasPin("p", it.lon!!.toFloat(), it.lat!!.toFloat()) }, margin = 0.012f) }
    var size by remember { mutableStateOf(Size.Zero) }
    val measurer = rememberTextMeasurer()
    Box(modifier.onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }) {
        DelhiAtlas(emptyList(), Modifier.fillMaxSize(), small = false, window = window)
        Canvas(Modifier.fillMaxSize()) {
            if (size.width <= 0f) return@Canvas
            val points = places.mapIndexedNotNull { i, p -> if (p.hasPin) i to projectPoint(window, size, p.lon!!.toFloat(), p.lat!!.toFloat()) else null }
            // The path: a soft glow under a dashed ink line, from each place to the next.
            points.zipWithNext().forEach { (a, b) ->
                drawLine(colors.pink.copy(alpha = 0.18f), a.second, b.second, 9.dp.toPx(), StrokeCap.Round)
                drawLine(
                    colors.pink,
                    a.second,
                    b.second,
                    2.dp.toPx(),
                    StrokeCap.Round,
                    PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                )
                // A small arrowhead halfway, pointing the way you went.
                val mid = (a.second + b.second) / 2f
                val dir = (b.second - a.second).let { it / it.getDistance().coerceAtLeast(1f) }
                val side = Offset(-dir.y, dir.x)
                val tip = mid + dir * 6.dp.toPx()
                drawLine(colors.pink, tip, tip - dir * 7.dp.toPx() + side * 5.dp.toPx(), 2.dp.toPx(), StrokeCap.Round)
                drawLine(colors.pink, tip, tip - dir * 7.dp.toPx() - side * 5.dp.toPx(), 2.dp.toPx(), StrokeCap.Round)
            }
            points.forEach { (i, at) ->
                val r = 11.dp.toPx()
                drawCircle(Color.Black.copy(alpha = 0.35f), r + 1.5f, at + Offset(0f, 2f))
                drawCircle(CabinetInk.Wine, r, at)
                drawCircle(colors.bone, r, at, style = Stroke(1.5.dp.toPx()))
                val label = measurer.measure("${i + 1}", TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.bone))
                drawText(label, topLeft = at - Offset(label.size.width / 2f, label.size.height / 2f))
            }
        }
    }
}

/** The places in order, down a dotted line: number, name, and whether it's on the map. */
@Composable
private fun Itinerary(places: List<MeetupPlace>) {
    val colors = ChoomHubTheme.colors
    Column(Modifier.fillMaxWidth().testTag("view_places")) {
        places.forEachIndexed { i, p ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(26.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(CabinetInk.Wine), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 10.sp), color = colors.bone)
                    }
                    if (i < places.lastIndex) {
                        Canvas(Modifier.width(2.dp).height(24.dp)) {
                            drawLine(colors.pink.copy(alpha = 0.5f), Offset(size.width / 2f, 2f), Offset(size.width / 2f, size.height), size.width, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(top = 1.dp)) {
                    Text(p.name.ifBlank { "A pin on the map" }, style = MaterialTheme.typography.titleMedium, color = colors.textMain)
                    if (!p.hasPin) Text("not on the map", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                }
            }
        }
    }
}
