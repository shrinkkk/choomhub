package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.AwardResult
import com.choomhub.app.domain.model.PointAward
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.repository.PointsRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * couples/{id}/points/{autoId}: { from, to, amount, reason, at }. Rules only allow giving points
 * to your partner, 1–100 at a time; entries can't be edited or deleted.
 */
class FirestorePointsRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : PointsRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun ledger(coupleId: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("points")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAwards(): Flow<List<PointAward>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.accountOrNull?.uid
        if (coupleId == null || me == null) return@flatMapLatest flowOf(emptyList())
        ledger(coupleId).orderBy("at", Query.Direction.DESCENDING).observe()
            .map { snap -> snap?.documents.orEmpty().mapNotNull { awardFrom(it, me) } }
    }.distinctUntilChanged()

    override suspend fun award(amount: Int, reason: String?): AwardResult {
        val space = spaceRepository.observeSpace().first()
        if (space !is SpaceState.Paired) return AwardResult.NotGiven("They haven't joined your space yet.")
        ledger(space.coupleId).add(
            mapOf(
                "from" to space.account.uid,
                "to" to space.partnerUid,
                "amount" to amount,
                "reason" to reason,
                "at" to FieldValue.serverTimestamp(),
            ),
        )
        return AwardResult.Given
    }

    companion object {
        fun awardFrom(doc: DocumentSnapshot, me: String): PointAward? {
            val amount = doc.getLong("amount")?.toInt() ?: return null
            val to = doc.getString("to") ?: return null
            return PointAward(
                id = doc.id,
                toMe = to == me,
                amount = amount,
                reason = doc.getString("reason")?.ifBlank { null },
                at = doc.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
            )
        }
    }
}
