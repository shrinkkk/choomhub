package com.choomhub.app.feature.pet

import com.choomhub.app.core.designsystem.component.GestureRow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.GestureButton
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetOutcome
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetBuild
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetPose
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data object PetHouseRoute

fun NavGraphBuilder.petHouseScreen(contentPadding: PaddingValues) {
    composable<PetHouseRoute> { PetHouseScreen(contentPadding) }
}

@Composable
fun PetHouseScreen(contentPadding: PaddingValues, viewModel: PetViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    PetHouseContent(
        ui = ui,
        message = message,
        contentPadding = contentPadding,
        onAct = viewModel::act,
        onProposeName = viewModel::proposeName,
        onAdopt = viewModel::adopt,
        onDismissMessage = viewModel::dismissMessage,
    )
}

/** The Pet House, without its view model (for tests and previews). */
@Composable
internal fun PetHouseContent(
    ui: PetUi,
    message: PetMessage?,
    contentPadding: PaddingValues,
    onAct: (PetAction) -> PetOutcome,
    onProposeName: (String) -> Unit,
    onAdopt: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    var panel by rememberSaveable { mutableStateOf<PetPanel?>(null) }
    var page by rememberSaveable { mutableStateOf<PetPage?>(null) }
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(4_000)
            onDismissMessage()
        }
    }
    if (page != null) BackHandler { page = null }
    // The room keeps its place; each page opens at the top.
    val roomScroll = rememberScrollState()
    val pageScroll = key(page) { rememberScrollState() }
    Box(Modifier.fillMaxSize().testTag("screen_pet"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(if (page != null) pageScroll else roomScroll)
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Spacer(Modifier.height(spacing.xl))
            val current = page
            when {
                !ui.loaded -> Unit
                !ui.pet.adopted -> Adoption(ui, onProposeName, onAdopt)
                current != null -> {
                    PageHeader(current.title) { page = null }
                    Spacer(Modifier.height(spacing.md))
                    PetPageContent(current, ui, onAct)
                }
                else -> PetRoom(ui, message, onAct, onPanel = { panel = it }, onPage = { page = it })
            }
            Spacer(Modifier.height(spacing.xxl))
        }
    }
    panel?.let { p -> PetPanelHost(p, ui, onAct) { panel = null } }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Back") { onBack() }
                .testTag("pet_back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textMain)
        }
        Spacer(Modifier.width(ChoomHubTheme.spacing.sm))
        Text(title, style = MaterialTheme.typography.displaySmall, color = colors.textMain)
    }
}

// --- The room ---

/** How old it is, in words. */
internal fun ageLine(pet: PetState, now: Instant): String {
    val days = PetRules.ageDays(pet, now)
    val age = when {
        days == 0L -> "brand new"
        days == 1L -> "1 day old"
        days < 60 -> "$days days old"
        days < 730 -> "${days / 30} months old"
        else -> "${days / 365} years old"
    }
    return if (PetRules.isKitten(pet, now)) "kitten · $age" else age
}

private val NeedColors = mapOf(
    PetNeed.Hunger to Color(0xFFE0866A),
    PetNeed.Thirst to Color(0xFF8FB3C4),
    PetNeed.Energy to Color(0xFFE6D6AE),
    PetNeed.Litter to Color(0xFF9DB08E),
    PetNeed.Fun to Color(0xFFC49BD6),
    PetNeed.Affection to Color(0xFFE39AB0),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetRoom(ui: PetUi, message: PetMessage?, onAct: (PetAction) -> PetOutcome, onPanel: (PetPanel) -> Unit, onPage: (PetPage) -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val pet = ui.pet
    val name = ui.catName()
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.displaySmall, color = colors.textMain, modifier = Modifier.testTag("pet_name"))
            Text(ageLine(pet, ui.now), style = ChoomHubTextStyles.Handwritten, color = colors.pink)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${pet.myCoins}", style = MaterialTheme.typography.headlineSmall, color = colors.candle, modifier = Modifier.testTag("pet_coins"))
            Text("COINS", style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint)
        }
    }
    Spacer(Modifier.height(spacing.sm))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        pet.traits.forEach { PillBadge(it.label, color = colors.lavender) }
        if (pet.streak > 1) PillBadge("${pet.streak}-day streak", color = colors.candle, showDot = true)
        if (PetRules.isChonky(pet)) PillBadge("chonky", color = colors.tomato)
    }
    Spacer(Modifier.height(spacing.md))
    PetApartment(pet, ui.now, Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)) { onPanel(PetPanel.Pet) }
    Spacer(Modifier.height(spacing.sm))
    Text(PetRules.status(pet, ui.now), style = ChoomHubTextStyles.Handwritten, color = colors.textMain, modifier = Modifier.testTag("pet_status"))
    Text(
        message?.text ?: " ",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.candle,
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag("pet_message"),
    )
    HelpBanner(ui, onAct)
    Needs(pet, ui.now)
    Spacer(Modifier.height(spacing.lg))
    val careDue = pet.sick || pet.muddy || pet.nailsAt == null || Duration.between(pet.nailsAt, ui.now) >= PetRules.NAILS_EVERY
    val actions: List<Triple<String, ImageVector, () -> Unit>> = listOf(
        Triple("Feed", PetIcons.Bowl) { onPanel(PetPanel.Feed) },
        Triple("Water", PetIcons.Drop) { onPanel(PetPanel.Water) },
        Triple("Litter", PetIcons.Scoop) { onPanel(PetPanel.Litter) },
        Triple("Pet", PetIcons.Heart) { onPanel(PetPanel.Pet) },
        Triple("Brush", PetIcons.Comb) { onPanel(PetPanel.Brush) },
        Triple("Play", PetIcons.Wand) { onPanel(PetPanel.Play) },
        Triple("Care", PetIcons.Cross) { onPanel(PetPanel.Care) },
        Triple("Wardrobe", PetIcons.Hanger) { onPage(PetPage.Wardrobe) },
    )
    // Every care and page button shares one label size.
    val allLabels = actions.map { it.first } + PetPage.entries.map { it.title }
    actions.chunked(4).forEach { row ->
        GestureRow(allLabels, tiles = 4) { labelStyle ->
            row.forEach { (label, icon, onClick) ->
                GestureButton(
                    label = label,
                    labelStyle = labelStyle,
                    icon = icon,
                    tint = if (label == "Care" && careDue) colors.tomato else colors.lavender,
                    onClick = onClick,
                    modifier = Modifier.weight(1f).testTag("pet_action_${label.lowercase()}"),
                )
            }
        }
        Spacer(Modifier.height(spacing.sm))
    }
    Spacer(Modifier.height(spacing.md))
    ChoomHubCard(Modifier.fillMaxWidth(), tone = CardTone.Calm) {
        Text("BOND", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.xs))
        BondLine("You", pet.myBond)
        BondLine(ui.partnerName, pet.partnerBond)
    }
    Spacer(Modifier.height(spacing.md))
    GestureRow(listOf("Feed", "Water", "Litter", "Pet", "Brush", "Play", "Care", "Wardrobe") + PetPage.entries.map { it.title }, tiles = 4) { labelStyle ->
        listOf(
            Triple(PetPage.Shop, PetIcons.Bag, null),
            Triple(PetPage.Diary, PetIcons.Book, ui.diary.size.takeIf { it > 0 }),
            Triple(PetPage.Gifts, PetIcons.Gem, pet.gifts.size.takeIf { it > 0 }),
            Triple(PetPage.Trophies, PetIcons.Trophy, PetRules.TROPHIES.count { it.unlocked(pet, ui.now) }.takeIf { it > 0 }),
        ).forEach { (p, icon, count) ->
            GestureButton(
                label = p.title,
                count = count,
                labelStyle = labelStyle,
                icon = icon,
                tint = colors.pink,
                onClick = { onPage(p) },
                modifier = Modifier.weight(1f).testTag("pet_page_${p.name.lowercase()}"),
            )
        }
    }
}

@Composable
private fun BondLine(who: String, points: Int) {
    val colors = ChoomHubTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(who, style = MaterialTheme.typography.bodyMedium, color = colors.textMain, modifier = Modifier.weight(1f))
        Text(PetRules.bondLabel(points), style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    }
}

@Composable
private fun Needs(pet: PetState, now: Instant) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val levels = PetRules.levels(pet, now)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PetNeed.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                pair.forEach { need ->
                    val v = levels.getValue(need)
                    val tint = NeedColors.getValue(need)
                    Column(Modifier.weight(1f).testTag("need_${need.key}")) {
                        Row {
                            Text(need.label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = if (v < 20f) colors.tomato else colors.textSecondary, modifier = Modifier.weight(1f))
                            Text("${v.toInt()}", style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint)
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.surfaceHighest)) {
                            Box(Modifier.fillMaxWidth(v / 100f).height(6.dp).clip(CircleShape).background(if (v < 20f) colors.tomato else tint))
                        }
                    }
                }
            }
        }
    }
}

/** When one of you has started a two-person job (vet, bath). */
@Composable
private fun HelpBanner(ui: PetUi, onAct: (PetAction) -> PetOutcome) {
    val pending = ui.pet.pending?.takeIf { PetRules.pendingOpen(it, ui.now) } ?: return
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    ChoomHubCard(Modifier.fillMaxWidth().testTag("pet_help_banner"), tone = CardTone.Glowing, glowColor = colors.candle) {
        if (pending.byMe) {
            Text("Waiting for ${ui.partnerName} to help with ${pending.kind.label}.", style = MaterialTheme.typography.bodyMedium, color = colors.textMain)
            val left = PetRules.TEAM_CARE_EXPIRES.minus(Duration.between(pending.at, ui.now))
            Text("They've been sent a notification. This lapses in ${left.toHours() + 1} h.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        } else {
            Text("${ui.partnerName} needs your help with ${pending.kind.label}.", style = MaterialTheme.typography.bodyMedium, color = colors.textMain)
            Spacer(Modifier.height(spacing.sm))
            ChoomHubPrimaryButton("Help", { onAct(PetAction.JoinTeamCare) }, Modifier.fillMaxWidth().testTag("pet_help_join"))
        }
    }
    Spacer(Modifier.height(spacing.md))
}

// --- Adoption ---

@Composable
private fun Adoption(ui: PetUi, onProposeName: (String) -> Unit, onAdopt: (String) -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val proposed = ui.pet.proposedName
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var confirmAlone by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("pet_adoption"), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Pet House", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
        Text("someone's at the door", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
        Spacer(Modifier.height(spacing.lg))
        KittenInABox(Modifier.size(200.dp))
        Spacer(Modifier.height(spacing.md))
        Text(
            "A small black kitten turned up and has no plans to leave. It needs a name: one of you suggests it, the other agrees. The day you agree is its birthday.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.xl))
        when {
            proposed == null || editing -> {
                ChoomHubTextField(
                    value = name,
                    onValueChange = { name = it.take(PetRules.MAX_NAME) },
                    label = "Name",
                    supportingText = "${name.trim().length}/${PetRules.MAX_NAME}",
                    modifier = Modifier.fillMaxWidth().testTag("pet_name_field"),
                    onDone = {
                        if (PetRules.cleanName(name) != null) {
                            onProposeName(name)
                            editing = false
                        }
                    },
                )
                Spacer(Modifier.height(spacing.md))
                ChoomHubPrimaryButton(
                    "Suggest this name",
                    {
                        onProposeName(name)
                        editing = false
                    },
                    Modifier.fillMaxWidth().testTag("pet_propose"),
                    enabled = PetRules.cleanName(name) != null,
                )
                if (editing) {
                    Spacer(Modifier.height(spacing.sm))
                    ChoomHubSecondaryButton("Never mind", { editing = false }, Modifier.fillMaxWidth())
                }
            }
            ui.pet.proposedByMe -> {
                Text("You suggested “$proposed”.", style = MaterialTheme.typography.titleLarge, color = colors.textMain, textAlign = TextAlign.Center)
                Text("Waiting for ${ui.partnerName} to agree.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(spacing.lg))
                ChoomHubSecondaryButton("Suggest a different name", {
                    name = proposed
                    editing = true
                }, Modifier.fillMaxWidth().testTag("pet_rename"))
                Spacer(Modifier.height(spacing.sm))
                ChoomHubSecondaryButton("Bring it home anyway", { confirmAlone = true }, Modifier.fillMaxWidth().testTag("pet_adopt_anyway"))
            }
            else -> {
                Text("${ui.partnerName} wants to call it “$proposed”.", style = MaterialTheme.typography.titleLarge, color = colors.textMain, textAlign = TextAlign.Center)
                Spacer(Modifier.height(spacing.lg))
                ChoomHubPrimaryButton("Agree and bring it home", { onAdopt(proposed) }, Modifier.fillMaxWidth().testTag("pet_agree"))
                Spacer(Modifier.height(spacing.sm))
                ChoomHubSecondaryButton("Suggest another name", { editing = true }, Modifier.fillMaxWidth().testTag("pet_rename"))
            }
        }
    }
    if (confirmAlone && proposed != null) {
        InkDialog(
            title = "Bring $proposed home?",
            message = "${ui.partnerName} hasn't agreed yet. The name stays for good, and today becomes its birthday.",
            confirm = "Bring it home",
            onConfirm = {
                confirmAlone = false
                onAdopt(proposed)
            },
            dismissive = "Wait for them",
            onDismissive = { confirmAlone = false },
            onDismissRequest = { confirmAlone = false },
            modifier = Modifier.testTag("pet_adopt_confirm"),
        ) {
            PetCat(PetPose.Sit, Modifier.size(90.dp), expression = CatExpression.Hopeful, build = PetBuild.Kitten)
        }
    }
}

/** The kitten, peeking out of a cardboard box. */
@Composable
private fun KittenInABox(modifier: Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawOval(Color(0xFF15111A).copy(alpha = 0.5f), Offset(w * 0.12f, w * 0.88f), Size(w * 0.76f, w * 0.08f))
            // Back flaps.
            drawRect(Color(0xFF6E5438), Offset(w * 0.18f, w * 0.5f), Size(w * 0.64f, w * 0.06f))
        }
        PetCat(PetPose.Sit, Modifier.size(150.dp).align(Alignment.TopCenter).padding(top = 6.dp), expression = CatExpression.Hopeful, build = PetBuild.Kitten)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawRect(Color(0xFF8C6F4E), Offset(w * 0.16f, w * 0.56f), Size(w * 0.68f, w * 0.34f))
            drawRect(Color(0xFF7A5E40), Offset(w * 0.16f, w * 0.56f), Size(w * 0.68f, w * 0.04f))
            val flap = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.16f, w * 0.56f); lineTo(w * 0.04f, w * 0.48f); lineTo(w * 0.1f, w * 0.6f); close()
            }
            drawPath(flap, Color(0xFF7A5E40))
            val flap2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.84f, w * 0.56f); lineTo(w * 0.96f, w * 0.48f); lineTo(w * 0.9f, w * 0.6f); close()
            }
            drawPath(flap2, Color(0xFF7A5E40))
            // "FREE" scrawled on it.
            drawLine(Color(0xFF15111A).copy(alpha = 0.6f), Offset(w * 0.34f, w * 0.7f), Offset(w * 0.66f, w * 0.7f), 2f)
            drawLine(Color(0xFF15111A).copy(alpha = 0.4f), Offset(w * 0.38f, w * 0.76f), Offset(w * 0.6f, w * 0.76f), 2f)
        }
    }
}
