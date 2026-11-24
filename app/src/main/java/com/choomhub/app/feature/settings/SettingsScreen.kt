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
import com.choomhub.app.core.network.BackendConfig
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
    SettingsContent(names = names, contentPadding = contentPadding, onRenamePartner = viewModel::renamePartner)
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
                SettingsRow("Shared space", "Not connected yet", badge = "later")
                SettingsRow(
                    title = "Backend",
                    value = if (BackendConfig.isConfigured) {
                        "Configured, not used yet"
                    } else {
                        "Not configured · showing sample data"
                    },
                    badge = if (BackendConfig.isConfigured) "ready" else "sample",
                    badgeColor = if (BackendConfig.isConfigured) colors.leaf else colors.textSecondary,
                    divider = false,
                )
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
