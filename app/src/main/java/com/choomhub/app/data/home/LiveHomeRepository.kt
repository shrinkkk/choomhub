package com.choomhub.app.data.home

import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Member
import com.choomhub.app.domain.model.PartnerMood
import com.choomhub.app.domain.model.PointRules
import com.choomhub.app.domain.repository.PointsRepository
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
 * Home's data: names, moods, gestures, snaps and points, all real. (The cat
 * comes later; [HomeSnapshot.isSampleData] is kept for anything sample again.)
 */
class LiveHomeRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val moodRepository: MoodRepository,
    private val gestureRepository: GestureRepository,
    private val snapRepository: SnapRepository,
    private val pointsRepository: PointsRepository,
    private val seenSnaps: SeenSnaps,
) : HomeRepository {

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        profileRepository.observeNames(),
        moodRepository.observeMoods(),
        gestureRepository.observeRecent(),
        snapRepository.observeRecent(),
        combine(seenSnaps.observe(), pointsRepository.observeAwards(), ::Pair),
    ) { names, moods, gestures, snaps, (seen, awards) ->
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
            points = PointRules.balances(awards, names.myRole),
            awards = awards,
            cat = SharedCatStatus(isAdopted = false, name = null),
            isSampleData = false,
        )
    }
}
