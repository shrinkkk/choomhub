package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    InkPanel(onDismissRequest, modifier) {
        InkPanelHeading(title, whisper, art)
        Spacer(Modifier.height(spacing.sm))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.xl))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ChoomHubPrimaryButton(text = confirm, onClick = onConfirm, modifier = Modifier.fillMaxWidth().testTag("dialog_confirm"))
            InkQuietButton(dismissive, onDismissive, Modifier.testTag("dialog_dismissive"))
        }
    }
}

/** The card every ink pop-up sits on: dark, moonlit hairline, film grain, centred content. */
@Composable
fun InkPanel(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        InkPanelCard(modifier, content)
    }
}

/** The ink panel's card on its own (what [InkPanel] shows in its pop-up); for previews. */
@Composable
fun InkPanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Surface(
        modifier = modifier.widthIn(max = 360.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(colors.lavender.copy(alpha = 0.22f), colors.lavender.copy(alpha = 0.04f)))),
    ) {
        Column(
            Modifier.filmGrain(0.05f).padding(horizontal = spacing.xl, vertical = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun InkPanelHeading(title: String, whisper: String?, art: @Composable () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Box(contentAlignment = Alignment.Center) { art() }
    Spacer(Modifier.height(spacing.md))
    Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textMain, textAlign = TextAlign.Center)
    if (whisper != null) {
        Text(whisper, style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InkQuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        OneLineText(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = ChoomHubTheme.colors.textSecondary)
    }
}

/** One choice in an [InkMenu]. [tag] is its test tag. */
class InkMenuOption(val label: String, val tag: String, val onClick: () -> Unit)

/** A few choices in the ink style (what a long-press opens), with a quiet "Never mind". */
@Composable
fun InkMenu(
    title: String,
    options: List<InkMenuOption>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    whisper: String? = null,
    art: @Composable () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    InkPanel(onDismissRequest, modifier) {
        InkPanelHeading(title, whisper, art)
        Spacer(Modifier.height(spacing.lg))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            options.forEach { option ->
                ChoomHubSecondaryButton(text = option.label, onClick = option.onClick, modifier = Modifier.fillMaxWidth().testTag(option.tag))
            }
            InkQuietButton("Never mind", onDismissRequest, Modifier.testTag("dialog_dismissive"))
        }
    }
}

/** Asks for a short name (a new collection, or renaming one). Empty names can't be saved. */
@Composable
fun InkNameDialog(
    title: String,
    initial: String,
    confirm: String,
    maxLength: Int,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Name",
    supportingText: String? = null,
    art: @Composable () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    var value by rememberSaveable { mutableStateOf(initial) }
    val clean = value.replace(Regex("\\s+"), " ").trim()
    val submit = { if (clean.isNotEmpty()) onConfirm(clean) }
    InkPanel(onDismissRequest, modifier) {
        InkPanelHeading(title, null, art)
        Spacer(Modifier.height(spacing.lg))
        ChoomHubTextField(
            value = value,
            onValueChange = { value = it.take(maxLength) },
            label = label,
            supportingText = supportingText ?: "${value.length}/$maxLength",
            onDone = submit,
            modifier = Modifier.fillMaxWidth().testTag("dialog_name"),
        )
        Spacer(Modifier.height(spacing.lg))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ChoomHubPrimaryButton(
                text = confirm,
                onClick = submit,
                enabled = clean.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().testTag("dialog_confirm"),
            )
            InkQuietButton("Never mind", onDismissRequest, Modifier.testTag("dialog_dismissive"))
        }
    }
}

/**
 * A confirmation for something that can't be undone lightly: [confirm] only works once [phrase]
 * has been typed out (case and extra spaces don't matter). [error] shows under the field.
 */
@Composable
fun InkTypeToConfirmDialog(
    title: String,
    message: String,
    phrase: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    error: String? = null,
    art: @Composable () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var value by rememberSaveable { mutableStateOf("") }
    val matches = typedPhraseMatches(value, phrase)
    InkPanel(onDismissRequest, modifier) {
        InkPanelHeading(title, null, art)
        Spacer(Modifier.height(spacing.sm))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.lg))
        Text("Type “$phrase” to confirm", style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.sm))
        ChoomHubTextField(
            value = value,
            onValueChange = { value = it.take(phrase.length + 20) },
            label = "Type it here",
            supportingText = error,
            isError = error != null,
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
            onDone = { if (matches && !busy) onConfirm() },
            modifier = Modifier.fillMaxWidth().testTag("dialog_phrase"),
        )
        Spacer(Modifier.height(spacing.lg))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ChoomHubPrimaryButton(
                text = if (busy) "Working on it…" else confirm,
                onClick = onConfirm,
                enabled = matches && !busy,
                modifier = Modifier.fillMaxWidth().testTag("dialog_confirm"),
            )
            InkQuietButton("Never mind", onDismissRequest, Modifier.testTag("dialog_dismissive"))
        }
    }
}

/** Whether [typed] is [phrase], ignoring case and spacing. */
fun typedPhraseMatches(typed: String, phrase: String): Boolean {
    fun norm(s: String) = s.replace(Regex("\\s+"), " ").trim().lowercase()
    return norm(typed).isNotEmpty() && norm(typed) == norm(phrase)
}
