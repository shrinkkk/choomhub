package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/**
 * The app's own pop-up: a dark card like the rest (moonlit hairline border,
 * film grain), a piece of ink art on top, a serif title, and a bone main
 * button with a quiet text button under it. [confirm] is the safe choice and
 * gets the bone button; [dismissive] is the one that does the thing.
 */
@Composable
fun InkDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    dismissive: String,
    onDismissive: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    whisper: String? = null,
    art: @Composable () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.widthIn(max = 360.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceElevated,
            border = BorderStroke(1.dp, Brush.verticalGradient(listOf(colors.lavender.copy(alpha = 0.22f), colors.lavender.copy(alpha = 0.04f)))),
        ) {
            Column(
                Modifier.filmGrain(0.05f).padding(horizontal = spacing.xl, vertical = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) { art() }
                Spacer(Modifier.height(spacing.md))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textMain, textAlign = TextAlign.Center)
                if (whisper != null) {
                    Text(whisper, style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(spacing.sm))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(spacing.xl))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ChoomHubPrimaryButton(text = confirm, onClick = onConfirm, modifier = Modifier.fillMaxWidth().testTag("dialog_confirm"))
                    TextButton(onClick = onDismissive, modifier = Modifier.fillMaxWidth().testTag("dialog_dismissive")) {
                        Text(dismissive.uppercase(), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
