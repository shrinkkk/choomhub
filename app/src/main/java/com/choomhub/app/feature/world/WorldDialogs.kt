package com.choomhub.app.feature.world

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.component.InkMenu
import com.choomhub.app.core.designsystem.component.InkMenuOption
import com.choomhub.app.core.designsystem.component.InkNameDialog
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.domain.model.SnapRules
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.CatFace

/** Every pop-up on Our World: the long-press menus, naming, and the confirmations before anything is lost. */
@Composable
internal fun WorldDialogs(
    dialog: WorldDialog?,
    collections: List<CollectionUi>,
    viewModel: MemoryLaneViewModel,
    onDismiss: () -> Unit,
    open: (WorldDialog) -> Unit,
) {
    when (dialog) {
        null -> Unit
        WorldDialog.New -> InkNameDialog(
            title = "New collection",
            initial = "",
            confirm = "Make it",
            maxLength = SnapRules.MAX_COLLECTION_NAME,
            label = "Collection name",
            onConfirm = { name ->
                viewModel.create(name)
                onDismiss()
            },
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("collection_new_dialog"),
        ) { GothicMotifArt(GothicMotif.SkeletonKey, Modifier.size(80.dp)) }

        is WorldDialog.CollectionMenu -> {
            val c = collections.firstOrNull { it.id == dialog.id } ?: return
            InkMenu(
                title = c.name,
                options = listOf(
                    InkMenuOption("Rename", "collection_rename") { open(WorldDialog.Rename(c.id)) },
                    InkMenuOption("Delete", "collection_delete") { open(WorldDialog.ConfirmDelete(c.id)) },
                ),
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("collection_menu"),
            ) { GothicMotifArt(GothicMotif.Candle, Modifier.size(80.dp)) }
        }

        is WorldDialog.Rename -> {
            val c = collections.firstOrNull { it.id == dialog.id } ?: return
            InkNameDialog(
                title = "Rename it",
                initial = c.name,
                confirm = "Save",
                maxLength = SnapRules.MAX_COLLECTION_NAME,
                label = "Collection name",
                onConfirm = { name ->
                    viewModel.rename(c.id, name)
                    onDismiss()
                },
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("collection_rename_dialog"),
            ) { GothicMotifArt(GothicMotif.SkeletonKey, Modifier.size(80.dp)) }
        }

        is WorldDialog.ConfirmDelete -> {
            val c = collections.firstOrNull { it.id == dialog.id } ?: return
            InkDialog(
                title = "Delete “${c.name}”?",
                whisper = "this one can't be undone",
                message = deleteCollectionMessage(viewModel.deletion(c)),
                confirm = "Keep it",
                onConfirm = onDismiss,
                dismissive = "Delete collection",
                onDismissive = {
                    viewModel.delete(c.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("collection_delete_confirm"),
            ) { CatFace(CatExpression.Crying, Modifier.size(96.dp)) }
        }

        is WorldDialog.SnapMenu -> {
            val c = collections.firstOrNull { it.id == dialog.collectionId } ?: return
            val snap = c.snaps.firstOrNull { it.id == dialog.snapId } ?: return
            val forGood = viewModel.deletesForGood(snap, c.id)
            InkMenu(
                title = "This snap",
                whisper = snap.caption?.let { if (it.length > 48) it.take(47).trimEnd() + "…" else it },
                options = buildList {
                    if (c.cover?.id != snap.id) {
                        add(
                            InkMenuOption("Make it the cover", "snap_make_cover") {
                                viewModel.setCover(c.id, snap.id)
                                onDismiss()
                            },
                        )
                    }
                    add(
                        InkMenuOption(if (forGood) "Delete" else "Take it out of here", "snap_remove") {
                            open(WorldDialog.ConfirmRemove(c.id, snap.id))
                        },
                    )
                },
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("snap_menu"),
            ) { GothicMotifArt(GothicMotif.Moth, Modifier.size(80.dp)) }
        }

        is WorldDialog.ConfirmRemove -> {
            val c = collections.firstOrNull { it.id == dialog.collectionId } ?: return
            val snap = c.snaps.firstOrNull { it.id == dialog.snapId } ?: return
            val forGood = viewModel.deletesForGood(snap, c.id)
            val others = collections.filter { it.id != c.id && it.id in snap.collections }.map { it.name }
            InkDialog(
                title = if (forGood) "Delete it for good?" else "Take it out of “${c.name}”?",
                whisper = if (forGood) "this one can't be undone" else null,
                message = when {
                    forGood -> "It isn't in any other collection, so it will be deleted for good, for both of you."
                    others.isNotEmpty() -> "It stays in ${others.joinToString(", ") { "“$it”" }}."
                    else -> "It's still the newest snap, so it stays on Home until a newer one replaces it."
                },
                confirm = "Keep it",
                onConfirm = onDismiss,
                dismissive = if (forGood) "Delete for good" else "Take it out",
                onDismissive = {
                    viewModel.remove(snap, c.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("snap_remove_confirm"),
            ) { CatFace(if (forGood) CatExpression.Crying else CatExpression.Sad, Modifier.size(96.dp)) }
        }
    }
}

internal fun deleteCollectionMessage(d: CollectionDeletion): String {
    fun snaps(n: Int) = if (n == 1) "1 snap" else "$n snaps"
    val parts = buildList {
        if (d.goneForGood > 0) add("${snaps(d.goneForGood)} only in this collection will be deleted for good, for both of you.")
        if (d.stayElsewhere > 0) add("${snaps(d.stayElsewhere)} also in other collections will stay there.")
        if (d.stayCurrent > 0) add("The newest snap stays on Home until a newer one replaces it.")
    }
    return parts.ifEmpty { listOf("It's empty, so nothing else is lost.") }.joinToString(" ")
}
