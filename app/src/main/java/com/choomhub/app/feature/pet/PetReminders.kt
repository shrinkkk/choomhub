package com.choomhub.app.feature.pet

import android.content.Context
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.PetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** A gentle nudge about the cat, shown on this phone. */
data class PetReminder(val key: String, val title: String, val body: String)

/**
 * When to nudge you about the cat: only when something's actually wrong, never at night, at most
 * [MAX_PER_DAY] a day and not more often than every [MIN_GAP]. Pure, so it's tested.
 */
object PetReminderRules {
    const val MAX_PER_DAY = 3
    val MIN_GAP: Duration = Duration.ofHours(4)
    private val QUIET_FROM: LocalTime = LocalTime.of(22, 30)
    private val QUIET_UNTIL: LocalTime = LocalTime.of(8, 30)

    fun next(state: PetState, now: Instant, zone: ZoneId, lastSent: Instant?, sentToday: Int): PetReminder? {
        if (!state.adopted) return null
        val time = now.atZone(zone).toLocalTime()
        if (time >= QUIET_FROM || time < QUIET_UNTIL) return null
        if (sentToday >= MAX_PER_DAY) return null
        if (lastSent != null && Duration.between(lastSent, now) < MIN_GAP) return null
        val n = state.name ?: "The cat"
        if (state.sick && !state.vetDone && !PetRules.pendingOpen(state.pending, now)) {
            return PetReminder("sick", "$n isn't well", "A vet trip takes both of you. Open the Pet House to start one.")
        }
        val today = now.atZone(zone).toLocalDate()
        if (state.sick && state.vetDone && state.lastMedicineDay != today) {
            return PetReminder("medicine", "Medicine time for $n", "${state.medicineLeft} more day${if (state.medicineLeft == 1) "" else "s"} to go.")
        }
        val l = PetRules.levels(state, now)
        val low = listOf(PetNeed.Hunger, PetNeed.Thirst, PetNeed.Litter, PetNeed.Affection, PetNeed.Fun)
            .filter { l.getValue(it) < 20f }
            .minByOrNull { l.getValue(it) } ?: return null
        return when (low) {
            PetNeed.Hunger -> PetReminder("hunger", "$n is sitting by the empty bowl", "Staring. Pointedly.")
            PetNeed.Thirst -> PetReminder("thirst", "$n's water bowl is dry", "Some fresh water would go down well.")
            PetNeed.Litter -> PetReminder("litter", "$n's litter box needs a scoop", "It has opinions about this.")
            PetNeed.Affection -> PetReminder("affection", "$n is hiding under the bed", "A bit of attention would bring it out.")
            else -> PetReminder("fun", "$n is bored", "And eyeing the curtains. Maybe play for a minute?")
        }
    }
}

/** Checks on the cat every few hours and nudges you if it needs something. */
@HiltWorker
class PetReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pets: PetRepository,
    private val notifications: NotificationRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = withTimeoutOrNull(20_000) { notifications.observePrefs().first() }
        if (prefs != null && !prefs.isEnabled(NotificationCategory.Pet)) return Result.success()
        val pet = withTimeoutOrNull(20_000) { pets.observePet().first { it.loaded } } ?: return Result.success()
        val now = clock.instant()
        val store = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = now.atZone(clock.zone).toLocalDate().toString()
        val sentToday = if (store.getString(KEY_DAY, null) == today) store.getInt(KEY_COUNT, 0) else 0
        val last = store.getLong(KEY_LAST, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        val reminder = PetReminderRules.next(pet, now, clock.zone, last, sentToday) ?: return Result.success()
        AppNotifications.show(applicationContext, reminder.title, reminder.body, NotificationCategory.Pet.channelId, "pet_reminder")
        store.edit {
            putString(KEY_DAY, today)
            putInt(KEY_COUNT, sentToday + 1)
            putLong(KEY_LAST, now.toEpochMilli())
        }
        return Result.success()
    }

    companion object {
        private const val PREFS = "pet_reminders"
        private const val KEY_DAY = "day"
        private const val KEY_COUNT = "count"
        private const val KEY_LAST = "last"
        private const val WORK = "pet-reminders"

        /** Books the check-ins (keeps an existing booking). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PetReminderWorker>(2, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
