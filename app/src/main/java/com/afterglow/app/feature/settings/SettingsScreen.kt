package com.afterglow.app.feature.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afterglow.app.BuildConfig
import com.afterglow.app.R
import com.afterglow.app.core.designsystem.component.AfterglowCard
import com.afterglow.app.core.designsystem.component.CardTone
import com.afterglow.app.core.designsystem.component.PillBadge
import com.afterglow.app.core.designsystem.component.SectionHeader
import com.afterglow.app.core.designsystem.illustration.CrescentMoon
import com.afterglow.app.core.designsystem.theme.AfterglowTextStyles
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import com.afterglow.app.core.network.BackendConfig
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(contentPadding: PaddingValues) {
    composable<SettingsRoute> { SettingsScreen(contentPadding) }
}

/**
 * Settings shows the honest status of the app: what is connected, what is
 * not, and what is still to come. Real toggles arrive with their features.
 */
@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val spacing = AfterglowTheme.spacing
    val colors = AfterglowTheme.colors
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
                    Text("the quiet corner", style = AfterglowTextStyles.Handwritten, color = colors.pink)
                }
                CrescentMoon(Modifier.size(56.dp))
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
                SettingsRow("Motion", "Follows your system “remove animations” setting", divider = false)
            }

            SettingsGroup("About") {
                SettingsRow(stringResource(R.string.app_name), "Version ${BuildConfig.VERSION_NAME}")
                SettingsRow("Tagline", stringResource(R.string.app_tagline), divider = false)
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title = title)
        Spacer(Modifier.height(AfterglowTheme.spacing.sm))
        AfterglowCard(
            modifier = Modifier.fillMaxWidth(),
            tone = CardTone.Calm,
            contentPadding = PaddingValues(horizontal = AfterglowTheme.spacing.lg, vertical = AfterglowTheme.spacing.xs),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    badge: String? = null,
    badgeColor: Color = AfterglowTheme.colors.lavender,
    divider: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AfterglowTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AfterglowTheme.colors.textMain)
            Text(value, style = MaterialTheme.typography.bodySmall, color = AfterglowTheme.colors.textSecondary)
        }
        if (badge != null) PillBadge(badge, color = badgeColor)
    }
    if (divider) HorizontalDivider(color = AfterglowTheme.colors.outline.copy(alpha = 0.4f))
}
