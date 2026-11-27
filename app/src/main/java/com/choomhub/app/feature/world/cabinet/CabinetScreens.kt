package com.choomhub.app.feature.world.cabinet

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

fun NavGraphBuilder.cabinetScreens(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit) {
    composable<CabinetRoute> { CabinetScreen(contentPadding, onOpenMeetup, onBack) }
    composable<MeetupRoute> { MeetupScreen(contentPadding, onBack) }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

internal fun LocalDate.long(): String = format(dayFormat)

/** A photo in the app's polaroid look. */
@Composable
internal fun MeetupPolaroid(bytes: ByteArray?, width: Dp, angle: Float = 0f, modifier: Modifier = Modifier) {
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
    }
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
fun CabinetScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit, viewModel: WorldViewModel = hiltViewModel()) {
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
            Text("Specimen cabinet", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
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
                    "${if (years.min() == years.max()) "${years.min()}" else "${years.min()} — ${years.max()}"} · ${list.size} SPECIMEN${if (list.size == 1) "" else "S"}",
                    style = ChoomHubTextStyles.Eyebrow,
                    color = CabinetInk.Brass,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            list.forEachIndexed { i, m ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Open") { onOpenMeetup(m.id) }.testTag("specimen_${m.id}"),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box {
                        if (m.thumb != null) {
                            MeetupPolaroid(m.thumb, 112.dp, if (i % 2 == 0) -3f else 2.5f)
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
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
        },
    )
    InkPanel(onDismiss, Modifier.testTag("date_panel")) {
        DatePicker(
            state = state,
            title = null,
            headline = null,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color.Transparent,
                dayContentColor = colors.textMain,
                weekdayContentColor = colors.textSecondary,
                navigationContentColor = colors.textSecondary,
                yearContentColor = colors.textMain,
                currentYearContentColor = colors.pink,
                selectedYearContainerColor = colors.pink,
                selectedDayContainerColor = colors.pink,
                selectedDayContentColor = colors.night,
                todayContentColor = colors.pink,
                todayDateBorderColor = colors.pink,
                subheadContentColor = colors.textSecondary,
                disabledDayContentColor = colors.textFaint.copy(alpha = 0.4f),
            ),
        )
        ChoomHubPrimaryButton("Use this date", {
            state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
        }, Modifier.fillMaxWidth().testTag("date_ok"), enabled = state.selectedDateMillis != null)
        TextButton(onDismiss, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
    }
}

// --- One meetup ---

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
        Box(Modifier.fillMaxSize().padding(contentPadding).padding(16.dp)) { BackRow(onBack, "THE CABINET") }
        return
    }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pinning by rememberSaveable { mutableStateOf(false) }
    var photoMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCapture?.let(Uri::parse)
        pendingCapture = null
        if (saved && uri != null) viewModel.addPhotos(listOf(uri))
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MeetupRules.MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris)
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) viewModel.useHere()
    }
    if (pickingDate) DatePanel(meetup.date, onPick = { pickingDate = false; viewModel.setDate(it) }, onDismiss = { pickingDate = false })
    if (pinning) {
        InkPanel({ pinning = false }, Modifier.testTag("pin_panel")) {
            Text("Tap where it was", style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
            Spacer(Modifier.height(8.dp))
            AtlasPicker(meetup.lat, meetup.lon, onPick = { lat, lon -> viewModel.pinAt(lat, lon) }, modifier = Modifier.fillMaxWidth().height(320.dp))
            Spacer(Modifier.height(8.dp))
            ChoomHubPrimaryButton("Done", { pinning = false }, Modifier.fillMaxWidth())
        }
    }
    photoMenu?.let { pid ->
        val index = meetup.photoIds.indexOf(pid)
        if (index < 0) {
            photoMenu = null
        } else {
            InkPanel({ photoMenu = null }, Modifier.testTag("photo_menu")) {
                MeetupPolaroid(meetup.photos[index].thumb, 120.dp)
                Spacer(Modifier.height(12.dp))
                Text("Photo ${index + 1} of ${meetup.photos.size}", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                if (index == 0) Text("the one on the label", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
                Spacer(Modifier.height(12.dp))
                if (index > 0) ChoomHubSecondaryButton("Put it first (the cover)", { viewModel.makeFirst(pid); photoMenu = null }, Modifier.fillMaxWidth().testTag("photo_first"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    ChoomHubSecondaryButton("← Earlier", { viewModel.movePhoto(pid, -1) }, Modifier.weight(1f).testTag("photo_earlier"), enabled = index > 0)
                    ChoomHubSecondaryButton("Later →", { viewModel.movePhoto(pid, 1) }, Modifier.weight(1f).testTag("photo_later"), enabled = index < meetup.photos.lastIndex)
                }
                TextButton({ viewModel.removePhoto(pid); photoMenu = null }, Modifier.fillMaxWidth().testTag("photo_remove")) {
                    OneLineText("TAKE IT OUT", style = MaterialTheme.typography.labelLarge, color = colors.tomato)
                }
                TextButton({ photoMenu = null }, Modifier.fillMaxWidth()) { OneLineText("DONE", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
            }
        }
    }
    if (confirmDelete) {
        InkDialog(
            title = "Take it out of the cabinet?",
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
        BackRow(onBack, "THE CABINET")
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
                MinutesField(meetup.minutes, viewModel::setMinutes)
            }
        }
        Field("Where") {
            var place by rememberSaveable(meetup.id) { mutableStateOf(meetup.place.orEmpty()) }
            LaunchedEffect(meetup.place) { if (meetup.place != null && place.isBlank()) place = meetup.place.orEmpty() }
            ChoomHubTextField(
                value = place,
                onValueChange = { place = it.take(MeetupRules.MAX_PLACE); viewModel.setPlace(place) },
                label = "Place",
                supportingText = if (meetup.hasPin) "On the map ✓" else "Not on the map yet",
                onDone = { viewModel.findPlace(place) },
                modifier = Modifier.fillMaxWidth().testTag("meetup_place"),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                SmallAction("FIND ON THE MAP", "meetup_find") { viewModel.findPlace(place) }
                SmallAction("USE WHERE WE ARE", "meetup_here") {
                    if (viewModel.canFindHere) viewModel.useHere() else askLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }
                SmallAction("PIN IT MYSELF", "meetup_pin") { pinning = true }
            }
        }
        Field("Photos · ${meetup.photos.size} of ${MeetupRules.MAX_PHOTOS}") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                meetup.photos.forEachIndexed { i, p ->
                    Box(
                        Modifier.combinedClickable(onClick = { photoMenu = p.id }, onLongClick = { photoMenu = p.id }, onClickLabel = "Move or take out")
                            .testTag("meetup_photo_$i"),
                    ) {
                        MeetupPolaroid(p.thumb, 74.dp, listOf(-4f, 2f, -1f, 3f, -2f, 1.5f)[i % 6])
                        if (i == 0) Text("COVER", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 8.sp), color = CabinetInk.Wine, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    }
                }
                if (meetup.photos.size < MeetupRules.MAX_PHOTOS) {
                    AddTile("◉", "TAKE", "meetup_take") {
                        val uri = CaptureFiles.newCaptureUri(context)
                        pendingCapture = uri.toString()
                        takePhoto.launch(uri)
                    }
                    AddTile("↑", "UPLOAD", "meetup_upload") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            }
            Text("Every photo gets the polaroid look. Tap one to move it; the first is the cover.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.padding(top = 6.dp))
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
        }
        Field("A first?") {
            var first by rememberSaveable(meetup.id) { mutableStateOf(meetup.first.orEmpty()) }
            ChoomHubTextField(first, { first = it.take(MeetupRules.MAX_FIRST); viewModel.setFirst(first) }, label = "first time at a rooftop café", capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth().testTag("meetup_first"))
        }
        Field("Note") {
            var note by rememberSaveable(meetup.id) { mutableStateOf(meetup.note.orEmpty()) }
            ChoomHubTextField(note, { note = it.take(MeetupRules.MAX_NOTE); viewModel.setNote(note) }, label = "Anything to remember", singleLine = false, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth().testTag("meetup_note"))
        }
        Spacer(Modifier.height(18.dp))
        ChoomHubPrimaryButton("Pin it in the cabinet", { viewModel.pinned(); onBack() }, Modifier.fillMaxWidth().testTag("meetup_done"))
        Text("Only the date is needed. Everything saves as you go, and either of you can change it later.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        TextButton({ confirmDelete = true }, Modifier.fillMaxWidth().testTag("meetup_delete")) {
            OneLineText("TAKE IT OUT OF THE CABINET", style = MaterialTheme.typography.labelLarge, color = colors.textFaint)
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
@Composable
private fun MinutesField(minutes: Int?, onChange: (Int?) -> Unit) {
    val colors = ChoomHubTheme.colors
    var h by rememberSaveable(minutes) { mutableStateOf(minutes?.let { (it / 60).toString() }.orEmpty()) }
    var m by rememberSaveable(minutes) { mutableStateOf(minutes?.let { (it % 60).toString() }.orEmpty()) }
    fun push() {
        val hours = h.toIntOrNull()
        val mins = m.toIntOrNull()
        onChange(if (hours == null && mins == null) null else ((hours ?: 0) * 60 + (mins ?: 0).coerceIn(0, 59)))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoomHubTextField(
            h, { h = it.filter(Char::isDigit).take(2) }, label = "Hours",
            keyboardType = KeyboardType.Number, onDone = ::push,
            modifier = Modifier.weight(1f).testTag("meetup_hours"),
        )
        ChoomHubTextField(
            m, { m = it.filter(Char::isDigit).take(2) }, label = "Minutes",
            keyboardType = KeyboardType.Number, onDone = ::push,
            modifier = Modifier.weight(1f).testTag("meetup_minutes"),
        )
        TextButton(::push, Modifier.testTag("meetup_time_save")) { OneLineText("SET", style = MaterialTheme.typography.labelLarge, color = colors.lavender) }
    }
    if (minutes != null) Text("Set to ${MeetupRules.duration(minutes)}", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SmallAction(text: String, tag: String, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Text(
        text,
        style = ChoomHubTextStyles.Eyebrow,
        color = colors.lavender,
        modifier = Modifier.clip(MaterialTheme.shapes.small).border(1.dp, colors.lavender.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp).testTag(tag),
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
