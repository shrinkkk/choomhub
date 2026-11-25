package com.choomhub.app.data.shop

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.domain.model.Reward
import com.choomhub.app.domain.model.RewardChange
import com.choomhub.app.domain.model.ShopData
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.ShopRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * couples/{id}/rewards/{id} (agreed changes: new prices, removals, added rewards),
 * couples/{id}/rewardProposals/{id} (waiting for the other one) and couples/{id}/purchases/{id}.
 * Writes aren't awaited: Firestore applies them locally at once and syncs when online.
 */
@Singleton
class FirestoreShopRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : ShopRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun couple(id: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeShop(): Flow<ShopData> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(ShopData(loaded = true))
        val c = couple(coupleId)
        combine(
            c.collection(REWARDS).observe(),
            c.collection(PROPOSALS).observe(),
            c.collection(PURCHASES).observe(),
        ) { rewards, proposals, purchases ->
            ShopData(
                loaded = true,
                changes = rewards?.documents.orEmpty().map(::changeFrom),
                proposals = proposals?.documents.orEmpty().mapNotNull { proposalFrom(it, me) }.sortedBy { it.at },
                purchases = purchases?.documents.orEmpty().mapNotNull { purchaseFrom(it, me) }.sortedByDescending { it.at },
            )
        }
    }.distinctUntilChanged()

    private suspend fun where(): Pair<String, String>? {
        val space = spaceRepository.observeSpace().first()
        return (space.coupleIdOrNull ?: return null) to (space.memberIdOrNull ?: return null)
    }

    override suspend fun buy(reward: Reward) {
        val (coupleId, me) = where() ?: return
        couple(coupleId).collection(PURCHASES).document().set(
            mapOf(
                "rewardId" to reward.id,
                "name" to reward.name,
                "price" to reward.price,
                "by" to me,
                "at" to FieldValue.serverTimestamp(),
                "state" to PurchaseState.Owned.key,
            ),
        )
    }

    override suspend fun use(purchase: Purchase) {
        val (coupleId, _) = where() ?: return
        couple(coupleId).collection(PURCHASES).document(purchase.id)
            .update(mapOf("state" to PurchaseState.Used.key, "usedAt" to FieldValue.serverTimestamp()))
    }

    override suspend fun giveBack(purchase: Purchase) {
        val (coupleId, _) = where() ?: return
        couple(coupleId).collection(PURCHASES).document(purchase.id).update("state", PurchaseState.Returned.key)
    }

    override suspend fun markDone(purchase: Purchase) {
        val (coupleId, _) = where() ?: return
        couple(coupleId).collection(PURCHASES).document(purchase.id)
            .update(mapOf("state" to PurchaseState.Done.key, "doneAt" to FieldValue.serverTimestamp()))
    }

    override suspend fun propose(kind: ProposalKind, rewardId: String?, name: String?, price: Int?) {
        val (coupleId, me) = where() ?: return
        couple(coupleId).collection(PROPOSALS).document().set(
            buildMap {
                put("kind", kind.key)
                if (rewardId != null) put("rewardId", rewardId)
                if (name != null) put("name", name)
                if (price != null) put("price", price)
                put("by", me)
                put("at", FieldValue.serverTimestamp())
            },
        )
    }

    override suspend fun accept(proposal: ShopProposal, current: RewardChange?) {
        val (coupleId, _) = where() ?: return
        val c = couple(coupleId)
        val rewardId = if (proposal.kind == ProposalKind.Add) proposal.id else proposal.rewardId ?: return
        val doc = buildMap<String, Any> {
            when (proposal.kind) {
                ProposalKind.Add -> {
                    put("name", proposal.name ?: return)
                    put("price", proposal.price ?: return)
                    put("custom", true)
                    put("removed", false)
                }
                ProposalKind.Price -> {
                    current?.name?.let { put("name", it) }
                    put("price", proposal.price ?: return)
                    put("custom", current?.custom ?: false)
                    put("removed", false)
                }
                ProposalKind.Remove -> {
                    current?.name?.let { put("name", it) }
                    current?.price?.let { put("price", it) }
                    put("custom", current?.custom ?: false)
                    put("removed", true)
                }
            }
            put("proposal", proposal.id)
        }
        db.batch()
            .set(c.collection(REWARDS).document(rewardId), doc)
            .delete(c.collection(PROPOSALS).document(proposal.id))
            .commit()
    }

    override suspend fun dismiss(proposal: ShopProposal) {
        val (coupleId, _) = where() ?: return
        couple(coupleId).collection(PROPOSALS).document(proposal.id).delete()
    }

    companion object {
        const val REWARDS = "rewards"
        const val PROPOSALS = "rewardProposals"
        const val PURCHASES = "purchases"

        fun changeFrom(doc: DocumentSnapshot) = RewardChange(
            id = doc.id,
            name = doc.getString("name"),
            price = doc.getLong("price")?.toInt(),
            custom = doc.getBoolean("custom") == true,
            removed = doc.getBoolean("removed") == true,
        )

        fun proposalFrom(doc: DocumentSnapshot, me: String): ShopProposal? = ShopProposal(
            id = doc.id,
            kind = ProposalKind.from(doc.getString("kind")) ?: return null,
            rewardId = doc.getString("rewardId"),
            name = doc.getString("name"),
            price = doc.getLong("price")?.toInt(),
            byMe = doc.getString("by") == me,
            at = doc.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
        )

        fun purchaseFrom(doc: DocumentSnapshot, me: String): Purchase? {
            fun time(field: String) = doc.getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant()
            return Purchase(
                id = doc.id,
                rewardId = doc.getString("rewardId") ?: return null,
                name = doc.getString("name") ?: return null,
                price = doc.getLong("price")?.toInt() ?: return null,
                byMe = doc.getString("by") == me,
                at = time("at"),
                state = PurchaseState.from(doc.getString("state")) ?: return null,
                usedAt = time("usedAt"),
                doneAt = time("doneAt"),
            )
        }
    }
}
