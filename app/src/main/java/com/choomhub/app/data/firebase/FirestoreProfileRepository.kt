package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
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
import kotlinx.coroutines.tasks.await
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
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(CoupleNames(partnerName = null, myName = null))
        val mine = namesDoc(coupleId, me).observe().map { it?.getString(FIELD) }
        val theirs = if (space is SpaceState.Paired) {
            namesDoc(coupleId, space.partnerUid).observe().map { it?.getString(FIELD) }
        } else {
            flowOf(null)
        }
        val roles = rolesDoc(coupleId).observe().map { snap ->
            rolesFor(me, snap?.getString(GOOD_BOY), snap?.getString(GOOD_GIRL))
        }
        combine(mine, theirs, roles) { forPartner, forMe, role ->
            CoupleNames(partnerName = forPartner, myName = forMe, myRole = role.first, roleChosenByPartner = role.second)
        }
    }.distinctUntilChanged()

    override suspend fun setMyRole(role: PointsRole): Boolean {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return false
        val me = space.memberIdOrNull ?: return false
        val current = runCatching { rolesDoc(coupleId).get().await() }.getOrNull()
        val next = claimRole(me, role, current?.getString(GOOD_BOY), current?.getString(GOOD_GIRL)) ?: return false
        rolesDoc(coupleId).set(
            mapOf(GOOD_BOY to next.first, GOOD_GIRL to next.second, "updatedAt" to FieldValue.serverTimestamp()),
        )
        return true
    }

    override suspend fun setPartnerName(name: String) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        // Not awaited: Firestore applies it locally at once and syncs when online.
        namesDoc(coupleId, me).set(mapOf(FIELD to name, "updatedAt" to FieldValue.serverTimestamp()))
    }

    private fun rolesDoc(coupleId: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("shared").document("roles")

    private fun namesDoc(coupleId: String, namer: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("names").document(namer)

    companion object {
        private const val FIELD = "nameForPartner"
        private const val GOOD_BOY = "goodBoy"
        private const val GOOD_GIRL = "goodGirl"

        /**
         * Your role from couples/{id}/shared/roles {goodBoy, goodGirl} (uids).
         * Returns (role, chosenByPartner): if only your partner has claimed, you
         * are the other role.
         */
        fun rolesFor(me: String, goodBoy: String?, goodGirl: String?): Pair<PointsRole?, Boolean> = when {
            goodBoy == me -> PointsRole.GoodBoy to false
            goodGirl == me -> PointsRole.GoodGirl to false
            goodBoy != null -> PointsRole.GoodGirl to true
            goodGirl != null -> PointsRole.GoodBoy to true
            else -> null to false
        }

        /** New (goodBoy, goodGirl) after [me] claims [role], or null if the partner holds it. */
        fun claimRole(me: String, role: PointsRole, goodBoy: String?, goodGirl: String?): Pair<String?, String?>? {
            val held = if (role == PointsRole.GoodBoy) goodBoy else goodGirl
            if (held != null && held != me) return null
            val boy = if (role == PointsRole.GoodBoy) me else goodBoy?.takeIf { it != me }
            val girl = if (role == PointsRole.GoodGirl) me else goodGirl?.takeIf { it != me }
            return boy to girl
        }
    }
}
