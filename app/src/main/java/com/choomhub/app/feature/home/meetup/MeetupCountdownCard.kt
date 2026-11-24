package com.choomhub.app.feature.home.meetup

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.CountdownMood
import com.choomhub.app.domain.model.CountdownState
import com.choomhub.app.domain.model.MeetupCountdown
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The countdown to the next meetup, at the top of Home. Ticks every second.
 *
 * @param onSchedule returns an error message to show, or null when accepted.
 */
@Composable
fun MeetupCountdownCard(
    state: CountdownState?,
    zone: ZoneId,
    onSchedule: (LocalDate, LocalTime) -> String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val expression = when (state) {
        null, CountdownState.NoneScheduled -> MoonExpression.Crying
        is CountdownState.Happening -> MoonExpression.Excited
        is CountdownState.Upcoming -> when (state.mood) {
            CountdownMood.Sad -> MoonExpression.Sad
            CountdownMood.Happy -> MoonExpression.Happy
            CountdownMood.Excited -> MoonExpression.Excited
        }
    }
    ChoomHubCard(
        modifier = modifier.fillMaxWidth().testTag("meetup_card"),
        tone = if (expression == MoonExpression.Excited) CardTone.Glowing else CardTone.Raised,
        glowColor = colors.candle,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXT MEETUP", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                Spacer(Modifier.height(ChoomHubTheme.spacing.xs))
                when (state) {
                    null -> Text(" ", style = MaterialTheme.typography.titleLarge)
                    CountdownState.NoneScheduled -> {
                        Text("Nothing planned", style = MaterialTheme.typography.titleLarge, color = colors.textMain)
                        Text("No meetup on the calendar yet.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    is CountdownState.Upcoming -> {
                        Text(longWhen(state.at, zone), style = MaterialTheme.typography.titleLarge, color = colors.textMain)
                    }
                    is CountdownState.Happening -> {
                        Text("It's today.", style = MaterialTheme.typography.titleLarge, color = colors.candle)
                        Text("Since ${shortTime(state.at, zone)}.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
            MoonFace(
                expression,
                Modifier
                    .size(72.dp)
                    .testTag("moon_${expression.name}")
                    .semantics { contentDescription = "Moon face, ${expression.name.lowercase()}" },
            )
        }

        if (state is CountdownState.Upcoming) {
            Spacer(Modifier.height(ChoomHubTheme.spacing.md))
            CountdownDigits(state)
        }

        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        when (state) {
            null -> Unit
            CountdownState.NoneScheduled -> ChoomHubPrimaryButton(
                text = "Schedule",
                onClick = { picking = true },
                modifier = Modifier.testTag("meetup_schedule"),
            )
            else -> Row(horizontalArrangement = Arrangement.spacedBy(ChoomHubTheme.spacing.xs)) {
                TextButton(onClick = { picking = true }, modifier = Modifier.testTag("meetup_reschedule")) {
                    Text(
                        if (state is CountdownState.Happening) "PLAN THE NEXT ONE" else "RESCHEDULE",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.lavender,
                    )
                }
                TextButton(onClick = onClear, modifier = Modifier.testTag("meetup_clear")) {
                    Text("CLEAR", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            }
        }
    }

    if (picking) {
        ScheduleMeetupDialogs(
            initial = (state as? CountdownState.Upcoming)?.at?.atZone(zone)?.toLocalDateTime(),
            zone = zone,
            onSchedule = onSchedule,
            onDone = { picking = false },
        )
    }
}

@Composable
private fun CountdownDigits(state: CountdownState.Upcoming) {
    val parts = MeetupCountdown.parts(state.remaining)
    val colors = ChoomHubTheme.colors
    val accent = when (state.mood) {
        CountdownMood.Sad -> colors.lavender
        CountdownMood.Happy -> colors.pink
        CountdownMood.Excited -> colors.candle
    }
    Row(
        Modifier.fillMaxWidth().testTag("meetup_countdown"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
            parts.days.toString() to "DAYS",
            "%02d".format(parts.hours) to "HRS",
            "%02d".format(parts.minutes) to "MIN",
            "%02d".format(parts.seconds) to "SEC",
        ).forEach { (value, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = DigitStyle, color = accent)
                Text(label, style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint)
            }
        }
    }
}

private val DigitStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelLarge.copy(fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = 0.sp)

@Composable
private fun longWhen(at: Instant, zone: ZoneId): String {
    val date = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(at.atZone(zone))
    return "$date · ${shortTime(at, zone)}"
}

@Composable
private fun shortTime(at: Instant, zone: ZoneId): String =
    TimeLabels.clockTime(at.atZone(zone), DateFormat.is24HourFormat(LocalContext.current))

/** Date picker, then time picker. Past times are rejected with a message. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMeetupDialogs(
    initial: java.time.LocalDateTime?,
    zone: ZoneId,
    onSchedule: (LocalDate, LocalTime) -> String?,
    onDone: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember(zone) { LocalDate.now(zone) }

    if (pickedDate == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = (initial?.toLocalDate() ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)

                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = onDone,
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                }) { Text("NEXT", style = MaterialTheme.typography.labelLarge, color = colors.lavender) }
            },
            dismissButton = {
                TextButton(onClick = onDone) { Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.surfaceElevated),
        ) {
            DatePicker(state = dateState, colors = DatePickerDefaults.colors(containerColor = colors.surfaceElevated))
        }
    } else {
        val date = pickedDate!!
        val timeState = rememberTimePickerState(
            initialHour = initial?.hour ?: 19,
            initialMinute = initial?.minute ?: 0,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = onDone,
            containerColor = colors.surfaceElevated,
            title = {
                Text(
                    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(date),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textMain,
                )
            },
            text = {
                Column {
                    TimePicker(state = timeState, colors = TimePickerDefaults.colors(clockDialColor = colors.surfaceHighest))
                    if (error != null) Text(error!!, style = MaterialTheme.typography.bodySmall, color = colors.tomato)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    error = onSchedule(date, LocalTime.of(timeState.hour, timeState.minute))
                    if (error == null) onDone()
                }) { Text("SAVE", style = MaterialTheme.typography.labelLarge, color = colors.lavender) }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) {
                    Text("BACK", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            },
        )
    }
}
