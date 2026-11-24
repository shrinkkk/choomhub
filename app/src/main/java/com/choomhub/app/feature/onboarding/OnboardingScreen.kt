package com.choomhub.app.feature.onboarding

import androidx.compose.foundation.layout.Box
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

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onContinue = viewModel::onContinue,
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onInputChange: (String) -> Unit,
    onContinue: () -> Unit,
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
                supportingText = state.error ?: "${NameRules.clean(state.input).length}/${NameRules.MAX_LENGTH}",
                isError = state.error != null,
                onDone = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_field"),
            )
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
