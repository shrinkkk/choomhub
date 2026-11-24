package com.afterglow.app.core.designsystem.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import com.afterglow.app.core.designsystem.theme.LocalAmbientMotionEnabled
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** A crescent moon with an optional soft halo. */
@Composable
fun CrescentMoon(
    modifier: Modifier = Modifier,
    color: Color = AfterglowTheme.colors.lavender,
    glow: Boolean = true,
) {
    Canvas(modifier) { drawCrescentMoon(center, size.minDimension / 2f, color, glow) }
}

internal fun DrawScope.drawCrescentMoon(
    center: Offset,
    outerRadius: Float,
    color: Color,
    glow: Boolean = true,
) {
    if (glow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = outerRadius,
            ),
            radius = outerRadius,
            center = center,
        )
    }
    val r = outerRadius * 0.62f
    val disc = Path().apply { addOval(Rect(center, r)) }
    val bite = Path().apply {
        addOval(Rect(Offset(center.x + r * 0.52f, center.y - r * 0.38f), r * 0.86f))
    }
    val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
    drawPath(crescent, color)
}

/** Four-pointed sparkle, used for stars and little moments of delight. */
@Composable
fun Sparkle(
    modifier: Modifier = Modifier,
    color: Color = AfterglowTheme.colors.pink,
) {
    Canvas(modifier) { drawSparkle(center, size.minDimension / 2f, color) }
}

internal fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val (cx, cy) = center
    val path = Path().apply {
        moveTo(cx, cy - radius)
        quadraticTo(cx, cy, cx + radius, cy)
        quadraticTo(cx, cy, cx, cy + radius)
        quadraticTo(cx, cy, cx - radius, cy)
        quadraticTo(cx, cy, cx, cy - radius)
        close()
    }
    drawPath(path, color)
}

@Immutable
private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float, val phase: Float)

/**
 * A field of softly twinkling stars that fills its bounds. Positions are
 * seeded so the sky looks the same on every launch.
 */
@Composable
fun StarField(
    modifier: Modifier = Modifier,
    starCount: Int = 70,
    seed: Int = 1123,
    color: Color = AfterglowTheme.colors.lavender,
) {
    val stars = remember(starCount, seed) {
        val random = Random(seed)
        List(starCount) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.3f,
                alpha = 0.15f + random.nextFloat() * 0.55f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val twinkle = twinklePhase()
    Canvas(modifier) {
        val t = twinkle.value
        stars.forEach { star ->
            val shimmer = 0.65f + 0.35f * sin(t + star.phase)
            drawCircle(
                color = color.copy(alpha = (star.alpha * shimmer).coerceIn(0f, 1f)),
                radius = star.radius * density,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

@Composable
private fun twinklePhase(): State<Float> {
    if (!LocalAmbientMotionEnabled.current) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "twinkle")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinklePhase",
    )
}

/** Diagonal rain streaks inside [bounds]. Deterministic for a given seed. */
internal fun DrawScope.drawRain(
    bounds: Rect,
    color: Color,
    drops: Int = 26,
    seed: Int = 42,
) {
    val random = Random(seed)
    repeat(drops) {
        val x = bounds.left + random.nextFloat() * bounds.width
        val y = bounds.top + random.nextFloat() * bounds.height
        val length = bounds.height * (0.05f + random.nextFloat() * 0.08f)
        val end = Offset(x - length * 0.18f, (y + length).coerceAtMost(bounds.bottom))
        drawLine(
            color = color.copy(alpha = color.alpha * (0.4f + random.nextFloat() * 0.6f)),
            start = Offset(x, y),
            end = end,
            strokeWidth = 1.2f * density,
            cap = StrokeCap.Round,
        )
    }
}
