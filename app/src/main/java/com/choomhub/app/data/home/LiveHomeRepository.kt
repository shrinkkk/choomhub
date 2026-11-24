package com.choomhub.app.data.home

import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Member
import com.choomhub.app.domain.model.PartnerMood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.HomeRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.data.snap.SeenSnaps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Home's data: real names, moods, gestures and snaps; points and the cat stay
 * sample data (flagged by [HomeSnapshot.isSampleData]) until their milestones.
 */
class LiveHomeRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val moodRepository: MoodRepository,
    private val gestureRepository: GestureRepository,
    private val snapRepository: SnapRepository,
    private val seenSnaps: SeenSnaps,
) : HomeRepository {

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        profileRepository.observeNames(),
        moodRepository.observeMoods(),
        gestureRepository.observeRecent(),
        snapRepository.observeRecent(),
        seenSnaps.observe(),
    ) { names, moods, gestures, snaps, seen ->
        val partner = Member(id = "partner", displayName = names.partnerName ?: "them")
        HomeSnapshot(
            viewerName = names.myName,
            myRole = names.myRole,
            partnerMood = moods.partner?.let { PartnerMood(partner, it.mood, it.note, it.updatedAt) },
            partnerName = names.partnerName,
            myMood = moods.mine,
            recentGestures = gestures,
            partnerSnap = snaps.firstOrNull { !it.fromMe },
            mySnap = snaps.firstOrNull { it.fromMe },
            seenSnapId = seen,
            points = PointBalances(goodBoy = 128, goodGirl = 142),
            cat = SharedCatStatus(isAdopted = false, name = null),
            isSampleData = true,
        )
    }
}
