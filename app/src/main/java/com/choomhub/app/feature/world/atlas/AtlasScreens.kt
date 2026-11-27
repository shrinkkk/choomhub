package com.choomhub.app.feature.world.atlas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPlace
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.feature.pet.PetIcons
import com.choomhub.app.feature.world.WorldViewModel
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data object AtlasRoute

fun NavGraphBuilder.atlasScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit) {
    composable<AtlasRoute> { AtlasScreen(contentPadding, onOpenMeetup, onBack) }
}

/**
 * Pins for every place on the map, across all your dates: places near each other share a pin,
 * which lists the dates you were there (each once, newest first).
 */
fun atlasPins(logs: List<MeetupLog>): List<Pair<AtlasPin, List<MeetupLog>>> {
    class Spot(val place: MeetupPlace, val id: String, val meetups: MutableList<MeetupLog>)
    val spots = mutableListOf<Spot>()
    logs.forEach { m ->
        m.places.forEachIndexed { i, p ->
            if (!p.hasPin) return@forEachIndexed
            val spot = spots.firstOrNull { MeetupRules.samePlace(it.place, p) }
            when {
                spot == null -> spots += Spot(p, "${m.id}#$i", mutableListOf(m))
                m !in spot.meetups -> spot.meetups += m
            }
        }
    }
    return spots.map { s ->
        val name = s.place.name.ifBlank { null } ?: s.meetups.firstNotNullOfOrNull { m -> m.places.firstOrNull { MeetupRules.samePlace(it, s.place) && it.name.isNotBlank() }?.name }
        AtlasPin(s.id, s.place.lon!!.toFloat(), s.place.lat!!.toFloat(), s.meetups.size, name) to s.meetups.sortedByDescending { it.date }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

/** Pinch to zoom, drag to move: the window follows your fingers. */
private fun Modifier.pinchMap(size: () -> Size, window: () -> MapWindow, onWindow: (MapWindow) -> Unit): Modifier = pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, _ ->
        val s = size()
        if (s.width > 0f) onWindow(window().transformed(s, centroid, pan, zoom))
    }
}

@Composable
fun AtlasScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit, viewModel: WorldViewModel = hiltViewModel()) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val colors = ChoomHubTheme.colors
    val density = LocalDensity.current
    val groups = remember(logs) { atlasPins(logs.orEmpty()) }
    var window by remember { mutableStateOf(MapWindow.Delhi) }
    var canvas by remember { mutableStateOf(Size.Zero) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color(0xFF16121C)).testTag("screen_atlas")) {
        DelhiAtlas(
            groups.map { it.first },
            Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding() + 124.dp, bottom = contentPadding.calculateBottomPadding())
                .onSizeChanged { canvas = Size(it.width.toFloat(), it.height.toFloat()) }
                .pinchMap({ canvas }, { window }) { window = it }
                .pointerInput(groups, window) {
                    detectTapGestures { tap ->
                        val reach = with(density) { 28.dp.toPx() }
                        // The pin's head sits above its point, so aim a little higher.
                        selected = groups.map { (pin, _) -> pin.id to (projectPoint(window, canvas, pin.lon, pin.lat) - Offset(0f, with(density) { 12.dp.toPx() }) - tap).getDistance() }
                            .filter { it.second < reach }.minByOrNull { it.second }?.first
                    }
                }
                .testTag("atlas_map"),
            selected = selected,
            window = window,
        )
        Column(Modifier.padding(top = contentPadding.calculateTopPadding()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textSecondary)
                }
                Text("WHERE WE'VE BEEN", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            }
            Text("Atlas", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
            Text(
                if (groups.isEmpty()) "pins appear as you log places in the archive" else "${groups.size} place${if (groups.size == 1) "" else "s"} · pinch to zoom, tap a pin",
                style = ChoomHubTextStyles.Handwritten,
                color = colors.pink,
            )
        }
        val group = groups.firstOrNull { it.first.id == selected }
        if (group != null) {
            val (pin, items) = group
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = contentPadding.calculateBottomPadding()).padding(14.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1F1926)).border(1.dp, colors.candle.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
                    .testTag("atlas_card"),
            ) {
                Text(
                    "${(pin.name ?: "This spot").uppercase()} · ${items.size} DATE${if (items.size == 1) "" else "S"}",
                    style = ChoomHubTextStyles.Eyebrow,
                    color = colors.candle,
                )
                Spacer(Modifier.height(6.dp))
                items.take(6).forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(role = Role.Button) { onOpenMeetup(m.id) }.padding(vertical = 5.dp).testTag("atlas_date_${m.id}"),
                    ) {
                        Text(m.date.format(dateFormat), style = MaterialTheme.typography.bodyMedium, color = colors.textMain, modifier = Modifier.weight(1f))
                        Text(m.minutes?.let { "${MeetupRules.duration(it)} together" } ?: "", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("TAP A DATE TO OPEN IT  ›", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = colors.lavender)
            }
        }
    }
}

/** A small map to pin a place by hand: pinch in, tap the spot. */
@Composable
fun AtlasPicker(lat: Double?, lon: Double?, onPick: (Double, Double) -> Unit, modifier: Modifier = Modifier) {
    var window by remember {
        mutableStateOf(if (lat != null && lon != null) MapWindow.around(listOf(AtlasPin("x", lon.toFloat(), lat.toFloat())), 0.03f) else MapWindow.Delhi)
    }
    var canvas by remember { mutableStateOf(Size.Zero) }
    val pins = if (lat != null && lon != null) listOf(AtlasPin("here", lon.toFloat(), lat.toFloat())) else emptyList()
    DelhiAtlas(
        pins,
        modifier
            .clip(MaterialTheme.shapes.medium)
            .onSizeChanged { canvas = Size(it.width.toFloat(), it.height.toFloat()) }
            .pinchMap({ canvas }, { window }) { window = it }
            .pointerInput(window) {
                detectTapGestures { tap ->
                    val (lo, la) = unproject(window, canvas, tap)
                    onPick(la.toDouble(), lo.toDouble())
                }
            }
            .testTag("atlas_picker"),
        selected = "here",
        small = false,
        window = window,
    )
}

