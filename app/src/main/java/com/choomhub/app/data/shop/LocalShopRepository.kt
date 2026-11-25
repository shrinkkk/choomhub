package com.choomhub.app.data.shop

import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.domain.model.Reward
import com.choomhub.app.domain.model.RewardChange
import com.choomhub.app.domain.model.ShopData
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Without Firebase: the shop lives on this phone (and there's nobody to agree, so suggestions just wait). */
@Singleton
open class LocalShopRepository @Inject constructor() : ShopRepository {
    val data = MutableStateFlow(ShopData(loaded = true))

    override fun observeShop(): Flow<ShopData> = data

    private fun id() = UUID.randomUUID().toString().take(12)

    override suspend fun buy(reward: Reward) {
        data.update { it.copy(purchases = listOf(Purchase(id(), reward.id, reward.name, reward.price, true, Instant.now(), PurchaseState.Owned)) + it.purchases) }
    }

    private fun change(purchase: Purchase, block: (Purchase) -> Purchase) {
        data.update { d -> d.copy(purchases = d.purchases.map { if (it.id == purchase.id) block(it) else it }) }
    }

    override suspend fun use(purchase: Purchase) = change(purchase) { it.copy(state = PurchaseState.Used, usedAt = Instant.now()) }

    override suspend fun giveBack(purchase: Purchase) = change(purchase) { it.copy(state = PurchaseState.Returned) }

    override suspend fun markDone(purchase: Purchase) = change(purchase) { it.copy(state = PurchaseState.Done, doneAt = Instant.now()) }

    override suspend fun propose(kind: ProposalKind, rewardId: String?, name: String?, price: Int?) {
        data.update { it.copy(proposals = it.proposals + ShopProposal(id(), kind, rewardId, name, price, byMe = true, at = Instant.now())) }
    }

    override suspend fun accept(proposal: ShopProposal, current: RewardChange?) {
        val rewardId = if (proposal.kind == ProposalKind.Add) proposal.id else proposal.rewardId ?: return
        val next = when (proposal.kind) {
            ProposalKind.Add -> RewardChange(rewardId, proposal.name, proposal.price, custom = true, removed = false)
            ProposalKind.Price -> RewardChange(rewardId, current?.name, proposal.price, current?.custom ?: false, removed = false)
            ProposalKind.Remove -> RewardChange(rewardId, current?.name, current?.price, current?.custom ?: false, removed = true)
        }
        data.update { d ->
            d.copy(
                changes = d.changes.filterNot { it.id == rewardId } + next,
                proposals = d.proposals.filterNot { it.id == proposal.id },
            )
        }
    }

    override suspend fun dismiss(proposal: ShopProposal) {
        data.update { d -> d.copy(proposals = d.proposals.filterNot { it.id == proposal.id }) }
    }
}
