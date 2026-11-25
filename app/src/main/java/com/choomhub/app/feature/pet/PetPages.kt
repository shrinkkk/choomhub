package com.choomhub.app.feature.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetOutcome
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.ShopCategory
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The Pet House's other pages, shown in place of the room. */
internal enum class PetPage(val title: String) { Shop("Shop"), Wardrobe("Wardrobe"), Diary("Diary"), Gifts("Gifts"), Trophies("Trophies") }

@Composable
internal fun PetPageContent(page: PetPage, ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    when (page) {
        PetPage.Shop -> ShopPage(ui, onAct)
        PetPage.Wardrobe -> WardrobePage(ui, onAct)
        PetPage.Diary -> DiaryPage(ui)
        PetPage.Gifts -> GiftsPage(ui)
        PetPage.Trophies -> TrophiesPage(ui)
    }
}

@Composable
private fun dateFormat(pattern: String): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
}

// --- Shop ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopPage(ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var category by remember { mutableStateOf(ShopCategory.Food) }
    var result by remember { mutableStateOf<String?>(null) }
    Text(
        "You have ${ui.pet.myCoins} coins · ${ui.partnerName} has ${ui.pet.partnerCoins}",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.candle,
        modifier = Modifier.testTag("shop_coins"),
    )
    Text(
        "Earn coins by looking after it (up to ${PetRules.DAILY_COIN_CAP} a day), coming to see it, and focus hours.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textFaint,
    )
    Spacer(Modifier.height(spacing.md))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ShopCategory.entries.forEach { c ->
            PillBadge(
                c.label,
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Tab) { category = c }
                    .testTag("shop_tab_${c.name}"),
                color = if (c == category) colors.pink else colors.textFaint,
                showDot = c == category,
            )
        }
    }
    if (result != null) {
        Spacer(Modifier.height(spacing.sm))
        Text(result!!, style = MaterialTheme.typography.bodyMedium, color = colors.candle, modifier = Modifier.testTag("shop_result"))
    }
    Spacer(Modifier.height(spacing.md))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PetRules.SHOP.filter { it.category == category }.forEach { item ->
            val owned = item.pack == 0 && ui.pet.has(item.id)
            val stock = ui.pet.inventory[item.id] ?: 0
            ChoomHubCard(Modifier.fillMaxWidth().testTag("shop_item_${item.id}"), tone = CardTone.Calm, contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.category == ShopCategory.Outfits) {
                        PetCat(PetPose.Sit, Modifier.size(56.dp), outfit = PetOutfit.fromId(item.id), build = ui.build())
                        Spacer(Modifier.width(spacing.sm))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall, color = colors.textMain)
                        Text(item.blurb, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(
                            buildString {
                                append("${item.price} coins")
                                if (item.pack > 0) append(" · pack of ${item.pack}")
                                if (item.pack > 0 && stock > 0) append(" · $stock in the cupboard")
                            },
                            style = ChoomHubTextStyles.Eyebrow,
                            color = colors.textFaint,
                        )
                    }
                    Spacer(Modifier.width(spacing.sm))
                    if (owned) {
                        Text("OWNED", style = ChoomHubTextStyles.Eyebrow, color = colors.leaf)
                    } else {
                        ChoomHubPrimaryButton(
                            "Buy",
                            { result = onAct(PetAction.Buy(item)).message },
                            Modifier.testTag("buy_${item.id}"),
                            enabled = ui.pet.myCoins >= item.price,
                        )
                    }
                }
            }
        }
    }
}

// --- Wardrobe ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WardrobePage(ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val outfits = listOf<String?>(null) + PetRules.SHOP.filter { it.category == ShopCategory.Outfits && ui.pet.has(it.id) }.map { it.id }
    if (outfits.size == 1) {
        Text("Nothing to wear yet. Outfits are in the shop.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.md))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        outfits.forEach { id ->
            val wearing = ui.pet.outfit == id
            Column(
                Modifier
                    .width(104.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (wearing) colors.pink.copy(alpha = 0.12f) else colors.surface)
                    .border(1.dp, if (wearing) colors.pink.copy(alpha = 0.6f) else colors.lavender.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { if (!wearing) onAct(PetAction.Wear(id)) }
                    .padding(spacing.sm)
                    .testTag("wear_${id ?: "nothing"}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PetCat(PetPose.Sit, Modifier.size(84.dp), expression = if (wearing) CatExpression.Happy else CatExpression.Hopeful, build = ui.build(), outfit = PetOutfit.fromId(id))
                Text(
                    (if (id == null) "Nothing" else PetRules.outfitName(id)).uppercase(),
                    style = ChoomHubTextStyles.Eyebrow,
                    color = if (wearing) colors.pink else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

// --- Diary ---

@Composable
private fun DiaryPage(ui: PetUi) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val zone = ZoneId.systemDefault()
    val format = dateFormat("EEE d MMM · h:mm a")
    Text("${ui.catName()}'s diary. Written by ${ui.catName()}. Not proofread.", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    Spacer(Modifier.height(spacing.md))
    if (ui.diary.isEmpty()) {
        Text("Nothing written yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ui.diary.forEach { entry ->
            ChoomHubCard(Modifier.fillMaxWidth().testTag("diary_entry"), tone = CardTone.Calm) {
                entry.at?.let { Text(format.format(it.atZone(zone)).uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint) }
                Spacer(Modifier.height(spacing.xs))
                Text(entry.text, style = ChoomHubTextStyles.Handwritten, color = colors.textMain)
            }
        }
    }
}

// --- Gifts ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GiftsPage(ui: PetUi) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val zone = ZoneId.systemDefault()
    val format = dateFormat("d MMM")
    val found = ui.pet.gifts.groupBy { it.id }
    Text(
        "${found.size} of ${PetRules.GIFTS.size} found. Curious cats find more; the rocks are rare.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
    Spacer(Modifier.height(spacing.md))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PetRules.GIFTS.forEach { kind ->
            val mine = found[kind.id]
            Column(
                Modifier
                    .width(104.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surface)
                    .border(1.dp, colors.lavender.copy(alpha = if (mine != null) 0.25f else 0.06f), MaterialTheme.shapes.medium)
                    .padding(spacing.sm)
                    .testTag("gift_${kind.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(56.dp)) {
                    val s = size.width / 10f
                    scale(s, s, pivot = Offset.Zero) {
                        if (mine != null) {
                            giftIcon(kind.id, Offset(5f, 7.5f), 2.6f)
                        } else {
                            drawCircle(colors.textFaint.copy(alpha = 0.2f), 2.4f, Offset(5f, 5f))
                        }
                    }
                }
                Text(
                    if (mine != null) kind.name else "???",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mine != null) colors.textMain else colors.textFaint,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                if (mine != null) {
                    val first = mine.minBy { it.at }
                    Text(
                        (format.format(first.at.atZone(zone)) + if (mine.size > 1) " · ×${mine.size}" else "").uppercase(),
                        style = ChoomHubTextStyles.Eyebrow,
                        color = colors.textFaint,
                    )
                }
            }
        }
    }
}

// --- Trophies ---

@Composable
private fun TrophiesPage(ui: PetUi) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val now: Instant = ui.now
    val got = PetRules.TROPHIES.count { it.unlocked(ui.pet, now) }
    Text("$got of ${PetRules.TROPHIES.size}", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
    Spacer(Modifier.height(spacing.md))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PetRules.TROPHIES.forEach { t ->
            val unlocked = t.unlocked(ui.pet, now)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (unlocked) colors.candle.copy(alpha = 0.07f) else colors.surface)
                    .padding(spacing.md)
                    .testTag("trophy_${t.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PetIcons.Trophy, contentDescription = null, tint = if (unlocked) colors.candle else colors.textFaint.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(t.name, style = MaterialTheme.typography.titleSmall, color = if (unlocked) colors.textMain else colors.textFaint)
                    Text(t.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}
