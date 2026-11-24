package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.Meetup
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The single shared "next meetup". Last write wins (by server time once synced). */
interface MeetupRepository {
    fun observeMeetup(): Flow<Meetup?>

    suspend fun schedule(at: Instant)

    suspend fun clear()
}
