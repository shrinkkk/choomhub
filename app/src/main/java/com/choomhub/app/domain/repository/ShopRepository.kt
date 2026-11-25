package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.Reward
import com.choomhub.app.domain.model.RewardChange
import com.choomhub.app.domain.model.ShopData
import com.choomhub.app.domain.model.ShopProposal
import kotlinx.coroutines.flow.Flow

/** The points shop: what's in it, what's been bought, and changes waiting for the other one to agree. */
interface ShopRepository {
    fun observeShop(): Flow<ShopData>

    suspend fun buy(reward: Reward)

    /** Cash in something you bought: it's their turn. */
    suspend fun use(purchase: Purchase)

    /** Give back something you haven't used, for the points. */
    suspend fun giveBack(purchase: Purchase)

    /** They cashed it in and you did it. */
    suspend fun markDone(purchase: Purchase)

    /** Suggest adding a reward, a new price, or taking one out; the other one has to agree. */
    suspend fun propose(kind: ProposalKind, rewardId: String?, name: String?, price: Int?)

    /** Agree to their suggestion: the shop changes. [current] is what the space already changed about that reward. */
    suspend fun accept(proposal: ShopProposal, current: RewardChange?)

    /** Turn down their suggestion, or take back your own. */
    suspend fun dismiss(proposal: ShopProposal)
}
