package com.choomhub.app.feature.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.choomhub.app.domain.model.FocusChange
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.Notifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything that happens when you move around the focus room: the stored session (and totals
 * and points), Do Not Disturb, the stopwatch notification, the break alarm, and telling them
 * you came in. The screen and the notification buttons both go through here.
 */
@Singleton
class FocusController @Inject constructor(
    private val repository: FocusRepository,
    private val notifier: Notifier,
    private val dnd: DoNotDisturb,
    private val notifications: FocusNotifications,
    private val alarm: BreakAlarm,
    private val clock: Clock,
    private val petLinks: com.choomhub.app.feature.pet.PetLinks,
) {
    private val lock = Mutex()

    private suspend fun change(block: (FocusSession, com.choomhub.app.domain.model.FocusTotals) -> FocusChange): FocusChange =
        lock.withLock {
            val room = repository.observeRoom().first()
            val change = block(room.me, room.myTotals)
            if (change.session != room.me || change.totals != room.myTotals) repository.apply(change)
            // The same hours that earn points earn the cat's coins.
            if (change.points > 0) petLinks.focusHours(change.points)
            change
        }

    private fun today(): LocalDate = LocalDate.now(clock)

    suspend fun enter() {
        // Quiet: going in doesn't notify them (only a nudge does).
        val change = change { s, t -> FocusRules.enter(s, t, clock.instant()) }
        sync(change.session)
    }

    suspend fun startBreak(minutes: Int) {
        val change = change { s, t -> FocusRules.startBreak(s, t, minutes, clock.instant(), today()) }
        sync(change.session)
    }

    suspend fun extendBreak(minutes: Int = FocusRules.EXTEND_MINUTES) {
        val change = change { s, t -> FocusRules.extendBreak(s, t, minutes, clock.instant()) }
        sync(change.session)
    }

    suspend fun resume() {
        val change = change { s, t -> FocusRules.resume(s, t, clock.instant()) }
        sync(change.session)
    }

    suspend fun leave() {
        val change = change { s, t -> FocusRules.leave(s, t, clock.instant(), today()) }
        sync(change.session)
    }

    suspend fun nudge() = notifier.notifyPartner(PushEvent.FocusNudge)

    /** Brings Do Not Disturb, the notification and the alarm in line with [session] (e.g. after a restart). */
    suspend fun sync(session: FocusSession? = null) {
        val s = session ?: repository.observeRoom().first().me
        val now = clock.instant()
        when (s.state) {
            FocusState.Away -> {
                alarm.cancel()
                notifications.clearAll()
                dnd.restore()
            }
            FocusState.Focusing -> {
                alarm.cancel()
                notifications.clearBreakOver()
                notifications.showFocusing(FocusRules.sessionMs(s, now), now)
                dnd.turnOn()
            }
            FocusState.OnBreak -> {
                val end = s.breakEndsAt ?: now
                notifications.showBreak(end)
                if (end.isAfter(now)) {
                    notifications.clearBreakOver()
                    alarm.schedule(end)
                }
                dnd.turnOn()
            }
        }
    }
}

/** Fires when a break ends: rings, unless you've already gone back or left. */
@AndroidEntryPoint
class BreakAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: FocusRepository

    @Inject lateinit var notifications: FocusNotifications

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (repository.observeRoom().first().me.state == FocusState.OnBreak) notifications.showBreakOver()
            } finally {
                pending.finish()
            }
        }
    }
}

/** The buttons on the focus room's notifications. */
@AndroidEntryPoint
class FocusActionReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: FocusController

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_RESUME -> controller.resume()
                    ACTION_EXTEND -> controller.extendBreak()
                    ACTION_LEAVE -> controller.leave()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RESUME = "com.choomhub.app.focus.RESUME"
        const val ACTION_EXTEND = "com.choomhub.app.focus.EXTEND"
        const val ACTION_LEAVE = "com.choomhub.app.focus.LEAVE"
    }
}
