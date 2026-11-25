package com.choomhub.app.feature.focus

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.domain.model.CatSchedule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.FocusTotals
import com.choomhub.app.domain.model.JamLink
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** What the focus room screen shows. */
data class FocusRoomUi(
    val loaded: Boolean = false,
    val scene: RoomScene = RoomScene(blank("You"), blank("Them"), together = false),
    val me: FocusSession = FocusSession(),
    val partner: FocusSession = FocusSession(),
    val partnerName: String = "them",
    /** Time left on your break (negative once it's over); null when you're not on one. */
    val myBreakLeftMs: Long? = null,
    val jam: JamLink? = null,
) {
    val partnerInRoom: Boolean get() = partner.state != FocusState.Away
}

private fun blank(name: String) = RoomPerson(name, Spot.Away, "--:--", weekHours = "0.0", totalHours = "0.0")

/** Builds the room picture from the stored sessions. [iAmHim]: you have the left desk. */
internal fun roomScene(room: FocusRoomData, names: CoupleNames, now: Instant, today: LocalDate): RoomScene {
    val iAmHim = names.myRole != PointsRole.GoodGirl
    val mine = person(names.myName ?: "You", room.me, room.myTotals, now, today)
    val theirs = person(names.partnerName ?: "Them", room.partner, room.partnerTotals, now, today)
    return RoomScene(
        him = if (iAmHim) mine else theirs,
        her = if (iAmHim) theirs else mine,
        together = room.me.state == FocusState.Focusing && room.partner.state == FocusState.Focusing,
        jam = room.jam != null,
        cat = CatSchedule.at(now.toEpochMilli()),
    )
}

private fun person(name: String, s: FocusSession, totals: FocusTotals, now: Instant, today: LocalDate): RoomPerson {
    val left = FocusRules.breakLeftMs(s, now)
    val week = totals.weekMsAt(today)
    return RoomPerson(
        name = name,
        spot = when (s.state) {
            FocusState.Focusing -> Spot.Desk
            FocusState.OnBreak -> Spot.Beanbag
            FocusState.Away -> Spot.Away
        },
        tvTime = when (s.state) {
            FocusState.Focusing -> FocusRules.stopwatch(FocusRules.sessionMs(s, now))
            FocusState.OnBreak -> FocusRules.countdown(left ?: 0)
            FocusState.Away -> "--:--"
        },
        tvOverrun = left != null && left < 0,
        weekHours = FocusRules.hours(week),
        totalHours = FocusRules.hours(totals.totalMs),
        weekMinutes = week / 60_000,
    )
}

@HiltViewModel
class FocusRoomViewModel @Inject constructor(
    private val repository: FocusRepository,
    profileRepository: ProfileRepository,
    ticker: SecondTicker,
    private val controller: FocusController,
    private val notifier: Notifier,
    private val sound: AmbientSound,
    private val dnd: DoNotDisturb,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val state: StateFlow<FocusRoomUi> = combine(
        repository.observeRoom(),
        profileRepository.observeNames(),
        ticker.ticks().onStart { emit(clock.instant()) },
    ) { room, names, now ->
        FocusRoomUi(
            loaded = true,
            scene = roomScene(room, names, now, LocalDate.now(clock)),
            me = room.me,
            partner = room.partner,
            partnerName = names.partnerName ?: "them",
            myBreakLeftMs = FocusRules.breakLeftMs(room.me, now),
            jam = room.jam,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusRoomUi())

    val soundOn: StateFlow<Boolean> = sound.enabled

    private val _dndAllowed = MutableStateFlow(dnd.isAllowed)
    val dndAllowed: StateFlow<Boolean> = _dndAllowed

    private val prefs get() = context.getSharedPreferences("focus_room", Context.MODE_PRIVATE)

    /** Ask about Do Not Disturb once, the first time you come in without it allowed. */
    val askAboutDnd = MutableStateFlow(!dnd.isAllowed && !prefs.getBoolean(KEY_ASKED_DND, false))

    /** The break end you've already dealt with in the app (so its pop-up doesn't come back). */
    val dismissedBreakEnd = MutableStateFlow<Instant?>(null)

    init {
        // Coming in: start focusing (unless you're already in, e.g. back from a break).
        viewModelScope.launch {
            val me = repository.observeRoom().first().me
            if (me.state == FocusState.Away) controller.enter() else controller.sync(me)
        }
    }

    fun dndAsked() {
        askAboutDnd.value = false
        prefs.edit { putBoolean(KEY_ASKED_DND, true) }
    }

    fun dndSettingsIntent() = dnd.settingsIntent()

    /** Back from Android's settings (or the app coming back): apply Do Not Disturb if it's now allowed. */
    fun onResume() {
        _dndAllowed.value = dnd.isAllowed
        viewModelScope.launch { controller.sync() }
        if (soundOn.value) sound.start()
    }

    fun onPause() = sound.stop()

    fun toggleSound() {
        val on = !sound.enabled.value
        sound.setEnabled(on)
        if (on) sound.start()
    }

    fun takeBreak(minutes: Int) = viewModelScope.launch { controller.startBreak(minutes) }

    fun extendBreak(minutes: Int) {
        dismissedBreakEnd.value = null
        viewModelScope.launch { controller.extendBreak(minutes) }
    }

    fun backToFocus() = viewModelScope.launch { controller.resume() }

    fun dismissBreakOver() {
        dismissedBreakEnd.value = state.value.me.breakEndsAt
    }

    fun leave(then: () -> Unit) = viewModelScope.launch {
        sound.stop()
        controller.leave()
        then()
    }

    fun nudge() = viewModelScope.launch { controller.nudge() }

    fun setJam(input: String): Boolean {
        val url = FocusRules.cleanJamUrl(input) ?: return false
        viewModelScope.launch {
            repository.setJam(url)
            notifier.notifyPartner(PushEvent.JamLeft)
        }
        return true
    }

    fun clearJam() = viewModelScope.launch { repository.setJam(null) }

    override fun onCleared() {
        sound.stop()
    }

    private companion object {
        const val KEY_ASKED_DND = "dnd_asked"
    }
}
