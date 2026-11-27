package com.choomhub.app.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import com.choomhub.app.core.designsystem.illustration.StarField
import com.choomhub.app.core.designsystem.theme.skyColorsFor
import com.choomhub.app.core.designsystem.theme.ChoomHubRadii
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/**
 * Full-screen backdrop that follows the time of day ([ChoomHubTheme.sky]):
 * a dark gradient, stars at night (fading in at dusk and out at dawn) and a
 * fine film grain. Home adds its own scene (branch, moon or sun) on top.
 */
@Composable
fun SkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val phase = ChoomHubTheme.sky
    val sky = skyColorsFor(phase)
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to sky.top, 0.5f to sky.middle, 1f to sky.bottom))
            .filmGrain(),
    ) {
        if (sky.starAlpha > 0f) {
            StarField(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.45f)
                    .alpha(sky.starAlpha),
                starCount = 34,
            )
        }
        content()
    }
}

/** Section title with an optional small eyebrow line above it. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = ChoomHubTextStyles.Eyebrow,
                    color = ChoomHubTheme.colors.textSecondary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ChoomHubTheme.colors.textMain,
                modifier = Modifier.semantics { heading() },
            )
        }
        trailing?.invoke()
    }
}

/** Small rounded label, e.g. "new", "sample data", "coming soon". */
@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ChoomHubTheme.colors.pink,
    showDot: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(ChoomHubRadii.Small / 2))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(ChoomHubRadii.Small / 2))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Avatar placeholder: a dark rounded square with the person's initial in the
 * display serif and a thin accent border. Real photos replace this later.
 */
@Composable
fun AvatarPlaceholder(
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    accent: Color = ChoomHubTheme.colors.pink,
) {
    val shape = RoundedCornerShape(ChoomHubRadii.Medium)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(ChoomHubTheme.colors.surfaceHighest)
            .border(1.dp, accent.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.trim().take(1).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
        )
    }
}

/**
 * Overlays a fine, static film grain. Generated once per process and tiled,
 * so it costs one small bitmap.
 */
fun Modifier.filmGrain(alpha: Float = 0.07f): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(GrainTexture.image, TileMode.Repeated, TileMode.Repeated))
    onDrawWithContent {
        drawContent()
        drawRect(brush, alpha = alpha)
    }
}

private object GrainTexture {
    val image: ImageBitmap by lazy {
        val side = 160
        val random = Random(97)
        val pixels = IntArray(side * side) {
            val v = random.nextInt(256)
            val a = random.nextInt(140)
            (a shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
