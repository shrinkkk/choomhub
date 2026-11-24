package com.choomhub.app.data.home

import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Member
import com.choomhub.app.domain.model.PartnerMood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.domain.model.Snap
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.HomeRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Home's data: real names, moods and gestures; snaps, points and the cat stay
 * sample data (flagged by [HomeSnapshot.isSampleData]) until their milestones.
 */
class LiveHomeRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val moodRepository: MoodRepository,
    private val gestureRepository: GestureRepository,
    private val clock: Clock,
) : HomeRepository {

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        profileRepository.observeNames(),
        moodRepository.observeMoods(),
        gestureRepository.observeRecent(),
    ) { names, moods, gestures ->
        val partner = Member(id = "partner", displayName = names.partnerName ?: "them")
        HomeSnapshot(
            viewerName = names.myName,
            myRole = names.myRole,
            partnerMood = moods.partner?.let { PartnerMood(partner, it.mood, it.note, it.updatedAt) },
            partnerName = names.partnerName,
            myMood = moods.mine,
            recentGestures = gestures,
            latestSnap = sampleSnap(partner),
            points = PointBalances(goodBoy = 128, goodGirl = 142),
            cat = SharedCatStatus(isAdopted = false, name = null),
            isSampleData = true,
        )
    }

    private fun sampleSnap(partner: Member) = Snap(
        id = "sample-snap-1",
        sender = partner,
        caption = "the crows are back on the wire",
        sentAt = clock.instant().minus(Duration.ofMinutes(47)),
        isUnseen = true,
    )
}
