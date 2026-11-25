package com.choomhub.app.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.PointRules
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.Reward
import com.choomhub.app.domain.model.RewardChange
import com.choomhub.app.domain.model.ShopData
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.model.ShopPush
import com.choomhub.app.domain.model.ShopRules
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.PointsRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the points shop shows. */
data class ShopUi(
    val loaded: Boolean = false,
    val myRole: PointsRole? = null,
    val partnerName: String = "them",
    /** Points you can spend (received minus spent). */
    val myPoints: Int = 0,
    val partnerPoints: Int = 0,
    val catalogue: List<Reward> = emptyList(),
    val changes: List<RewardChange> = emptyList(),
    val proposals: List<ShopProposal> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
) {
    val myPointsLabel: String get() = myRole?.let { "${it.label} points" } ?: "points"
    val partnerPointsLabel: String get() = myRole?.other?.let { "${it.label} points" } ?: "points"

    /** The shop's name for a reward, also for one that's no longer in it. */
    fun nameOf(rewardId: String?): String =
        catalogue.firstOrNull { it.id == rewardId }?.name
            ?: ShopRules.DEFAULTS.firstOrNull { it.id == rewardId }?.name
            ?: changes.firstOrNull { it.id == rewardId }?.name
            ?: "a reward"

    fun priceOf(rewardId: String?): Int? = catalogue.firstOrNull { it.id == rewardId }?.price
}

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shop: ShopRepository,
    points: PointsRepository,
    profile: ProfileRepository,
    private val notifier: Notifier,
) : ViewModel() {

    val state: StateFlow<ShopUi> = combine(shop.observeShop(), points.observeAwards(), profile.observeNames()) { data: ShopData, awards, names ->
        val balances = PointRules.balances(awards, names.myRole, data.purchases)
        val mine = when (names.myRole) {
            PointsRole.GoodBoy -> balances.goodBoy
            PointsRole.GoodGirl -> balances.goodGirl
            null -> awards.filter { it.toMe }.sumOf { it.amount } - ShopRules.spent(data.purchases, byMe = true)
        }
        val theirs = when (names.myRole) {
            PointsRole.GoodBoy -> balances.goodGirl
            PointsRole.GoodGirl -> balances.goodBoy
            null -> awards.filterNot { it.toMe }.sumOf { it.amount } - ShopRules.spent(data.purchases, byMe = false)
        }
        ShopUi(
            loaded = data.loaded,
            myRole = names.myRole,
            partnerName = names.partnerName ?: "them",
            myPoints = mine,
            partnerPoints = theirs,
            catalogue = ShopRules.catalogue(data.changes),
            changes = data.changes,
            proposals = data.proposals,
            purchases = data.purchases,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUi())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    private fun tell(kind: ShopPush, detail: String) = notifier.notifyPartner(PushEvent.Shop, detail = detail, kind = kind.wire)

    /** Buys [reward] if you have the points; returns false (with a message) if not. */
    fun buy(reward: Reward): Boolean {
        val ui = state.value
        if (ui.myPoints < reward.price) {
            _message.value = "You need ${reward.price - ui.myPoints} more ${ui.myPointsLabel} for that."
            return false
        }
        viewModelScope.launch { shop.buy(reward) }
        tell(ShopPush.Bought, reward.name)
        _message.value = "Bought. It's in Yours until you use it."
        return true
    }

    fun use(purchase: Purchase) {
        viewModelScope.launch { shop.use(purchase) }
        tell(ShopPush.Used, purchase.name)
        _message.value = "Cashed in. ${state.value.partnerName} has been told."
    }

    fun giveBack(purchase: Purchase) {
        viewModelScope.launch { shop.giveBack(purchase) }
        _message.value = "Given back: ${purchase.price} points returned."
    }

    fun markDone(purchase: Purchase) {
        viewModelScope.launch { shop.markDone(purchase) }
        tell(ShopPush.Done, purchase.name)
    }

    /** Suggests a new reward; returns an error to show, or null when sent. */
    fun suggestReward(nameInput: String, priceInput: String): String? {
        val name = ShopRules.cleanName(nameInput) ?: return "What's the reward?"
        val price = ShopRules.parsePrice(priceInput).getOrElse { return it.message }
        viewModelScope.launch { shop.propose(ProposalKind.Add, null, name, price) }
        tell(ShopPush.SuggestAdd, "$name · $price points")
        _message.value = "Suggested. It goes in the shop once ${state.value.partnerName} agrees."
        return null
    }

    /** Suggests a new price; returns an error to show, or null when sent. */
    fun suggestPrice(reward: Reward, priceInput: String): String? {
        val price = ShopRules.parsePrice(priceInput).getOrElse { return it.message }
        if (price == reward.price) return "That's already the price."
        viewModelScope.launch { shop.propose(ProposalKind.Price, reward.id, null, price) }
        tell(ShopPush.SuggestPrice, "${reward.name} · $price points")
        _message.value = "Suggested. The price changes once ${state.value.partnerName} agrees."
        return null
    }

    fun suggestRemove(reward: Reward) {
        viewModelScope.launch { shop.propose(ProposalKind.Remove, reward.id, null, null) }
        tell(ShopPush.SuggestRemove, reward.name)
        _message.value = "Suggested. It comes out once ${state.value.partnerName} agrees."
    }

    fun accept(proposal: ShopProposal) {
        val ui = state.value
        val current = ui.changes.firstOrNull { it.id == proposal.rewardId }
        viewModelScope.launch { shop.accept(proposal, current) }
        tell(ShopPush.Agreed, describe(proposal, ui))
    }

    /** Turns down their suggestion, or takes back your own. */
    fun dismiss(proposal: ShopProposal) {
        val ui = state.value
        viewModelScope.launch { shop.dismiss(proposal) }
        if (!proposal.byMe) tell(ShopPush.Declined, describe(proposal, ui))
    }

    private fun describe(p: ShopProposal, ui: ShopUi): String = when (p.kind) {
        ProposalKind.Add -> "${p.name} · ${p.price} points"
        ProposalKind.Price -> "${ui.nameOf(p.rewardId)} · ${p.price} points"
        ProposalKind.Remove -> "Taking out ${ui.nameOf(p.rewardId)}"
    }
}
