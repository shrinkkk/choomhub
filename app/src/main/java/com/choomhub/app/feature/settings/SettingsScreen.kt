package com.choomhub.app.feature.settings

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.BuildConfig
import com.choomhub.app.R
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.SectionHeader
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.InviteCodes
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.CoupleNames
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(contentPadding: PaddingValues) {
    composable<SettingsRoute> { SettingsScreen(contentPadding) }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues, viewModel: SettingsViewModel = hiltViewModel()) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val space by viewModel.space.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsContent(
        names = names,
        space = space,
        contentPadding = contentPadding,
        onRenamePartner = viewModel::renamePartner,
        onShareCode = { code -> context.shareInviteCode(code) },
        onNewCode = viewModel::newInviteCode,
        onSignOut = viewModel::signOut,
    )
}

/**
 * Settings shows names (editable) and the honest status of the app: what is
 * connected, what is not, and what is still to come.
 */
@Composable
fun SettingsContent(
    names: CoupleNames,
    contentPadding: PaddingValues,
    onRenamePartner: (String) -> String?,
    space: SpaceState? = SpaceState.DeviceOnly,
    onShareCode: (String) -> Unit = {},
    onNewCode: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    var renaming by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().testTag("screen_settings"), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
                    Text("the quiet corner", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
                }
                GothicMotifArt(GothicMotif.Candle, Modifier.size(64.dp))
            }

            SettingsGroup("Names") {
                if (renaming) {
                    InlineRename(
                        current = names.partnerName.orEmpty(),
                        onSave = onRenamePartner,
                        onClose = { renaming = false },
                    )
                } else {
                    SettingsRow(
                        title = "You call them",
                        value = names.partnerName ?: "Not set",
                        badge = "edit",
                        onClick = { renaming = true },
                        modifier = Modifier.testTag("settings_partner_name"),
                    )
                }
                SettingsRow(
                    title = "They call you",
                    value = names.myName ?: "They pick this on their phone. It shows up once you're connected.",
                    divider = false,
                )
            }

            SettingsGroup("Your space") {
                when (space) {
                    is SpaceState.Waiting -> {
                        SettingsRow(
                            title = "Waiting for them to join",
                            value = "Invite code ${InviteCodes.display(space.inviteCode)} \u00B7 works once, for 48 hours",
                            badge = "share",
                            onClick = { onShareCode(space.inviteCode) },
                            modifier = Modifier.testTag("settings_invite"),
                        )
                        SettingsRow("New code", "If the old one expired or got lost", badge = "new", onClick = onNewCode)
                    }
                    is SpaceState.Paired -> SettingsRow("Shared space", "Connected. Everything here syncs between your phones.", badge = "synced", badgeColor = colors.leaf)
                    SpaceState.DeviceOnly, null -> SettingsRow(
                        "Shared space",
                        "Not set up in this build \u00B7 everything stays on this phone",
                        badge = "device",
                        badgeColor = colors.textSecondary,
                    )
                    else -> SettingsRow("Shared space", "Not connected yet", badge = "later")
                }
                val account = space?.accountOrNull
                if (account != null) {
                    SettingsRow(
                        title = "Signed in",
                        value = account.email ?: "Google account",
                        badge = "sign out",
                        badgeColor = colors.textSecondary,
                        onClick = onSignOut,
                        divider = false,
                    )
                }
            }

            SettingsGroup("Notifications") {
                SettingsRow("Categories", "Per-category controls arrive with push notifications", badge = "later", divider = false)
            }

            SettingsGroup("Appearance") {
                SettingsRow("Theme", "Always dark, by design")
                SettingsRow("Header art", "Changes every ${GothicMotif.DAYS_PER_MOTIF} days")
                SettingsRow("Motion", "Follows your system “remove animations” setting", divider = false)
            }

            SettingsGroup("About") {
                SettingsRow(stringResource(R.string.app_name), "Version ${BuildConfig.VERSION_NAME}")
                SettingsRow("Fonts", "Cormorant Garamond and Space Mono, SIL Open Font License", divider = false)
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }
}

/** Edits the partner's name in place, inside the Names card. */
@Composable
private fun InlineRename(
    current: String,
    onSave: (String) -> String?,
    onClose: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = ChoomHubTheme.colors
    val save = {
        error = onSave(input)
        if (error == null) onClose()
    }
    Column(Modifier.fillMaxWidth().padding(vertical = ChoomHubTheme.spacing.md)) {
        ChoomHubTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            label = "You call them",
            supportingText = error ?: "They'll see this as their name once you're connected.",
            isError = error != null,
            onDone = save,
            modifier = Modifier.fillMaxWidth().testTag("rename_field"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            TextButton(onClick = save, modifier = Modifier.testTag("rename_save")) {
                Text("SAVE", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
            }
        }
    }
    HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title = title)
        Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth(),
            tone = CardTone.Calm,
            contentPadding = PaddingValues(horizontal = ChoomHubTheme.spacing.lg, vertical = ChoomHubTheme.spacing.xs),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = ChoomHubTheme.colors.lavender,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = ChoomHubTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = ChoomHubTheme.colors.textMain)
            Text(value, style = MaterialTheme.typography.bodySmall, color = ChoomHubTheme.colors.textSecondary)
        }
        if (badge != null) {
            Spacer(Modifier.size(ChoomHubTheme.spacing.sm))
            PillBadge(badge, color = badgeColor)
        }
    }
    if (divider) HorizontalDivider(color = ChoomHubTheme.colors.outline.copy(alpha = 0.4f))
}

/** Opens the system share sheet with the invite code. */
private fun android.content.Context.shareInviteCode(code: String) {
    val text = "Join me on ChoomHub. Invite code: ${InviteCodes.display(code)} (works once, for 48 hours)"
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
    startActivity(android.content.Intent.createChooser(send, "Share invite code"))
}
