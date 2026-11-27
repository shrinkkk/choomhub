package com.choomhub.app.feature.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetOutcome
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetBuild
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetPose
import com.choomhub.app.feature.pet.art.bubble
import com.choomhub.app.feature.pet.art.featherAt
import com.choomhub.app.feature.pet.art.toyArt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** A game's score is counted like catches; past this it stops adding. */
private const val MAX_SCORE = 10

private val Arena = Brush.verticalGradient(listOf(Color(0xFF241D2C), Color(0xFF2A1F25)))
private val Red = Color(0xFFFF6A5C)
private val Gold = Color(0xFFD9B45A)

// --- The panel: pick a toy, then its game ---

@Composable
internal fun PlayPanel(ui: PetUi, onAct: (PetAction) -> PetOutcome, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val toys = PetRules.TOYS.filter { it == "feather" || ui.pet.has(it) }
    var toy by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    val energy = PetRules.level(ui.pet, PetNeed.Energy, ui.now)
    InkPanel(onDismiss, Modifier.testTag("pet_panel_play")) {
        PanelTitle(if (toy == null) "Playtime" else gameTitle(toy!!), if (toy == null) "pick a toy" else gameHow(toy!!))
        val chosen = toy
        when {
            result != null -> {
                PetCat(PetPose.BellyUp, Modifier.size(150.dp), build = ui.build(), outfits = ui.look())
                ResultLine(result)
            }
            chosen == null -> {
                if (energy < 12f) {
                    Text("${ui.catName()} is too sleepy to play. Let it nap first.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
                } else {
                    ToyPicker(toys) { toy = it }
                    if (toys.size == 1) {
                        Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
                        Text("The shop has more toys, each with its own game.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                    }
                }
            }
            else -> Game(ui, chosen) { score -> result = onAct(PetAction.Play(chosen, score.coerceIn(0, MAX_SCORE))).message }
        }
        DoneButton(onDismiss)
    }
}

private fun gameTitle(toy: String) = when (toy) {
    "laser" -> "Red dot"
    "yarn" -> "Yarn ball"
    "mouse" -> "Toy mouse"
    "kicker" -> "Catnip kicker"
    "bubble_wand" -> "Bubbles"
    "box" -> "Box"
    else -> "Feather"
}

private fun gameHow(toy: String) = when (toy) {
    "laser" -> "draw a path; the dot runs it, the cat chases"
    "yarn" -> "tap the ball when it lands in the ring"
    "mouse" -> "tap the hole the mouse pops out of"
    "kicker" -> "tap as fast as you can"
    "bubble_wand" -> "tap a bubble to have it swiped"
    "box" -> "tap the hole the paw comes out of"
    else -> "pounce when the marker's in the gold"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToyPicker(toys: List<String>, onPick: (String) -> Unit) {
    val colors = ChoomHubTheme.colors
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        toys.forEach { t ->
            Column(
                Modifier
                    .width(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surface)
                    .border(1.dp, colors.lavender.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClickLabel = "Play with the ${PetRules.toyName(t)}") { onPick(t) }
                    .padding(6.dp)
                    .testTag("play_$t"),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(52.dp)) { toyArt(t, center, size.width / 46f) }
                Text(gameTitle(t).substringBefore(' ').uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            }
        }
    }
}

// --- What every game shares ---

/** Time, score and the end, for one round. */
private class Round(val seconds: Float, private val onFinish: (Int) -> Unit) {
    var timeLeft by mutableFloatStateOf(seconds)
    var score by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
    var clock by mutableFloatStateOf(0f)

    // The feather game's own bits.
    var tries by mutableIntStateOf(0)
    var pouncedAgo by mutableFloatStateOf(99f)
    var lastHit by mutableStateOf(false)

    fun tick(dt: Float): Boolean {
        if (finished) return false
        clock += dt
        timeLeft -= dt
        if (timeLeft <= 0f) {
            finish()
            return false
        }
        return true
    }

    fun finish() {
        if (finished) return
        finished = true
        onFinish(score)
    }
}

/** The cat, in the arena, at [at] (0..1 of the arena) and [size] tall. */
@Composable
private fun BoxWithConstraintsScope.ArenaCat(ui: PetUi, at: Offset, pose: PetPose, size: Dp = 86.dp, left: Boolean = false, expression: CatExpression = CatExpression.Excited, frame: Int = 0) {
    val w = maxWidth
    val h = maxHeight
    PetCat(
        pose,
        Modifier.offset { IntOffset(((w * at.x - size / 2).toPx()).toInt(), ((h * at.y - size / 2).toPx()).toInt()) }.size(size),
        expression = expression,
        build = ui.build(),
        frame = frame,
        facingLeft = left,
        outfits = ui.look(),
    )
}

@Composable
private fun Game(ui: PetUi, toy: String, onFinish: (Int) -> Unit) {
    val animate = LocalAmbientMotionEnabled.current
    val seconds = when (toy) {
        "kicker" -> 8f
        "feather" -> 30f
        else -> 15f
    }
    val round = remember(toy) { Round(seconds, onFinish) }
    FrameLoop(animate && !round.finished) { dt -> round.tick(dt) }
    val colors = ChoomHubTheme.colors
    Text(
        "${round.timeLeft.coerceAtLeast(0f).toInt()}s · ${scoreLine(toy, round.score)}",
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
            .background(Arena)
            .testTag("play_area"),
    ) {
        when (toy) {
            "laser" -> LaserGame(ui, round, animate)
            "yarn" -> YarnGame(ui, round, animate)
            "mouse" -> MouseGame(ui, round, animate)
            "kicker" -> KickerGame(ui, round, animate)
            "bubble_wand" -> BubbleGame(ui, round, animate)
            "box" -> BoxGame(ui, round, animate)
            else -> FeatherGame(ui, round, animate)
        }
        if (!animate) {
            // Without motion (and in tests) nothing moves, so a tap simply scores.
            Box(Modifier.fillMaxSize().clickable(role = Role.Button) { round.score++ })
        }
    }
    if (toy == "feather") {
        Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
        // The button lives with the game's state; see FeatherGame.
        FeatherControls(round)
    }
    Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
    ChoomHubSecondaryButton("That's enough", { round.finish() }, Modifier.fillMaxWidth().testTag("play_stop"))
}

private fun scoreLine(toy: String, score: Int) = when (toy) {
    "feather" -> "$score of 5 landed"
    "laser" -> "$score points"
    "yarn" -> "rally $score"
    "kicker" -> "$score kicks"
    "bubble_wand" -> "popped $score"
    "box" -> "boops $score"
    else -> "caught $score"
}

// --- Feather: a meter swings; pounce in the gold ---

/** Where the marker is (0..1): a swing that speeds up a little with each try. */
private fun Round.marker(): Float {
    val speed = 1.4f + tries * 0.25f
    return 0.5f + 0.48f * sin(clock * speed).toFloat()
}

@Composable
private fun BoxWithConstraintsScope.FeatherGame(ui: PetUi, round: Round, animate: Boolean) {
    val colors = ChoomHubTheme.colors
    val state = round
    val m = if (animate) round.marker() else 0.5f
    Canvas(Modifier.fillMaxSize()) {
        val anchor = Offset(size.width * 0.5f, 0f)
        drawLine(Color(0xFF8C6F4E), anchor, anchor + Offset(size.width * 0.06f, 12f), 6.dp.toPx(), StrokeCap.Round)
        val swing = (m - 0.5f) * 70f
        val tip = Offset(size.width * (0.5f + (m - 0.5f) * 0.5f), size.height * 0.32f)
        drawLine(colors.textFaint, anchor + Offset(size.width * 0.06f, 12f), tip, 1.dp.toPx())
        featherAt(tip, size.height * 0.5f, swing)
    }
    val pouncing = state.pouncedAgo < 0.6f
    ArenaCat(
        ui,
        if (pouncing && state.lastHit) Offset(0.5f, 0.62f) else Offset(0.22f, 0.72f),
        if (pouncing) PetPose.Pounce else PetPose.Sit,
        expression = if (pouncing && state.lastHit) CatExpression.Ecstatic else CatExpression.Excited,
    )
    FrameLoop(animate && !round.finished) { dt -> state.pouncedAgo += dt }
}

@Composable
private fun FeatherControls(round: Round) {
    val state = round
    val m = round.marker()
    Canvas(Modifier.fillMaxWidth().height(22.dp).testTag("feather_meter")) {
        drawRoundRect(Color(0xFF2A2430), Offset.Zero, size, CornerRadius(11.dp.toPx()))
        drawRoundRect(Gold.copy(alpha = 0.25f), Offset(size.width * 0.36f, 0f), Size(size.width * 0.28f, size.height), CornerRadius(4.dp.toPx()))
        drawRoundRect(Gold.copy(alpha = 0.6f), Offset(size.width * 0.44f, 0f), Size(size.width * 0.12f, size.height), CornerRadius(4.dp.toPx()))
        drawLine(Color(0xFFE6DED0), Offset(size.width * m, -2f), Offset(size.width * m, size.height + 2f), 3.dp.toPx(), StrokeCap.Round)
    }
    Spacer(Modifier.height(8.dp))
    ChoomHubPrimaryButton(
        "Pounce",
        {
            if (round.finished || state.tries >= 5) return@ChoomHubPrimaryButton
            val hit = abs(round.marker() - 0.5f) < 0.07f
            state.lastHit = hit
            state.pouncedAgo = 0f
            state.tries++
            // Landing in the gold counts double; a near miss still counts once.
            round.score += if (hit) 2 else if (abs(round.marker() - 0.5f) < 0.14f) 1 else 0
            if (state.tries >= 5) round.finish()
        },
        Modifier.fillMaxWidth().testTag("feather_pounce"),
    )
}

// --- Red dot: draw a path, the dot runs it, the cat chases ---

@Composable
private fun BoxWithConstraintsScope.LaserGame(ui: PetUi, round: Round, animate: Boolean) {
    val drawn = remember { mutableStateListOf<Offset>() }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var cat by remember { mutableStateOf(Offset(0.2f, 0.75f)) }
    var near by remember { mutableFloatStateOf(0f) }
    var total by remember { mutableFloatStateOf(0f) }
    var paths by remember { mutableIntStateOf(0) }
    val length = remember(drawn.size, running) { pathLength(drawn) }
    fun dotAt(t: Float): Offset = pointAlong(drawn, length * t)
    FrameLoop(animate && running && !round.finished) { dt ->
        progress += dt * 0.55f / (length.coerceAtLeast(0.4f))
        val dot = dotAt(progress.coerceAtMost(1f))
        val d = dot - cat
        val dist = hypot(d.x, d.y)
        val speed = 0.5f * (if (PetTrait.Lazy in ui.pet.traits) 0.8f else 1f)
        if (dist > 0.01f) cat += Offset(d.x / dist, d.y / dist) * (speed * dt).coerceAtMost(dist)
        total += dt
        if (dist < 0.16f) near += dt
        if (progress >= 1f) {
            running = false
            paths++
            // Kept up for most of it: up to 4 points a path; curvy paths (longer) score more.
            round.score += ((near / total.coerceAtLeast(0.01f)) * (1f + length.coerceAtMost(3f))).toInt().coerceAtMost(4)
            if (paths >= 3) round.finish()
        }
    }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(animate) {
                if (!animate) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (running) return@awaitEachGesture
                    drawn.clear()
                    drawn += Offset(down.position.x / size.width, down.position.y / size.height)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { c ->
                            drawn += Offset((c.position.x / size.width).coerceIn(0f, 1f), (c.position.y / size.height).coerceIn(0f, 1f))
                            c.consume()
                        }
                    } while (event.changes.any { it.pressed })
                    if (drawn.size > 4) {
                        progress = 0f
                        near = 0f
                        total = 0f
                        running = true
                    }
                }
            },
    ) {
        if (drawn.size > 1) {
            val p = Path().apply {
                moveTo(drawn[0].x * size.width, drawn[0].y * size.height)
                drawn.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
            }
            drawPath(p, Red.copy(alpha = 0.12f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(p, Red.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))))
        }
        val dot = if (running) dotAt(progress.coerceAtMost(1f)) else drawn.lastOrNull()
        if (dot != null) {
            val c = Offset(dot.x * size.width, dot.y * size.height)
            drawCircle(Red.copy(alpha = 0.25f), 16f, c)
            drawCircle(Red, 6f, c)
        }
    }
    if (!running && drawn.isEmpty()) {
        Text("Draw a path", style = ChoomHubTextStyles.Handwritten, color = ChoomHubTheme.colors.pink.copy(alpha = 0.7f), modifier = Modifier.padding(12.dp))
    }
    ArenaCat(ui, cat, if (running) PetPose.Walk else PetPose.Sit, left = running && dotAt(progress.coerceAtMost(1f)).x < cat.x, frame = (round.clock * 6).toInt())
}

private fun pathLength(points: List<Offset>): Float {
    var total = 0f
    for (i in 1 until points.size) total += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    return total
}

private fun pointAlong(points: List<Offset>, distance: Float): Offset {
    if (points.isEmpty()) return Offset(0.5f, 0.5f)
    var left = distance
    for (i in 1 until points.size) {
        val seg = hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        if (left <= seg && seg > 0f) return points[i - 1] + (points[i] - points[i - 1]) * (left / seg)
        left -= seg
    }
    return points.last()
}

// --- Yarn: a rally. It bats the ball to you; tap it in the ring ---

@Composable
private fun BoxWithConstraintsScope.YarnGame(ui: PetUi, round: Round, animate: Boolean) {
    val random = remember { Random(round.hashCode()) }
    var flight by remember { mutableFloatStateOf(0f) }
    var landX by remember { mutableFloatStateOf(0.25f) }
    var toCat by remember { mutableStateOf(false) }
    var missed by remember { mutableStateOf(false) }
    val speed = 0.7f + round.score * 0.08f
    val catAt = Offset(0.8f, 0.55f)
    fun ballAt(t: Float): Offset {
        val from = if (toCat) Offset(landX, 0.82f) else Offset(0.72f, 0.5f)
        val to = if (toCat) Offset(0.72f, 0.5f) else Offset(landX, 0.82f)
        val x = from.x + (to.x - from.x) * t
        val y = from.y + (to.y - from.y) * t - sin(PI * t).toFloat() * 0.5f
        return Offset(x, y)
    }
    FrameLoop(animate && !round.finished && !missed) { dt ->
        flight += dt * speed
        if (flight >= 1f) {
            if (toCat) {
                // The cat bats it straight back, somewhere new.
                toCat = false
                flight = 0f
                landX = 0.12f + random.nextFloat() * 0.45f
            } else if (flight > 1.25f) {
                missed = true
                round.finish()
            }
        }
    }
    val inRing = !toCat && flight in 0.8f..1.25f
    Canvas(
        Modifier.fillMaxSize().pointerInput(animate) {
            if (!animate) return@pointerInput
            detectTapGestures {
                if (!toCat && flight in 0.8f..1.25f) {
                    round.score++
                    toCat = true
                    flight = 0f
                }
            }
        },
    ) {
        val ring = Offset(landX * size.width, 0.82f * size.height)
        drawCircle(Gold.copy(alpha = if (inRing) 0.16f else 0.06f), 30.dp.toPx(), ring)
        drawCircle(Gold.copy(alpha = 0.6f), 30.dp.toPx(), ring, style = Stroke(2.dp.toPx()))
        val arc = Path()
        for (i in 0..24) {
            val p = ballAt(i / 24f)
            if (i == 0) arc.moveTo(p.x * size.width, p.y * size.height) else arc.lineTo(p.x * size.width, p.y * size.height)
        }
        drawPath(arc, Color(0xFFB7A6D6).copy(alpha = 0.25f), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))))
        // Past the ring it rolls on down and away.
        val ball = if (flight > 1f && !toCat) Offset(landX, 0.82f + (flight - 1f) * 0.3f) else ballAt(flight.coerceAtMost(1f))
        toyArt("yarn", Offset(ball.x * size.width, ball.y * size.height), 0.9.dp.toPx())
    }
    ArenaCat(ui, catAt, if (toCat && flight > 0.7f) PetPose.Pounce else PetPose.Sit, left = true, expression = if (missed) CatExpression.Bored else CatExpression.Excited)
}

// --- Mouse: it pops out of holes; tap the right one ---

@Composable
private fun BoxWithConstraintsScope.MouseGame(ui: PetUi, round: Round, animate: Boolean) {
    val random = remember { Random(round.hashCode()) }
    var hole by remember { mutableIntStateOf(2) }
    var shown by remember { mutableFloatStateOf(0f) }
    var hidden by remember { mutableFloatStateOf(0.6f) }
    var catX by remember { mutableFloatStateOf(0.5f) }
    var pounce by remember { mutableFloatStateOf(99f) }
    val window = (1.2f - round.score * 0.06f).coerceAtLeast(0.55f)
    FrameLoop(animate && !round.finished) { dt ->
        pounce += dt
        if (hidden > 0f) {
            hidden -= dt
            if (hidden <= 0f) {
                hole = random.nextInt(5)
                shown = window
            }
        } else {
            shown -= dt
            if (shown <= 0f) hidden = 0.4f + random.nextFloat() * 0.6f
        }
    }
    fun holeX(i: Int) = 0.1f + i * 0.2f
    Canvas(
        Modifier.fillMaxSize().pointerInput(animate) {
            if (!animate) return@pointerInput
            detectTapGestures { tap ->
                val i = ((tap.x / size.width - 0.0f) / 0.2f).toInt().coerceIn(0, 4)
                catX = holeX(i)
                pounce = 0f
                if (i == hole && hidden <= 0f && shown > 0f) {
                    round.score++
                    shown = 0f
                    hidden = 0.5f
                }
            }
        },
    ) {
        drawRect(Color(0xFF2E2433), Offset(0f, size.height * 0.55f), Size(size.width, size.height * 0.45f))
        drawRect(Color(0xFF4A3A3F), Offset(0f, size.height * 0.55f), Size(size.width, 5.dp.toPx()))
        for (i in 0..4) {
            val cx = size.width * holeX(i)
            val top = size.height * 0.7f
            val r = 16.dp.toPx()
            drawArc(Color(0xFF0C0A0E), 180f, 180f, true, Offset(cx - r, top), Size(r * 2, r * 2))
            drawRect(Color(0xFF0C0A0E), Offset(cx - r, top + r), Size(r * 2, size.height - top))
            if (i == hole && hidden <= 0f && shown > 0f) mousePeek(Offset(cx, top), r)
        }
    }
    ArenaCat(ui, Offset(catX, 0.3f), if (pounce < 0.4f) PetPose.Pounce else PetPose.Sit)
}

private fun DrawScope.mousePeek(top: Offset, r: Float) {
    val body = Color(0xFF57505E)
    val pink = Color(0xFFC4A3A8)
    drawCircle(body, r * 0.56f, top + Offset(0f, r * 1.4f))
    listOf(-1f, 1f).forEach { s ->
        drawCircle(body, r * 0.32f, top + Offset(s * r * 0.44f, r * 0.75f))
        drawCircle(pink, r * 0.19f, top + Offset(s * r * 0.44f, r * 0.75f))
        drawCircle(Color(0xFFE6DED0), r * 0.1f, top + Offset(s * r * 0.19f, r * 1.25f))
    }
    drawCircle(pink, r * 0.11f, top + Offset(0f, r * 1.56f))
}

// --- Catnip kicker: tap frenzy ---

@Composable
private fun BoxWithConstraintsScope.KickerGame(ui: PetUi, round: Round, animate: Boolean) {
    var taps by remember { mutableIntStateOf(0) }
    var kickAgo by remember { mutableFloatStateOf(99f) }
    FrameLoop(animate && !round.finished) { dt -> kickAgo += dt }
    // Every four taps is a kick that counts.
    Box(
        Modifier.fillMaxSize().pointerInput(animate) {
            if (!animate) return@pointerInput
            detectTapGestures {
                taps++
                kickAgo = 0f
                round.score = taps / 4
            }
        },
    )
    ArenaCat(ui, Offset(0.5f, 0.55f), PetPose.BellyUp, size = 130.dp, expression = if (kickAgo < 0.3f) CatExpression.Ecstatic else CatExpression.Excited)
    val wiggle = if (kickAgo < 0.25f) sin(kickAgo * 60f) * 10f else 0f
    Canvas(Modifier.fillMaxSize()) {
        rotate(wiggle, center) { toyArt("kicker", Offset(size.width * 0.5f, size.height * 0.62f), 1.6.dp.toPx()) }
        if (kickAgo < 0.25f) {
            listOf(0.28f to 0.75f, 0.24f to 0.6f, 0.72f to 0.8f).forEach { (x, y) ->
                drawLine(Gold.copy(alpha = 0.6f), Offset(size.width * x, size.height * y), Offset(size.width * (x + if (x < 0.5f) -0.06f else 0.06f), size.height * (y + 0.02f)), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        // The frenzy bar along the bottom.
        val fill = (taps / 40f).coerceAtMost(1f)
        drawRoundRect(Color(0xFF2A2430), Offset(12.dp.toPx(), size.height - 20.dp.toPx()), Size(size.width - 24.dp.toPx(), 10.dp.toPx()), CornerRadius(5.dp.toPx()))
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFF7D9C76), Gold, Color(0xFFC46A3A))),
            Offset(12.dp.toPx(), size.height - 20.dp.toPx()),
            Size((size.width - 24.dp.toPx()) * fill, 10.dp.toPx()),
            CornerRadius(5.dp.toPx()),
        )
    }
}

// --- Bubbles: they drift up; tap one to have it swiped ---

private class Bubble(var x: Float, var y: Float, val r: Float, val wobble: Float)

@Composable
private fun BoxWithConstraintsScope.BubbleGame(ui: PetUi, round: Round, animate: Boolean) {
    val random = remember { Random(round.hashCode()) }
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var spawn by remember { mutableFloatStateOf(0f) }
    var pops by remember { mutableStateOf(listOf<Pair<Offset, Float>>()) }
    var swipeX by remember { mutableFloatStateOf(0.3f) }
    var swipeAgo by remember { mutableFloatStateOf(99f) }
    var tick by remember { mutableIntStateOf(0) }
    FrameLoop(animate && !round.finished) { dt ->
        spawn -= dt
        swipeAgo += dt
        if (spawn <= 0f) {
            bubbles += Bubble(0.1f + random.nextFloat() * 0.8f, 1.1f, 0.03f + random.nextFloat() * 0.05f, random.nextFloat() * 6f)
            spawn = 0.5f + random.nextFloat() * 0.5f
        }
        bubbles.forEach { it.y -= dt * (0.18f + (0.08f - it.r)); it.x += sin(round.clock * 2f + it.wobble).toFloat() * dt * 0.05f }
        bubbles.removeAll { it.y < -0.1f }
        pops = pops.map { it.first to it.second + dt }.filter { it.second < 0.35f }
        tick++
    }
    Canvas(
        Modifier.fillMaxSize().pointerInput(animate) {
            if (!animate) return@pointerInput
            detectTapGestures { tap ->
                val p = Offset(tap.x / size.width, tap.y / size.height)
                val hit = bubbles.firstOrNull { hypot(it.x - p.x, (it.y - p.y) * size.height / size.width) < it.r + 0.04f }
                if (hit != null) {
                    bubbles.remove(hit)
                    // Little ones are worth more.
                    round.score += if (hit.r < 0.05f) 2 else 1
                    pops = pops + (Offset(hit.x, hit.y) to 0f)
                    swipeX = hit.x
                    swipeAgo = 0f
                }
            }
        },
    ) {
        // Reading the tick redraws every frame (the bubbles move without being state).
        if (tick < 0) return@Canvas
        bubbles.forEach { bubble(Offset(it.x * size.width, it.y * size.height), it.r * size.width) }
        pops.forEach { (at, age) ->
            val c = Offset(at.x * size.width, at.y * size.height)
            for (i in 0..7) {
                val a = i * PI / 4
                val d1 = (8f + age * 40f).dp.toPx()
                drawLine(Color(0xFFE6DED0).copy(alpha = 1f - age / 0.35f), c + Offset(cos(a).toFloat() * d1, sin(a).toFloat() * d1), c + Offset(cos(a).toFloat() * (d1 + 5.dp.toPx()), sin(a).toFloat() * (d1 + 5.dp.toPx())), 1.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
    ArenaCat(ui, Offset(swipeX, 0.78f), if (swipeAgo < 0.35f) PetPose.Pounce else PetPose.Sit)
}

// --- Box: peekaboo. A paw shoots out of a hole; tap that hole ---

@Composable
private fun BoxWithConstraintsScope.BoxGame(ui: PetUi, round: Round, animate: Boolean) {
    val random = remember { Random(round.hashCode()) }
    var hole by remember { mutableIntStateOf(1) }
    var out by remember { mutableFloatStateOf(0f) }
    var wait by remember { mutableFloatStateOf(0.8f) }
    var mouseHole by remember { mutableIntStateOf(1) }
    var booped by remember { mutableFloatStateOf(99f) }
    val window = (1.1f - round.score * 0.05f).coerceAtLeast(0.5f)
    FrameLoop(animate && !round.finished) { dt ->
        booped += dt
        if (wait > 0f) {
            wait -= dt
            if (wait <= 0f) {
                hole = random.nextInt(3)
                out = window
            }
        } else {
            out -= dt
            if (out <= 0f) wait = 0.5f + random.nextFloat() * 0.7f
        }
    }
    fun holeX(i: Int) = 0.27f + i * 0.2f
    Canvas(
        Modifier.fillMaxSize().pointerInput(animate) {
            if (!animate) return@pointerInput
            detectTapGestures { tap ->
                val i = (((tap.x / size.width) - 0.17f) / 0.2f).toInt().coerceIn(0, 2)
                mouseHole = i
                if (i == hole && wait <= 0f && out > 0f) {
                    round.score++
                    booped = 0f
                    out = 0f
                    wait = 0.4f
                }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val left = w * 0.17f
        val top = h * 0.4f
        drawRect(Color(0xFFB08A5E), Offset(left, top), Size(w * 0.66f, h * 0.5f))
        drawRect(Color(0xFF8A6A45), Offset(left, top), Size(w * 0.66f, h * 0.05f))
        drawLine(Color(0xFFD9C29A), Offset(w * 0.5f, top), Offset(w * 0.5f, top + h * 0.5f), 5.dp.toPx())
        for (i in 0..2) {
            val c = Offset(w * holeX(i), top + h * 0.27f)
            drawCircle(Color(0xFF0C0A0E), 14.dp.toPx(), c)
            if (i == hole && wait <= 0f && out > 0f) {
                val reach = c + Offset(10.dp.toPx(), -26.dp.toPx())
                drawLine(Color(0xFF15111A), c, reach, 12.dp.toPx(), StrokeCap.Round)
                drawCircle(Color(0xFF15111A), 9.dp.toPx(), reach)
                drawOval(Color(0xFFC4A3A8), reach + Offset(-4.dp.toPx(), -1.dp.toPx()), Size(8.dp.toPx(), 6.dp.toPx()))
            } else if (i == 1 && out <= 0f) {
                drawCircle(Color(0xFFE6D6AE), 2.dp.toPx(), c + Offset(-4.dp.toPx(), -1.dp.toPx()))
                drawCircle(Color(0xFFE6D6AE), 2.dp.toPx(), c + Offset(4.dp.toPx(), -1.dp.toPx()))
            }
        }
        // Your mouse on a string, where you last dangled it.
        val m = Offset(w * holeX(mouseHole) + 18.dp.toPx(), top - 18.dp.toPx() - (if (booped < 0.3f) sin(booped * 30f) * 6f else 0f))
        drawLine(Color(0xFF8C8794), Offset(m.x, 0f), m, 1.dp.toPx())
        toyArt("mouse", m + Offset(0f, 6.dp.toPx()), 0.7.dp.toPx())
    }
}
