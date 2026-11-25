package com.choomhub.app.domain.model

import java.time.Instant

/** Something in the points shop: bought with your points, given by your partner. */
data class Reward(val id: String, val name: String, val price: Int, val custom: Boolean = false)

/** What the space has changed about a reward (both of you agreed): a new price, removed, or added. */
data class RewardChange(val id: String, val name: String?, val price: Int?, val custom: Boolean, val removed: Boolean)

/** A reward someone bought, from owning it to it being done. */
enum class PurchaseState(val key: String) {
    /** Bought, not used yet. */
    Owned("owned"),

    /** Cashed in: the other person's turn. */
    Used("used"),

    /** They did it. */
    Done("done"),

    /** Given back for the points before it was used. */
    Returned("returned"),
    ;

    companion object {
        fun from(key: String?): PurchaseState? = entries.firstOrNull { it.key == key }
    }
}

data class Purchase(
    val id: String,
    val rewardId: String,
    val name: String,
    val price: Int,
    /** True if you bought it (they owe it to you). */
    val byMe: Boolean,
    val at: Instant?,
    val state: PurchaseState,
    val usedAt: Instant? = null,
    val doneAt: Instant? = null,
)

/** A change to the shop that needs the other person to agree. */
enum class ProposalKind(val key: String) {
    Add("add"),
    Price("price"),
    Remove("remove"),
    ;

    companion object {
        fun from(key: String?): ProposalKind? = entries.firstOrNull { it.key == key }
    }
}

data class ShopProposal(
    val id: String,
    val kind: ProposalKind,
    /** The reward it's about (null when adding a new one). */
    val rewardId: String?,
    /** The new reward's name, for [ProposalKind.Add]. */
    val name: String?,
    /** The new reward's price, or the new price. */
    val price: Int?,
    val byMe: Boolean,
    val at: Instant?,
)

/** Everything the shop shows. */
data class ShopData(
    val loaded: Boolean = false,
    val changes: List<RewardChange> = emptyList(),
    val proposals: List<ShopProposal> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
)

/** What the shop notification says happened; matches the Worker's ShopKinds. */
enum class ShopPush(val wire: String) {
    Bought("bought"),
    Used("used"),
    Done("done"),
    SuggestAdd("suggest_add"),
    SuggestPrice("suggest_price"),
    SuggestRemove("suggest_remove"),
    Agreed("agreed"),
    Declined("declined"),
}

object ShopRules {
    const val MAX_NAME = 40
    const val MIN_PRICE = 1
    const val MAX_PRICE = 500

    /** The shop you start with. Prices can be changed (both of you agree). */
    val DEFAULTS = listOf(
        Reward("selfie", "Selfie on demand, within 10 minutes", 5),
        Reward("voice_song", "Voice note of them singing a song you choose", 12),
        Reward("head_massage", "5-minute head massage at the next meetup", 15),
        Reward("drawing", "A drawing of you by them, any skill level", 25),
        Reward("playlist", "A playlist made for you, 15+ songs", 25),
        Reward("letter", "Handwritten letter, posted or handed over at the next meetup", 40),
        Reward("neck_massage", "15-minute neck/shoulder massage at the next meetup", 45),
        Reward("outfit", "Choose their outfit for the next meetup", 60),
        Reward("framed_snap", "A Memory Lane snap printed and framed", 75),
        Reward("yes_hour", "One hour of “yes” on demand", 80),
        Reward("surprise_gift", "A surprise gift, their choice", 100),
        Reward("your_day", "A full day at the next meetup where you decide everything", 100),
    )

    /** The shop as it stands: the defaults with agreed changes, then added rewards, cheapest first. */
    fun catalogue(changes: List<RewardChange>): List<Reward> {
        val byId = changes.associateBy { it.id }
        val defaults = DEFAULTS.mapNotNull { d ->
            val c = byId[d.id]
            when {
                c == null -> d
                c.removed -> null
                else -> d.copy(price = c.price ?: d.price)
            }
        }
        val defaultIds = DEFAULTS.map { it.id }.toSet()
        val added = changes.filter { it.custom && !it.removed && it.id !in defaultIds && it.name != null && it.price != null }
            .map { Reward(it.id, it.name!!, it.price!!, custom = true) }
        return (defaults + added).sortedWith(compareBy({ it.price }, { it.name }))
    }

    /** Points spent on things not given back. */
    fun spent(purchases: List<Purchase>, byMe: Boolean): Int =
        purchases.filter { it.byMe == byMe && it.state != PurchaseState.Returned }.sumOf { it.price }

    /** What you can spend: the points you've received minus what you've bought. */
    fun available(received: Int, purchases: List<Purchase>): Int = received - spent(purchases, byMe = true)

    fun cleanName(input: String): String? = input.replace(Regex("\\s+"), " ").trim().take(MAX_NAME).ifEmpty { null }

    fun parsePrice(input: String): Result<Int> {
        val n = input.trim().toIntOrNull() ?: return Result.failure(IllegalArgumentException("How many points?"))
        if (n < MIN_PRICE || n > MAX_PRICE) return Result.failure(IllegalArgumentException("Between $MIN_PRICE and $MAX_PRICE points."))
        return Result.success(n)
    }

    /** A change waiting on the other person for this reward, if any. */
    fun pendingFor(rewardId: String, proposals: List<ShopProposal>): ShopProposal? =
        proposals.firstOrNull { it.rewardId == rewardId && it.kind != ProposalKind.Add }
}
