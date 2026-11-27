package com.choomhub.app.feature.snaps

import com.choomhub.app.core.designsystem.component.OneLineText
import com.choomhub.app.core.designsystem.icon.PointIcons
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Icon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** One emoji, with its name for searching. */
data class EmojiEntry(val char: String, val name: String)

/** A category in the emoji menu; [icon] is the emoji its tab shows. */
data class EmojiGroup(val name: String, val icon: String, val emojis: List<EmojiEntry>)

/** The reactions under a snap: your four quick ones, anything else you reacted with, and + for the rest. */
@Composable
fun QuickReactionRow(quick: List<String>, selected: String?, onReact: (String) -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = ChoomHubTheme.spacing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        quick.forEach { r -> ReactionChip(r, selected == r) { onReact(r) } }
        // Reacted with something from the menu: it shows here too, picked.
        if (selected != null && selected !in quick) ReactionChip(selected, true) { onReact(selected) }
        ReactionChip("+", false, plus = true, onClick = onMore)
    }
}

@Composable
private fun ReactionChip(text: String, selected: Boolean, plus: Boolean = false, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        Modifier
            .clip(MaterialTheme.shapes.small)
            // A lighter chip than the card, so the black heart still shows.
            .background(if (selected) colors.pink.copy(alpha = 0.22f) else colors.lavender.copy(alpha = 0.14f))
            .border(1.dp, if (selected) colors.pink else if (plus) colors.lavender.copy(alpha = 0.5f) else colors.lavender.copy(alpha = 0.25f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(if (plus) "react_more" else "react_$text"),
        contentAlignment = Alignment.Center,
    ) {
        if (plus) {
            // Same height as an emoji chip, with the thick plus from the points card dead centre.
            val line = with(LocalDensity.current) { MaterialTheme.typography.titleLarge.lineHeight.toDp() }
            Box(Modifier.size(line + 8.dp, line), contentAlignment = Alignment.Center) {
                Icon(PointIcons.Plus, contentDescription = "More reactions", tint = colors.lavender, modifier = Modifier.size(20.dp))
            }
        } else {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Every emoji, by category, with search. Your quick reactions have a candle ring; the one you
 * reacted with is filled. Long-press offers to make it a quick reaction.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.EmojiMenuContent(
    groups: List<EmojiGroup>,
    quick: List<String>,
    selected: String?,
    query: String,
    onQuery: (String) -> Unit,
    group: Int,
    onGroup: (Int) -> Unit,
    showTip: Boolean,
    onDismissTip: () -> Unit,
    onPick: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Text("React with anything", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
    Spacer(Modifier.height(spacing.md))
    if (showTip) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(colors.candle.copy(alpha = 0.08f))
                .border(1.dp, colors.candle.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                .padding(start = spacing.md, top = spacing.sm, bottom = spacing.sm)
                .testTag("emoji_tip"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Long-press any emoji to make it one of your four quick reactions.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.candle,
                modifier = Modifier.weight(1f),
            )
            TextButton(onDismissTip) { OneLineText("GOT IT", style = MaterialTheme.typography.labelLarge, color = colors.candle) }
        }
        Spacer(Modifier.height(spacing.md))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("YOUR QUICK REACTIONS", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.weight(1f))
        quick.forEach { q ->
            Box(Modifier.padding(start = 6.dp).clip(CircleShape).background(colors.lavender.copy(alpha = 0.14f)).padding(4.dp)) {
                Text(q, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    Spacer(Modifier.height(spacing.sm))
    ChoomHubTextField(value = query, onValueChange = onQuery, label = "Search", modifier = Modifier.fillMaxWidth().testTag("emoji_search"))
    Spacer(Modifier.height(spacing.sm))
    val searching = query.isNotBlank()
    if (!searching) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            groups.forEachIndexed { i, g ->
                Column(
                    Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(role = Role.Tab) { onGroup(i) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(g.icon, style = MaterialTheme.typography.titleMedium, modifier = Modifier.then(if (i == group) Modifier else Modifier))
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.width(14.dp).height(2.dp).background(if (i == group) colors.pink else colors.pink.copy(alpha = 0f)))
                }
            }
        }
        Text(groups.getOrNull(group)?.name?.uppercase().orEmpty(), style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint, modifier = Modifier.padding(top = spacing.xs))
    }
    val shown = if (searching) {
        val q = query.trim().lowercase()
        groups.flatMap { it.emojis }.filter { q in it.name }
    } else {
        groups.getOrNull(group)?.emojis.orEmpty()
    }
    Spacer(Modifier.height(spacing.xs))
    LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(260.dp).testTag("emoji_grid")) {
        items(shown, key = { it.char }) { e ->
            val isQuick = e.char in quick
            val isMine = e.char == selected
            Box(
                Modifier
                    .padding(2.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isMine) colors.pink.copy(alpha = 0.22f) else if (isQuick) colors.lavender.copy(alpha = 0.12f) else colors.pink.copy(alpha = 0f))
                    .border(if (isQuick) 1.5.dp else 0.dp, if (isQuick) colors.candle else colors.candle.copy(alpha = 0f), CircleShape)
                    .combinedClickable(onClick = { onPick(e.char) }, onLongClick = { onLongPress(e.char) }, onLongClickLabel = "Make it a quick reaction")
                    .testTag("emoji_${e.char}"),
                contentAlignment = Alignment.Center,
            ) { Text(e.char, style = MaterialTheme.typography.titleLarge) }
        }
    }
    if (shown.isEmpty()) {
        Text("Nothing called that.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, textAlign = TextAlign.Center)
    }
}

/** After a long-press: which quick reaction it replaces. */
@Composable
fun ColumnScope.ReplaceQuickContent(emoji: String, quick: List<String>, onReplace: (Int) -> Unit, onCancel: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Text(emoji, style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(spacing.sm))
    Text("Make it a quick reaction", style = MaterialTheme.typography.headlineSmall, color = colors.textMain)
    Text("tap the one it replaces", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    Spacer(Modifier.height(spacing.lg))
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
        quick.forEachIndexed { i, q ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.lavender.copy(alpha = 0.14f))
                        .border(1.dp, colors.lavender.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button) { onReplace(i) }
                        .testTag("replace_quick_$i"),
                    contentAlignment = Alignment.Center,
                ) { Text(q, style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.height(4.dp))
                Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            }
        }
    }
    Spacer(Modifier.height(spacing.lg))
    TextButton(onCancel, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
}

/** The emoji menu as a pop-up, switching to "which one does it replace?" after a long-press. */
@Composable
fun EmojiMenu(
    quick: List<String>,
    selected: String?,
    showTip: Boolean,
    onDismissTip: () -> Unit,
    replacing: String?,
    onPick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onReplace: (Int) -> Unit,
    onCancelReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val groups by androidx.compose.runtime.produceState(emptyList<EmojiGroup>()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { EmojiData.load(context) }
    }
    var query by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var group by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    com.choomhub.app.core.designsystem.component.InkPanel(onDismiss, Modifier.testTag("emoji_menu")) {
        if (replacing != null) {
            ReplaceQuickContent(replacing, quick, onReplace, onCancelReplace)
        } else {
            EmojiMenuContent(groups, quick, selected, query, { query = it }, group, { group = it }, showTip, onDismissTip, onPick, onLongPress)
        }
    }
}
