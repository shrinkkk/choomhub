package com.choomhub.app.testing

import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.data.meetuplog.LocalMeetupLogRepository
import com.choomhub.app.data.song.LocalSongRepository
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.feature.world.WorldViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.Clock
import java.time.Instant

/** A ticker that never ticks: screens show the time they were opened at. */
object StillTicker : SecondTicker {
    override fun ticks(): Flow<Instant> = emptyFlow()
}

/** Our World's rooms on in-memory data, for screens tested outside the app. */
fun worldViewModel(
    meetups: LocalMeetupLogRepository = LocalMeetupLogRepository(Clock.systemUTC()),
    songs: LocalSongRepository = LocalSongRepository(),
    profile: ProfileRepository = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null)),
    clock: Clock = Clock.systemUTC(),
): WorldViewModel = WorldViewModel(meetups, songs, profile, StillTicker, FakeNotifier(), clock)
