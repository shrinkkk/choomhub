package com.choomhub.app.feature.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choomhub.app.R
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.InviteCodes

@Composable
fun SignInScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SignInContent(state = state, onSignIn = { viewModel.signIn(context) })
}

@Composable
fun SignInContent(state: AccountUiState, onSignIn: () -> Unit) {
    AccountFrame(testTag = "screen_sign_in", motif = GothicMotif.Raven) {
        Text("A private space for two people. Sign in with your Google account to start.", style = MaterialTheme.typography.bodyMedium, color = ChoomHubTheme.colors.textSecondary)
        Spacer(Modifier.height(ChoomHubTheme.spacing.xl))
        ChoomHubPrimaryButton(
            text = if (state.busy) "Signing in…" else "Sign in with Google",
            onClick = onSignIn,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().testTag("sign_in_google"),
        )
        ErrorLine(state.error)
    }
}

@Composable
fun PairingScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PairingContent(
        state = state,
        onStart = viewModel::startSpace,
        onCodeChange = viewModel::onCodeChange,
        onJoin = viewModel::join,
        onSignOut = viewModel::signOut,
    )
}

@Composable
fun PairingContent(
    state: AccountUiState,
    onStart: () -> Unit,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    AccountFrame(testTag = "screen_pairing", motif = GothicMotif.SkeletonKey) {
        Text("ONE OF YOU", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.xs))
        Text("Start your space", style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
        Text(
            "You'll get a code to give the other person. You can use the app while you wait.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.md))
        ChoomHubPrimaryButton(
            text = "Start our space",
            onClick = onStart,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().testTag("pairing_start"),
        )

        Spacer(Modifier.height(spacing.xl))
        HorizontalDivider(color = colors.outline)
        Spacer(Modifier.height(spacing.xl))

        Text("THE OTHER ONE", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.xs))
        Text("Join with a code", style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
        Spacer(Modifier.height(spacing.md))
        ChoomHubTextField(
            value = InviteCodes.display(state.codeInput),
            onValueChange = onCodeChange,
            label = "Invite code",
            onDone = onJoin,
            modifier = Modifier.fillMaxWidth().testTag("pairing_code"),
        )
        Spacer(Modifier.height(spacing.md))
        ChoomHubSecondaryButton(
            text = "Join",
            onClick = onJoin,
            enabled = !state.busy && InviteCodes.isWellFormed(state.codeInput),
            modifier = Modifier.fillMaxWidth().testTag("pairing_join"),
        )
        ErrorLine(state.error)

        Spacer(Modifier.height(spacing.xl))
        var confirmingSignOut by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { confirmingSignOut = true }, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("pairing_sign_out")) {
            Text("SIGN OUT", style = MaterialTheme.typography.labelLarge, color = colors.textFaint)
        }
        if (confirmingSignOut) {
            SignOutDialog(onSignOut, { confirmingSignOut = false }, "You can sign back in any time and carry on from here.")
        }
    }
}

@Composable
private fun ErrorLine(error: String?) {
    if (error == null) return
    Spacer(Modifier.height(ChoomHubTheme.spacing.md))
    Text(error, style = MaterialTheme.typography.bodySmall, color = ChoomHubTheme.colors.tomato, modifier = Modifier.testTag("account_error"))
}

@Composable
private fun AccountFrame(testTag: String, motif: GothicMotif, content: @Composable ColumnScope.() -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Box(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().testTag(testTag),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.xl),
        ) {
            GothicMotifArt(motif, Modifier.size(104.dp))
            Spacer(Modifier.height(spacing.lg))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text(stringResource(R.string.app_tagline), style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(spacing.xxl))
            content()
        }
    }
}
