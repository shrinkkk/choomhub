package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Names live at couples/{id}/names/{namerUid}.nameForPartner: each person
 * writes only the name they give the other. Your partner's name is the one
 * you wrote; your name is the one they wrote.
 */
class FirestoreProfileRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : ProfileRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNames(): Flow<CoupleNames> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.accountOrNull?.uid
        if (coupleId == null || me == null) return@flatMapLatest flowOf(CoupleNames(partnerName = null, myName = null))
        val mine = namesDoc(coupleId, me).observe().map { it?.getString(FIELD) }
        val theirs = if (space is SpaceState.Paired) {
            namesDoc(coupleId, space.partnerUid).observe().map { it?.getString(FIELD) }
        } else {
            flowOf(null)
        }
        combine(mine, theirs) { forPartner, forMe -> CoupleNames(partnerName = forPartner, myName = forMe) }
    }.distinctUntilChanged()

    override suspend fun setPartnerName(name: String) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.accountOrNull?.uid ?: return
        // Not awaited: Firestore applies it locally at once and syncs when online.
        namesDoc(coupleId, me).set(mapOf(FIELD to name, "updatedAt" to FieldValue.serverTimestamp()))
    }

    private fun namesDoc(coupleId: String, namer: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("names").document(namer)

    private companion object {
        const val FIELD = "nameForPartner"
    }
}
