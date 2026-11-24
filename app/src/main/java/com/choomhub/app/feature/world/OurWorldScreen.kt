package com.choomhub.app.feature.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.SectionHeader
import com.choomhub.app.core.designsystem.component.SnapPhoto
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.SnapInfo
import kotlinx.serialization.Serializable

@Serializable
data object OurWorldRoute

@Serializable
data class CollectionRoute(val id: String)

fun NavGraphBuilder.ourWorldScreen(contentPadding: PaddingValues, onOpenCollection: (String) -> Unit = {}, onOpenSnap: (String) -> Unit = {}) {
    composable<OurWorldRoute> { OurWorldScreen(contentPadding, onOpenCollection) }
    composable<CollectionRoute> { CollectionScreen(contentPadding, onOpenSnap) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OurWorldScreen(contentPadding: PaddingValues, onOpenCollection: (String) -> Unit, viewModel: MemoryLaneViewModel = hiltViewModel()) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    Box(Modifier.fillMaxSize().testTag("screen_our_world")) {
        GothicMotifArt(GothicMotif.Cobweb, Modifier.padding(top = contentPadding.calculateTopPadding()).size(110.dp))
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Spacer(Modifier.height(spacing.xxl))
            Text("Our World", style = MaterialTheme.typography.displaySmall, color = colors.textMain, modifier = Modifier.align(Alignment.End))
            Text("a shared room, slowly filling up", style = ChoomHubTextStyles.Handwritten, color = colors.pink, modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(spacing.xl))
            SectionHeader(title = "Memory Lane", eyebrow = "kept snaps")
            Spacer(Modifier.height(spacing.md))
            val list = collections
            when {
                list == null -> Unit
                list.isEmpty() -> ChoomHubCard(modifier = Modifier.fillMaxWidth().testTag("memory_lane_empty"), tone = CardTone.Calm) {
                    Text("Nothing kept yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
                    Text(
                        "Open a snap and keep it in a collection. Kept snaps stay here and are never replaced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    list.forEach { c -> CollectionCard(c, onClick = { onOpenCollection(c.id) }) }
                }
            }
            Spacer(Modifier.height(spacing.xl))
            ChoomHubCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.Calm) {
                Text("STILL TO COME", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                Spacer(Modifier.height(spacing.md))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    listOf("Shared room", "Photo wall", "The cat", "Milestones", "Tomato garden", "Soundtrack").forEach {
                        PillBadge(text = it, color = colors.lavender)
                    }
                }
            }
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

@Composable
private fun CollectionCard(collection: CollectionUi, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    ChoomHubCard(modifier = Modifier.fillMaxWidth().testTag("collection_card_${collection.name}"), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(collection.name, style = MaterialTheme.typography.titleLarge, color = colors.textMain, modifier = Modifier.weight(1f))
            Text("${collection.snaps.size}", style = MaterialTheme.typography.titleMedium, color = colors.pink)
        }
        if (collection.snaps.isNotEmpty()) {
            Spacer(Modifier.height(spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                collection.snaps.take(4).forEach { snap ->
                    Thumb(snap, Modifier.weight(1f))
                }
                repeat(4 - collection.snaps.take(4).size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Thumb(snap: SnapInfo, modifier: Modifier = Modifier) {
    SnapPhoto(snap.thumb, aspect = 1f, contentDescription = snap.caption, modifier = modifier.clip(RoundedCornerShape(6.dp)))
}

@Composable
fun CollectionScreen(contentPadding: PaddingValues, onOpenSnap: (String) -> Unit, viewModel: CollectionViewModel = hiltViewModel()) {
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    val c = collection
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().testTag("screen_collection"),
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            top = contentPadding.calculateTopPadding() + spacing.lg,
            bottom = contentPadding.calculateBottomPadding() + spacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Column(Modifier.padding(bottom = spacing.md)) {
                Text("MEMORY LANE", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                Text(c?.name ?: "", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
                if (c != null && c.snaps.isEmpty()) {
                    Text("Empty for now.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        items(c?.snaps.orEmpty(), key = { it.id }) { snap ->
            Thumb(
                snap,
                Modifier
                    .aspectRatio(1f)
                    .clickable(role = Role.Button, onClickLabel = "Open snap") { onOpenSnap(snap.id) }
                    .testTag("collection_snap_${snap.id}"),
            )
        }
    }
}
