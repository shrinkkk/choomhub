package com.choomhub.app.feature.snaps.prompt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.domain.model.SnapPrompts
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The daily snap prompt: a local notification at the day's prompt time
 * (same on both phones, no server). On by default; Settings can turn it off.
 */
@Singleton
class SnapPromptScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: DataStore<Preferences>,
    private val clock: Clock,
) : SnapPromptSettings {
    override fun observeEnabled(): Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ENABLED] != false }

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[ENABLED] = enabled }
        if (enabled) scheduleNext() else WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    /** Schedules the next prompt (today's if still ahead, else tomorrow's), if prompts are on. */
    suspend fun scheduleNext() {
        if (!observeEnabled().first()) return
        val delay = delayUntilNext(ZonedDateTime.now(clock))
        val request = OneTimeWorkRequestBuilder<SnapPromptWorker>().setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private val ENABLED = booleanPreferencesKey("snap_prompts_enabled")
        private const val WORK = "snap-prompt"

        /** Pure, unit-tested: how long until the next prompt from [now]. */
        fun delayUntilNext(now: ZonedDateTime): Duration {
            val today = now.toLocalDate()
            val todayAt = today.atTime(SnapPrompts.timeFor(today)).atZone(now.zone)
            val next = if (todayAt.isAfter(now.plusMinutes(1))) todayAt else {
                val tomorrow = today.plusDays(1)
                tomorrow.atTime(SnapPrompts.timeFor(tomorrow)).atZone(now.zone)
            }
            return Duration.between(now, next)
        }
    }
}

/** Turning the daily prompt on or off (what Settings needs). */
interface SnapPromptSettings {
    fun observeEnabled(): Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class SnapPromptModule {
    @dagger.Binds
    abstract fun bindSettings(impl: SnapPromptScheduler): SnapPromptSettings
}

/** Shows the prompt, then books the next one. */
@HiltWorker
class SnapPromptWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: SnapPromptScheduler,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (scheduler.observeEnabled().first()) {
            AppNotifications.show(
                context = applicationContext,
                title = "Snap prompt",
                body = SnapPrompts.forDate(LocalDate.now(clock)),
                channelId = AppNotifications.PROMPTS_CHANNEL,
                tag = "snap_prompt",
            )
        }
        scheduler.scheduleNext()
        return Result.success()
    }
}
