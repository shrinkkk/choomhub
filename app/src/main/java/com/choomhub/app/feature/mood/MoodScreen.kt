package com.choomhub.app.feature.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodRules
import com.choomhub.app.feature.home.component.moodColor
import kotlinx.serialization.Serializable

@Serializable
data object MoodRoute

fun NavGraphBuilder.moodScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    composable<MoodRoute> { MoodScreen(contentPadding, onClose) }
}

@Composable
fun MoodScreen(contentPadding: PaddingValues, onClose: () -> Unit, viewModel: MoodViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    MoodContent(
        state = state,
        contentPadding = contentPadding,
        onSelect = viewModel::select,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::save,
        onCancel = onClose,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodContent(
    state: MoodUiState,
    contentPadding: PaddingValues,
    onSelect: (MoodOption) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    Box(Modifier.fillMaxSize().testTag("screen_mood"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Text("Your mood", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text("they see it on their Home", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(spacing.xl))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                state.options.forEach { mood ->
                    MoodChip(mood, selected = state.selected?.key == mood.key, onClick = { onSelect(mood) })
                }
            }
            Spacer(Modifier.height(spacing.xl))
            ChoomHubTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = "What's going on (optional)",
                supportingText = "${state.note.length}/${MoodRules.MAX_NOTE}",
                onDone = onSave,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth().testTag("mood_note"),
            )
            if (state.error != null) {
                Spacer(Modifier.height(spacing.sm))
                Text(state.error, style = MaterialTheme.typography.bodySmall, color = colors.tomato)
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubPrimaryButton(text = "Save", onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("mood_save"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onCancel) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun MoodChip(mood: MoodOption, selected: Boolean, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val accent = moodColor(mood)
    val shape = MaterialTheme.shapes.small
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else colors.surface)
            .border(1.dp, if (selected) accent else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("mood_${mood.key}"),
    ) {
        Text(mood.label, style = MaterialTheme.typography.titleSmall, color = if (selected) accent else colors.textSecondary)
    }
}
