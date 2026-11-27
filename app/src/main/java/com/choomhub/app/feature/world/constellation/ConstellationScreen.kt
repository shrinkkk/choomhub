package com.choomhub.app.feature.world.constellation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.feature.pet.PetIcons
import com.choomhub.app.feature.world.WorldViewModel
import com.choomhub.app.feature.world.cabinet.MeetupPolaroid
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Serializable
data object ConstellationRoute

fun NavGraphBuilder.constellationScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit) {
    composable<ConstellationRoute> { ConstellationScreen(contentPadding, onOpenMeetup, onBack) }
}

/** The meetups as stars, oldest first. */
fun starsOf(logs: List<MeetupLog>, now: java.time.Instant): Pair<List<MeetupLog>, List<StarUi>> {
    val ordered = MeetupRules.inOrder(logs)
    return ordered to ordered.map { StarUi(it.id, it.date, it.minutesAt(now), it.place) }
}

/** Seconds since this screen opened, ticking with the display (still, without motion). */
@Composable
fun skyTime(): Float {
    val animate = LocalAmbientMotionEnabled.current
    var t by remember { mutableFloatStateOf(2f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = 2f + (it - start) / 1e9f }
    }
    return t
}

private const val MAX_ZOOM = 4f
private const val FOCUS_ZOOM = 2.6f

@Composable
fun ConstellationScreen(contentPadding: PaddingValues, onOpenMeetup: (String) -> Unit, onBack: () -> Unit, viewModel: WorldViewModel = hiltViewModel()) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val colors = ChoomHubTheme.colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val (ordered, stars) = remember(logs, now.epochSecond / 60) { starsOf(logs.orEmpty(), now) }
    var canvas by remember { mutableStateOf(Size.Zero) }
    // Your own pinch and drag; the zoom to a star animates on top and back.
    var userZoom by remember { mutableFloatStateOf(1f) }
    var userPan by remember { mutableStateOf(Offset.Zero) }
    val zoom = remember { Animatable(1f) }
    val panX = remember { Animatable(0f) }
    val panY = remember { Animatable(0f) }
    var focus by rememberSaveable { mutableStateOf<String?>(null) }
    val focusIndex = ordered.indexOfFirst { it.id == focus }.takeIf { it >= 0 }
    val time = skyTime()

    fun clampPan(p: Offset, z: Float): Offset {
        val mx = canvas.width * (z - 1f) / 2f + canvas.width * 0.15f
        val my = canvas.height * (z - 1f) / 2f + canvas.height * 0.15f
        return Offset(p.x.coerceIn(-mx, mx), p.y.coerceIn(-my, my))
    }

    fun animateTo(z: Float, p: Offset) {
        scope.launch { zoom.animateTo(z, tween(650)) }
        scope.launch { panX.animateTo(p.x, tween(650)) }
        scope.launch { panY.animateTo(p.y, tween(650)) }
    }

    LaunchedEffect(focusIndex, canvas) {
        if (canvas.width <= 0f) return@LaunchedEffect
        if (focusIndex != null) {
            // Zoom in so the star sits in the upper part of the screen, above the card.
            val star = starPositions(stars.size)[focusIndex]
            val c = Offset(canvas.width / 2f, canvas.height / 2f)
            val at = Offset(star.x * canvas.width, star.y * canvas.height)
            val target = Offset(canvas.width * 0.5f, canvas.height * 0.28f)
            animateTo(FOCUS_ZOOM, target - c - (at - c) * FOCUS_ZOOM)
        } else {
            animateTo(userZoom, userPan)
        }
    }
    BackHandler { if (focus != null) focus = null else onBack() }

    Box(Modifier.fillMaxSize().background(Color(0xFF07060B)).testTag("screen_constellation")) {
        ConstellationSky(
            stars,
            Modifier
                .fillMaxSize()
                .onSizeChanged { canvas = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(focus) {
                    if (focus != null) return@pointerInput
                    detectTransformGestures { centroid, pan, z, _ ->
                        val newZoom = (userZoom * z).coerceIn(1f, MAX_ZOOM)
                        val k = newZoom / userZoom
                        // Zoom about the fingers: the point under them stays put.
                        val c = Offset(canvas.width / 2f, canvas.height / 2f)
                        val p = (userPan - (centroid - c)) * k + (centroid - c) + pan
                        userZoom = newZoom
                        userPan = clampPan(p, newZoom)
                        scope.launch { zoom.snapTo(userZoom); panX.snapTo(userPan.x); panY.snapTo(userPan.y) }
                    }
                }
                .pointerInput(stars, focus) {
                    detectTapGestures { tap ->
                        val reach = with(density) { 30.dp.toPx() }
                        val hit = starAt(tap, stars.size, canvas, zoom.value, Offset(panX.value, panY.value), reach)
                        focus = when {
                            hit != null -> stars[hit].id
                            else -> null
                        }
                    }
                }
                .testTag("constellation_sky"),
            time = time,
            focus = focusIndex,
            zoom = zoom.value,
            pan = Offset(panX.value, panY.value),
        )
        Row(Modifier.padding(top = contentPadding.calculateTopPadding()).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back") { if (focus != null) focus = null else onBack() }.testTag("constellation_back"),
                contentAlignment = Alignment.Center,
            ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textSecondary) }
        }
        if (stars.isEmpty()) {
            Text(
                "Your sky fills up as you log meetups in the specimen cabinet: a star for every one.",
                style = ChoomHubTextStyles.Handwritten,
                color = colors.pink.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
        AnimatedVisibility(focusIndex == null && stars.isNotEmpty(), Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            val hours = ordered.sumOf { it.minutesAt(now) ?: 0 } / 60
            Column(Modifier.padding(bottom = contentPadding.calculateBottomPadding() + 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${stars.size} date${if (stars.size == 1) "" else "s"} · $hours hour${if (hours == 1) "" else "s"} together", style = ChoomHubTextStyles.Eyebrow, color = colors.bone.copy(alpha = 0.7f))
                Text("pinch to zoom · tap a star", style = ChoomHubTextStyles.Handwritten, color = colors.pink.copy(alpha = 0.7f))
            }
        }
        val shown = focusIndex?.let { ordered[it] }
        AnimatedVisibility(
            shown != null,
            Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(400, 250)) + slideInVertically(tween(450, 250)) { it / 3 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 3 },
        ) {
            val m = shown ?: return@AnimatedVisibility
            StarOverview(m, now, Modifier.padding(bottom = contentPadding.calculateBottomPadding()), onOpen = { onOpenMeetup(m.id) })
        }
    }
}

private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** That date, close up: when, how long, where, a few photos, what you wrote. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarOverview(m: MeetupLog, now: java.time.Instant, modifier: Modifier, onOpen: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Column(
        modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF120E1C).copy(alpha = 0.94f))
            .border(1.dp, colors.candle.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
            .testTag("star_overview"),
    ) {
        Text(m.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.candle)
        Text(m.date.format(longDate), style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
        val line = listOfNotNull(m.minutesAt(now)?.let { "${MeetupRules.duration(it)} together" }, m.place).joinToString(" · ")
        if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        if (m.photos.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(130.dp)) {
                m.photos.take(3).forEachIndexed { i, p ->
                    Box(Modifier.offset(x = (i * 96).dp, y = listOf(10, 0, 14)[i].dp)) {
                        MeetupPolaroid(p.thumb, 104.dp, listOf(-7f, 3f, 8f)[i])
                    }
                }
            }
            if (m.photos.size > 3) Text("+${m.photos.size - 3} more", style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint, modifier = Modifier.align(Alignment.End))
        }
        m.note?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = ChoomHubTextStyles.Handwritten, color = colors.pink, maxLines = 4)
        }
        if (m.did.isNotEmpty() || m.first != null) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                m.did.sortedBy { it.ordinal }.forEach { PillBadge(it.label, color = colors.lavender) }
                m.first?.let { PillBadge("First: $it", color = colors.candle) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "OPEN IN THE CABINET  ›",
            style = ChoomHubTextStyles.Eyebrow,
            color = colors.lavender,
            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onOpen).padding(vertical = 6.dp).testTag("star_open"),
        )
    }
}
