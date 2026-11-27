package com.choomhub.app.feature.world.shelf

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.ShelfSong
import com.choomhub.app.domain.model.SongRules
import com.choomhub.app.feature.pet.PetIcons
import com.choomhub.app.feature.world.WorldViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ShelfRoute

fun NavGraphBuilder.shelfScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<ShelfRoute> { RecordShelfScreen(contentPadding, onBack) }
}

/** Whose records the shelf shows. */
enum class ShelfFilter { All, Mine, Theirs }

fun List<ShelfSong>.shownFor(filter: ShelfFilter): List<ShelfSong> = when (filter) {
    ShelfFilter.All -> this
    ShelfFilter.Mine -> filter { it.fromMe }
    ShelfFilter.Theirs -> filter { !it.fromMe }
}

/** Covers decoded once per record, so scrolling never decodes. */
fun ShelfSong.coverBitmap(): ImageBitmap? = cover?.let { b -> runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull() }

private const val PER_SHELF = 2

@Composable
fun RecordShelfScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: WorldViewModel = hiltViewModel()) {
    val shelf by viewModel.shelf.collectAsStateWithLifecycle()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val partner = names.partnerName ?: "Them"
    val songs = shelf.orEmpty()
    val records = remember(songs, names) {
        songs.associate { s ->
            s.key to ShelfRecord(s.key, s.title, s.artist.orEmpty(), if (s.fromMe) "You" else partner, s.rating, s.coverBitmap(), s.kind)
        }
    }
    var filter by rememberSaveable { mutableStateOf(ShelfFilter.All) }
    val shown = songs.shownFor(filter)
    val play: (ShelfRecord) -> Unit = { r ->
        songs.firstOrNull { it.key == r.id }?.let { s ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SongRules.playUrl(s.trackId, s.kind))))
            } catch (_: ActivityNotFoundException) {
                // No browser and no Spotify: nothing sensible to open.
            }
        }
    }
    RecordShelfContent(
        shown.mapNotNull { records[it.key] },
        loading = shelf == null,
        filter = filter,
        partner = partner,
        contentPadding = contentPadding,
        onFilter = { filter = it },
        onPlay = play,
        onBack = onBack,
    )
}

@Composable
fun RecordShelfContent(
    records: List<ShelfRecord>,
    loading: Boolean,
    filter: ShelfFilter,
    partner: String,
    contentPadding: PaddingValues,
    onFilter: (ShelfFilter) -> Unit,
    onPlay: (ShelfRecord) -> Unit,
    onBack: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val shelves = records.chunked(PER_SHELF)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF15101B), Color(0xFF1E1620), Color(0xFF15101B)))).testTag("screen_shelf")) {
        // A warm wall light from above.
        Canvas(Modifier.fillMaxWidth().height(360.dp)) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFC9A45C).copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * 0.5f, 0f), radius = size.width * 0.8f),
                size.width * 0.8f,
                Offset(size.width * 0.5f, 0f),
            )
        }
        LazyColumn(
            Modifier.fillMaxSize().testTag("shelf_list"),
            contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 32.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).testTag("shelf_back"),
                        contentAlignment = Alignment.Center,
                    ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textSecondary) }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text("RECORD SHELF", style = ChoomHubTextStyles.Eyebrow, color = colors.candle)
                        Text("Every song, album and playlist you've sent", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("All", filter == ShelfFilter.All, "shelf_all") { onFilter(ShelfFilter.All) }
                    FilterPill("Mine", filter == ShelfFilter.Mine, "shelf_mine") { onFilter(ShelfFilter.Mine) }
                    FilterPill(partner, filter == ShelfFilter.Theirs, "shelf_theirs") { onFilter(ShelfFilter.Theirs) }
                }
                Spacer(Modifier.height(18.dp))
            }
            items(shelves.size, key = { i -> shelves[i].joinToString { it.id } }) { i ->
                ShelfRow(shelves[i], 132.dp, PER_SHELF, Modifier.padding(bottom = 26.dp), onTap = onPlay)
            }
            if (!loading && shelves.size < 3) {
                // Empty planks waiting, so a short shelf still looks like a shelf.
                items(3 - shelves.size) { i ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 26.dp)) {
                        Spacer(Modifier.height(140.dp))
                        ShelfPlank()
                        if (i == 0) {
                            Text(
                                when {
                                    records.isEmpty() && filter == ShelfFilter.All -> "Nothing on the shelf yet. Every song, album and playlist you send each other lands here."
                                    records.isEmpty() -> "Nothing from ${if (filter == ShelfFilter.Mine) "you" else partner} yet."
                                    else -> "room for more"
                                },
                                style = ChoomHubTextStyles.Handwritten,
                                color = colors.pink.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 10.dp).testTag("shelf_room"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }.testTag(tag)) {
        PillBadge(label, color = if (selected) colors.candle else colors.textFaint)
    }
}
