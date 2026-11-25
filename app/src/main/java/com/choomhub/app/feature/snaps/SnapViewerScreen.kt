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
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.CatFace
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
    var picking by rememberSaveable { mutableStateOf(false) }
    // Taking a snap out of its last collection can delete it for good, so ask first.
    var confirmUnkeep by rememberSaveable { mutableStateOf<String?>(null) }
    confirmUnkeep?.let { collectionId ->
        val forGood = viewModel.deletesForGood(collectionId)
        InkDialog(
            title = if (forGood) "Delete it for good?" else "Stop keeping it?",
            whisper = if (forGood) "this one can't be undone" else null,
            message = if (forGood) {
                "This was its last collection, so it will be deleted for good, for both of you."
            } else {
                "This was its last collection. It's still the newest snap, so it stays on Home until a newer one replaces it, then it's gone."
            },
            confirm = "Keep it",
            onConfirm = { confirmUnkeep = null },
            dismissive = if (forGood) "Delete for good" else "Stop keeping it",
            onDismissive = {
                confirmUnkeep = null
                viewModel.toggle(collectionId)
            },
            onDismissRequest = { confirmUnkeep = null },
            modifier = Modifier.testTag("snap_unkeep_confirm"),
        ) { CatFace(if (forGood) CatExpression.Crying else CatExpression.Sad, Modifier.size(96.dp)) }
    }
    val pickingSnap = state.snap
    if (picking && pickingSnap != null && confirmUnkeep == null) {
        KeepPicker(
            collections = state.collections,
            keptIn = pickingSnap.collections,
            newName = newCollection,
            onNewNameChange = viewModel::onNewCollectionChange,
            onCreate = viewModel::createAndKeep,
            onToggle = { c ->
                if (pickingSnap.collections == listOf(c.id)) confirmUnkeep = c.id else viewModel.toggle(c.id)
            },
            onDismiss = { picking = false },
        )
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
                    if (state.gone) "This snap is gone" else "Opening…",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textMain,
                )
                if (state.gone) {
                    Text("It was deleted, or replaced by a newer one while nobody kept it.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
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
            val keptNames = state.collections.filter { it.id in snap.collections }.map { it.name }
            Text(
                if (keptNames.isEmpty()) "Not kept yet" else "Kept in ${keptNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.testTag("snap_kept_in"),
            )
            Spacer(Modifier.height(spacing.sm))
            ChoomHubSecondaryButton(
                text = if (snap.isKept) "Change collections" else "Keep it",
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth().testTag("snap_keep"),
            )
            Text(
                "Kept snaps hang in Our World → Memory Lane and are never replaced.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(top = spacing.sm),
            )
        }
    }
}

/** Where to keep a snap: tick existing collections, or name a new one (it's kept there at once). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeepPicker(
    collections: List<SnapCollection>,
    keptIn: List<String>,
    newName: String,
    onNewNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onToggle: (SnapCollection) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    InkPanel(onDismissRequest = onDismiss, modifier = Modifier.testTag("snap_keep_picker")) {
        GothicMotifArt(GothicMotif.Moth, Modifier.size(72.dp))
        Spacer(Modifier.height(spacing.md))
        Text("Keep it in…", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Spacer(Modifier.height(spacing.lg))
        if (collections.isEmpty()) {
            Text("No collections yet. Name the first one:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        } else {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                collections.forEach { c -> CollectionChip(c, selected = c.id in keptIn, onToggle = { onToggle(c) }) }
            }
        }
        Spacer(Modifier.height(spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoomHubTextField(
                value = newName,
                onValueChange = onNewNameChange,
                label = "New collection",
                supportingText = "${newName.length}/${SnapRules.MAX_COLLECTION_NAME}",
                onDone = onCreate,
                modifier = Modifier.weight(1f).testTag("snap_new_collection"),
            )
            TextButton(onClick = onCreate, modifier = Modifier.testTag("snap_new_collection_add")) {
                Text("ADD", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
            }
        }
        Spacer(Modifier.height(spacing.md))
        ChoomHubPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("snap_keep_done"))
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
