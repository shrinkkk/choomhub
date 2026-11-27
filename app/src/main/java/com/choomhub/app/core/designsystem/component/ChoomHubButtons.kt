package com.choomhub.app.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubRadii
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

private val PillPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)

// For buttons sharing a row three or more abreast, so short labels stay on one line.
private val CompactPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
private val ButtonShape = RoundedCornerShape(ChoomHubRadii.Small)

/** Main call-to-action: a filled lavender-purple pill. */
@Composable
fun ChoomHubPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = PillPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        leading?.invoke(this)
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action: an outlined pill that sits quietly beside primaries. */
@Composable
fun ChoomHubSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = if (compact) CompactPadding else PillPadding,
        border = BorderStroke(1.dp, ChoomHubTheme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChoomHubTheme.colors.lavender),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = if (compact) 1 else Int.MAX_VALUE, softWrap = !compact)
    }
}

/**
 * A quiet square tile with a line icon and a mono label. Used for the gesture
 * buttons on Home (hug, kiss, head pats, tomato).
 */
@Composable
fun GestureButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "gesturePress",
    )
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (pressed) tint.copy(alpha = 0.12f) else tint.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = 0.22f), shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = ChoomHubTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChoomHubTheme.spacing.sm, Alignment.Top),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ChoomHubTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
