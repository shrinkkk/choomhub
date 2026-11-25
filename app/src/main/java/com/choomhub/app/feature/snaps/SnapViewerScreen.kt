package com.choomhub.app.feature.snaps

import android.text.format.DateFormat
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.SnapPhoto
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapRules
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
data class SnapViewerRoute(val id: String)

fun NavGraphBuilder.snapViewerScreen(contentPadding: PaddingValues) {
    composable<SnapViewerRoute> { SnapViewerScreen(contentPadding) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnapViewerScreen(contentPadding: PaddingValues, viewModel: SnapViewerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val image by viewModel.image.collectAsStateWithLifecycle()
    val newCollection by viewModel.newCollection.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    // Taking a snap out of its last collection means it can be replaced (and gone for good), so ask first.
    var confirmUnkeep by rememberSaveable { mutableStateOf<String?>(null) }
    confirmUnkeep?.let { collectionId ->
        InkDialog(
            title = "Stop keeping it?",
            message = "This was its last collection. Once it's not kept, the next snap from whoever sent it replaces it for good.",
            confirm = "Keep it",
            onConfirm = { confirmUnkeep = null },
            dismissive = "Remove it",
            onDismissive = {
                confirmUnkeep = null
                viewModel.toggle(collectionId)
            },
            onDismissRequest = { confirmUnkeep = null },
            modifier = Modifier.testTag("snap_unkeep_confirm"),
        ) { GothicMotifArt(GothicMotif.Moth, Modifier.size(88.dp)) }
    }
    Box(Modifier.fillMaxSize().testTag("screen_snap"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            val snap = state.snap
            if (snap == null) {
                Text(
                    if (state.gone) "This snap has been replaced" else "Opening…",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textMain,
                )
                if (state.gone) {
                    Text("Nobody kept it, so it went when a newer one arrived.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                return@Column
            }
            val who = if (snap.fromMe) "From you" else "From ${state.partnerName ?: "them"}"
            Text(who.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text(stamp(snap.sentAt), style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
            Spacer(Modifier.height(spacing.md))
            SnapPhoto(
                image,
                aspect = if (snap.width > 0 && snap.height > 0) snap.width.toFloat() / snap.height else 4f / 3f,
                contentDescription = snap.caption ?: who,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().testTag("snap_image"),
            )
            if (snap.caption != null) {
                Spacer(Modifier.height(spacing.md))
                Text(snap.caption, style = ChoomHubTextStyles.Handwritten, color = colors.textMain)
            }
            Spacer(Modifier.height(spacing.md))
            if (snap.fromMe) {
                Text(
                    snap.partnerReaction?.let { "${state.partnerName ?: "They"} reacted $it" } ?: "No reaction yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("snap_their_reaction"),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SnapRules.REACTIONS.forEach { r ->
                        val selected = snap.myReaction == r
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(if (selected) colors.pink.copy(alpha = 0.18f) else colors.surface)
                                .border(1.dp, if (selected) colors.pink else colors.outline, MaterialTheme.shapes.small)
                                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { viewModel.react(r) })
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("react_$r"),
                        ) { Text(r, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
            Spacer(Modifier.height(spacing.xl))
            Text("Keep it", style = MaterialTheme.typography.titleLarge, color = colors.textMain)
            Text(
                if (snap.isKept) "Kept in Memory Lane. It won't be replaced." else "Kept snaps go to Our World → Memory Lane and are never replaced.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.md))
            if (state.collections.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    state.collections.forEach { c ->
                        CollectionChip(
                            c,
                            selected = c.id in snap.collections,
                            onToggle = {
                                if (snap.collections == listOf(c.id)) confirmUnkeep = c.id else viewModel.toggle(c.id)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(spacing.md))
            }
            ChoomHubCard(tone = CardTone.Calm, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChoomHubTextField(
                        value = newCollection,
                        onValueChange = viewModel::onNewCollectionChange,
                        label = "New collection",
                        supportingText = "e.g. crows, rainy days, the first trip",
                        onDone = viewModel::createAndKeep,
                        modifier = Modifier.weight(1f).testTag("snap_new_collection"),
                    )
                    TextButton(onClick = viewModel::createAndKeep, modifier = Modifier.testTag("snap_new_collection_add")) {
                        Text("KEEP", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionChip(collection: SnapCollection, selected: Boolean, onToggle: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val shape = MaterialTheme.shapes.small
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) colors.pink.copy(alpha = 0.16f) else colors.surface)
            .border(1.dp, if (selected) colors.pink else colors.outline, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("collection_${collection.name}"),
    ) {
        Text(
            (if (selected) "✓ " else "") + collection.name,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.pink else colors.textSecondary,
        )
    }
}

@Composable
private fun stamp(at: Instant?): String {
    if (at == null) return "just now"
    val zone = ZoneId.systemDefault()
    val time = at.atZone(zone)
    val use24 = DateFormat.is24HourFormat(LocalContext.current)
    return TimeLabels.longDate(time) + " · " + TimeLabels.clockTime(time, use24)
}
