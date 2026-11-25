package com.choomhub.app.feature.focus

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.JamLink
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.CatFace
import kotlinx.serialization.Serializable

@Serializable
data object FocusRoomRoute

fun NavGraphBuilder.focusRoomScreen(contentPadding: PaddingValues, onLeft: () -> Unit) {
    composable<FocusRoomRoute> { FocusRoomScreen(contentPadding, onLeft) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusRoomScreen(contentPadding: PaddingValues, onLeft: () -> Unit, viewModel: FocusRoomViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val soundOn by viewModel.soundOn.collectAsStateWithLifecycle()
    val dndAllowed by viewModel.dndAllowed.collectAsStateWithLifecycle()
    val askDnd by viewModel.askAboutDnd.collectAsStateWithLifecycle()
    val dismissedEnd by viewModel.dismissedBreakEnd.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = ChoomHubTheme.colors
    var pickingBreak by rememberSaveable { mutableStateOf(false) }
    var showingJam by rememberSaveable { mutableStateOf(false) }
    var confirmingLeave by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    val openDndSettings = {
        runCatching { context.startActivity(viewModel.dndSettingsIntent()) }
        Unit
    }

    if (askDnd && ui.loaded) {
        InkDialog(
            title = "Mute everything?",
            message = "While you're in the focus room, ChoomHub can turn on Do Not Disturb (alarms, like the end of your break, still ring) and turn it back off when you leave. Android asks you to allow this once.",
            confirm = "Allow it",
            onConfirm = {
                viewModel.dndAsked()
                openDndSettings()
            },
            dismissive = "Not now",
            onDismissive = viewModel::dndAsked,
            onDismissRequest = viewModel::dndAsked,
            modifier = Modifier.testTag("focus_dnd_ask"),
        ) { CatFace(CatExpression.Hopeful, Modifier.size(88.dp)) }
    }

    if (pickingBreak) {
        BreakPicker(
            onPick = { minutes ->
                pickingBreak = false
                viewModel.takeBreak(minutes)
            },
            onDismiss = { pickingBreak = false },
        )
    }

    val left = ui.myBreakLeftMs
    if (ui.me.state == FocusState.OnBreak && left != null && left < 0 && dismissedEnd != ui.me.breakEndsAt) {
        BreakOver(
            overBy = FocusRules.countdown(left),
            onBack = viewModel::backToFocus,
            onExtend = viewModel::extendBreak,
            onLeave = { viewModel.leave(onLeft) },
            onDismiss = viewModel::dismissBreakOver,
        )
    }

    val summary = ui.summary
    if (confirmingLeave && summary != null) {
        LeaveSummary(
            summary = summary,
            onStay = { confirmingLeave = false },
            onLeave = {
                confirmingLeave = false
                viewModel.leave(onLeft)
            },
        )
    }

    if (showingJam) {
        JamPanel(
            jam = ui.jam,
            partnerName = ui.partnerName,
            onLeave = viewModel::setJam,
            onTakeOut = { viewModel.clearJam() },
            onJoin = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: ActivityNotFoundException) {
                    // No app or browser can open it; nothing to do.
                }
            },
            onDismiss = { showingJam = false },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding())
            .testTag("screen_focus_room"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val onBreak = ui.me.state == FocusState.OnBreak
                ChoomHubPrimaryButton(
                    text = if (onBreak) "Back to focus" else "Take a break",
                    onClick = { if (onBreak) viewModel.backToFocus() else pickingBreak = true },
                    enabled = ui.me.state != FocusState.Away,
                    modifier = Modifier.weight(1f).testTag(if (onBreak) "focus_resume" else "focus_break"),
                )
                ChoomHubSecondaryButton(text = "Leave", onClick = { confirmingLeave = true }, modifier = Modifier.testTag("focus_leave"))
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = viewModel::toggleSound, modifier = Modifier.testTag("focus_sound")) {
                    Text(if (soundOn) "♪ FIRE + RAIN: ON" else "♪ FIRE + RAIN: OFF", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                }
                if (!dndAllowed) {
                    TextButton(onClick = openDndSettings, modifier = Modifier.testTag("focus_dnd_allow")) {
                        Text("ALLOW MUTING", style = ChoomHubTextStyles.Eyebrow, color = colors.lavender)
                    }
                }
                if (ui.loaded && !ui.partnerInRoom) {
                    TextButton(onClick = { viewModel.nudge() }, modifier = Modifier.testTag("focus_nudge")) {
                        Text("NUDGE ${ui.partnerName.uppercase()} ›", style = ChoomHubTextStyles.Eyebrow, color = colors.pink, maxLines = 1)
                    }
                }
            }
        }
        // The room takes all the space that's left: as wide as the screen, with extra floor on taller phones.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val rows = roomRowsFor(maxWidth.value, maxHeight.value)
            FocusRoomArt(
                ui.scene,
                Modifier.testTag("focus_room_art"),
                rows = rows,
                onRecordPlayer = { showingJam = true },
                onCat = viewModel::sendCat,
            )
        }
    }
}

/** Before you go: what this session came to. */
@Composable
private fun LeaveSummary(summary: com.choomhub.app.domain.model.SessionSummary, onStay: () -> Unit, onLeave: () -> Unit) {
    val colors = ChoomHubTheme.colors
    InkPanel(onDismissRequest = onStay, modifier = Modifier.testTag("focus_leave_confirm")) {
        CatFace(CatExpression.Sad, Modifier.size(88.dp))
        Spacer(Modifier.height(12.dp))
        Text("Leave the room?", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text("here's this session", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("Focused", FocusRules.duration(summary.focusMs), "summary_focus")
            SummaryLine("On breaks", FocusRules.duration(summary.breakMs), "summary_breaks")
            SummaryLine(
                "Points",
                when (summary.points) { 0 -> "none yet"; 1 -> "+1 point"; else -> "+${summary.points} points" },
                "summary_points",
            )
            SummaryLine("Next point in", FocusRules.duration(summary.toNextPointMs).replace("under a minute", "a minute"), "summary_next")
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ChoomHubPrimaryButton(text = "Stay", onClick = onStay, modifier = Modifier.fillMaxWidth().testTag("dialog_confirm"))
            TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().testTag("dialog_dismissive")) {
                Text("LEAVE", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, tag: String) {
    val colors = ChoomHubTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textMain, modifier = Modifier.testTag(tag))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BreakPicker(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    var custom by rememberSaveable { mutableStateOf("") }
    val customMinutes = custom.toIntOrNull()?.takeIf { it in 1..FocusRules.MAX_BREAK_MINUTES }
    InkPanel(onDismissRequest = onDismiss, modifier = Modifier.testTag("focus_break_picker")) {
        GothicMotifArt(GothicMotif.Candle, Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text("Take a break", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text("how long?", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
        Spacer(Modifier.height(16.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusRules.BREAK_CHOICES.forEach { m ->
                ChoomHubSecondaryButton(text = "$m min", onClick = { onPick(m) }, modifier = Modifier.testTag("break_$m"))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoomHubTextField(
                value = custom,
                onValueChange = { v -> custom = v.filter { it.isDigit() }.take(3) },
                label = "Other (minutes)",
                keyboardType = KeyboardType.Number,
                onDone = { customMinutes?.let(onPick) },
                modifier = Modifier.weight(1f).testTag("break_custom"),
            )
            TextButton(onClick = { customMinutes?.let(onPick) }, enabled = customMinutes != null, modifier = Modifier.testTag("break_custom_go")) {
                Text("START", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
    }
}

@Composable
private fun BreakOver(overBy: String, onBack: () -> Unit, onExtend: (Int) -> Unit, onLeave: () -> Unit, onDismiss: () -> Unit) {
    val colors = ChoomHubTheme.colors
    InkPanel(onDismissRequest = onDismiss, modifier = Modifier.testTag("focus_break_over")) {
        CatFace(CatExpression.Bored, Modifier.size(88.dp))
        Spacer(Modifier.height(12.dp))
        Text("Break's over", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text("$overBy over", style = ChoomHubTextStyles.Handwritten, color = colors.tomato)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoomHubPrimaryButton(text = "Back to focus", onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("over_back"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoomHubSecondaryButton(text = "+5 min", onClick = { onExtend(5) }, modifier = Modifier.weight(1f).testTag("over_extend_5"))
                ChoomHubSecondaryButton(text = "+10 min", onClick = { onExtend(10) }, modifier = Modifier.weight(1f).testTag("over_extend_10"))
            }
            TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().testTag("over_leave")) {
                Text("LEAVE THE ROOM", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
}

/** The record player: a jam link stays hidden in here until someone opens it. */
@Composable
private fun JamPanel(
    jam: JamLink?,
    partnerName: String,
    onLeave: (String) -> Boolean,
    onTakeOut: () -> Unit,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    var editing by rememberSaveable { mutableStateOf(jam == null) }
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    InkPanel(onDismissRequest = onDismiss, modifier = Modifier.testTag("focus_jam")) {
        GothicMotifArt(GothicMotif.MoonAndCrows, Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text("The record player", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        if (jam != null && !editing) {
            Text(
                if (jam.fromMe) "you left a jam in here" else "$partnerName left a jam in here",
                style = ChoomHubTextStyles.Handwritten,
                color = colors.pink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                jam.url,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("jam_url"),
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoomHubPrimaryButton(text = "Join the jam", onClick = { onJoin(jam.url) }, modifier = Modifier.fillMaxWidth().testTag("jam_join"))
                ChoomHubSecondaryButton(text = "Swap the link", onClick = { editing = true }, modifier = Modifier.fillMaxWidth().testTag("jam_swap"))
                TextButton(
                    onClick = {
                        onTakeOut()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("jam_take_out"),
                ) { Text("TAKE IT OUT", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
            }
        } else {
            Text("nothing in here yet", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
            Spacer(Modifier.height(16.dp))
            ChoomHubTextField(
                value = input,
                onValueChange = {
                    input = it
                    invalid = false
                },
                label = "Jam link",
                supportingText = if (invalid) "That doesn't look like a link" else "A Spotify jam, or any music app's",
                isError = invalid,
                keyboardType = KeyboardType.Uri,
                onDone = {
                    if (onLeave(input)) onDismiss() else invalid = true
                },
                modifier = Modifier.fillMaxWidth().testTag("jam_input"),
            )
            Spacer(Modifier.height(12.dp))
            ChoomHubPrimaryButton(
                text = "Leave it here",
                onClick = { if (onLeave(input)) onDismiss() else invalid = true },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("jam_leave"),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
}
