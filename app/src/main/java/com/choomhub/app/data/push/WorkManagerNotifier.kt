package com.choomhub.app.data.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.TestPushResult
import com.choomhub.app.domain.repository.Notifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Real notifier: queues sends with WorkManager; the test send goes out directly. */
@Singleton
class WorkManagerNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val delivery: PushDelivery,
    private val registrar: PushRegistrar,
) : Notifier {

    override val availability: PushAvailability = PushAvailability.Ready

    override fun notifyPartner(event: PushEvent, detail: String?) {
        val request = OneTimeWorkRequestBuilder<PushWorker>()
            .setInputData(workDataOf(PushWorker.KEY_EVENT to event.wire, PushWorker.KEY_DETAIL to detail))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // A newer meetup change replaces one that's still waiting to go out.
        WorkManager.getInstance(context).enqueueUniqueWork("push-${event.collapseKey}", ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun sendTest(): TestPushResult = testResultFor(delivery.deliver(PushEvent.Test, null))

    override suspend fun forgetThisPhone() = registrar.forgetThisPhone()

    companion object {
        fun testResultFor(response: PushResponse): TestPushResult = when (response) {
            is PushResponse.Delivered -> TestPushResult.Sent
            is PushResponse.NotDelivered -> TestPushResult.NotSent(
                when (response.reason) {
                    "no-devices" -> "This phone isn't registered yet. Give it a minute after opening the app, then try again."
                    else -> "Google couldn't deliver it to this phone. Try again in a bit."
                },
            )
            PushResponse.Unauthorized -> TestPushResult.NotSent("Your sign-in was refused. Sign out and back in, then try again.")
            is PushResponse.Rejected -> TestPushResult.NotSent(
                if (response.error == "signed-out") "Sign in first." else "The push service refused it (${response.status}). Check the Worker URL.",
            )
            is PushResponse.Unavailable -> TestPushResult.NotSent("Couldn't reach the push service. Check your connection.")
        }
    }
}

/** Used when pushes can't work in this build; says why in the test result. */
class NoopNotifier(override val availability: PushAvailability) : Notifier {
    override fun notifyPartner(event: PushEvent, detail: String?) = Unit

    override suspend fun sendTest(): TestPushResult = TestPushResult.NotSent(
        if (availability == PushAvailability.NotSetUp) "Push isn't set up in this build yet (no Worker URL)." else "This build has no backend.",
    )

    override suspend fun forgetThisPhone() = Unit
}
