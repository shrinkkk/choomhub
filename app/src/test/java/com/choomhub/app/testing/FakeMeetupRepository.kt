package com.choomhub.app.testing

import com.choomhub.app.domain.model.Meetup
import com.choomhub.app.domain.repository.MeetupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeMeetupRepository(initial: Meetup? = null) : MeetupRepository {
    val meetup = MutableStateFlow(initial)
    override fun observeMeetup(): Flow<Meetup?> = meetup
    override suspend fun schedule(at: Instant) { meetup.value = Meetup(at) }
    override suspend fun clear() { meetup.value = null }
}
