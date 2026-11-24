package com.choomhub.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choomhub.app.R
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.NameRules
import com.choomhub.app.domain.model.PointsRole

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onContinue = viewModel::onContinue,
        onRoleSelected = viewModel::onRoleSelected,
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onInputChange: (String) -> Unit,
    onContinue: () -> Unit,
    onRoleSelected: (PointsRole) -> Unit = {},
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .testTag("screen_onboarding"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.xl),
        ) {
            GothicMotifArt(GothicMotif.Raven, Modifier.size(120.dp))
            Spacer(Modifier.height(spacing.lg))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text(stringResource(R.string.app_tagline), style = ChoomHubTextStyles.Handwritten, color = colors.pink)

            Spacer(Modifier.height(spacing.xxxl))
            Text("FIRST THING", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Spacer(Modifier.height(spacing.xs))
            Text("What do you call them?", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "This is the name they go by in ChoomHub, on both phones. " +
                    "They choose yours on their side. You can change it any time in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.xl))
            ChoomHubTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = "Their name",
                supportingText = "${NameRules.clean(state.input).length}/${NameRules.MAX_LENGTH}",
                onDone = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_field"),
            )
            Spacer(Modifier.height(spacing.xl))
            Text("POINTS", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Spacer(Modifier.height(spacing.xs))
            if (state.lockedRole != null) {
                Text("You're the ${state.lockedRole.label}.", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
                Text(
                    "They picked ${state.lockedRole.other.label}. You give them their points; they give you yours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            } else {
                Text("Which are you?", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
                Text(
                    "You can only give points to them, and they give you yours. Whoever picks first decides for both.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PointsRole.entries.forEach { role ->
                        RoleChoice(
                            role = role,
                            selected = state.selectedRole == role,
                            onClick = { onRoleSelected(role) },
                            modifier = Modifier.weight(1f).testTag("role_${role.name}"),
                        )
                    }
                }
            }
            if (state.error != null) {
                Spacer(Modifier.height(spacing.md))
                Text(state.error, style = MaterialTheme.typography.bodySmall, color = colors.tomato, modifier = Modifier.testTag("onboarding_error"))
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_continue"),
            )
        }
    }
}

@Composable
private fun RoleChoice(role: PointsRole, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val accent = if (role == PointsRole.GoodBoy) colors.lavender else colors.pink
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else colors.surface)
            .border(1.dp, if (selected) accent else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = ChoomHubTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(role.label, style = MaterialTheme.typography.titleLarge, color = if (selected) accent else colors.textSecondary)
    }
}
