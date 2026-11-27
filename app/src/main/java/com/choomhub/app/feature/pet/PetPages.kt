package com.choomhub.app.feature.pet

import com.choomhub.app.domain.model.PetDiaryEntry
import java.util.Locale
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.activity.compose.BackHandler
import java.time.YearMonth
import com.choomhub.app.core.designsystem.component.OneLineText
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
import com.choomhub.app.feature.pet.art.OutfitSlot
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.toyArt
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.saveable.rememberSaveable
import com.choomhub.app.feature.pet.art.PetPose
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The Pet House's other pages, shown in place of the room. */
enum class PetPage(val title: String) { Shop("Shop"), Wardrobe("Wardrobe"), Diary("Diary"), Gifts("Gifts"), Trophies("Trophies") }

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

/** A shop tab, in the points shop's style. */
@Composable
private fun ShopTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.pink.copy(alpha = 0.14f) else colors.surface)
            .border(1.dp, if (selected) colors.pink.copy(alpha = 0.6f) else colors.lavender.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = ChoomHubTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        OneLineText(label, MaterialTheme.typography.labelLarge, color = if (selected) colors.pink else colors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/** The short tab names, so all five fit on a phone. */
private fun ShopCategory.tab(): String = if (this == ShopCategory.Furniture) "Home" else label

/** Which slot an outfit goes in. */
internal fun outfitSlot(id: String): OutfitSlot? = PetOutfit.fromId(id).slot

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopPage(ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var category by rememberSaveable { mutableStateOf(ShopCategory.Food) }
    var slot by rememberSaveable { mutableStateOf(OutfitSlot.Head) }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShopCategory.entries.forEach { c ->
            ShopTab(c.tab(), c == category, Modifier.weight(if (c == ShopCategory.Outfits) 1.25f else 1f).testTag("shop_tab_${c.name}")) { category = c }
        }
    }
    if (category == ShopCategory.Outfits) {
        Spacer(Modifier.height(spacing.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutfitSlot.entries.forEach { s ->
                PillBadge(
                    s.label,
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Tab) { slot = s }
                        .testTag("shop_slot_${s.name}"),
                    color = if (s == slot) colors.pink else colors.textFaint,
                    showDot = s == slot,
                )
            }
        }
    }
    if (result != null) {
        Spacer(Modifier.height(spacing.sm))
        Text(result!!, style = MaterialTheme.typography.bodyMedium, color = colors.candle, modifier = Modifier.testTag("shop_result"))
    }
    Spacer(Modifier.height(spacing.md))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PetRules.SHOP.filter { it.category == category && (category != ShopCategory.Outfits || outfitSlot(it.id) == slot) }.forEach { item ->
            val owned = item.pack == 0 && ui.pet.has(item.id)
            val stock = ui.pet.inventory[item.id] ?: 0
            ChoomHubCard(Modifier.fillMaxWidth().testTag("shop_item_${item.id}"), tone = CardTone.Calm, contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.category) {
                        ShopCategory.Outfits -> {
                            PetCat(PetPose.Sit, Modifier.size(56.dp), outfit = PetOutfit.fromId(item.id), build = ui.build())
                            Spacer(Modifier.width(spacing.sm))
                        }
                        ShopCategory.Toys -> {
                            Canvas(Modifier.size(56.dp)) { toyArt(item.id, center, size.width / 46f) }
                            Spacer(Modifier.width(spacing.sm))
                        }
                        else -> Unit
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
    if (category == ShopCategory.Toys) {
        Spacer(Modifier.height(spacing.sm))
        Text("The feather wand is free and always there.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
    }
}

// --- Wardrobe ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WardrobePage(ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val owned = PetRules.SHOP.filter { it.category == ShopCategory.Outfits && ui.pet.has(it.id) }.map { it.id }
    val wearing = ui.pet.wearing()
    val look = PetOutfit.lookOf(wearing)
    fun wearIn(slot: OutfitSlot, id: String?) {
        // Swap whatever's in that slot; everything else stays on.
        val rest = wearing.filter { outfitSlot(it) != slot }
        onAct(PetAction.Wear(if (id == null) rest else rest + id, added = id))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PetCat(PetPose.Sit, Modifier.size(140.dp).testTag("wardrobe_preview"), expression = CatExpression.Happy, build = ui.build(), outfits = look)
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            Text("WEARING", style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint)
            OutfitSlot.entries.forEach { s ->
                val on = look.firstOrNull { it.slot == s }
                Row {
                    Text(s.label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.width(52.dp))
                    Text(on?.let { PetRules.outfitName(it.id) } ?: "-", style = MaterialTheme.typography.bodySmall, color = if (on != null) colors.pink else colors.textFaint)
                }
            }
            if (wearing.isNotEmpty()) {
                Spacer(Modifier.height(spacing.xs))
                Text(
                    "TAKE IT ALL OFF",
                    style = ChoomHubTextStyles.Eyebrow,
                    color = colors.lavender,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) { onAct(PetAction.Wear(emptyList())) }
                        .padding(vertical = 6.dp)
                        .testTag("wear_nothing"),
                )
            }
        }
    }
    Spacer(Modifier.height(spacing.md))
    if (owned.isEmpty()) {
        Text("Nothing to wear yet. Outfits are in the shop.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }
    OutfitSlot.entries.forEach { slot ->
        val items = owned.filter { outfitSlot(it) == slot }
        if (items.isEmpty()) return@forEach
        Text(slot.label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.candle, modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs))
        val current = wearing.firstOrNull { outfitSlot(it) == slot }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            (listOf<String?>(null) + items).forEach { id ->
                val on = current == id
                Column(
                    Modifier
                        .width(84.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (on) colors.pink.copy(alpha = 0.12f) else colors.surface)
                        .border(1.dp, if (on) colors.pink.copy(alpha = 0.6f) else colors.lavender.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button) { if (!on) wearIn(slot, id) }
                        .padding(4.dp)
                        .testTag(if (id == null) "wear_none_${slot.name}" else "wear_$id"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (id == null) {
                        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { Text("-", color = colors.textFaint) }
                    } else {
                        PetCat(PetPose.Sit, Modifier.size(60.dp), expression = CatExpression.Hopeful, build = ui.build(), outfit = PetOutfit.fromId(id))
                    }
                    Text(
                        (if (id == null) "None" else PetRules.outfitName(id)).uppercase(),
                        style = ChoomHubTextStyles.Eyebrow,
                        color = if (on) colors.pink else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(spacing.sm))
    Text("Tapping something swaps it in for whatever's in that slot. Everything else stays on.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
}

// --- Diary ---

@Composable
private fun DiaryPage(ui: PetUi) {
    val zone = ZoneId.systemDefault()
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val months = remember(ui.diary) { diaryMonths(ui.diary, zone) }
    val shown = open?.let { YearMonth.parse(it) }?.takeIf { it in months }
    if (shown != null) {
        BackHandler { open = null }
        DiaryMonth(ui, shown, months.getValue(shown), zone) { open = null }
    } else {
        DiaryShelf(ui, months) { open = it.toString() }
    }
}

/** The diary by month, newest month first; each month's entries oldest first, as a diary reads. */
internal fun diaryMonths(diary: List<PetDiaryEntry>, zone: ZoneId): Map<YearMonth, List<PetDiaryEntry>> =
    diary.filter { it.at != null }
        .groupBy { YearMonth.from(it.at!!.atZone(zone)) }
        .mapValues { (_, list) -> list.sortedBy { it.at } }
        .toSortedMap(compareByDescending { it })

private val CoverColours = listOf(Color(0xFF5A2333), Color(0xFF263449), Color(0xFF34402F), Color(0xFF45284A), Color(0xFF5C3A22), Color(0xFF2E3B3F))
private val DiaryPaper = Color(0xFFE9E1D2)
private val DiaryInk = Color(0xFF2A2230)
private val Gilt = Color(0xFFD9B45A)

/** A small diary for every month, on a shelf. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryShelf(ui: PetUi, months: Map<YearMonth, List<PetDiaryEntry>>, onOpen: (YearMonth) -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Text("${ui.catName()}'s diaries. Written by ${ui.catName()}. Not proofread.", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    Spacer(Modifier.height(spacing.md))
    if (months.isEmpty()) {
        Text("Nothing written yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bookWidth = (maxWidth - spacing.lg * 2) / 3
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            months.entries.forEach { (month, entries) ->
                Column(
                    Modifier.width(bookWidth).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Open ${month.format(monthName)}") { onOpen(month) }
                        .testTag("diary_month_$month"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DiaryBook(month, Modifier.fillMaxWidth().aspectRatio(0.74f))
                    Spacer(Modifier.height(6.dp))
                    Text("${entries.size} entr${if (entries.size == 1) "y" else "ies"}", style = ChoomHubTextStyles.Eyebrow.copy(fontSize = 9.sp), color = colors.textFaint)
                }
            }
        }
    }
}

private val monthName = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** A little leather diary: a darker spine, stitching, the month on a gilt label, a paw, the strap. */
@Composable
private fun DiaryBook(month: YearMonth, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val cover = CoverColours[(month.year * 12 + month.monthValue) % CoverColours.size]
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.06f
        // Pages showing at the edge.
        drawRoundRect(DiaryPaper, Offset(w * 0.06f, h * 0.03f), Size(w * 0.92f, h * 0.95f), CornerRadius(r))
        repeat(4) { k -> drawLine(DiaryInk.copy(alpha = 0.18f), Offset(w * 0.98f - 1f, h * (0.1f + k * 0.22f)), Offset(w * 0.98f - 1f, h * (0.2f + k * 0.22f)), 1f) }
        // The cover.
        drawRoundRect(Brush.linearGradient(listOf(cover.copy(alpha = 1f), Color(cover.red * 0.7f, cover.green * 0.7f, cover.blue * 0.7f)), Offset.Zero, Offset(w, h)), Offset.Zero, Size(w * 0.94f, h), CornerRadius(r))
        drawRect(Color.Black.copy(alpha = 0.25f), Offset(0f, 0f), Size(w * 0.12f, h))
        drawLine(Color.White.copy(alpha = 0.08f), Offset(w * 0.12f, 0f), Offset(w * 0.12f, h), 1.5f)
        // Stitching.
        drawRoundRect(Gilt.copy(alpha = 0.45f), Offset(w * 0.17f, h * 0.035f), Size(w * 0.72f, h * 0.93f), CornerRadius(r * 0.6f), style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
        // The label: month and year.
        val label = Offset(w * 0.24f, h * 0.18f)
        val labelSize = Size(w * 0.58f, h * 0.28f)
        drawRoundRect(DiaryPaper.copy(alpha = 0.92f), label, labelSize, CornerRadius(r * 0.5f))
        drawRoundRect(Gilt, label, labelSize, CornerRadius(r * 0.5f), style = Stroke(1.2.dp.toPx()))
        val m = measurer.measure(month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(), TextStyle(fontSize = (w / density * 0.13f).sp, color = DiaryInk, fontFamily = ChoomHubTextStyles.Eyebrow.fontFamily, letterSpacing = (w / density * 0.012f).sp))
        val y = measurer.measure("${month.year}", TextStyle(fontSize = (w / density * 0.1f).sp, color = DiaryInk.copy(alpha = 0.7f), fontFamily = ChoomHubTextStyles.Handwritten.fontFamily, fontStyle = FontStyle.Italic))
        drawText(m, topLeft = Offset(label.x + (labelSize.width - m.size.width) / 2f, label.y + labelSize.height * 0.12f))
        drawText(y, topLeft = Offset(label.x + (labelSize.width - y.size.width) / 2f, label.y + labelSize.height * 0.12f + m.size.height * 0.95f))
        // A gilt paw.
        paw(Offset(w * 0.53f, h * 0.66f), w * 0.12f, Gilt.copy(alpha = 0.85f))
        // The strap.
        drawRect(Color.Black.copy(alpha = 0.3f), Offset(w * 0.8f, 0f), Size(w * 0.07f, h))
        drawRect(Gilt.copy(alpha = 0.6f), Offset(w * 0.79f, h * 0.46f), Size(w * 0.09f, h * 0.06f))
    }
}

/** A paw print: a pad and four toes. */
private fun DrawScope.paw(c: Offset, s: Float, color: Color) {
    drawOval(color, c - Offset(s * 0.55f, s * 0.2f), Size(s * 1.1f, s * 0.85f))
    listOf(-0.62f to -0.55f, -0.22f to -0.88f, 0.22f to -0.88f, 0.62f to -0.55f).forEach { (x, y) ->
        drawOval(color, c + Offset(x * s - s * 0.2f, y * s - s * 0.24f), Size(s * 0.4f, s * 0.48f))
    }
}

/** Writing on ruled paper: a faint blue rule under every line, wherever the words wrap. */
@Composable
private fun RuledText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    Text(
        text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val l = layout ?: return@drawBehind
            for (i in 0 until l.lineCount) {
                val y = l.getLineBaseline(i) + 5.dp.toPx()
                drawLine(Color(0xFF8FA3B8).copy(alpha = 0.4f), Offset(-12.dp.toPx(), y), Offset(size.width + 6.dp.toPx(), y), 1f)
            }
        },
    )
}

/** One month, opened: a ruled paper page, each day's entries under its date, paws between days. */
@Composable
private fun DiaryMonth(ui: PetUi, month: YearMonth, entries: List<PetDiaryEntry>, zone: ZoneId, onClose: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val days = entries.groupBy { it.at!!.atZone(zone).toLocalDate() }
    val dayFormat = dateFormat("EEEE d")
    val timeFormat = dateFormat("h:mm a")
    val line = 30.sp
    Row(
        Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "All diaries", onClick = onClose).padding(vertical = 6.dp).testTag("diary_back"),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text("‹  ALL DIARIES", style = ChoomHubTextStyles.Eyebrow, color = colors.lavender) }
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(DiaryPaper)
            .drawBehind {
                // A red margin, like a school diary; the rules sit under each line of writing.
                drawLine(Color(0xFFC0504A).copy(alpha = 0.5f), Offset(30.dp.toPx(), 0f), Offset(30.dp.toPx(), size.height), 1.5f)
            }
            .padding(start = 42.dp, end = 18.dp, top = 18.dp, bottom = 28.dp)
            .testTag("diary_month_page"),
    ) {
        Column {
            Text(month.format(monthName), style = ChoomHubTextStyles.Handwritten.copy(fontSize = 30.sp, lineHeight = 36.sp), color = DiaryInk)
            Text("${ui.catName()}'s diary · private (we both read it anyway)", style = ChoomHubTextStyles.Handwritten.copy(fontSize = 14.sp, lineHeight = line), color = DiaryInk.copy(alpha = 0.55f))
            days.entries.forEachIndexed { i, (day, list) ->
                if (i > 0) {
                    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                        listOf(0.38f, 0.5f, 0.62f).forEachIndexed { k, x -> paw(Offset(size.width * x, size.height * (if (k == 1) 0.45f else 0.6f)), 5.dp.toPx(), DiaryInk.copy(alpha = 0.25f)) }
                    }
                }
                Text(day.format(dayFormat), style = ChoomHubTextStyles.Handwritten.copy(fontSize = 20.sp, lineHeight = line, fontWeight = FontWeight.Bold), color = Color(0xFF7A2E45), modifier = Modifier.testTag("diary_day"))
                list.forEach { entry ->
                    RuledText(entry.text, ChoomHubTextStyles.Handwritten.copy(fontSize = 19.sp, lineHeight = line, color = DiaryInk), Modifier.testTag("diary_entry"))
                    entry.at?.let { Text("— ${timeFormat.format(it.atZone(zone)).lowercase()}", style = ChoomHubTextStyles.Handwritten.copy(fontSize = 13.sp, lineHeight = 20.sp), color = DiaryInk.copy(alpha = 0.45f)) }
                    Spacer(Modifier.height(4.dp))
                }
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
