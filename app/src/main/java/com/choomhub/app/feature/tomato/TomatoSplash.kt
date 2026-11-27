package com.choomhub.app.feature.tomato

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** How long the whole volley takes, from the first throw to the last of the mess fading. */
const val TOMATO_SPLASH_MS = 2_800

private val Ink = Color(0xFF15111A)
private val Pulp = Color(0xFFE06A5E)
private val Shine = Color(0xFFFFE9DC)

/**
 * Sits over the whole app. When they've thrown a tomato this phone hasn't shown yet, and the
 * app is in front of you, it lands: once, then it's marked seen.
 */
@Composable
fun TomatoSplashHost(viewModel: TomatoSplashViewModel = hiltViewModel()) {
    val volley by viewModel.volley.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    var showing by remember { mutableStateOf<TomatoVolley?>(null) }
    LaunchedEffect(volley) {
        val next = volley ?: return@LaunchedEffect
        if (showing == null) {
            showing = next
            viewModel.shown(next)
        }
    }
    showing?.let { v -> TomatoSplash(v.count, v.from, onDone = { showing = null }) }
}

/** One tomato's flight and the mess it makes, all fixed up front. */
private class Throw(r: Random, index: Int, count: Int, cell: Offset, cellSize: Size) {
    /** When it's thrown and when it hits, as fractions of the whole splash. */
    val launch = 0.02f + 0.36f * index / count.coerceAtLeast(1) + r.nextFloat() * 0.03f
    val impact = launch + 0.15f
    val startX = 0.1f + r.nextFloat() * 0.8f

    /** Somewhere in its own patch of the screen, so the mess covers all of it. */
    val target = Offset(cell.x + (0.2f + r.nextFloat() * 0.6f) * cellSize.width, cell.y + (0.2f + r.nextFloat() * 0.6f) * cellSize.height)
    val size = 0.15f + r.nextFloat() * 0.08f
    val spin = (r.nextFloat() - 0.5f) * 720f
    val tilt = r.nextFloat() * 360f

    /** The splat's outline: a wobbly ring with narrow spurts flung outwards. */
    val rim: List<Float> = List(32) { k ->
        val base = 0.68f + r.nextFloat() * 0.18f
        if (k % 4 == 1 && r.nextFloat() < 0.8f) base * (1.35f + r.nextFloat() * 0.55f) else base
    }
    val pulp: List<Float> = List(16) { 0.38f + r.nextFloat() * 0.18f }

    /** Flecks thrown clear of the splat: angle, distance, size (fractions of the splat). */
    val flecks = List(8 + r.nextInt(6)) { Triple(r.nextFloat() * 360f, 1.25f + r.nextFloat() * 0.9f, 0.03f + r.nextFloat() * 0.06f) }

    /** Seeds: where inside, which way they lie. */
    val seeds = List(7 + r.nextInt(5)) { Triple(r.nextFloat() * 360f, 0.1f + r.nextFloat() * 0.38f, r.nextFloat() * 180f) }

    /** Drips running down: across the bottom edge, how thick, how far they get. */
    val drips = List(2 + r.nextInt(2)) { Triple(-0.45f + r.nextFloat() * 0.9f, 0.07f + r.nextFloat() * 0.05f, 0.7f + r.nextFloat() * 1.4f) }

    /** The stem landed with it sometimes. */
    val stem = r.nextFloat() < 0.45f
}

/** [n] throws over a jittered grid of the screen (fractions), in a shuffled order. */
private fun volley(n: Int, seed: Int): List<Throw> {
    val r = Random(seed)
    val cols = 3
    val rows = (n + cols - 1) / cols
    val cells = (0 until rows * cols).shuffled(r).take(n)
    val cell = Size(1f / cols, 0.86f / rows)
    return cells.mapIndexed { i, c -> Throw(r, i, n, Offset((c % cols) * cell.width, 0.05f + (c / cols) * cell.height), cell) }
}

/**
 * Tomatoes thrown at the screen from below: they fly up, get bigger as they come, and burst
 * into splats with seeds and drips, then the mess fades. Tap to skip.
 */
@Composable
fun TomatoSplash(count: Int, from: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val motion = LocalAmbientMotionEnabled.current
    val haptics = LocalHapticFeedback.current
    val shown = count.coerceIn(1, TomatoRules.MAX_SHOWN)
    // A few tomatoes even for one: one tomato, delivered with feeling.
    val throws = remember(shown) { volley(maxOf(7, shown + 5), seed = 7) }
    // Without motion it's still the mess, just already there.
    val p = remember { Animatable(if (motion) 0f else 0.62f) }
    LaunchedEffect(Unit) {
        if (motion) {
            p.animateTo(throws.first().impact, tween((throws.first().impact * TOMATO_SPLASH_MS).toInt(), easing = LinearEasing))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val left = (1f - p.value) * if (motion) TOMATO_SPLASH_MS.toFloat() else TOMATO_SPLASH_MS * 0.65f
        p.animateTo(1f, tween(left.toInt(), easing = LinearEasing))
        onDone()
    }
    val t = p.value
    val fade = if (t < 0.8f) 1f else ((1f - t) / 0.2f).coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxSize()
            .alpha(fade)
            .pointerInput(Unit) { detectTapGestures { onDone() } }
            .testTag("tomato_splash"),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.28f * (t / 0.06f).coerceAtMost(1f)))
            val w = size.width
            throws.forEach { th ->
                if (t < th.launch) return@forEach
                val at = Offset(th.target.x * w, th.target.y * size.height)
                val radius = th.size * w
                if (t < th.impact) {
                    // In flight: up from below the screen on a slight arc, growing as it comes.
                    val f = (t - th.launch) / (th.impact - th.launch)
                    val start = Offset(th.startX * w, size.height * 1.08f)
                    val pos = start + (at - start) * f - Offset(0f, size.height * 0.1f * sin(PI.toFloat() * f))
                    tomato(pos, radius * (0.3f + 0.42f * f), th.tilt + th.spin * f, colors.tomato, colors.tomatoDeep, colors.leaf)
                } else {
                    splat(th, at, radius, t - th.impact, colors.tomato, colors.tomatoDeep, colors.leaf, colors.bone)
                }
            }
        }
        val firstHit = throws.minOf { it.impact }
        if (t > firstHit + 0.04f) {
            val words = if (count == 1) "$from delivered a tomato." else "$from delivered $count tomatoes."
            Text(
                words,
                style = ChoomHubTextStyles.Handwritten,
                color = colors.bone,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 24.dp, end = 24.dp)
                    .alpha(((t - firstHit - 0.04f) / 0.08f).coerceIn(0f, 1f))
                    .background(Color(0xCC15111A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("tomato_words"),
            )
        }
    }
}

/** A whole tomato: round and red, a shine, an ink outline and its green stem star on top. */
private fun DrawScope.tomato(at: Offset, r: Float, angle: Float, red: Color, deep: Color, leaf: Color) {
    rotate(angle, at) {
        drawCircle(Brush.radialGradient(listOf(Pulp, red, deep), center = at + Offset(-r * 0.3f, -r * 0.35f), radius = r * 1.5f), r, at)
        drawCircle(Ink.copy(alpha = 0.7f), r, at, style = Stroke(r * 0.06f))
        drawOval(Shine.copy(alpha = 0.35f), at + Offset(-r * 0.55f, -r * 0.6f), Size(r * 0.45f, r * 0.28f))
        val top = at - Offset(0f, r * 0.82f)
        repeat(5) { k ->
            val a = (-90f + k * 72f) * PI.toFloat() / 180f
            drawLine(leaf, top, top + Offset(cos(a), sin(a) * 0.6f) * r * 0.42f, r * 0.12f, StrokeCap.Round)
        }
        drawLine(leaf, top, top - Offset(-r * 0.08f, r * 0.3f), r * 0.1f, StrokeCap.Round)
    }
}

/** A closed, smooth blob through points around [c] at [radii] (fractions of [r]). */
private fun blob(c: Offset, r: Float, radii: List<Float>, turn: Float): Path {
    val pts = radii.mapIndexed { k, f ->
        val a = (turn + k * 360f / radii.size) * PI.toFloat() / 180f
        c + Offset(cos(a), sin(a)) * r * f
    }
    return Path().apply {
        val mid = { i: Int -> (pts[i % pts.size] + pts[(i + 1) % pts.size]) / 2f }
        moveTo(mid(0).x, mid(0).y)
        for (i in 1..pts.size) {
            val p = pts[i % pts.size]
            val m = mid(i)
            quadraticTo(p.x, p.y, m.x, m.y)
        }
        close()
    }
}

/** The mess one tomato makes, [since] (fraction of the splash) after it hit. */
private fun DrawScope.splat(th: Throw, c: Offset, r: Float, since: Float, red: Color, deep: Color, leaf: Color, bone: Color) {
    // It pops out a touch past full size and settles.
    val pop = when {
        since < 0.035f -> 0.4f + 0.75f * (since / 0.035f)
        since < 0.07f -> 1.15f - 0.15f * ((since - 0.035f) / 0.035f)
        else -> 1f
    }
    val s = r * pop
    // Drips first, so the splat sits over where they start.
    val run = ((since - 0.03f) / 0.42f).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) }
    th.drips.forEach { (x, width, length) ->
        val top = c + Offset(x * s * 1.1f, s * 0.55f)
        val bottom = top + Offset(0f, s * length * run)
        if (run > 0f) {
            drawLine(red, top, bottom, s * width, StrokeCap.Round)
            drawCircle(red, s * width * 0.75f, bottom + Offset(0f, s * width * 0.2f))
            drawLine(Ink.copy(alpha = 0.18f), top + Offset(s * width * 0.45f, 0f), bottom + Offset(s * width * 0.45f, 0f), s * 0.012f)
        }
    }
    th.flecks.forEach { (angle, dist, size) ->
        // Thrown out from the middle: a streak behind each fleck, pointing back at the hit.
        val a = angle * PI.toFloat() / 180f
        val dir = Offset(cos(a), sin(a))
        val at = c + dir * s * min(dist, dist * (since / 0.05f).coerceAtMost(1f))
        drawLine(red, at - dir * s * size * 2.6f, at, s * size * 1.1f, StrokeCap.Round)
        drawCircle(red, s * size, at)
    }
    val outline = blob(c, s, th.rim, th.tilt)
    drawPath(outline, Brush.radialGradient(listOf(red, red, deep), center = c, radius = s * 1.7f))
    drawPath(outline, Ink.copy(alpha = 0.4f), style = Stroke(s * 0.022f))
    // The softer flesh in the middle.
    drawPath(blob(c + Offset(-s * 0.05f, -s * 0.04f), s, th.pulp, th.tilt + 40f), Brush.radialGradient(listOf(Pulp, Pulp.copy(alpha = 0.6f)), center = c, radius = s * 0.6f))
    th.seeds.forEach { (angle, dist, lie) ->
        val a = angle * PI.toFloat() / 180f
        val at = c + Offset(cos(a), sin(a)) * s * dist
        withTransform({ rotate(lie, at) }) {
            drawOval(bone.copy(alpha = 0.85f), at - Offset(s * 0.045f, s * 0.026f), Size(s * 0.09f, s * 0.052f))
        }
    }
    // Wet shine.
    drawArc(Shine.copy(alpha = 0.22f), 200f, 70f, false, c - Offset(s * 0.6f, s * 0.6f), Size(s * 1.2f, s * 1.2f), style = Stroke(s * 0.05f, cap = StrokeCap.Round))
    if (th.stem) {
        val top = c + Offset(s * 0.12f, -s * 0.18f)
        repeat(5) { k ->
            val a = (th.tilt + k * 72f) * PI.toFloat() / 180f
            drawLine(leaf, top, top + Offset(cos(a), sin(a)) * s * 0.2f, s * 0.05f, StrokeCap.Round)
        }
    }
}
