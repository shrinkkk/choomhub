package com.choomhub.app.feature.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.model.ShelfSong
import com.choomhub.app.feature.world.atlas.DelhiAtlas
import com.choomhub.app.feature.world.atlas.MapWindow
import com.choomhub.app.feature.world.atlas.atlasPins
import com.choomhub.app.feature.world.cabinet.SpecimenCabinetArt
import com.choomhub.app.feature.world.constellation.ConstellationSky
import com.choomhub.app.feature.world.constellation.skyTime
import com.choomhub.app.feature.world.constellation.starsOf
import com.choomhub.app.feature.world.shelf.ShelfPreviewArt
import com.choomhub.app.feature.world.shelf.coverBitmap
import java.time.Instant

/** Where Our World's rooms lead. */
data class WorldDoors(
    val onCabinet: () -> Unit = {},
    val onConstellation: () -> Unit = {},
    val onShelf: () -> Unit = {},
    val onAtlas: () -> Unit = {},
)

/** The little sky in the top corner of Our World, behind the title: your constellation so far. */
@Composable
internal fun SkyPreview(logs: List<MeetupLog>, now: Instant, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val stars = remember(logs, now.epochSecond / 600) { starsOf(logs, now).second }
    Box(
        modifier
            .size(230.dp, 190.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClickLabel = "Open the constellation", onClick = onOpen)
            .testTag("world_sky"),
    ) {
        ConstellationSky(stars, Modifier.fillMaxSize().padding(start = 6.dp, top = 10.dp), time = skyTime(), preview = true)
    }
}

@Composable
private fun RoomCard(eyebrow: String, title: String, line: String, tag: String, onClick: () -> Unit, art: @Composable BoxScope.() -> Unit) {
    val colors = ChoomHubTheme.colors
    ChoomHubCard(
        Modifier.fillMaxWidth().testTag(tag),
        tone = CardTone.Raised,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.large)) { art() }
        Column(Modifier.padding(16.dp)) {
            Text(eyebrow, style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textMain)
            Text(line, style = MaterialTheme.typography.bodySmall, color = colors.candle, modifier = Modifier.testTag("${tag}_line"))
        }
    }
}

/** What the cabinet card says under its art. */
internal fun cabinetLine(logs: List<MeetupLog>, now: Instant): String {
    val live = logs.firstOrNull { it.live }
    val kept = "${logs.size} kept"
    return when {
        logs.isEmpty() -> "Log a date: live, or one from before"
        live != null -> "● Logging now · ${MeetupRules.duration(live.minutesAt(now) ?: 0)} · $kept"
        else -> kept
    }
}

/** What the shelf card says under its art. */
internal fun shelfLine(songs: List<ShelfSong>, myName: String, partner: String): String {
    if (songs.isEmpty()) return "Every song you send lands here"
    val last = songs.maxByOrNull { it.sentAt ?: Instant.EPOCH }
    val count = "${songs.size} record${if (songs.size == 1) "" else "s"}"
    return if (last == null) count else "$count · last from ${if (last.fromMe) myName else partner}"
}

/** What the atlas card says under its art. */
internal fun atlasLine(logs: List<MeetupLog>): String {
    val groups = atlasPins(logs)
    if (groups.isEmpty()) return "Pin a date to a place in the archive"
    val places = "${groups.size} place${if (groups.size == 1) "" else "s"} in Delhi"
    val top = groups.maxByOrNull { it.second.size }?.takeIf { it.second.size > 1 }
    val name = top?.second?.firstNotNullOfOrNull { it.place }
    return if (name == null) places else "$places · $name ×${top.second.size}"
}

/** The cabinet, the record shelf and the atlas, between the title and Memory Lane. */
@Composable
internal fun WorldRooms(logs: List<MeetupLog>?, shelf: List<ShelfSong>?, names: CoupleNames, now: Instant, doors: WorldDoors) {
    val spacing = ChoomHubTheme.spacing
    val kept = logs.orEmpty()
    val songs = shelf.orEmpty()
    val partner = names.partnerName ?: "them"
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter).padding(top = spacing.lg), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RoomCard("EVERY TIME WE MET", "The Archive", cabinetLine(kept, now), "world_cabinet", doors.onCabinet) {
            SpecimenCabinetArt(Modifier.fillMaxSize())
        }
        val latest = remember(songs) { songs.sortedByDescending { it.sentAt ?: Instant.EPOCH }.take(2) }
        val covers = remember(latest.map { it.key }) { latest.map { it.coverBitmap() } }
        RoomCard("SONGS WE SENT", "Record shelf", shelfLine(songs, "you", partner), "world_shelf", doors.onShelf) {
            ShelfPreviewArt(covers, Modifier.fillMaxSize())
        }
        val pins = remember(kept) { atlasPins(kept).map { it.first } }
        RoomCard("WHERE WE'VE BEEN", "Atlas", atlasLine(kept), "world_atlas", doors.onAtlas) {
            DelhiAtlas(pins, Modifier.fillMaxSize(), small = true, window = MapWindow.around(pins))
        }
    }
}
