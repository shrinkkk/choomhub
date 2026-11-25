package com.choomhub.app.feature.mood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.feature.home.component.moodColor
import kotlinx.serialization.Serializable

@Serializable
data object MoodOptionsRoute

fun NavGraphBuilder.moodOptionsScreen(contentPadding: PaddingValues) {
    composable<MoodOptionsRoute> { MoodOptionsScreen(contentPadding) }
}

@Composable
fun MoodOptionsScreen(contentPadding: PaddingValues, viewModel: MoodOptionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MoodOptionsContent(
        state = state,
        contentPadding = contentPadding,
        onInputChange = viewModel::onInputChange,
        onAdd = viewModel::add,
        onRemove = viewModel::remove,
        onRestoreDefaults = viewModel::restoreDefaults,
    )
}

@Composable
fun MoodOptionsContent(
    state: MoodOptionsUiState,
    contentPadding: PaddingValues,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    // Restoring throws away every custom mood, so it asks first (as everything that undoes manual work does).
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    if (confirmRestore) {
        InkDialog(
            title = "Restore the defaults?",
            message = "Your list goes back to the 11 original moods. Moods you added will be gone from it, for both of you.",
            confirm = "Keep our list",
            onConfirm = { confirmRestore = false },
            dismissive = "Restore defaults",
            onDismissive = {
                confirmRestore = false
                onRestoreDefaults()
            },
            onDismissRequest = { confirmRestore = false },
            modifier = Modifier.testTag("mood_options_restore_confirm"),
        ) { GothicMotifArt(GothicMotif.Candle, Modifier.size(88.dp)) }
    }
    Box(Modifier.fillMaxSize().testTag("screen_mood_options"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Text("Mood list", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text("shared by both of you", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(spacing.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoomHubTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    label = "Add a mood",
                    supportingText = state.error ?: "Up to 24 characters",
                    isError = state.error != null,
                    onDone = onAdd,
                    modifier = Modifier.weight(1f).testTag("mood_option_input"),
                )
                TextButton(onClick = onAdd, modifier = Modifier.testTag("mood_option_add")) {
                    Text("ADD", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
                }
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubCard(
                modifier = Modifier.fillMaxWidth(),
                tone = CardTone.Calm,
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xs),
            ) {
                state.options.forEachIndexed { i, option ->
                    OptionRow(option, onRemove = { onRemove(option.key) })
                    if (i < state.options.lastIndex) HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(spacing.md))
            Text(
                "Changes show up on both phones. Removing a mood doesn't change anyone's current mood.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { confirmRestore = true }, modifier = Modifier.testTag("mood_options_defaults")) {
                    Text("RESTORE DEFAULTS", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(option: MoodOption, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ChoomHubTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(option.label, style = MaterialTheme.typography.titleSmall, color = moodColor(option), modifier = Modifier.weight(1f))
        PillBadge(
            text = "remove",
            color = ChoomHubTheme.colors.textSecondary,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onRemove)
                .testTag("mood_option_remove_${option.label}"),
        )
    }
}
