package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.NotificationPrefs
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.TestPushResult
import kotlinx.coroutines.flow.Flow

/** Your notification choices, synced so the push Worker can respect them. */
interface NotificationRepository {
    fun observePrefs(): Flow<NotificationPrefs>

    suspend fun setEnabled(category: NotificationCategory, enabled: Boolean)
}

/** Sends notifications to your partner's phone (through the push Worker). */
interface Notifier {
    val availability: PushAvailability

    /**
     * Queues a notification about something you just did. Fire and forget:
     * it waits for a connection and retries by itself. Does nothing when
     * pushes aren't set up.
     */
    fun notifyPartner(event: PushEvent, detail: String? = null)

    /** Sends a test notification to your own phone, right now. */
    suspend fun sendTest(): TestPushResult

    /** Stops pushes to this phone for the signed-in account (call before signing out). */
    suspend fun forgetThisPhone()
}
