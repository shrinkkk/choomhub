package com.choomhub.app.data.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choomhub.app.domain.model.PushEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends one queued notification. WorkManager runs it once there's a network
 * and retries with backoff when the Worker or the network is having trouble,
 * so a notification about something done offline still goes out later.
 */
@HiltWorker
class PushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val delivery: PushDelivery,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val event = PushEvent.fromWire(inputData.getString(KEY_EVENT)) ?: return Result.failure()
        val response = delivery.deliver(event, inputData.getString(KEY_DETAIL), inputData.getString(KEY_KIND))
        return when (outcome(response, runAttemptCount)) {
            Outcome.Done -> Result.success()
            Outcome.GiveUp -> Result.failure()
            Outcome.Retry -> Result.retry()
        }
    }

    enum class Outcome { Done, Retry, GiveUp }

    companion object {
        const val KEY_EVENT = "event"
        const val KEY_DETAIL = "detail"
        const val KEY_KIND = "kind"
        const val MAX_ATTEMPTS = 6

        /** Pure decision, unit-tested: what to do with the Worker's answer. */
        fun outcome(response: PushResponse, attempt: Int): Outcome = when (response) {
            is PushResponse.Delivered, is PushResponse.NotDelivered -> Outcome.Done
            is PushResponse.Rejected, PushResponse.Unauthorized -> Outcome.GiveUp
            is PushResponse.Unavailable -> if (attempt + 1 >= MAX_ATTEMPTS) Outcome.GiveUp else Outcome.Retry
        }
    }
}
