package com.choomhub.app.feature.snaps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.SnapRules
import kotlinx.serialization.Serializable

@Serializable
data class SnapComposeRoute(val uri: String)

fun NavGraphBuilder.snapComposeScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    composable<SnapComposeRoute> { SnapComposeScreen(contentPadding, onClose) }
}

@Composable
fun SnapComposeScreen(contentPadding: PaddingValues, onClose: () -> Unit, viewModel: SnapComposeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    var picking by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (picking) {
        KeepPicker(
            collections = state.collections,
            keptIn = state.keepIn,
            newName = state.newCollection,
            onNewNameChange = viewModel::onNewCollectionChange,
            onCreate = viewModel::createAndKeep,
            onToggle = { viewModel.toggleKeep(it.id) },
            onDismiss = { picking = false },
        )
    }
    Box(Modifier.fillMaxSize().testTag("screen_snap_compose"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Text("Send a snap", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text("it replaces your last one, unless it was kept", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(spacing.lg))
            val preview = state.preview
            if (preview != null) {
                Image(
                    preview.asImageBitmap(),
                    contentDescription = "The photo you're sending",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(preview.width.toFloat() / preview.height),
                )
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(colors.nightDeep), contentAlignment = Alignment.Center) {
                    Text(if (state.loading) "developing…" else "no photo", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
                }
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubTextField(
                value = state.caption,
                onValueChange = viewModel::onCaptionChange,
                label = "Caption (optional)",
                supportingText = state.error ?: "${state.caption.length}/${SnapRules.MAX_CAPTION}",
                isError = state.error != null,
                onDone = viewModel::send,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth().testTag("snap_caption"),
            )
            if (state.keepIn.isNotEmpty()) {
                val names = state.collections.filter { it.id in state.keepIn }.joinToString(", ") { it.name }
                Text("Kept in $names as it's sent", style = MaterialTheme.typography.bodySmall, color = colors.candle, modifier = Modifier.testTag("snap_compose_kept"))
            }
            Spacer(Modifier.height(spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton(
                    text = if (state.keepIn.isEmpty()) "Keep it" else "Kept · ${state.keepIn.size}",
                    onClick = { picking = true },
                    enabled = !state.sending,
                    modifier = Modifier.weight(1f).testTag("snap_compose_keep"),
                )
                ChoomHubPrimaryButton(
                    text = if (state.sending) "Sending…" else "Send",
                    onClick = viewModel::send,
                    enabled = !state.sending && state.preview != null,
                    modifier = Modifier.weight(1f).testTag("snap_send"),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onClose) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            }
        }
    }
}
