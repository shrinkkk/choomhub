package com.choomhub.app.feature.activities

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

fun NavGraphBuilder.activitiesScreen(contentPadding: PaddingValues, onOpenFocusRoom: () -> Unit = {}, onOpenShop: () -> Unit = {}) {
    composable<ActivitiesRoute> { ActivitiesScreen(contentPadding, onOpenFocusRoom, onOpenShop) }
}

/** The focus room card: who's in, and this week's hours. */
data class FocusCardUi(
    val loaded: Boolean = false,
    val imIn: Boolean = false,
    val partnerLine: String = "",
    val weekLine: String = "",
    val partnerIn: Boolean = false,
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
    ) { room, names, now ->
        val them = names.partnerName ?: "They"
        val today = LocalDate.now(clock)
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusCardUi())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitiesScreen(
    contentPadding: PaddingValues,
    onOpenFocusRoom: () -> Unit,
    onOpenShop: () -> Unit = {},
    viewModel: ActivitiesViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel(),
) {
    val card by viewModel.focus.collectAsStateWithLifecycle()
    val shop by shopViewModel.state.collectAsStateWithLifecycle()
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
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
            ChoomHubCard(
                modifier = Modifier.fillMaxWidth().testTag("focus_card"),
                tone = if (card.partnerIn) CardTone.Glowing else CardTone.Raised,
                glowColor = colors.candle,
            ) {
                Text("FOCUS ROOM", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                Text("A quiet room for the two of you", style = MaterialTheme.typography.titleLarge, color = colors.textMain)
                Spacer(Modifier.height(spacing.sm))
                if (card.loaded) {
                    Text(card.partnerLine, style = MaterialTheme.typography.bodyMedium, color = if (card.partnerIn) colors.candle else colors.textSecondary, modifier = Modifier.testTag("focus_card_partner"))
                    Text(card.weekLine, style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                }
                Spacer(Modifier.height(spacing.md))
                Text(
                    "Notifications go quiet while you're in. Take breaks by the fire; every hour you focus earns you a point.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(spacing.md))
                ChoomHubPrimaryButton(
                    text = if (card.imIn) "Back to the room" else "Enter the focus room",
                    onClick = onOpenFocusRoom,
                    modifier = Modifier.fillMaxWidth().testTag("focus_enter"),
                )
            }
            Spacer(Modifier.height(spacing.xl))
            ShopCard(shop, onOpenShop)
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

/** The points shop card: your points, and anything waiting on you. */
@Composable
private fun ShopCard(shop: ShopUi, onOpen: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val theirTurn = shop.purchases.count { !it.byMe && it.state == PurchaseState.Used }
    val ready = shop.purchases.count { it.byMe && it.state == PurchaseState.Owned }
    val toAgree = shop.proposals.count { !it.byMe }
    val waiting = theirTurn > 0 || toAgree > 0
    ChoomHubCard(
        modifier = Modifier.fillMaxWidth().testTag("shop_card"),
        tone = if (waiting) CardTone.Glowing else CardTone.Raised,
        glowColor = colors.pink,
    ) {
        Text("POINTS SHOP", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Text("Spend what you've earned", style = MaterialTheme.typography.titleLarge, color = colors.textMain)
        Spacer(Modifier.height(spacing.sm))
        Text("You have ${shop.myPoints} ${shop.myPointsLabel}", style = MaterialTheme.typography.bodyMedium, color = colors.candle, modifier = Modifier.testTag("shop_card_points"))
        val notes = buildList {
            if (theirTurn > 0) add("${shop.partnerName} cashed in $theirTurn")
            if (toAgree > 0) add("$toAgree change${if (toAgree == 1) "" else "s"} to agree to")
            if (ready > 0) add("$ready ready to use")
        }
        if (notes.isNotEmpty()) Text(notes.joinToString(" \u00B7 "), style = MaterialTheme.typography.bodySmall, color = if (waiting) colors.pink else colors.textFaint)
        Spacer(Modifier.height(spacing.md))
        ChoomHubPrimaryButton(text = "Open the shop", onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("shop_open"))
    }
}
