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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

private val PillPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)

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
        shape = CircleShape,
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
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PillPadding,
        border = BorderStroke(1.dp, ChoomHubTheme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChoomHubTheme.colors.lavender),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A round, squishy button holding a small illustration with a label below.
 * Used for affection gestures (hug, kiss, pats, tomato delivery) and other
 * playful one-tap actions.
 */
@Composable
fun AffectionButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bubbleSize: Dp = 64.dp,
    illustration: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "affectionPress",
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = ChoomHubTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChoomHubTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(bubbleSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.08f))))
                .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(bubbleSize * 0.55f), contentAlignment = Alignment.Center) { illustration() }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ChoomHubTheme.colors.textMain,
            textAlign = TextAlign.Center,
        )
    }
}
