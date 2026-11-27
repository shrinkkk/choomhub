package com.choomhub.app.feature.bounty

import com.choomhub.app.core.designsystem.component.TabLabel
import com.choomhub.app.core.designsystem.component.OneLineText
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.Bounty
import com.choomhub.app.domain.model.BountyDue
import com.choomhub.app.domain.model.BountyRules
import com.choomhub.app.domain.model.BountyStatus
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.BountyRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.feature.pet.PetIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

@Serializable
data object BountyBoardRoute

fun NavGraphBuilder.bountyBoardScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<BountyBoardRoute> { BountyBoardScreen(contentPadding, onBack) }
}

data class BountyBoardUi(
    val loaded: Boolean = false,
    val bounties: List<Bounty> = emptyList(),
    val me: String = "You",
    val partner: String = "them",
) {
    val forMe: List<Bounty> get() = bounties.filter { !it.byMe && it.status != BountyStatus.Paid }
    val forThem: List<Bounty> get() = bounties.filter { it.byMe && it.status != BountyStatus.Paid }
    val done: List<Bounty> get() = bounties.filter { it.status == BountyStatus.Paid }

    /** Waiting on you: their jobs to do, and yours to pay. */
    val waitingOnMe: Int get() = forMe.count { it.status == BountyStatus.Open } + forThem.count { it.status == BountyStatus.Claimed }
}

@HiltViewModel
class BountyViewModel @Inject constructor(
    private val repository: BountyRepository,
    private val notifier: Notifier,
    profile: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {
    val state: StateFlow<BountyBoardUi> = combine(repository.observe(), profile.observeNames()) { all, names ->
        BountyBoardUi(true, BountyRules.shown(all, clock.instant()), names.myName ?: "You", names.partnerName ?: "them")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BountyBoardUi())

    fun lapsed(b: Bounty): Boolean = BountyRules.lapsed(b, clock.instant(), ZoneId.systemDefault())

    fun post(task: String, reward: Int, due: BountyDue): Boolean {
        val clean = BountyRules.cleanTask(task) ?: return false
        viewModelScope.launch {
            repository.post(clean, reward, due)
            notifier.notifyPartner(PushEvent.Bounty, detail = "$clean · $reward points", kind = "posted")
        }
        return true
    }

    fun claim(b: Bounty) = viewModelScope.launch {
        repository.claim(b)
        notifier.notifyPartner(PushEvent.Bounty, detail = b.task, kind = "claimed")
    }

    fun notYet(b: Bounty) = viewModelScope.launch {
        repository.notYet(b)
        notifier.notifyPartner(PushEvent.Bounty, detail = b.task, kind = "not_yet")
    }

    fun pay(b: Bounty) = viewModelScope.launch {
        repository.pay(b)
        notifier.notifyPartner(PushEvent.Bounty, detail = "${b.task} · ${b.reward} points", kind = "paid")
    }

    fun takeDown(b: Bounty) = viewModelScope.launch { repository.takeDown(b) }
}

private enum class BoardTab { ForMe, ForThem, Done }

@Composable
fun BountyBoardScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: BountyViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    BountyBoardContent(
        ui = ui,
        contentPadding = contentPadding,
        lapsed = viewModel::lapsed,
        onBack = onBack,
        onPost = viewModel::post,
        onClaim = { viewModel.claim(it) },
        onNotYet = { viewModel.notYet(it) },
        onPay = { viewModel.pay(it) },
        onTakeDown = { viewModel.takeDown(it) },
    )
}

@Composable
internal fun BountyBoardContent(
    ui: BountyBoardUi,
    contentPadding: PaddingValues,
    lapsed: (Bounty) -> Boolean,
    onBack: () -> Unit,
    onPost: (String, Int, BountyDue) -> Boolean,
    onClaim: (Bounty) -> Unit,
    onNotYet: (Bounty) -> Unit,
    onPay: (Bounty) -> Unit,
    onTakeDown: (Bounty) -> Unit,
) {
    val colors = ChoomHubTheme.colors
    var tab by rememberSaveable { mutableStateOf(BoardTab.ForMe) }
    var posting by rememberSaveable { mutableStateOf(false) }
    var paying by rememberSaveable { mutableStateOf<String?>(null) }
    if (posting) {
        var task by rememberSaveable { mutableStateOf("") }
        var reward by rememberSaveable { mutableIntStateOf(3) }
        var due by rememberSaveable { mutableStateOf(BountyDue.Tonight.label) }
        InkPanel({ posting = false }, Modifier.testTag("bounty_post")) {
            PostBountyContent(
                task = task,
                onTask = { task = it.take(BountyRules.MAX_TASK) },
                reward = reward,
                onReward = { reward = it.coerceIn(BountyRules.MIN_REWARD, BountyRules.MAX_REWARD) },
                due = due,
                onDue = { due = it },
                partner = ui.partner,
                onPost = {
                    if (onPost(task, reward, BountyDue.entries.first { it.label == due })) {
                        posting = false
                        tab = BoardTab.ForThem
                    }
                },
                onClose = { posting = false },
            )
        }
    }
    ui.bounties.firstOrNull { it.id == paying }?.let { b ->
        InkPanel({ paying = null }, Modifier.testTag("bounty_pay_confirm")) {
            Text("${ui.partner} says it's done", style = MaterialTheme.typography.headlineMedium, color = colors.textMain, textAlign = TextAlign.Center)
            Text("“${b.task}”", style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            ChoomHubPrimaryButton("Pay ${b.reward} points", { onPay(b); paying = null }, Modifier.fillMaxWidth().testTag("dialog_confirm"))
            TextButton({ onNotYet(b); paying = null }, Modifier.fillMaxWidth()) {
                OneLineText("NOT YET", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
    Box(Modifier.fillMaxSize().boardPlanks().testTag("screen_bounty_board"), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).testTag("bounty_back"),
                    contentAlignment = Alignment.Center,
                ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.bone) }
            }
            Text("Bounty board", style = MaterialTheme.typography.displaySmall, color = colors.bone)
            Text("jobs for each other, paid in points", style = ChoomHubTextStyles.Handwritten, color = colors.candle)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(BoardTab.ForMe, "For you", ui.forMe.size),
                    Triple(BoardTab.ForThem, "For ${ui.partner}", ui.forThem.size),
                    Triple(BoardTab.Done, "Paid", 0),
                ).forEach { (t, label, count) ->
                    val on = t == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (on) colors.pink.copy(alpha = 0.18f) else colors.night.copy(alpha = 0.5f))
                            .clickable(role = Role.Tab) { tab = t }
                            .padding(vertical = 8.dp)
                            .testTag("bounty_tab_${t.name}"),
                        contentAlignment = Alignment.Center,
                    ) { TabLabel(label, count, if (on) colors.pink else colors.textSecondary) }
                }
            }
            Spacer(Modifier.height(20.dp))
            val shown = when (tab) {
                BoardTab.ForMe -> ui.forMe
                BoardTab.ForThem -> ui.forThem
                BoardTab.Done -> ui.done
            }
            if (shown.isEmpty()) {
                Text(
                    when (tab) {
                        BoardTab.ForMe -> "Nothing for you right now."
                        BoardTab.ForThem -> "Nothing pinned up for ${ui.partner}. Post one."
                        BoardTab.Done -> "Paid bounties stay up for a few days."
                    },
                    style = ChoomHubTextStyles.Handwritten,
                    color = colors.bone.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
            shown.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEachIndexed { i, b ->
                        val state = when {
                            b.status == BountyStatus.Paid -> BountyState.Paid
                            b.status == BountyStatus.Claimed -> BountyState.Claimed
                            lapsed(b) -> BountyState.Lapsed
                            else -> BountyState.Open
                        }
                        BountyNotice(
                            BountyUi(b.id, b.task, b.reward, forMe = !b.byMe, due = b.due.noticeLabel, state = state),
                            partner = ui.partner,
                            me = ui.me,
                            angle = if ((row + i) % 2 == 0) -2.5f else 2f,
                            onDone = { onClaim(b) },
                            onPay = { paying = b.id },
                            onNotYet = { onNotYet(b) },
                            onTakeDown = { onTakeDown(b) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
            }
            Spacer(Modifier.height(16.dp))
            PlankButton("Post a bounty", { posting = true }, Modifier.align(Alignment.CenterHorizontally).testTag("bounty_new"))
            Spacer(Modifier.height(24.dp))
        }
    }
}
