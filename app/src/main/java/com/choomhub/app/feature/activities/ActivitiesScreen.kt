package com.choomhub.app.feature.activities

import com.choomhub.app.core.designsystem.component.OneLineText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.feature.bounty.BountyBoardUi
import com.choomhub.app.feature.bounty.BountyViewModel
import com.choomhub.app.feature.focus.RoomPerson
import com.choomhub.app.feature.focus.RoomScene
import com.choomhub.app.feature.focus.Spot
import com.choomhub.app.feature.focus.roomScene
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.feature.shop.ShopUi
import com.choomhub.app.feature.shop.ShopViewModel
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@Serializable
data object ActivitiesRoute

fun NavGraphBuilder.activitiesScreen(
    contentPadding: PaddingValues,
    onOpenFocusRoom: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    onOpenBounties: () -> Unit = {},
) {
    composable<ActivitiesRoute> { ActivitiesScreen(contentPadding, onOpenFocusRoom, onOpenShop, onOpenBounties) }
}

/** The focus room card: who's in, and this week's hours. */
data class FocusCardUi(
    val loaded: Boolean = false,
    val imIn: Boolean = false,
    val partnerLine: String = "",
    val weekLine: String = "",
    val partnerIn: Boolean = false,
    /** The room as it is, seen through the doors. */
    val scene: RoomScene = RoomScene(
        RoomPerson("You", Spot.Away, "--:--", weekHours = "0.0", totalHours = "0.0"),
        RoomPerson("Them", Spot.Away, "--:--", weekHours = "0.0", totalHours = "0.0"),
        together = false,
    ),
    val title: String = "Nobody's in",
    /** For the "Go in?" pop-up: what's going on inside. */
    val inside: String = "",
    val rateLine: String = "",
)

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    focusRepository: FocusRepository,
    profileRepository: ProfileRepository,
    ticker: SecondTicker,
    clock: Clock,
) : ViewModel() {
    val focus: StateFlow<FocusCardUi> = combine(
        focusRepository.observeRoom(),
        profileRepository.observeNames(),
        ticker.ticks().onStart { emit(clock.instant()) },
    ) { room: FocusRoomData, names, now ->
        val them = names.partnerName ?: "They"
        val today = LocalDate.now(clock)
        val rate = room.rate.rate
        FocusCardUi(
            loaded = true,
            imIn = room.me.state != FocusState.Away,
            partnerIn = room.partner.state != FocusState.Away,
            partnerLine = when (room.partner.state) {
                FocusState.Focusing -> "$them is focusing · ${FocusRules.stopwatch(FocusRules.sessionMs(room.partner, now))}"
                FocusState.OnBreak -> "$them is on a break by the fire"
                FocusState.Away -> "$them isn't in right now"
            },
            weekLine = "This week: you ${FocusRules.hours(room.myTotals.weekMsAt(today))} h · $them ${FocusRules.hours(room.partnerTotals.weekMsAt(today))} h",
            scene = roomScene(room, names, now, today),
            title = when {
                room.me.state != FocusState.Away -> "You're inside"
                room.partner.state == FocusState.Focusing -> "$them is inside"
                room.partner.state == FocusState.OnBreak -> "$them is by the fire"
                else -> "Nobody's in"
            },
            inside = when (room.partner.state) {
                FocusState.Focusing -> "$them has been at their desk for ${FocusRules.duration(FocusRules.sessionMs(room.partner, now))}."
                FocusState.OnBreak -> "$them is taking a break by the fire."
                FocusState.Away -> "Nobody's in yet. The desks are waiting."
            },
            rateLine = if (rate == 0) "Focus points are off" else "$rate point${if (rate == 1) "" else "s"} an hour · ${rate * 2} when you're both in",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusCardUi())
}

@Composable
fun ActivitiesScreen(
    contentPadding: PaddingValues,
    onOpenFocusRoom: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenBounties: () -> Unit = {},
    viewModel: ActivitiesViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel(),
    bountyViewModel: BountyViewModel = hiltViewModel(),
) {
    val card by viewModel.focus.collectAsStateWithLifecycle()
    val shop by shopViewModel.state.collectAsStateWithLifecycle()
    val bounties by bountyViewModel.state.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    var askingToEnter by rememberSaveable { mutableStateOf(false) }
    if (askingToEnter) {
        GoInDialog(card, onGoIn = { askingToEnter = false; onOpenFocusRoom() }, onDismiss = { askingToEnter = false })
    }
    Box(Modifier.fillMaxSize().testTag("screen_activities"), contentAlignment = Alignment.TopCenter) {
        GothicMotifArt(
            GothicMotif.SkeletonKey,
            Modifier.align(Alignment.TopEnd).padding(top = contentPadding.calculateTopPadding() + 12.dp, end = 12.dp).size(64.dp),
        )
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Spacer(Modifier.height(spacing.xxl))
            Text("Activities", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
            Text("a few minutes a day", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(spacing.xl))
            // The doors are the button. Already inside: straight back in, no question.
            ArtCard(
                eyebrow = "FOCUS ROOM",
                title = card.title,
                lines = listOfNotNull(if (card.loaded) card.partnerLine else null, if (card.loaded) card.weekLine else null),
                highlight = card.partnerIn,
                onClick = { if (card.imIn) onOpenFocusRoom() else askingToEnter = true },
                tag = "focus_enter",
            ) { FocusDoors(card.scene, inside = card.partnerIn || card.imIn, Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(spacing.xl))
            ShopCard(shop, onOpenShop)
            Spacer(Modifier.height(spacing.xl))
            BountyCard(bounties, onOpenBounties)
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

/** A card whose top half is a picture you tap. */
@Composable
private fun ArtCard(
    eyebrow: String,
    title: String,
    lines: List<String>,
    highlight: Boolean,
    onClick: () -> Unit,
    tag: String,
    art: @Composable () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    ChoomHubCard(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(role = Role.Button, onClickLabel = title, onClick = onClick).testTag(tag),
        tone = if (highlight) CardTone.Glowing else CardTone.Raised,
        glowColor = colors.candle,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)) { art() }
        Column(Modifier.padding(16.dp)) {
            Text(eyebrow, style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textMain)
            lines.forEachIndexed { i, line ->
                Text(line, style = MaterialTheme.typography.bodySmall, color = if (i == 0) colors.candle else colors.textFaint)
            }
        }
    }
}

/** "Go in?": what's going on inside, and what going in does. */
@Composable
private fun GoInDialog(card: FocusCardUi, onGoIn: () -> Unit, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    InkPanel(onDismiss, Modifier.testTag("focus_go_in_dialog")) {
        Box(Modifier.fillMaxWidth(0.6f).clip(MaterialTheme.shapes.medium)) { FocusDoors(card.scene, inside = card.partnerIn, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(14.dp))
        Text("Go in?", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text(card.inside, style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Do Not Disturb comes on until you leave (alarms still ring). ${card.rateLine}.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        ChoomHubPrimaryButton("Go in", onGoIn, Modifier.fillMaxWidth().testTag("focus_go_in"))
        TextButton(onDismiss, Modifier.fillMaxWidth()) { OneLineText("NOT NOW", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
    }
}

/** The points shop: a tent with the cat behind the counter. Straight in, no questions. */
@Composable
private fun ShopCard(shop: ShopUi, onOpen: () -> Unit) {
    val theirTurn = shop.purchases.count { !it.byMe && it.state == PurchaseState.Used }
    val ready = shop.purchases.count { it.byMe && it.state == PurchaseState.Owned }
    val toAgree = shop.proposals.count { !it.byMe }
    val waiting = theirTurn > 0 || toAgree > 0
    val notes = buildList {
        if (theirTurn > 0) add("${shop.partnerName} cashed in $theirTurn")
        if (toAgree > 0) add("$toAgree change${if (toAgree == 1) "" else "s"} to agree to")
        if (ready > 0) add("$ready ready to use")
    }
    ArtCard(
        eyebrow = "POINTS SHOP",
        title = "Spend what you've earned",
        lines = listOf((listOf("You have ${shop.myPoints} ${shop.myPointsLabel}") + notes).joinToString(" \u00B7 ")),
        highlight = waiting,
        onClick = onOpen,
        tag = "shop_open",
    ) { ShopTent(Modifier.fillMaxWidth()) }
}

/** The bounty board: jobs for each other, paid in points. */
@Composable
private fun BountyCard(ui: BountyBoardUi, onOpen: () -> Unit) {
    val open = ui.bounties.count { it.status != com.choomhub.app.domain.model.BountyStatus.Paid }
    val toPay = ui.forThem.count { it.status == com.choomhub.app.domain.model.BountyStatus.Claimed }
    val toDo = ui.forMe.count { it.status == com.choomhub.app.domain.model.BountyStatus.Open }
    ArtCard(
        eyebrow = "BOUNTY BOARD",
        title = "Jobs, for points",
        lines = listOf(
            buildList {
                add(if (open == 0) "Nothing pinned up yet" else "$open up")
                if (toDo > 0) add("$toDo for you")
                if (toPay > 0) add("$toPay waiting for you to pay")
            }.joinToString(" \u00B7 "),
        ),
        highlight = ui.waitingOnMe > 0,
        onClick = onOpen,
        tag = "bounty_card",
    ) { BountyBoardArt(open = open, Modifier.fillMaxWidth()) }
}
