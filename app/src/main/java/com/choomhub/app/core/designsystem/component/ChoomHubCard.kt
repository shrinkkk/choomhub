package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** Visual weight of a card. */
enum class CardTone {
    /** Quiet surface for secondary information. */
    Calm,

    /** Raised surface for the main content of a section. */
    Raised,

    /** Raised with a faint coloured glow along the border, for "something new". */
    Glowing,
}

/**
 * The standard ChoomHub card: a soft rounded surface with a hairline,
 * top-lit border that makes it feel like it is catching moonlight.
 */
@Composable
fun ChoomHubCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Raised,
    glowColor: Color = ChoomHubTheme.colors.purple,
    contentPadding: PaddingValues = PaddingValues(ChoomHubTheme.spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val container = when (tone) {
        CardTone.Calm -> colors.surface
        CardTone.Raised, CardTone.Glowing -> colors.surfaceElevated
    }
    val border = when (tone) {
        CardTone.Glowing -> Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.75f), glowColor.copy(alpha = 0.10f)))
        else -> Brush.verticalGradient(listOf(colors.lavender.copy(alpha = 0.16f), colors.lavender.copy(alpha = 0.03f)))
    }
    val shape = MaterialTheme.shapes.large
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = container,
            border = BorderStroke(1.dp, border),
            content = body,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
            border = BorderStroke(1.dp, border),
            content = body,
        )
    }
}
