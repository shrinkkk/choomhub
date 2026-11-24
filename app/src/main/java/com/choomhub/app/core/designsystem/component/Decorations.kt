package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.illustration.StarField
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/**
 * Full-screen backdrop: a deep night gradient with a faint purple dusk at the
 * bottom and a sparse, twinkling star field near the top.
 */
@Composable
fun NightSkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ChoomHubTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.nightDeep,
                    0.45f to colors.night,
                    1f to Color(0xFF1C1626),
                ),
            ),
    ) {
        StarField(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.45f),
            starCount = 55,
        )
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
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
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
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

/**
 * Round avatar placeholder: a soft gradient disc with the person's initial,
 * ringed in their accent colour. Real photos replace this later.
 */
@Composable
fun AvatarPlaceholder(
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    accent: Color = ChoomHubTheme.colors.pink,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), ChoomHubTheme.colors.surfaceHighest)))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.take(1).uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = ChoomHubTheme.colors.textMain,
        )
    }
}
