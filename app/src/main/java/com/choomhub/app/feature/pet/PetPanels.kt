package com.choomhub.app.feature.pet

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetOutcome
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.domain.model.TeamCare
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetBuild
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.math.hypot
import kotlin.random.Random

/** The hands-on things you can do with the cat, each in its own pop-up. */
internal enum class PetPanel { Feed, Water, Litter, Pet, Brush, Play, Care }

internal fun PetUi.build(): PetBuild = when {
    PetRules.isKitten(pet, now) -> PetBuild.Kitten
    PetRules.isChonky(pet) -> PetBuild.Chonky
    else -> PetBuild.Cat
}

internal fun PetUi.outfit(): PetOutfit = PetOutfit.fromId(pet.outfit)

internal fun PetUi.catName(): String = pet.name ?: "The cat"

@Composable
internal fun PetPanelHost(panel: PetPanel, ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    when (panel) {
        PetPanel.Feed -> FeedPanel(ui, onAct, onDismiss)
        PetPanel.Water -> WaterPanel(ui, onAct, onDismiss)
        PetPanel.Litter -> LitterPanel(ui, onAct, onDismiss)
        PetPanel.Pet -> PettingPanel(ui, onAct, onDismiss)
        PetPanel.Brush -> BrushPanel(ui, onAct, onDismiss)
        PetPanel.Play -> PlayPanel(ui, onAct, onDismiss)
        PetPanel.Care -> CarePanel(ui, onAct, onDismiss)
    }
}

@Composable
private fun PanelTitle(title: String, whisper: String? = null) {
    val colors = ChoomHubTheme.colors
    Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textMain, textAlign = TextAlign.Center)
    if (whisper != null) Text(whisper, style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
    Spacer(Modifier.height(ChoomHubTheme.spacing.md))
}

@Composable
private fun ResultLine(text: String?) {
    val colors = ChoomHubTheme.colors
    Text(
        text ?: " ",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.candle,
        textAlign = TextAlign.Center,
        minLines = 2,
        modifier = Modifier.fillMaxWidth().padding(vertical = ChoomHubTheme.spacing.sm).testTag("pet_panel_result"),
    )
}

@Composable
private fun DoneButton(onDismiss: () -> Unit, text: String = "Done") {
    Spacer(Modifier.height(ChoomHubTheme.spacing.md))
    ChoomHubSecondaryButton(text, onDismiss, Modifier.fillMaxWidth().testTag("pet_panel_done"))
}

/** Runs [onFrame] every frame with the seconds since the last one, while [running]. Skipped in tests. */
@Composable
private fun FrameLoop(running: Boolean, onFrame: (Float) -> Unit) {
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { t ->
                if (last > 0) onFrame(((t - last) / 1e9f).coerceAtMost(0.05f))
                last = t
            }
        }
    }
}

// --- Feed ---

@Composable
private fun FeedPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var eating by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(eating) {
        if (eating) {
            repeat(12) {
                delay(150)
                frame++
            }
            eating = false
        }
    }
    InkPanel(onDismiss, Modifier.testTag("pet_panel_feed")) {
        PanelTitle("Dinner", "what'll it be?")
        PetCat(
            if (eating) PetPose.Eat else PetPose.Sit,
            Modifier.size(140.dp),
            expression = petExpression(ui.pet, ui.now),
            build = ui.build(),
            outfit = if (eating) PetOutfit.None else ui.outfit(),
            frame = frame,
        )
        ResultLine(result)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Food.entries.forEach { food ->
                val count = if (food.free) null else ui.pet.inventory[food.key] ?: 0
                val enabled = food.free || (count ?: 0) > 0
                val note = when (food) {
                    ui.pet.favoriteFood -> "its favourite"
                    ui.pet.dislikedFood -> "not a fan"
                    else -> null
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, colors.lavender.copy(alpha = if (enabled) 0.25f else 0.08f), MaterialTheme.shapes.medium)
                        .clickable(enabled = enabled && !eating, role = Role.Button) {
                            val o = onAct(PetAction.Feed(food))
                            result = o.message
                            if (!o.patch.isEmpty()) eating = true
                        }
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                        .testTag("feed_${food.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(food.label, style = MaterialTheme.typography.titleSmall, color = if (enabled) colors.textMain else colors.textFaint)
                        if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = if (note == "not a fan") colors.tomato else colors.pink)
                    }
                    Text(
                        if (count == null) "always" else "× $count",
                        style = ChoomHubTextStyles.Eyebrow,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.xs))
        Text("More food in the shop.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
        DoneButton(onDismiss)
    }
}

// --- Water: hold to pour ---

@Composable
private fun WaterPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    var level by remember { mutableFloatStateOf(0f) }
    var pouring by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    fun pour(amount: Float) {
        if (result != null) return
        level = (level + amount).coerceAtMost(1f)
        if (level >= 1f) result = onAct(PetAction.Water).message
    }
    LaunchedEffect(pouring) {
        while (pouring && result == null) {
            delay(40)
            pour(0.03f)
        }
    }
    InkPanel(onDismiss, Modifier.testTag("pet_panel_water")) {
        PanelTitle("Fresh water", "old water out, new water in")
        Box(Modifier.size(width = 220.dp, height = 140.dp)) {
            PetCat(
                PetPose.Sit,
                Modifier.size(110.dp).align(Alignment.BottomStart),
                expression = if (level >= 1f) CatExpression.Happy else CatExpression.Hopeful,
                build = ui.build(),
                outfit = ui.outfit(),
            )
            Canvas(Modifier.size(width = 120.dp, height = 140.dp).align(Alignment.BottomEnd)) {
                val w = size.width
                val h = size.height
                // The jug's stream, while pouring.
                if (pouring && level < 1f) {
                    drawLine(colors.lavender.copy(alpha = 0.7f), Offset(w * 0.6f, 0f), Offset(w * 0.55f, h * 0.72f), 6f, StrokeCap.Round)
                }
                val bowlTop = h * 0.68f
                val bowl = Size(w * 0.8f, h * 0.3f)
                drawArc(Color(0xFF3B3349), 0f, 180f, true, Offset(w * 0.1f, bowlTop - bowl.height / 2f), bowl)
                if (level > 0f) {
                    val water = Size(bowl.width * 0.86f, bowl.height * 0.86f * level)
                    drawArc(Color(0xFF5E8A8C).copy(alpha = 0.9f), 0f, 180f, true, Offset(w * 0.5f - water.width / 2f, bowlTop - water.height / 2f + (1f - level) * bowl.height * 0.2f), water)
                }
                drawLine(Color(0xFF4A3F52), Offset(w * 0.1f, bowlTop), Offset(w * 0.9f, bowlTop), 3f)
            }
        }
        ResultLine(result ?: if (level > 0f && level < 1f) "${(level * 100).toInt()}%" else null)
        if (result == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (pouring) colors.lavender.copy(alpha = 0.25f) else colors.lavender.copy(alpha = 0.1f))
                    .border(1.dp, colors.lavender.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            // A tap is a splash; holding keeps pouring.
                            pour(0.06f)
                            pouring = true
                            tryAwaitRelease()
                            pouring = false
                        })
                    }
                    .testTag("water_pour"),
                contentAlignment = Alignment.Center,
            ) {
                Text("TAP OR HOLD TO POUR", style = MaterialTheme.typography.labelLarge, color = colors.textMain)
            }
        }
        DoneButton(onDismiss)
    }
}

// --- Litter: tap each clump to scoop it ---

@Composable
private fun LitterPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val dirt = 100f - PetRules.level(ui.pet, PetNeed.Litter, ui.now)
    val clumps = remember {
        val n = (dirt / 15f).toInt().coerceIn(1, 7)
        val r = Random(ui.now.epochSecond / 60)
        mutableStateListOf<Offset>().apply { repeat(n) { add(Offset(0.12f + r.nextFloat() * 0.76f, 0.2f + r.nextFloat() * 0.6f)) } }
    }
    var result by remember { mutableStateOf<String?>(null) }
    InkPanel(onDismiss, Modifier.testTag("pet_panel_litter")) {
        PanelTitle("The litter box", if (dirt < 15f) "pretty clean already" else "you know what to do")
        BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color(0xFF3B3349), Offset(0f, size.height * 0.08f), Size(size.width, size.height * 0.92f), CornerRadius(18f))
                drawRoundRect(Color(0xFF8C7A62), Offset(size.width * 0.04f, size.height * 0.14f), Size(size.width * 0.92f, size.height * 0.78f), CornerRadius(12f))
                val r = Random(4)
                repeat(140) { drawCircle(Color(0xFF9C8A72), 1.6f, Offset(size.width * (0.05f + r.nextFloat() * 0.9f), size.height * (0.16f + r.nextFloat() * 0.74f))) }
            }
            clumps.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .offset(w * c.x - 18.dp, h * c.y - 14.dp)
                        .size(36.dp, 28.dp)
                        .clickable(role = Role.Button, onClickLabel = "Scoop") {
                            clumps.removeAt(i)
                            if (clumps.isEmpty()) result = onAct(PetAction.CleanLitter).message
                        }
                        .testTag("litter_clump"),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(22.dp, 15.dp)) {
                        drawOval(Color(0xFF6B5A4E))
                        drawOval(Color(0xFF7D6A5C), Offset(size.width * 0.2f, size.height * 0.1f), Size(size.width * 0.45f, size.height * 0.4f))
                    }
                }
            }
        }
        ResultLine(result ?: if (clumps.isNotEmpty()) "${clumps.size} to go" else null)
        DoneButton(onDismiss)
    }
}

// --- Petting ---

@Composable
private fun PettingPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val density = LocalDensity.current
    var result by remember { mutableStateOf<String?>(null) }
    var purring by remember { mutableIntStateOf(0) }
    var bitten by remember { mutableIntStateOf(0) }
    val hearts = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(purring) {
        if (purring > 0) {
            delay(1_400)
            hearts.clear()
        }
    }
    LaunchedEffect(bitten) { if (bitten > 0) delay(900) }
    fun stroke(zone: Int, at: Offset?) {
        val o = onAct(PetAction.Pet(zone))
        result = o.message
        if ((o.patch.counters["bites"] ?: 0) > 0) {
            bitten++
        } else if (!o.patch.isEmpty()) {
            purring++
            if (at != null) hearts.add(at)
        }
    }
    val partnerHere = ui.pet.partnerPettingAt?.let { Duration.between(it, ui.now).abs() <= Duration.ofSeconds(20) } == true
    InkPanel(onDismiss, Modifier.testTag("pet_panel_pet")) {
        PanelTitle("Pets", "stroke it: head, back, or (brave) belly")
        if (partnerHere) {
            Text("${ui.partnerName} is petting ${ui.catName()} right now. Join in for a double purr.", style = MaterialTheme.typography.bodySmall, color = colors.candle, textAlign = TextAlign.Center)
            Spacer(Modifier.height(spacing.sm))
        }
        Box(
            Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    var start = Offset.Zero
                    var travelled = 0f
                    detectDragGestures(
                        onDragStart = {
                            start = it
                            travelled = 0f
                        },
                        onDrag = { change, drag -> travelled += hypot(drag.x, drag.y); change.consume() },
                        onDragEnd = {
                            if (travelled > with(density) { 40.dp.toPx() }) {
                                val f = start.y / size.height
                                stroke(if (f < 0.45f) 0 else if (f < 0.72f) 1 else 2, start)
                            }
                        },
                    )
                }
                .testTag("pet_petting_cat"),
        ) {
            PetCat(
                if (purring > 0 && bitten == 0) PetPose.Petted else PetPose.Sit,
                Modifier.fillMaxSize(),
                expression = if (bitten > 0) CatExpression.Excited else petExpression(ui.pet, ui.now),
                build = ui.build(),
                outfit = ui.outfit(),
            )
            Canvas(Modifier.fillMaxSize()) {
                hearts.forEach { h ->
                    val c = Offset(h.x, h.y - 30f)
                    drawCircle(colors.pink.copy(alpha = 0.8f), 9f, c + Offset(-6f, 0f))
                    drawCircle(colors.pink.copy(alpha = 0.8f), 9f, c + Offset(6f, 0f))
                    drawPath(
                        androidx.compose.ui.graphics.Path().apply {
                            moveTo(c.x - 14.5f, c.y + 3f)
                            lineTo(c.x, c.y + 18f)
                            lineTo(c.x + 14.5f, c.y + 3f)
                            close()
                        },
                        colors.pink.copy(alpha = 0.8f),
                    )
                }
                if (bitten > 0) {
                    // Chomp marks.
                    val c = Offset(size.width * 0.5f, size.height * 0.82f)
                    for (i in -1..1) drawLine(colors.tomato, c + Offset(i * 14f - 5f, -8f), c + Offset(i * 14f + 5f, 8f), 4f, StrokeCap.Round)
                }
            }
        }
        ResultLine(result)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf("Head", "Back", "Belly").forEachIndexed { i, label ->
                ChoomHubSecondaryButton(label, { stroke(i, null) }, Modifier.weight(1f).testTag("pet_zone_$i"))
            }
        }
        DoneButton(onDismiss)
    }
}

// --- Brushing ---

private data class Tuft(val start: Offset, val born: Long)

@Composable
private fun BrushPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val density = LocalDensity.current
    val goal = 14
    var tufts by remember { mutableIntStateOf(0) }
    val floating = remember { mutableStateListOf<Tuft>() }
    var tick by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(floating.size) {
        while (floating.isNotEmpty()) {
            delay(50)
            tick++
            if (floating.isNotEmpty() && tick - floating.first().born > 24) floating.removeAt(0)
        }
    }
    fun addTuft(at: Offset) {
        if (result != null) return
        tufts++
        floating.add(Tuft(at, tick.toLong()))
        if (tufts >= goal) result = onAct(PetAction.Brush).message
    }
    InkPanel(onDismiss, Modifier.testTag("pet_panel_brush")) {
        PanelTitle("Brushing", "long strokes along its back")
        Box(
            Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    var travelled = 0f
                    detectDragGestures(onDragStart = { travelled = 0f }) { change, drag ->
                        travelled += hypot(drag.x, drag.y)
                        if (travelled > with(density) { 26.dp.toPx() }) {
                            travelled = 0f
                            addTuft(change.position)
                        }
                        change.consume()
                    }
                }
                .testTag("pet_brush_cat"),
        ) {
            PetCat(
                if (tufts in 1 until goal) PetPose.Petted else PetPose.Sit,
                Modifier.fillMaxSize(),
                expression = if (result != null) CatExpression.Ecstatic else petExpression(ui.pet, ui.now),
                build = ui.build(),
                outfit = ui.outfit(),
            )
            Canvas(Modifier.fillMaxSize()) {
                floating.forEach { t ->
                    val age = (tick - t.born).coerceAtLeast(0) / 24f
                    val c = Offset(t.start.x + age * 30f, t.start.y - age * 70f)
                    val a = (1f - age).coerceIn(0f, 1f)
                    for (k in 0..4) {
                        val ang = k * 1.2f + t.born
                        drawLine(
                            Color(0xFF2A2430).copy(alpha = a),
                            c,
                            c + Offset(kotlin.math.cos(ang) * 9f, kotlin.math.sin(ang) * 9f),
                            2f,
                            StrokeCap.Round,
                        )
                    }
                }
                if (result != null) {
                    // Shiny now.
                    listOf(Offset(0.3f, 0.35f), Offset(0.68f, 0.5f), Offset(0.45f, 0.7f)).forEach { p ->
                        val c = Offset(size.width * p.x, size.height * p.y)
                        drawLine(colors.bone, c + Offset(-7f, 0f), c + Offset(7f, 0f), 2f, StrokeCap.Round)
                        drawLine(colors.bone, c + Offset(0f, -7f), c + Offset(0f, 7f), 2f, StrokeCap.Round)
                    }
                }
            }
        }
        // How far along.
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.surfaceHighest)) {
            Box(Modifier.fillMaxWidth((tufts.coerceAtMost(goal) / goal.toFloat())).height(6.dp).background(colors.lavender))
        }
        ResultLine(result)
        if (result == null) {
            ChoomHubSecondaryButton("A few quick strokes", { repeat(4) { addTuft(Offset(330f, 300f)) } }, Modifier.fillMaxWidth().testTag("brush_quick"))
        }
        DoneButton(onDismiss)
    }
}

// --- Play: drag the toy, the cat chases it ---

private const val PLAY_SECONDS = 15f

@Composable
private fun PlayPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val toys = buildList {
        add("feather")
        listOf("laser", "yarn", "mouse").filter { ui.pet.has(it) }.forEach { add(it) }
    }
    var toy by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    val energy = PetRules.level(ui.pet, PetNeed.Energy, ui.now)
    InkPanel(onDismiss, Modifier.testTag("pet_panel_play")) {
        PanelTitle("Playtime", if (toy == null) "pick a toy" else "keep it moving")
        val chosen = toy
        when {
            result != null -> {
                PetCat(PetPose.BellyUp, Modifier.size(150.dp), build = ui.build())
                ResultLine(result)
            }
            chosen == null -> {
                if (energy < 12f) {
                    Text("${ui.catName()} is too sleepy to play. Let it nap first.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        toys.forEach { t ->
                            ChoomHubSecondaryButton(PetRules.toyName(t).replaceFirstChar { it.uppercase() }, { toy = t }, Modifier.fillMaxWidth().testTag("play_$t"))
                        }
                    }
                    if (toys.size == 1) {
                        Spacer(Modifier.height(spacing.sm))
                        Text("The shop has a laser, yarn and a mouse.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                    }
                }
            }
            else -> PlayGame(ui, chosen) { catches -> result = onAct(PetAction.Play(chosen, catches)).message }
        }
        DoneButton(onDismiss)
    }
}

@Composable
private fun PlayGame(ui: PetUi, toy: String, onFinish: (Int) -> Unit) {
    val colors = ChoomHubTheme.colors
    val animate = LocalAmbientMotionEnabled.current
    var timeLeft by remember { mutableFloatStateOf(PLAY_SECONDS) }
    var catches by remember { mutableIntStateOf(0) }
    var toyAt by remember { mutableStateOf(Offset(0.75f, 0.35f)) }
    var catAt by remember { mutableStateOf(Offset(0.25f, 0.7f)) }
    var movedAgo by remember { mutableFloatStateOf(99f) }
    var holding by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(true) }
    var frame by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val lazy = PetTrait.Lazy in ui.pet.traits
    val playful = PetTrait.Playful in ui.pet.traits
    val speed = (if (ui.build() == PetBuild.Kitten) 0.75f else 0.6f) * (if (lazy) 0.75f else 1f) * (if (playful) 1.2f else 1f)
    fun finish() {
        if (finished) return
        finished = true
        onFinish(catches)
    }
    FrameLoop(animate && !finished) { dt ->
        timeLeft -= dt
        movedAgo += dt
        if (timeLeft <= 0f) {
            finish()
            return@FrameLoop
        }
        frame = ((PLAY_SECONDS - timeLeft) * 6).toInt()
        if (holding > 0f) {
            holding -= dt
            return@FrameLoop
        }
        // Interested only while the toy moves (laser: always, it's a laser).
        if (movedAgo < 0.8f || toy == "laser") {
            val d = toyAt - catAt
            val dist = hypot(d.x, d.y)
            if (dist < 0.08f && armed) {
                catches++
                holding = 0.7f
                armed = false
            } else if (dist > 0.001f) {
                val step = (speed * dt).coerceAtMost(dist)
                catAt += Offset(d.x / dist * step, d.y / dist * step)
            }
        }
        if (!armed && hypot(toyAt.x - catAt.x, toyAt.y - catAt.y) > 0.22f) armed = true
    }
    Text(
        "${timeLeft.coerceAtLeast(0f).toInt()}s · caught $catches",
        style = ChoomHubTextStyles.Eyebrow,
        color = colors.textSecondary,
        modifier = Modifier.testTag("play_score"),
    )
    Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.verticalGradient(listOf(Color(0xFF241D2C), Color(0xFF2A1F25))))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    toyAt = Offset(down.position.x / size.width, down.position.y / size.height)
                    movedAgo = 0f
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { c ->
                            toyAt = Offset((c.position.x / size.width).coerceIn(0f, 1f), (c.position.y / size.height).coerceIn(0f, 1f))
                            movedAgo = 0f
                            c.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("play_area"),
    ) {
        val w = maxWidth
        val h = maxHeight
        val catSize = 86.dp
        Canvas(Modifier.fillMaxSize()) {
            val t = Offset(toyAt.x * size.width, toyAt.y * size.height)
            when (toy) {
                "laser" -> {
                    drawCircle(Color(0xFFE0453A).copy(alpha = 0.25f), 16f, t)
                    drawCircle(Color(0xFFFF6A5C), 6f, t)
                }
                "yarn" -> {
                    drawCircle(Color(0xFFC4A3A8), 16f, t)
                    drawArc(Color(0xFF9C7F84), 200f, 140f, false, t - Offset(12f, 12f), Size(24f, 24f), style = Stroke(2f))
                    drawArc(Color(0xFF9C7F84), 20f, 120f, false, t - Offset(9f, 9f), Size(18f, 18f), style = Stroke(2f))
                }
                "mouse" -> {
                    drawOval(Color(0xFF57505E), t - Offset(16f, 9f), Size(32f, 18f))
                    drawCircle(Color(0xFFC4A3A8), 5f, t + Offset(12f, -8f))
                    drawLine(Color(0xFF57505E), t - Offset(16f, -2f), t - Offset(32f, -8f), 2f, StrokeCap.Round)
                }
                else -> {
                    // A feather on a string from a wand above.
                    drawLine(Color(0xFF8C6F4E), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.55f, 10f), 6f, StrokeCap.Round)
                    drawLine(colors.textFaint, Offset(size.width * 0.55f, 10f), t, 1.5f)
                    drawOval(Color(0xFF15111A), t - Offset(6f, 0f), Size(12f, 34f))
                    drawLine(colors.lavender.copy(alpha = 0.5f), t + Offset(0f, 2f), t + Offset(0f, 32f), 1.2f)
                }
            }
        }
        val d = toyAt - catAt
        val chasing = movedAgo < 0.8f || toy == "laser"
        val near = hypot(d.x, d.y) < 0.2f
        PetCat(
            when {
                holding > 0f -> PetPose.Pounce
                chasing && near -> PetPose.Pounce
                chasing -> PetPose.Walk
                else -> PetPose.Sit
            },
            Modifier
                .offset { IntOffset(((w * catAt.x - catSize / 2).toPx()).toInt(), ((h * catAt.y - catSize / 2).toPx()).toInt()) }
                .size(catSize),
            expression = if (holding > 0f) CatExpression.Ecstatic else CatExpression.Excited,
            build = ui.build(),
            frame = frame,
            facingLeft = d.x < 0f,
        )
        if (!animate) {
            // Without motion (and in tests) the cat can't run, so let a tap count as a catch.
            Box(Modifier.fillMaxSize().clickable(role = Role.Button) { catches++ })
        }
    }
    Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
    ChoomHubSecondaryButton("That's enough", { finish() }, Modifier.fillMaxWidth().testTag("play_stop"))
}

// --- Care: nails, medicine, the vet, bath ---

@Composable
private fun CarePanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val pet = ui.pet
    val name = ui.catName()
    var result by remember { mutableStateOf<String?>(null) }
    var trimming by remember { mutableStateOf(false) }
    InkPanel(onDismiss, Modifier.testTag("pet_panel_care")) {
        PanelTitle("Care", "the less fun bits")
        if (trimming) {
            NailTrim { done ->
                trimming = false
                if (done) result = onAct(PetAction.TrimNails).message
            }
            return@InkPanel
        }
        val weight = "%.1f kg".format(pet.weight)
        CareRow(
            "Weight",
            "$weight · " + when {
                PetRules.isChonky(pet) -> "chonky. More play, fewer treats."
                pet.weight > PetRules.BASE_WEIGHT + 0.8f -> "getting round"
                else -> "healthy"
            },
        )
        val nailsDue = pet.nailsAt == null || Duration.between(pet.nailsAt, ui.now) >= PetRules.NAILS_EVERY
        CareRow(
            "Nails",
            if (nailsDue) "Due for a trim." else "Next trim in ${Duration.between(ui.now, pet.nailsAt!!.plus(PetRules.NAILS_EVERY)).toDays() + 1} days.",
            action = if (nailsDue) "Trim" else null,
            tag = "care_nails",
        ) { trimming = true }
        val pending = pet.pending?.takeIf { PetRules.pendingOpen(it, ui.now) }
        if (pet.sick && !pet.vetDone) {
            TeamRow(TeamCare.Vet, "$name isn't well and needs the vet. It takes two: one carries, one drives.", pending, ui.partnerName) { result = onAct(it).message }
        }
        if (pet.sick && pet.vetDone) {
            val given = pet.lastMedicineDay == ui.now.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            CareRow(
                "Medicine",
                if (given) "Today's done. ${pet.medicineLeft} more day${if (pet.medicineLeft == 1) "" else "s"}." else "One a day, hidden in a treat. ${pet.medicineLeft} left.",
                action = if (given) null else "Give",
                tag = "care_medicine",
            ) { result = onAct(PetAction.GiveMedicine).message }
        }
        if (pet.muddy) {
            TeamRow(TeamCare.Bath, "$name is muddy. Bath time takes two: one holds, one washes.", pending, ui.partnerName) { result = onAct(it).message }
        }
        if (!pet.sick && !pet.muddy) {
            CareRow("Health", "Healthy and clean.")
        }
        ResultLine(result)
        DoneButton(onDismiss)
    }
}

@Composable
private fun CareRow(title: String, body: String, action: String? = null, tag: String? = null, onAction: () -> Unit = {}) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Row(Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.textMain)
        }
        if (action != null) {
            Spacer(Modifier.width(spacing.sm))
            ChoomHubPrimaryButton(action, onAction, if (tag != null) Modifier.testTag(tag) else Modifier)
        }
    }
}

@Composable
private fun TeamRow(kind: TeamCare, body: String, pending: com.choomhub.app.domain.model.PendingCare?, partner: String, onAct: (PetAction) -> Unit) {
    val mine = pending?.kind == kind && pending.byMe
    val theirs = pending?.kind == kind && !pending.byMe
    CareRow(
        if (kind == TeamCare.Vet) "The vet" else "Bath",
        when {
            mine -> "$body Waiting for $partner."
            theirs -> "$partner is ready. Your turn to help."
            else -> body
        },
        action = when {
            mine -> null
            theirs -> "Help"
            kind == TeamCare.Vet -> "Get the carrier"
            else -> "Get a towel"
        },
        tag = "care_${kind.key}",
    ) { onAct(if (theirs) PetAction.JoinTeamCare else PetAction.StartTeamCare(kind)) }
}

/** A paw with four claws; tap each to snip it. */
@Composable
private fun NailTrim(onEnd: (Boolean) -> Unit) {
    val colors = ChoomHubTheme.colors
    val snipped = remember { mutableStateListOf<Int>() }
    Text("Tap each claw. Quickly. It's tolerating this.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(ChoomHubTheme.spacing.md))
    BoxWithConstraints(Modifier.size(200.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawOval(Color(0xFF15111A), Offset(w * 0.18f, w * 0.42f), Size(w * 0.64f, w * 0.5f))
            drawOval(Color(0xFF6E3A44), Offset(w * 0.36f, w * 0.6f), Size(w * 0.28f, w * 0.2f))
            for (i in 0..3) {
                val x = w * (0.2f + i * 0.2f)
                val y = w * (if (i == 0 || i == 3) 0.4f else 0.3f)
                drawOval(Color(0xFF15111A), Offset(x - w * 0.08f, y), Size(w * 0.16f, w * 0.18f))
                drawOval(Color(0xFF6E3A44), Offset(x - w * 0.045f, y + w * 0.05f), Size(w * 0.09f, w * 0.08f))
                if (i !in snipped) {
                    drawLine(colors.bone, Offset(x, y + w * 0.01f), Offset(x + w * 0.015f, y - w * 0.09f), 5f, StrokeCap.Round)
                }
            }
        }
        for (i in 0..3) {
            Box(
                Modifier
                    .offset(maxWidth * (0.2f + i * 0.2f) - 22.dp, maxWidth * (if (i == 0 || i == 3) 0.32f else 0.22f) - 16.dp)
                    .size(44.dp)
                    .clickable(enabled = i !in snipped, role = Role.Button, onClickLabel = "Snip") {
                        snipped.add(i)
                        if (snipped.size == 4) onEnd(true)
                    }
                    .testTag("claw_$i"),
            )
        }
    }
    ChoomHubSecondaryButton("Give up", { onEnd(false) }, Modifier.fillMaxWidth())
}
