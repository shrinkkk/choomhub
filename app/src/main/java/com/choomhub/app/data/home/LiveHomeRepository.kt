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
import com.choomhub.app.domain.repository.PetRepository
import com.choomhub.app.domain.repository.ShopRepository
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PointAward
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PetRules
import java.time.Clock
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.data.snap.SeenSnaps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Home's data: names, moods, gestures, snaps, points and the Pet House cat, all real.
 * ([HomeSnapshot.isSampleData] is kept for anything sample again.)
 */
class LiveHomeRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val moodRepository: MoodRepository,
    private val gestureRepository: GestureRepository,
    private val snapRepository: SnapRepository,
    private val pointsRepository: PointsRepository,
    private val seenSnaps: SeenSnaps,
    private val petRepository: PetRepository,
    private val shopRepository: ShopRepository,
    private val clock: Clock,
) : HomeRepository {

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        profileRepository.observeNames(),
        moodRepository.observeMoods(),
        gestureRepository.observeRecent(),
        snapRepository.observeRecent(),
        combine(seenSnaps.observe(), pointsRepository.observeAwards(), petRepository.observePet(), shopRepository.observeShop()) { seen, awards, pet, shop ->
            Extras(seen, awards, pet, shop.purchases)
        },
    ) { names, moods, gestures, snaps, (seen, awards, pet, purchases) ->
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
            points = PointRules.balances(awards, names.myRole, purchases),
            awards = awards,
            cat = SharedCatStatus(
                isAdopted = pet.adopted,
                name = pet.name,
                status = if (pet.adopted) PetRules.status(pet, clock.instant()) else null,
            ),
            isSampleData = false,
        )
    }
}

private data class Extras(val seen: String?, val awards: List<PointAward>, val pet: PetState, val purchases: List<Purchase>)
