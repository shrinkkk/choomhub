package com.choomhub.app.feature.world.cabinet

import com.choomhub.app.domain.model.MeetupPhoto
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.choomhub.app.data.place.FoundPlace
import com.choomhub.app.feature.world.atlas.AtlasPin
import com.choomhub.app.feature.world.atlas.DelhiAtlas
import androidx.compose.ui.text.style.TextOverflow
import com.choomhub.app.core.designsystem.icon.PointIcons
import com.choomhub.app.domain.model.MeetupPlace
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import com.choomhub.app.core.designsystem.component.OneLineText
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.PolaroidFilter
import com.choomhub.app.core.designsystem.component.SnapPhoto
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.data.snap.CaptureFiles
import com.choomhub.app.domain.model.MeetupActivity
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.feature.pet.PetIcons
import com.choomhub.app.feature.world.MeetupRoute
import com.choomhub.app.feature.world.MeetupViewModel
import com.choomhub.app.feature.world.WorldViewModel
import com.choomhub.app.feature.world.atlas.AtlasPicker
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data object CabinetRoute

/**
 * The cabinet, a date to look at, and its editor. [onEditMeetup] opens the editor (new dates and
 * live ones go straight there); [onViewMeetup] opens a finished date to look at.
 */
fun NavGraphBuilder.cabinetScreens(contentPadding: PaddingValues, onEditMeetup: (String) -> Unit, onViewMeetup: (String) -> Unit, onBack: () -> Unit) {
    composable<CabinetRoute> { CabinetScreen(contentPadding, onEditMeetup, onBack, onViewMeetup = onViewMeetup) }
    composable<MeetupViewRoute> { MeetupViewScreen(contentPadding, onEditMeetup, onBack) }
    composable<MeetupRoute> { MeetupScreen(contentPadding, onBack) }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

internal fun LocalDate.long(): String = format(dayFormat)

/** A photo in the app's polaroid look. */
@Composable
internal fun MeetupPolaroid(bytes: ByteArray?, width: Dp, angle: Float = 0f, modifier: Modifier = Modifier, stamp: String? = null) {
    val colors = ChoomHubTheme.colors
    Box(
        modifier
            .rotate(angle)
            .shadow(6.dp)
            .background(colors.bone)
            .padding(start = width * 0.06f, end = width * 0.06f, top = width * 0.06f, bottom = width * 0.2f)
            .width(width * 0.88f),
    ) {
        SnapPhoto(bytes, aspect = 1f, contentDescription = null, colorFilter = PolaroidFilter.colorFilter, modifier = Modifier.fillMaxWidth())
        if (stamp != null) FilmStamp(stamp, (width.value * 0.11f).sp, Modifier.align(Alignment.BottomEnd).padding(end = width * 0.05f, bottom = width * 0.03f))
    }
}

internal val FilmOrange = Color(0xFFFF8A1F)

/**
 * The orange time an old film camera burns into the corner of a picture: "21:43", glowing a
 * little, for photos taken while the date was live.
 */
@Composable
internal fun FilmStamp(text: String, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ChoomHubTextStyles.Eyebrow.copy(
            fontSize = size,
            letterSpacing = size * 0.08f,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            shadow = androidx.compose.ui.graphics.Shadow(FilmOrange.copy(alpha = 0.75f), blurRadius = size.value * 0.9f),
        ),
        color = FilmOrange.copy(alpha = 0.92f),
        maxLines = 1,
        softWrap = false,
        modifier = modifier.testTag("film_stamp"),
    )
}

@Composable
private fun BackRow(onBack: () -> Unit, eyebrow: String) {
    val colors = ChoomHubTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).testTag("world_back"),
            contentAlignment = Alignment.Center,
        ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textSecondary) }
        Text(eyebrow, style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
    }
}

// --- The cabinet ---

@Composable
fun CabinetScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit, viewModel: WorldViewModel = hiltViewModel(), onViewMeetup: (String) -> Unit = onOpenMeetup) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val colors = ChoomHubTheme.colors
    var choosing by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    if (choosing) {
        InkPanel({ choosing = false }, Modifier.testTag("cabinet_new")) {
            GothicMotifArt(GothicMotif.Candle, Modifier.size(64.dp))
            Text("Log a meetup", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
            Spacer(Modifier.height(16.dp))
            ChoomHubPrimaryButton("It's happening now", {
                choosing = false
                viewModel.startLive(onOpenMeetup)
            }, Modifier.fillMaxWidth().testTag("cabinet_live"))
            Text("Time together counts while you're out; add photos as you go.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            ChoomHubSecondaryButton("One from before", {
                choosing = false
                pickingDate = true
            }, Modifier.fillMaxWidth().testTag("cabinet_past"))
            Text("Pick the date, then type in the time you spent together.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
    if (pickingDate) {
        DatePanel(viewModel.today(), onPick = { d -> pickingDate = false; viewModel.logPast(d, onOpenMeetup) }, onDismiss = { pickingDate = false })
    }
    val list = logs.orEmpty()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .testTag("screen_cabinet"),
    ) {
        Column(Modifier.padding(16.dp)) {
            BackRow(onBack, "EVERY TIME WE MET")
            Text("The Archive", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
            Spacer(Modifier.height(10.dp))
            list.firstOrNull { it.live }?.let { live ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.pink.copy(alpha = 0.1f))
                        .border(1.dp, colors.pink.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button) { onOpenMeetup(live.id) }
                        .padding(12.dp)
                        .testTag("cabinet_live_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).background(colors.tomato, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Logging now · ${MeetupRules.duration(live.minutesAt(now) ?: 0)}", style = MaterialTheme.typography.titleSmall, color = colors.textMain)
                        Text(listOfNotNull(live.place, "${live.photos.size} photo${if (live.photos.size == 1) "" else "s"}").joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Text("OPEN", style = ChoomHubTextStyles.Eyebrow, color = colors.pink)
                }
            }
        }
        // The drawer: felt, with each meetup pinned in it.
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(CabinetInk.Felt, Color(0xFF231823)))).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (list.isEmpty()) {
                Text("Nothing pinned yet. Log the next time you meet, or one from before.", style = ChoomHubTextStyles.Handwritten, color = CabinetInk.Bone.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp))
            } else {
                val years = list.map { it.date.year }
                Text(
                    "${if (years.min() == years.max()) "${years.min()}" else "${years.min()} — ${years.max()}"} · ${list.size} ARCHIVED",
                    style = ChoomHubTextStyles.Eyebrow,
                    color = CabinetInk.Brass,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            list.forEachIndexed { i, m ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Open") { if (m.live) onOpenMeetup(m.id) else onViewMeetup(m.id) }.testTag("specimen_${m.id}"),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box {
                        if (m.thumb != null) {
                            MeetupPolaroid(m.thumb, 112.dp, if (i % 2 == 0) -3f else 2.5f, stamp = m.photos.firstOrNull()?.let { m.stamps[it.id] })
                        } else {
                            Box(Modifier.size(112.dp, 124.dp).rotate(if (i % 2 == 0) -3f else 2.5f).border(1.dp, CabinetInk.Bone.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                Text("no photos", style = ChoomHubTextStyles.Handwritten, color = CabinetInk.Bone.copy(alpha = 0.5f))
                            }
                        }
                        BrassPin(Modifier.size(14.dp).align(Alignment.TopCenter).offset(y = (-4).dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        SpecimenLabel(
                            list.size - i,
                            listOf(
                                "Date" to m.date.long(),
                                "Locality" to m.place,
                                "Together" to (m.minutesAt(now)?.let(MeetupRules::duration)?.let { if (m.live) "$it, counting" else it }),
                                "Photos" to m.photos.size.takeIf { it > 0 }?.toString(),
                            ),
                            Modifier.fillMaxWidth().rotate(if (i % 2 == 0) 1f else -1f).padding(top = 8.dp),
                        )
                        BrassPin(Modifier.size(12.dp).align(Alignment.TopCenter).offset(y = 2.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) { ChoomHubPrimaryButton("Log a meetup", { choosing = true }, Modifier.fillMaxWidth().testTag("cabinet_log")) }
    }
}

/** Picking a date, on the app's own pop-up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePanel(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    var picked by rememberSaveable { mutableStateOf(initial.toString()) }
    InkPanel(onDismiss, Modifier.testTag("date_panel")) {
        Text("WHEN WAS IT?", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        MonthCalendar(LocalDate.parse(picked), onPick = { picked = it.toString() }, latest = LocalDate.now())
        Spacer(Modifier.height(12.dp))
        ChoomHubPrimaryButton("Use ${LocalDate.parse(picked).format(shortDate)}", { onPick(LocalDate.parse(picked)) }, Modifier.fillMaxWidth().testTag("date_ok"))
        TextButton(onDismiss, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
    }
}

private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/**
 * A month of days in seven even columns that always fit the panel (they share its width rather
 * than asking for a fixed one). Days after [latest] can't be picked.
 */
@Composable
internal fun MonthCalendar(selected: LocalDate, onPick: (LocalDate) -> Unit, latest: LocalDate, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    var month by rememberSaveable { mutableStateOf(java.time.YearMonth.from(selected).toString()) }
    val shown = java.time.YearMonth.parse(month)
    // The phone's own first day of the week (Sunday in India, Monday in much of Europe).
    val firstDay = java.time.temporal.WeekFields.of(androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0)).firstDayOfWeek
    val days = remember(shown, firstDay) { monthGrid(shown, firstDay) }
    val canGoOn = shown < java.time.YearMonth.from(latest)
    Column(modifier.fillMaxWidth().testTag("month_calendar")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Previous month") { month = shown.minusMonths(1).toString() }.testTag("month_prev"),
                contentAlignment = Alignment.Center,
            ) { Icon(PetIcons.Back, contentDescription = "Previous month", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
            Text(
                shown.format(monthTitle),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textMain,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("month_title"),
            )
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(enabled = canGoOn, role = Role.Button, onClickLabel = "Next month") { month = shown.plusMonths(1).toString() }.testTag("month_next"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PetIcons.Back,
                    contentDescription = "Next month",
                    tint = if (canGoOn) colors.textSecondary else colors.textFaint.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp).graphicsLayer { scaleX = -1f },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { k ->
                Text(
                    firstDay.plus(k.toLong()).getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH),
                    style = ChoomHubTextStyles.Eyebrow,
                    color = colors.textFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).heightIn(max = 48.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val future = day.isAfter(latest)
                            val isPicked = day == selected
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .then(if (isPicked) Modifier.background(colors.pink) else Modifier)
                                    .then(if (day == latest && !isPicked) Modifier.border(1.dp, colors.pink.copy(alpha = 0.7f), CircleShape) else Modifier)
                                    .clickable(enabled = !future, role = Role.Button, onClickLabel = day.format(shortDate)) { onPick(day) }
                                    .testTag("day_$day"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "lnum"),
                                    color = when {
                                        isPicked -> colors.night
                                        future -> colors.textFaint.copy(alpha = 0.35f)
                                        day == latest -> colors.pink
                                        else -> colors.textMain
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A month laid out in weeks starting on [firstDay]; null where a cell is outside the month. */
internal fun monthGrid(month: java.time.YearMonth, firstDay: java.time.DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    return cells + List((7 - cells.size % 7) % 7) { null }
}

// --- One meetup ---

/** Which of the spots with that name it was (or, with one match, whether it's the right one). */
@Composable
internal fun PlaceChoicesPanel(c: MeetupViewModel.PlaceChoices, onChoose: (FoundPlace?) -> Unit, onPinMyself: () -> Unit) {
    val colors = ChoomHubTheme.colors
    InkPanel({ onChoose(null) }, Modifier.testTag("place_choices")) {
        Text(if (c.options.size == 1) "Is this the one?" else "Which ${c.typed}?", style = MaterialTheme.typography.headlineSmall, color = colors.textMain, textAlign = TextAlign.Center)
        Text(if (c.options.size == 1) "check the area" else "there's more than one in Delhi", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
        Spacer(Modifier.height(12.dp))
        c.options.forEachIndexed { i, option ->
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surface)
                    .border(1.dp, colors.lavender.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { onChoose(option) }
                    .padding(14.dp)
                    .testTag("place_choice_$i"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Where in Delhi it is, at a glance.
                    DelhiAtlas(
                        listOf(AtlasPin("c$i", option.lon.toFloat(), option.lat.toFloat())),
                        Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                        selected = "c$i",
                        small = true,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.name ?: c.typed, style = MaterialTheme.typography.titleSmall, color = colors.textMain)
                        Text(option.address ?: "%.4f, %.4f".format(Locale.ENGLISH, option.lat, option.lon), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Text(
            "Not here? Add the area to the name (\"${c.typed}, Shalimar Bagh\") and find it again, or pin it yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton({ onPinMyself() }, Modifier.fillMaxWidth().testTag("place_choice_pin")) {
            OneLineText(if (c.options.size == 1) "NOT THIS ONE · PIN IT MYSELF" else "NONE OF THESE · PIN IT MYSELF", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
        }
    }
}



@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MeetupScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: MeetupViewModel = hiltViewModel()) {
    val m by viewModel.meetup.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val colors = ChoomHubTheme.colors
    val context = LocalContext.current
    val meetup = m ?: run {
        Box(Modifier.fillMaxSize().padding(contentPadding).padding(16.dp)) { BackRow(onBack, "THE ARCHIVE") }
        return
    }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pinning by rememberSaveable { mutableStateOf<Int?>(null) }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    val firsts by viewModel.firsts.collectAsStateWithLifecycle()
    val extras by viewModel.extras.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCapture?.let(Uri::parse)
        pendingCapture = null
        if (saved && uri != null) viewModel.addPhotos(listOf(uri), fromCamera = true)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MeetupRules.MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris)
    }
    val places by viewModel.places.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    var locatingFor by rememberSaveable { mutableStateOf(0) }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) viewModel.useHere(locatingFor)
    }
    if (pickingDate) DatePanel(meetup.date, onPick = { pickingDate = false; viewModel.setDate(it) }, onDismiss = { pickingDate = false })
    pinning?.let { index ->
        val place = places.getOrNull(index)
        InkPanel({ pinning = null }, Modifier.testTag("pin_panel")) {
            Text("Tap where it was", style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
            place?.name?.takeIf { it.isNotBlank() }?.let { Text(it, style = ChoomHubTextStyles.Handwritten, color = colors.pink) }
            Spacer(Modifier.height(8.dp))
            AtlasPicker(place?.lat, place?.lon, onPick = { lat, lon -> viewModel.pinAt(index, lat, lon) }, modifier = Modifier.fillMaxWidth().height(320.dp))
            Spacer(Modifier.height(8.dp))
            ChoomHubPrimaryButton("Done", { pinning = null }, Modifier.fillMaxWidth())
        }
    }
    choices?.let { c -> PlaceChoicesPanel(c, onChoose = viewModel::choose, onPinMyself = { viewModel.choose(null); pinning = c.index }) }
    removing?.let { pid ->
        val photo = meetup.photos.firstOrNull { it.id == pid }
        if (photo == null) {
            removing = null
        } else {
            InkDialog(
                title = "Take this photo out?",
                whisper = "it's gone for both of you",
                message = "The rest stay as they are.",
                confirm = "Keep it",
                onConfirm = { removing = null },
                dismissive = "Take it out",
                onDismissive = { viewModel.removePhoto(pid); removing = null },
                onDismissRequest = { removing = null },
                modifier = Modifier.testTag("photo_remove_confirm"),
            ) { MeetupPolaroid(photo.thumb, 96.dp, -3f, stamp = meetup.stamps[pid]) }
        }
    }
    if (confirmDelete) {
        InkDialog(
            title = "Take it out of the archive?",
            whisper = "this one can't be undone",
            message = "The date, the photos and everything written on it go, for both of you, and its star leaves the sky.",
            confirm = "Keep it",
            onConfirm = { confirmDelete = false },
            dismissive = "Take it out",
            onDismissive = { confirmDelete = false; viewModel.delete(onBack) },
            onDismissRequest = { confirmDelete = false },
            modifier = Modifier.testTag("meetup_delete_confirm"),
        ) { GothicMotifArt(GothicMotif.Moth, Modifier.size(72.dp)) }
    }
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.clearMessage()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .padding(16.dp)
            .widthIn(max = 600.dp)
            .testTag("screen_meetup"),
    ) {
        BackRow(onBack, "THE ARCHIVE")
        Text(if (meetup.live) "Logging it now" else meetup.date.long(), style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        if (meetup.live) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(Modifier.size(8.dp).background(colors.tomato, CircleShape))
                Text("  LIVE · ${MeetupRules.duration(meetup.minutesAt(now) ?: 0)} together so far", style = ChoomHubTextStyles.Eyebrow, color = colors.tomato)
            }
        }
        busy?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.candle, modifier = Modifier.padding(top = 6.dp)) }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.pink, modifier = Modifier.padding(top = 6.dp).testTag("meetup_message")) }

        Field("Date", required = true) {
            ValueBox(meetup.date.long(), "CHANGE", Modifier.testTag("meetup_date")) { pickingDate = true }
        }
        Field("Time together") {
            if (meetup.live) {
                ValueBox("${MeetupRules.duration(meetup.minutesAt(now) ?: 0)} · counting", "STOP", Modifier.testTag("meetup_stop")) { viewModel.stopLive() }
                Text("Counts while you're out. Stop it when you part.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.padding(top = 4.dp))
            } else {
                MinutesField(meetup.minutes, viewModel::typeMinutes)
            }
        }
        Field("Where") {
            val shown = places.ifEmpty { listOf(MeetupPlace("")) }
            shown.forEachIndexed { index, place ->
                PlaceRow(
                    index = index,
                    place = place,
                    count = shown.size,
                    onName = { viewModel.setPlaceName(index, it) },
                    onFind = { viewModel.findPlace(index) },
                    onHere = {
                        if (viewModel.canFindHere) viewModel.useHere(index) else {
                            locatingFor = index
                            askLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    },
                    onPin = { pinning = index },
                    onRemove = { viewModel.removePlace(index) },
                )
            }
            if (shown.size < MeetupRules.MAX_PLACES && shown.last().name.isNotBlank()) {
                SmallAction("+ ANOTHER PLACE", "meetup_add_place") { viewModel.addPlace() }
                Text("Went somewhere after? Add it in order; the atlas draws your route.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Field("Photos · ${meetup.photos.size} of ${MeetupRules.MAX_PHOTOS}") {
            PhotoGrid(
                meetup.photos,
                meetup.stamps,
                onReorder = viewModel::reorderTo,
                onRemove = { removing = it },
            ) {
                if (meetup.photos.size < MeetupRules.MAX_PHOTOS) {
                    AddTile("◉", "TAKE", "meetup_take") {
                        val uri = CaptureFiles.newCaptureUri(context)
                        pendingCapture = uri.toString()
                        takePhoto.launch(uri)
                    }
                    AddTile("↑", "UPLOAD", "meetup_upload") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            }
            Text(
                if (meetup.photos.size > 1) "Hold a photo and drag it to move it; the first is the cover." else "Every photo gets the polaroid look; the first is the cover.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (meetup.live) Text("Taken with TAKE while it's live: they get the film time stamp.", style = MaterialTheme.typography.bodySmall, color = FilmOrange.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
        }
        Field("What we did") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MeetupActivity.entries.forEach { a ->
                    val on = a in meetup.did
                    PillBadge(
                        a.label,
                        Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Checkbox) { viewModel.toggle(a) }.testTag("meetup_did_${a.wire}"),
                        color = if (on) colors.pink else colors.textFaint,
                        showDot = on,
                    )
                }
            }
            if (MeetupActivity.Other in meetup.did) {
                Spacer(Modifier.height(10.dp))
                LinesEditor(
                    lines = extras,
                    placeholder = { if (it == 0) "What else? (karaoke)" else "And?" },
                    tag = "meetup_extra",
                    max = MeetupRules.MAX_EXTRAS,
                    maxLength = MeetupRules.MAX_EXTRA,
                    addLabel = "+ ANOTHER",
                    onChange = viewModel::setExtra,
                    onRemove = viewModel::removeExtra,
                    onAdd = viewModel::addExtra,
                )
            }
        }
        Field("Firsts") {
            LinesEditor(
                lines = firsts,
                placeholder = { if (it == 0) "first time at a rooftop café" else "and another first" },
                tag = "meetup_first",
                max = MeetupRules.MAX_FIRSTS,
                maxLength = MeetupRules.MAX_FIRST,
                addLabel = "+ ANOTHER FIRST",
                onChange = viewModel::setFirst,
                onRemove = viewModel::removeFirst,
                onAdd = viewModel::addFirst,
            )
        }
        Field("Note") {
            var note by rememberSaveable(meetup.id) { mutableStateOf(meetup.note.orEmpty()) }
            ChoomHubTextField(note, { note = it.take(MeetupRules.MAX_NOTE); viewModel.setNote(note) }, label = "Anything to remember", singleLine = false, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth().testTag("meetup_note"))
        }
        Spacer(Modifier.height(18.dp))
        ChoomHubPrimaryButton("Pin it in the archive", { viewModel.pinned(); onBack() }, Modifier.fillMaxWidth().testTag("meetup_done"))
        Text("Only the date is needed. Everything saves as you go, and either of you can change it later.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        TextButton({ confirmDelete = true }, Modifier.fillMaxWidth().testTag("meetup_delete")) {
            OneLineText("TAKE IT OUT OF THE ARCHIVE", style = MaterialTheme.typography.labelLarge, color = colors.textFaint)
        }
    }
}

@Composable
private fun Field(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    val colors = ChoomHubTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row {
            Text(label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text(if (required) "  REQUIRED" else "  OPTIONAL", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = if (required) colors.pink else colors.textFaint)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ValueBox(text: String, action: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.surface)
            .border(1.dp, colors.lavender.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.textMain, modifier = Modifier.weight(1f))
        Text(action, style = ChoomHubTextStyles.Eyebrow, color = colors.lavender)
    }
}

/** Hours and minutes, typed in (for one from before, or to correct the clock). */
/** One place of the day: its name, whether it's on the map, and ways to put it there. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceRow(index: Int, place: MeetupPlace, count: Int, onName: (String) -> Unit, onFind: () -> Unit, onHere: () -> Unit, onPin: () -> Unit, onRemove: () -> Unit) {
    val colors = ChoomHubTheme.colors
    var text by rememberSaveable(index) { mutableStateOf(place.name) }
    LaunchedEffect(place.name) { if (place.name != text) text = place.name }
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("meetup_place_row_$index")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count > 1) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(CabinetInk.Wine.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = ChoomHubTextStyles.Eyebrow, color = colors.bone)
                }
                Spacer(Modifier.width(8.dp))
            }
            ChoomHubTextField(
                value = text,
                onValueChange = { text = it.take(MeetupRules.MAX_PLACE); onName(text) },
                label = if (index == 0) "Place" else "Then",
                supportingText = if (place.hasPin) "On the map ✓" else "Not on the map yet",
                onDone = onFind,
                modifier = Modifier.weight(1f).testTag(if (index == 0) "meetup_place" else "meetup_place_$index"),
            )
            if (count > 1) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Remove this place", onClick = onRemove).testTag("meetup_place_remove_$index"),
                    contentAlignment = Alignment.Center,
                ) { Icon(PointIcons.Close, contentDescription = "Remove this place", tint = colors.textFaint, modifier = Modifier.size(16.dp)) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, start = if (count > 1) 34.dp else 0.dp)) {
            SmallAction("FIND ON THE MAP", if (index == 0) "meetup_find" else "meetup_find_$index", onFind)
            SmallAction("USE WHERE WE ARE", if (index == 0) "meetup_here" else "meetup_here_$index", onHere)
            SmallAction("PIN IT MYSELF", if (index == 0) "meetup_pin" else "meetup_pin_$index", onPin)
        }
    }
}

/**
 * A few short lines (firsts, your own activities), one field each: a cross takes one out, and
 * there's room to add another until [max].
 */
@Composable
private fun LinesEditor(
    lines: List<String>,
    placeholder: (Int) -> String,
    tag: String,
    max: Int,
    maxLength: Int,
    addLabel: String,
    onChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val shown = lines.ifEmpty { listOf("") }
    shown.forEachIndexed { index, line ->
        var text by rememberSaveable(tag, index) { mutableStateOf(line) }
        LaunchedEffect(line) { if (line != text) text = line }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            ChoomHubTextField(
                text,
                { text = it.take(maxLength); onChange(index, text) },
                label = placeholder(index),
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                modifier = Modifier.weight(1f).testTag(if (index == 0) tag else "${tag}_$index"),
            )
            if (shown.size > 1 || line.isNotBlank()) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Remove") { text = ""; onRemove(index) }.testTag("${tag}_remove_$index"),
                    contentAlignment = Alignment.Center,
                ) { Icon(PointIcons.Close, contentDescription = "Remove", tint = colors.textFaint, modifier = Modifier.size(16.dp)) }
            }
        }
    }
    if (shown.size < max && shown.last().isNotBlank()) SmallAction(addLabel, "${tag}_add", onAdd)
}

/**
 * The photos as polaroids. Hold one and drag it to a new place (the others make room); the
 * small cross in a corner asks before taking that one out. [trailing] (take, upload) comes last.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoGrid(
    photos: List<MeetupPhoto>,
    stamps: Map<String, String>,
    onReorder: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
    trailing: @Composable () -> Unit,
) {
    val ids = photos.map { it.id }
    var order by remember(ids) { mutableStateOf(ids) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var grid by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val haptics = LocalHapticFeedback.current
    FlowRow(
        Modifier.onGloballyPositioned { grid = it }.testTag("meetup_photos"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        order.forEachIndexed { i, id ->
            val p = photos.firstOrNull { it.id == id } ?: return@forEachIndexed
            key(id) {
                val lifted = dragging == id
                Box(
                    Modifier
                        .onGloballyPositioned { c -> grid?.let { g -> if (g.isAttached && c.isAttached) bounds[id] = g.localBoundingBoxOf(c) } }
                        .zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer {
                            if (lifted) {
                                translationX = drag.x
                                translationY = drag.y
                                scaleX = 1.1f
                                scaleY = 1.1f
                            }
                        }
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = id
                                    drag = Offset.Zero
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    drag += amount
                                    val mine = bounds[id] ?: return@detectDragGesturesAfterLongPress
                                    val at = mine.center + drag
                                    val over = order.firstOrNull { other -> other != id && bounds[other]?.contains(at) == true }
                                    if (over != null) {
                                        val target = bounds[over]!!
                                        order = order.toMutableList().apply { remove(id); add(order.indexOf(over), id) }
                                        // It now lives where [over] was: keep it under the finger.
                                        drag -= target.topLeft - mine.topLeft
                                        bounds[id] = target
                                    }
                                },
                                onDragEnd = {
                                    dragging = null
                                    drag = Offset.Zero
                                    onReorder(order)
                                },
                                onDragCancel = {
                                    dragging = null
                                    drag = Offset.Zero
                                    order = ids
                                },
                            )
                        }
                        .testTag("meetup_photo_$i"),
                ) {
                    MeetupPolaroid(p.thumb, 78.dp, if (lifted) 0f else listOf(-4f, 2f, -1f, 3f, -2f, 1.5f)[i % 6], stamp = stamps[id])
                    if (i == 0) Text("COVER", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 8.sp), color = CabinetInk.Wine, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    // The cross, just over the corner.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xE6120E16))
                            .border(1.dp, CabinetInk.Bone.copy(alpha = 0.5f), CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Take this photo out") { onRemove(id) }
                            .testTag("meetup_photo_remove_$i"),
                        contentAlignment = Alignment.Center,
                    ) { Icon(PointIcons.Close, contentDescription = "Take this photo out", tint = CabinetInk.Bone, modifier = Modifier.size(11.dp)) }
                }
            }
        }
        trailing()
    }
}

@Composable
private fun MinutesField(minutes: Int?, onChange: (Int?) -> Unit) {
    val colors = ChoomHubTheme.colors
    var h by rememberSaveable { mutableStateOf(minutes?.let { (it / 60).toString() }.orEmpty()) }
    var m by rememberSaveable { mutableStateOf(minutes?.let { (it % 60).toString() }.orEmpty()) }
    // Saved as you type (after a short pause), like the other fields: no button to forget.
    fun push() {
        val hours = h.toIntOrNull()
        val mins = m.toIntOrNull()
        onChange(if (hours == null && mins == null) null else ((hours ?: 0) * 60 + (mins ?: 0).coerceIn(0, 59)))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoomHubTextField(
            h, { h = it.filter(Char::isDigit).take(2); push() }, label = "Hours",
            keyboardType = KeyboardType.Number, onDone = ::push,
            modifier = Modifier.weight(1f).testTag("meetup_hours"),
        )
        ChoomHubTextField(
            m, { m = it.filter(Char::isDigit).take(2); push() }, label = "Minutes",
            keyboardType = KeyboardType.Number, onDone = ::push,
            modifier = Modifier.weight(1f).testTag("meetup_minutes"),
        )
    }
    Text(
        if (minutes != null) "Saved: ${MeetupRules.duration(minutes)}" else "Saves as you type",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textFaint,
        modifier = Modifier.padding(top = 4.dp).testTag("meetup_time_saved"),
    )
}

@Composable
private fun SmallAction(text: String, tag: String, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Text(
        text,
        style = ChoomHubTextStyles.Eyebrow,
        color = colors.lavender,
        modifier = Modifier.clip(MaterialTheme.shapes.small).border(1.dp, colors.lavender.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick).padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp).testTag(tag),
    )
}

@Composable
private fun AddTile(icon: String, label: String, tag: String, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Column(
        Modifier.size(74.dp, 86.dp).clip(MaterialTheme.shapes.small).border(1.dp, colors.lavender.copy(alpha = 0.4f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 22.sp, color = colors.lavender)
        Text(label, style = ChoomHubTextStyles.Eyebrow, color = colors.lavender)
    }
}
