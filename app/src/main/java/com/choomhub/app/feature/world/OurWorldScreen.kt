package com.choomhub.app.feature.world

import com.choomhub.app.core.designsystem.component.OneLineText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.PolaroidFilter
import com.choomhub.app.core.designsystem.component.SnapPhoto
import com.choomhub.app.core.designsystem.component.polaroidFinish
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.SnapInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

@Serializable
data object OurWorldRoute

fun NavGraphBuilder.ourWorldScreen(contentPadding: PaddingValues, onOpenSnap: (String) -> Unit = {}, doors: WorldDoors = WorldDoors()) {
    composable<OurWorldRoute> { OurWorldScreen(contentPadding, onOpenSnap, doors) }
}

/** Polaroid size in the string. */
private val PolaroidWidth = 216.dp
private val FrameEdge = 10.dp

/** How far each polaroid tucks under the next one. */
private val Overlap = 16.dp

/** Space above each collection card. */
private val CardGap = 12.dp
private val Twine = Color(0xFF6B5D4F)
private val Peg = Color(0xFF3A3731)
private val StampOrange = Color(0xFFFF8A3D)
private val FrameInk = Color(0xFF2B2622)
private val FrameDate = Color(0xFF6B625A)

/** Each polaroid's tilt and sideways nudge, by position, so the string zig-zags. */
private val Tilts = floatArrayOf(-3f, 2.5f, -1.5f, 3f, -2f, 1.5f)
private val Nudges = floatArrayOf(-14f, 12f, -8f, 14f, -12f, 8f)

internal sealed interface WorldDialog {
    data object New : WorldDialog
    data class CollectionMenu(val id: String) : WorldDialog
    data class Rename(val id: String) : WorldDialog
    data class ConfirmDelete(val id: String) : WorldDialog
    data class SnapMenu(val collectionId: String, val snapId: String) : WorldDialog
    data class ConfirmRemove(val collectionId: String, val snapId: String) : WorldDialog
}

@Composable
fun OurWorldScreen(
    contentPadding: PaddingValues,
    onOpenSnap: (String) -> Unit,
    doors: WorldDoors = WorldDoors(),
    viewModel: MemoryLaneViewModel = hiltViewModel(),
    world: WorldViewModel = hiltViewModel(),
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val logs by world.logs.collectAsStateWithLifecycle()
    val shelf by world.shelf.collectAsStateWithLifecycle()
    val names by world.names.collectAsStateWithLifecycle()
    val now by world.now.collectAsStateWithLifecycle()
    val expanded by viewModel.expanded.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var dialog by remember { mutableStateOf<WorldDialog?>(null) }
    // Collections unfolding right now (their polaroids drop in) or folding back up.
    var revealing by remember { mutableStateOf(emptySet<String>()) }
    var folding by remember { mutableStateOf(emptySet<String>()) }

    fun toggle(c: CollectionUi) {
        if (c.id in folding) return
        if (c.id in expanded) {
            folding = folding + c.id
            scope.launch {
                delay(FOLD_MS + min(c.snaps.size, 6) * STAGGER_OUT_MS)
                viewModel.setExpanded(c.id, false)
                folding = folding - c.id
            }
        } else {
            revealing = revealing + c.id
            viewModel.setExpanded(c.id, true)
            scope.launch {
                delay(1_500)
                revealing = revealing - c.id
            }
        }
    }

    WorldDialogs(dialog, collections.orEmpty(), viewModel, onDismiss = { dialog = null }, open = { dialog = it })

    Box(Modifier.fillMaxSize().testTag("screen_our_world"), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .collectionCards(listState, spacing.screenGutter, ChoomHubTheme.colors.surface, ChoomHubTheme.colors.lavender)
                .testTag("our_world_list"),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + spacing.lg,
                bottom = contentPadding.calculateBottomPadding() + spacing.xxl,
            ),
        ) {
            item(key = "title") { Title(logs.orEmpty(), now, doors.onConstellation) }
            item(key = "rooms") { WorldRooms(logs, shelf, names, now, doors) }
            item(key = "lane") { LaneHeader(onNew = { dialog = WorldDialog.New }) }
            val list = collections
            when {
                list == null -> Unit
                list.isEmpty() -> item(key = "empty") { EmptyLane() }
                else -> list.forEach { c ->
                    collectionItems(
                        c,
                        open = c.id in expanded,
                        revealing = c.id in revealing,
                        folding = c.id in folding,
                        viewModel = viewModel,
                        onToggle = { toggle(c) },
                        onLongPress = { dialog = WorldDialog.CollectionMenu(c.id) },
                        onOpenSnap = onOpenSnap,
                        onSnapLongPress = { snap -> dialog = WorldDialog.SnapMenu(c.id, snap.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Title(logs: List<MeetupLog>, now: Instant, onSky: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Box(Modifier.fillMaxWidth()) {
        // The constellation, small and quiet, in the top-left corner behind the title.
        SkyPreview(logs, now, onSky, Modifier.align(Alignment.TopStart))
        Column(Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter).padding(top = 70.dp), horizontalAlignment = Alignment.End) {
            Text("Our World", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text("a shared room, slowly filling up", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Text("TAP THE STARS", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = colors.textFaint, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun LaneHeader(onNew: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter).padding(top = spacing.xl),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text("KEPT SNAPS", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text("Memory Lane", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        }
        TextButton(onClick = onNew, modifier = Modifier.testTag("collection_new")) {
            OneLineText("+ NEW", ChoomHubTextStyles.Eyebrow.copy(fontSize = 12.sp), color = colors.pink)
        }
    }
}

@Composable
private fun EmptyLane() {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    ChoomHubCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter).padding(top = CardGap).testTag("memory_lane_empty"),
        tone = CardTone.Calm,
    ) {
        Text("Nothing kept yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
        Text(
            "Make a collection with + New, or keep a snap from its screen. Kept snaps hang here and are never replaced.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

// --- One collection: its header (the stack), and when open, the string of polaroids. ---

private fun LazyListScope.collectionItems(
    c: CollectionUi,
    open: Boolean,
    revealing: Boolean,
    folding: Boolean,
    viewModel: MemoryLaneViewModel,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onOpenSnap: (String) -> Unit,
    onSnapLongPress: (SnapInfo) -> Unit,
) {
    item(key = CardKey.head(c.id), contentType = "collection") {
        CollectionHeader(c, open = open && !folding, onToggle, onLongPress, Modifier.animateItem())
    }
    if (!open) return
    c.snaps.forEachIndexed { index, snap ->
        item(key = CardKey.polaroid(c.id, snap.id), contentType = "polaroid") {
            HangingPolaroid(
                snap = snap,
                index = index,
                count = c.snaps.size,
                first = index == 0,
                drop = revealing,
                folding = folding,
                image = viewModel::image,
                onOpen = { onOpenSnap(snap.id) },
                onLongPress = { onSnapLongPress(snap) },
                modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            )
        }
    }
    item(key = CardKey.end(c.id), contentType = "end") {
        StringEnd(empty = c.snaps.isEmpty(), Modifier.animateItem())
    }
}

/** Item keys, which the card backdrop also reads to know what belongs to which collection. */
private object CardKey {
    fun head(id: String) = "head|$id"
    fun polaroid(id: String, snapId: String) = "pol|$id|$snapId"
    fun end(id: String) = "end|$id"
    fun collectionOf(key: Any): String? = (key as? String)?.split('|')?.takeIf { it.size >= 2 && it[0] in PARTS }?.get(1)
    fun isHead(key: Any) = (key as? String)?.startsWith("head|") == true
    fun isEnd(key: Any) = (key as? String)?.startsWith("end|") == true
    private val PARTS = setOf("head", "pol", "end")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionHeader(c: CollectionUi, open: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val fan by animateFloatAsState(if (open) 0f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "fan")
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter)
            .padding(top = CardGap)
            .combinedClickable(
                onClickLabel = if (open) "Fold up" else "Open",
                onLongClickLabel = "Rename or delete",
                onClick = onToggle,
                onLongClick = onLongPress,
            )
            .padding(14.dp)
            .testTag("collection_card_${c.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolaroidStack(c, fan)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.titleLarge, color = colors.textMain, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                when (c.snaps.size) { 0 -> "EMPTY"; 1 -> "1 SNAP"; else -> "${c.snaps.size} SNAPS" },
                style = ChoomHubTextStyles.Eyebrow,
                color = colors.textSecondary,
            )
            // A delete waiting on a yes shows here, so it isn't missed (long-press to answer).
            when (c.deleteAskedByMe) {
                false -> Text("they want to delete it · hold to answer", style = MaterialTheme.typography.bodySmall, color = colors.candle, modifier = Modifier.testTag("collection_delete_asked"))
                true -> Text("waiting for a yes to delete it", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                null -> Unit
            }
        }
        Text("▾", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, modifier = Modifier.graphicsLayer { rotationZ = 180f * (1 - fan) })
    }
}

/** The closed collection: up to three small polaroids, fanned, the cover on top. Straightens when open. */
@Composable
private fun PolaroidStack(c: CollectionUi, fan: Float) {
    val others = c.snaps.filter { it.id != c.cover?.id }
    val layers = listOf(others.getOrNull(1), others.getOrNull(0), c.cover) // bottom → top
    val tilts = floatArrayOf(-7f, 5f, -1f)
    val shifts = floatArrayOf(-4f, 6f, 0f)
    Box(Modifier.size(74.dp, 70.dp)) {
        layers.forEachIndexed { i, snap ->
            Column(
                Modifier
                    .offset(x = 10.dp, y = 4.dp)
                    .graphicsLayer {
                        rotationZ = tilts[i] * fan
                        translationX = shifts[i] * fan * density
                        translationY = -4f * (1 - fan) * density
                    }
                    .width(52.dp)
                    .background(ChoomHubTheme.colors.bone)
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
            ) {
                if (snap == null) {
                    Box(Modifier.size(44.dp).background(ChoomHubTheme.colors.nightDeep))
                } else {
                    SnapPhoto(
                        snap.thumb,
                        aspect = 1f,
                        contentDescription = null,
                        colorFilter = PolaroidFilter.colorFilter,
                        modifier = Modifier.size(44.dp).polaroidFinish(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HangingPolaroid(
    snap: SnapInfo,
    index: Int,
    count: Int,
    first: Boolean,
    drop: Boolean,
    folding: Boolean,
    image: (String) -> kotlinx.coroutines.flow.Flow<ByteArray?>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tilt by animateFloatAsState(Tilts[index % Tilts.size], spring(dampingRatio = 0.6f), label = "tilt")
    val nudge by animateFloatAsState(Nudges[index % Nudges.size], spring(dampingRatio = 0.6f), label = "nudge")
    // 0 = folded into the stack (hidden, raised), 1 = hanging.
    val hang = remember(snap.id) { Animatable(if (drop) 0f else 1f) }
    LaunchedEffect(snap.id, folding) {
        if (folding) {
            delay(((count - 1 - index).coerceAtMost(6)) * STAGGER_OUT_MS)
            hang.animateTo(0f, tween(FOLD_MS.toInt()))
        } else if (hang.value < 1f) {
            delay(min(index, 8) * STAGGER_IN_MS)
            hang.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 280f))
        }
    }
    val full by remember(snap.id) { image(snap.id) }.collectAsStateWithLifecycle(null)
    val overlapPx = if (first) 0 else with(androidx.compose.ui.platform.LocalDensity.current) { Overlap.roundToPx() }
    Box(
        modifier
            .fillMaxWidth()
            // Tuck under the one above: report a shorter height and draw up into its space.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height - overlapPx) { placeable.place(0, -overlapPx) }
            }
            .padding(top = if (first) 18.dp else 0.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val p = hang.value
                    alpha = p.coerceIn(0f, 1f)
                    translationY = -(1f - p) * (48f + index.coerceAtMost(4) * 24f) * density
                    translationX = nudge * p * density
                    rotationZ = tilt * p
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
                .semantics { contentDescription = snap.caption ?: "Kept snap" }
                .testTag("collection_snap_${snap.id}"),
        ) {
            Polaroid(snap, full)
            // The peg that clips it to the twine.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
                    .size(10.dp, 22.dp)
                    .background(Peg, MaterialTheme.shapes.extraSmall),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .combinedClickable(
                        onClickLabel = "Open snap",
                        onLongClickLabel = "Cover or delete",
                        onClick = onOpen,
                        onLongClick = onLongPress,
                    ),
            )
        }
    }
}

@Composable
private fun Polaroid(snap: SnapInfo, full: ByteArray?) {
    val zone = ZoneId.systemDefault()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .width(PolaroidWidth)
            .background(ChoomHubTheme.colors.bone)
            .padding(start = FrameEdge, end = FrameEdge, top = FrameEdge)
            // A deep bottom edge, like real instant film; the next polaroid tucks over its lower part.
            .padding(bottom = Overlap + 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            SnapPhoto(
                full ?: snap.thumb,
                aspect = 1f,
                contentDescription = null,
                colorFilter = PolaroidFilter.colorFilter,
                modifier = Modifier.fillMaxWidth().polaroidFinish(),
            )
            snap.sentAt?.let { at ->
                Text(
                    stampTime(at, zone, locale),
                    style = TextStyle(fontFamily = ChoomHubTextStyles.Eyebrow.fontFamily, fontSize = 12.sp, letterSpacing = 0.05.sp),
                    color = StampOrange,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp).testTag("polaroid_stamp"),
                )
            }
        }
        // Two lines at most, so a long caption never spills out of the frame.
        Text(
            snap.caption.orEmpty(),
            style = ChoomHubTextStyles.Handwritten.copy(fontSize = 17.sp, lineHeight = 20.sp),
            color = FrameInk,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("polaroid_caption"),
        )
        Text(
            snap.sentAt?.let { frameDate(it, zone, locale) } ?: "",
            style = TextStyle(fontFamily = ChoomHubTextStyles.Eyebrow.fontFamily, fontSize = 11.sp, letterSpacing = 0.08.sp),
            color = FrameDate,
            modifier = Modifier.padding(top = 2.dp).testTag("polaroid_date"),
        )
    }
}

@Composable
private fun StringEnd(empty: Boolean, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Box(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 26.dp), contentAlignment = Alignment.Center) {
        if (empty) {
            Text(
                "Nothing hanging here yet.\nKeep a snap from its screen and it'll show up.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(ChoomHubTheme.colors.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            // A knot at the end of the twine.
            Box(Modifier.size(8.dp).background(Twine, MaterialTheme.shapes.extraLarge))
        }
    }
}

/** "9:47 PM": the orange stamp on the photo. */
internal fun stampTime(at: Instant, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("h:mm a", locale).format(at.atZone(zone)).uppercase(locale)

/** "24 nov 2026": typed on the frame. */
internal fun frameDate(at: Instant, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(at.atZone(zone)).lowercase(locale)

/**
 * Draws each collection's card behind its items (header, polaroids, end), plus the twine
 * down the middle when it's open. Drawn by the list rather than by each item so the
 * polaroids can overlap and the card and twine stay one piece as items come and go.
 */
private fun Modifier.collectionCards(state: LazyListState, gutter: Dp, surface: Color, lavender: Color): Modifier = drawBehind {
    val info = state.layoutInfo
    val shift = -info.viewportStartOffset.toFloat()
    val gap = CardGap.toPx()
    val radius = 20.dp.toPx()
    val left = gutter.toPx()
    val right = size.width - gutter.toPx()
    val groups = info.visibleItemsInfo.filter { CardKey.collectionOf(it.key) != null }.groupBy { CardKey.collectionOf(it.key)!! }
    groups.values.forEach { items ->
        val head = items.firstOrNull { CardKey.isHead(it.key) }
        val end = items.firstOrNull { CardKey.isEnd(it.key) }
        val hasString = items.any { !CardKey.isHead(it.key) }
        val top = head?.let { it.offset + shift + gap } ?: -radius
        val bottom = when {
            end != null -> end.offset + shift + end.size
            !hasString && head != null -> head.offset + shift + head.size
            else -> size.height + radius
        }
        if (bottom <= top) return@forEach
        val cardSize = Size(right - left, bottom - top)
        drawRoundRect(surface, Offset(left, top), cardSize, CornerRadius(radius))
        drawRoundRect(
            Brush.verticalGradient(
                listOf(lavender.copy(alpha = 0.16f), lavender.copy(alpha = 0.03f)),
                startY = top,
                endY = min(bottom, top + 1200f),
            ),
            Offset(left, top),
            cardSize,
            CornerRadius(radius),
            style = Stroke(1.dp.toPx()),
        )
        if (hasString) {
            val twineTop = head?.let { it.offset + shift + it.size - 8.dp.toPx() } ?: 0f
            val twineBottom = end?.let { it.offset + shift + 18.dp.toPx() } ?: size.height
            drawLine(Twine, Offset(size.width / 2f, twineTop), Offset(size.width / 2f, twineBottom), 1.5.dp.toPx())
        }
    }
}

private const val FOLD_MS = 260L
private const val STAGGER_IN_MS = 70L
private const val STAGGER_OUT_MS = 30L
