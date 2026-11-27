package com.choomhub.app.feature.settings

import com.choomhub.app.core.designsystem.component.OneLineText
import com.choomhub.app.core.designsystem.icon.PointIcons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.domain.model.FocusRate
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** A suggested new rate waiting on the other one. */
data class FocusRateProposal(val rate: Int, val byMe: Boolean)

/** How focus points are given, in words, for a rate. */
fun focusPointsRules(rate: Int): List<String> = com.choomhub.app.domain.model.FocusRules.rateRules(rate)

/** Settings → Focus points: the agreed rate, and suggesting or answering a change. */
@HiltViewModel
class FocusPointsViewModel @Inject constructor(
    private val focus: FocusRepository,
    private val notifier: Notifier,
    profile: ProfileRepository,
) : ViewModel() {
    val rate: StateFlow<FocusRate> = focus.observeRoom().map { it.rate }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusRate())
    val partner: StateFlow<String> = profile.observeNames().map { it.partnerName ?: "them" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "them")

    fun suggest(rate: Int) = viewModelScope.launch {
        focus.proposeRate(rate)
        notifier.notifyPartner(PushEvent.FocusRate, detail = rate.toString(), kind = "suggest")
    }

    fun answer(agree: Boolean) = viewModelScope.launch {
        val proposed = rate.value.proposed ?: return@launch
        focus.answerRate(agree)
        notifier.notifyPartner(PushEvent.FocusRate, detail = proposed.toString(), kind = if (agree) "agreed" else "declined")
    }

    fun takeBack() = viewModelScope.launch { focus.takeBackRate() }
}

/** The Customise row that opens the focus-points pop-up. */
@Composable
fun FocusPointsRow(viewModel: FocusPointsViewModel = hiltViewModel()) {
    val rate by viewModel.rate.collectAsStateWithLifecycle()
    val partner by viewModel.partner.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(rate.rate) { mutableIntStateOf(rate.rate) }
    SettingsRow(
        title = "Focus points",
        value = when {
            rate.proposed != null && !rate.proposedByMe -> "$partner suggested ${rate.proposed}. Tap to answer."
            rate.proposed != null -> "You suggested ${rate.proposed}; waiting for $partner."
            rate.rate == 0 -> "Off"
            else -> "${rate.rate} an hour, ${rate.rate * 2} for together time"
        },
        badge = if (rate.proposed != null && !rate.proposedByMe) "answer" else "change",
        badgeColor = if (rate.proposed != null && !rate.proposedByMe) ChoomHubTheme.colors.candle else ChoomHubTheme.colors.lavender,
        divider = false,
        onClick = { open = true },
        modifier = Modifier.testTag("settings_focus_points"),
    )
    if (open) {
        InkPanel({ open = false }, Modifier.testTag("focus_points_panel")) {
            FocusPointsContent(
                current = rate.rate,
                draft = draft,
                onDraft = { draft = it },
                partner = partner,
                pending = rate.proposed?.let { FocusRateProposal(it, rate.proposedByMe) },
                onSuggest = { viewModel.suggest(draft) },
                onAgree = { viewModel.answer(true); open = false },
                onDecline = { if (rate.proposedByMe) viewModel.takeBack() else viewModel.answer(false) },
                onClose = { open = false },
            )
        }
    }
}

/** The pop-up for points per focus hour: a stepper, how it works, and the suggestion state. */
@Composable
fun ColumnScope.FocusPointsContent(
    current: Int,
    draft: Int,
    onDraft: (Int) -> Unit,
    partner: String,
    pending: FocusRateProposal?,
    onSuggest: () -> Unit,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    GothicMotifArt(GothicMotif.Candle, Modifier.size(64.dp))
    Spacer(Modifier.height(spacing.sm))
    Text("Focus points", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
    Text("per hour in the focus room", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    Spacer(Modifier.height(spacing.lg))
    if (pending != null && !pending.byMe) {
        Text("$partner suggested ${pending.rate} per hour (now $current).", style = MaterialTheme.typography.bodyMedium, color = colors.candle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.md))
        RulesBox(pending.rate)
        Spacer(Modifier.height(spacing.lg))
        ChoomHubPrimaryButton("Agree", onAgree, Modifier.fillMaxWidth().testTag("focus_rate_agree"))
        TextButton(onDecline, Modifier.fillMaxWidth()) { OneLineText("NO, KEEP $current", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        StepButton(PointIcons.Minus, "Less", enabled = draft > 0 && pending == null) { onDraft(draft - 1) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$draft", style = MaterialTheme.typography.displayMedium, color = if (draft == 0) colors.textFaint else colors.candle, modifier = Modifier.testTag("focus_rate_value"))
            Text(if (draft == 0) "OFF" else "PER HOUR", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        }
        StepButton(PointIcons.Plus, "More", enabled = draft < 10 && pending == null) { onDraft(draft + 1) }
    }
    Spacer(Modifier.height(spacing.lg))
    RulesBox(draft)
    Spacer(Modifier.height(spacing.lg))
    when {
        pending != null -> {
            Text("You suggested ${pending.rate}. Waiting for $partner to agree.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
            TextButton(onDecline, Modifier.fillMaxWidth()) { OneLineText("TAKE IT BACK", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
        }
        else -> {
            ChoomHubPrimaryButton("Suggest $draft", onSuggest, Modifier.fillMaxWidth().testTag("focus_rate_suggest"), enabled = draft != current)
            Text("Changes once $partner agrees.", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.padding(top = spacing.xs))
        }
    }
    TextButton(onClose, Modifier.fillMaxWidth()) { OneLineText("CLOSE", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
}

@Composable
private fun RulesBox(rate: Int) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surface)
            .padding(spacing.md),
    ) {
        Text("HOW IT WORKS", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.xs))
        focusPointsRules(rate).forEach { line ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text("•", style = MaterialTheme.typography.bodySmall, color = colors.pink, modifier = Modifier.width(14.dp))
                Text(line, style = MaterialTheme.typography.bodySmall, color = colors.textMain)
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, colors.lavender.copy(alpha = if (enabled) 0.5f else 0.12f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = label, tint = if (enabled) colors.textMain else colors.textFaint, modifier = Modifier.size(20.dp)) }
}
