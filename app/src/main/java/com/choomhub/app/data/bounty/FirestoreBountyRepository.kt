package com.choomhub.app.data.bounty

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.Bounty
import com.choomhub.app.domain.model.BountyDue
import com.choomhub.app.domain.model.BountyRules
import com.choomhub.app.domain.model.BountyStatus
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.BountyRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * couples/{id}/bounties/{bid}  { task, reward, due, by, status, at, claimedAt?, paidAt? }
 * Paying writes the points (from you, to them) in the same batch.
 */
@Singleton
class FirestoreBountyRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : BountyRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun bounties(coupleId: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection(COLLECTION)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<Bounty>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(emptyList())
        bounties(coupleId).observe().map { snap -> snap?.documents.orEmpty().mapNotNull { bountyFrom(it, me) }.sortedByDescending { it.postedAt } }
    }

    private suspend fun ids(): Triple<String, String, String?>? {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return null
        val me = space.memberIdOrNull ?: return null
        return Triple(coupleId, me, (space as? SpaceState.Paired)?.partnerUid)
    }

    override suspend fun post(task: String, reward: Int, due: BountyDue) {
        val (coupleId, me, _) = ids() ?: return
        val clean = BountyRules.cleanTask(task) ?: return
        bounties(coupleId).document().set(
            mapOf(
                "task" to clean,
                "reward" to reward.coerceIn(BountyRules.MIN_REWARD, BountyRules.MAX_REWARD),
                "due" to due.wire,
                "by" to me,
                "status" to BountyStatus.Open.wire,
                "at" to FieldValue.serverTimestamp(),
            ),
        )
    }

    override suspend fun claim(bounty: Bounty) {
        val (coupleId, _, _) = ids() ?: return
        if (bounty.byMe || bounty.status != BountyStatus.Open) return
        bounties(coupleId).document(bounty.id).update(mapOf("status" to BountyStatus.Claimed.wire, "claimedAt" to FieldValue.serverTimestamp()))
    }

    override suspend fun notYet(bounty: Bounty) {
        val (coupleId, _, _) = ids() ?: return
        if (!bounty.byMe || bounty.status != BountyStatus.Claimed) return
        bounties(coupleId).document(bounty.id).update(mapOf("status" to BountyStatus.Open.wire, "claimedAt" to FieldValue.delete()))
    }

    override suspend fun pay(bounty: Bounty) {
        val (coupleId, me, partner) = ids() ?: return
        if (!bounty.byMe || bounty.status != BountyStatus.Claimed || partner == null) return
        val batch = db.batch()
        batch.update(bounties(coupleId).document(bounty.id), mapOf("status" to BountyStatus.Paid.wire, "paidAt" to FieldValue.serverTimestamp()))
        batch.set(
            db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("points").document(),
            mapOf("from" to me, "to" to partner, "amount" to bounty.reward, "reason" to BountyRules.reason(bounty.task), "at" to FieldValue.serverTimestamp()),
        )
        batch.commit()
    }

    override suspend fun takeDown(bounty: Bounty) {
        val (coupleId, _, _) = ids() ?: return
        if (!bounty.byMe || bounty.status == BountyStatus.Claimed) return
        bounties(coupleId).document(bounty.id).delete()
    }

    companion object {
        const val COLLECTION = "bounties"

        fun bountyFrom(doc: DocumentSnapshot, me: String): Bounty? {
            val task = doc.getString("task") ?: return null
            return Bounty(
                id = doc.id,
                task = task,
                reward = doc.getLong("reward")?.toInt() ?: return null,
                due = BountyDue.fromWire(doc.getString("due")),
                byMe = doc.getString("by") == me,
                status = BountyStatus.fromWire(doc.getString("status")),
                postedAt = doc.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
                paidAt = doc.getTimestamp("paidAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
            )
        }
    }
}
