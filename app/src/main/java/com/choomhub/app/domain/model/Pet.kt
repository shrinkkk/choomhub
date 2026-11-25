package com.choomhub.app.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** The cat's needs. [perHour] is how fast each drifts on its own (energy comes back by itself). */
enum class PetNeed(val key: String, val label: String, val perHour: Float) {
    Hunger("hunger", "Food", -6f),
    Thirst("thirst", "Water", -8f),
    Energy("energy", "Energy", 9f),
    Litter("litter", "Litter", -5f),
    Fun("fun", "Fun", -7f),
    Affection("affection", "Love", -5f),
}

/** A need as stored: its value at a moment; it's worked out from there whenever it's shown. */
data class NeedLevel(val value: Float, val at: Instant)

/** Personality: two are picked at adoption, and they change how it behaves. */
enum class PetTrait(val key: String, val label: String) {
    Lazy("lazy", "lazy"),
    Curious("curious", "curious"),
    Clingy("clingy", "clingy"),
    Sassy("sassy", "sassy"),
    Playful("playful", "playful"),
    Dramatic("dramatic", "dramatic"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class Food(val key: String, val label: String, val hunger: Float, val play: Float = 0f, val love: Float = 0f, val free: Boolean = false) {
    Kibble("kibble", "Kibble", 35f, free = true),
    WetFood("wet_food", "Wet food", 55f, love = 4f),
    Tuna("tuna", "Tuna", 45f, love = 6f),
    Salmon("salmon", "Salmon treat", 18f, love = 10f),
    Catnip("catnip", "Catnip", 0f, play = 45f),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class ShopCategory(val label: String) { Food("Food"), Toys("Toys"), Furniture("Furniture"), Outfits("Outfits"), Decor("Decor") }

/** Something from the shop. Food comes in packs ([pack] servings); everything else is bought once. */
data class ShopItem(val id: String, val name: String, val category: ShopCategory, val price: Int, val blurb: String, val pack: Int = 0)

/** Something the cat found and brought you. */
data class PetGift(val id: String, val at: Instant, val forMe: Boolean)

/** A job that takes both of you: one starts it, the other finishes it. */
data class PendingCare(val kind: TeamCare, val byMe: Boolean, val at: Instant)

enum class TeamCare(val key: String, val label: String) {
    Vet("vet", "the vet trip"),
    Bath("bath", "bath time"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

data class PetDiaryEntry(val id: String, val text: String, val at: Instant?)

/** Everything about the cat, as both phones see it. */
data class PetState(
    val loaded: Boolean = false,
    val name: String? = null,
    val adoptedAt: Instant? = null,
    /** A name one of you suggested and the other hasn't agreed to yet. */
    val proposedName: String? = null,
    val proposedByMe: Boolean = false,
    val traits: List<PetTrait> = emptyList(),
    val favoriteFood: Food? = null,
    val dislikedFood: Food? = null,
    val needs: Map<PetNeed, NeedLevel> = emptyMap(),
    val weight: Float = PetRules.BASE_WEIGHT,
    val sickSince: Instant? = null,
    val medicineLeft: Int = 0,
    val lastMedicineDay: LocalDate? = null,
    val vetDone: Boolean = false,
    val muddy: Boolean = false,
    val nailsAt: Instant? = null,
    val outfit: String? = null,
    val owned: Set<String> = emptySet(),
    val inventory: Map<String, Int> = emptyMap(),
    val myCoins: Int = 0,
    val partnerCoins: Int = 0,
    val myBond: Int = 0,
    val partnerBond: Int = 0,
    val streak: Int = 0,
    val streakDay: LocalDate? = null,
    val counters: Map<String, Int> = emptyMap(),
    val gifts: List<PetGift> = emptyList(),
    val pending: PendingCare? = null,
    val lastDailyCheck: LocalDate? = null,
    val mySeenDay: LocalDate? = null,
    val myCoinDay: LocalDate? = null,
    val myCoinToday: Int = 0,
    val partnerPettingAt: Instant? = null,
) {
    val adopted: Boolean get() = name != null && adoptedAt != null
    val sick: Boolean get() = sickSince != null
    fun count(key: String) = counters[key] ?: 0
    fun has(item: String) = item in owned
}

/** A change to the cat, made by you. Fields left null aren't touched. */
data class PetPatch(
    /** Needs set to a new value as of now. */
    val needs: Map<PetNeed, Float> = emptyMap(),
    val weight: Float? = null,
    val coins: Int = 0,
    val bond: Int = 0,
    val counters: Map<String, Int> = emptyMap(),
    val inventory: Map<String, Int> = emptyMap(),
    val own: String? = null,
    /** New outfit; "" takes it off. */
    val outfit: String? = null,
    val sickSince: Instant? = null,
    val clearSick: Boolean = false,
    val medicineLeft: Int? = null,
    val lastMedicineDay: LocalDate? = null,
    val vetDone: Boolean? = null,
    val muddy: Boolean? = null,
    val nailsAt: Instant? = null,
    val pending: PendingCare? = null,
    val clearPending: Boolean = false,
    val gift: String? = null,
    val streak: Pair<Int, LocalDate>? = null,
    val lastDailyCheck: LocalDate? = null,
    val seenDay: LocalDate? = null,
    val coinDay: Pair<LocalDate, Int>? = null,
    val pettingAt: Instant? = null,
    val diary: List<String> = emptyList(),
) {
    fun isEmpty() = this == PetPatch()

    operator fun plus(o: PetPatch) = PetPatch(
        needs = needs + o.needs,
        weight = o.weight ?: weight,
        coins = coins + o.coins,
        bond = bond + o.bond,
        counters = (counters.keys + o.counters.keys).associateWith { (counters[it] ?: 0) + (o.counters[it] ?: 0) },
        inventory = (inventory.keys + o.inventory.keys).associateWith { (inventory[it] ?: 0) + (o.inventory[it] ?: 0) },
        own = o.own ?: own,
        outfit = o.outfit ?: outfit,
        sickSince = o.sickSince ?: sickSince,
        clearSick = clearSick || o.clearSick,
        medicineLeft = o.medicineLeft ?: medicineLeft,
        lastMedicineDay = o.lastMedicineDay ?: lastMedicineDay,
        vetDone = o.vetDone ?: vetDone,
        muddy = o.muddy ?: muddy,
        nailsAt = o.nailsAt ?: nailsAt,
        pending = o.pending ?: pending,
        clearPending = clearPending || o.clearPending,
        gift = o.gift ?: gift,
        streak = o.streak ?: streak,
        lastDailyCheck = o.lastDailyCheck ?: lastDailyCheck,
        seenDay = o.seenDay ?: seenDay,
        coinDay = o.coinDay ?: coinDay,
        pettingAt = o.pettingAt ?: pettingAt,
        diary = diary + o.diary,
    )
}

/** What a care action came to: what to store, and a line to show you. */
data class PetOutcome(val patch: PetPatch, val message: String)

/** Things you can do. */
sealed interface PetAction {
    data class Feed(val food: Food) : PetAction
    data object Water : PetAction
    data object CleanLitter : PetAction

    /** A stroke on the head ([zone] 0), back (1) or belly (2). */
    data class Pet(val zone: Int) : PetAction
    data object Brush : PetAction

    /** A play session: [catches] is how many times it caught the toy. */
    data class Play(val toy: String, val catches: Int) : PetAction
    data object TrimNails : PetAction
    data object GiveMedicine : PetAction
    data class StartTeamCare(val kind: TeamCare) : PetAction
    data object JoinTeamCare : PetAction
    data class Buy(val item: ShopItem) : PetAction
    data class Wear(val outfit: String?) : PetAction
}

/** The cat's rules: how needs drift, what each action does, the shop, gifts, trophies and diary lines. */
object PetRules {
    const val BASE_WEIGHT = 4.0f
    const val CHONKY_WEIGHT = 5.6f
    const val KITTEN_DAYS = 21L
    const val MAX_NAME = 16
    const val COIN_PER_CARE = 3
    const val DAILY_COIN_CAP = 60
    const val DAILY_BONUS = 5
    const val COINS_PER_FOCUS_HOUR = 10
    val NAILS_EVERY: Duration = Duration.ofDays(7)
    val TEAM_CARE_EXPIRES: Duration = Duration.ofHours(3)
    val DOUBLE_PURR_WINDOW: Duration = Duration.ofSeconds(20)

    // --- Needs ---

    /** How fast a need drifts for this cat (traits and furniture change it). */
    fun rate(need: PetNeed, state: PetState): Float {
        var r = need.perHour
        val t = state.traits
        when (need) {
            PetNeed.Energy -> {
                if (PetTrait.Lazy in t) r *= 1.4f
                if (state.has("heated_bed")) r *= 1.5f
            }
            PetNeed.Fun -> {
                if (PetTrait.Lazy in t) r *= 0.7f
                if (PetTrait.Playful in t) r *= 1.4f
                if (state.has("tree")) r *= 0.85f
                if (state.has("perch")) r *= 0.9f
            }
            PetNeed.Affection -> if (PetTrait.Clingy in t) r *= 1.5f
            PetNeed.Thirst -> if (state.has("fountain")) r *= 0.6f
            else -> Unit
        }
        if (state.sick && need != PetNeed.Energy) r *= 1.2f
        return r
    }

    fun level(state: PetState, need: PetNeed, now: Instant): Float {
        val stored = state.needs[need] ?: return 70f
        val hours = Duration.between(stored.at, now).toMillis().coerceAtLeast(0) / 3_600_000f
        return (stored.value + rate(need, state) * hours).coerceIn(0f, 100f)
    }

    fun levels(state: PetState, now: Instant): Map<PetNeed, Float> = PetNeed.entries.associateWith { level(state, it, now) }

    /** 0–100: how it's doing overall (the lowest need counts most). */
    fun wellbeing(state: PetState, now: Instant): Float {
        val l = levels(state, now).values
        return (l.min() * 0.6f + l.average().toFloat() * 0.4f) - if (state.sick) 15f else 0f
    }

    fun ageDays(state: PetState, now: Instant): Long = state.adoptedAt?.let { Duration.between(it, now).toDays() } ?: 0

    fun isKitten(state: PetState, now: Instant) = ageDays(state, now) < KITTEN_DAYS

    fun isChonky(state: PetState) = state.weight >= CHONKY_WEIGHT

    /** The need that most needs doing, if any is low. */
    fun mostUrgent(state: PetState, now: Instant, below: Float = 30f): PetNeed? =
        levels(state, now).filter { it.value < below }.minByOrNull { it.value }?.key

    /** A short line about how it is right now. */
    fun status(state: PetState, now: Instant): String {
        val n = state.name ?: "The cat"
        val l = levels(state, now)
        return when {
            state.sick && !state.vetDone -> "$n is poorly. Time for the vet."
            state.sick -> "$n is getting better (${state.medicineLeft} more medicine)."
            state.muddy -> "$n got into something muddy. Bath time (for two)."
            l.getValue(PetNeed.Affection) < 12 -> "$n is hiding under the bed. Some love?"
            l.getValue(PetNeed.Hunger) < 20 -> "$n is sitting by the bowl, staring at you."
            l.getValue(PetNeed.Thirst) < 20 -> "$n keeps checking the water bowl."
            l.getValue(PetNeed.Litter) < 20 -> "$n refuses to use that litter box."
            l.getValue(PetNeed.Energy) < 20 -> "$n is fast asleep."
            l.getValue(PetNeed.Fun) < 20 -> "$n is bored and plotting something."
            wellbeing(state, now) > 75 -> "$n is very content."
            else -> "$n is doing alright."
        }
    }

    // --- Daily things: gifts, getting poorly, getting muddy, streak bonus ---

    /**
     * Run once a day by whichever phone opens the cat first (both get the same answer, from the
     * date). Plus your own daily coins for coming to see it.
     */
    fun daily(state: PetState, now: Instant, zone: ZoneId): PetPatch {
        if (!state.adopted) return PetPatch()
        val today = now.atZone(zone).toLocalDate()
        var p = PetPatch()
        if (state.mySeenDay != today) p += PetPatch(seenDay = today, coins = DAILY_BONUS)
        if (state.lastDailyCheck == today) return p
        val r = Random(today.toEpochDay() * 31 + (state.adoptedAt?.epochSecond ?: 0))
        val l = levels(state, now)
        p += PetPatch(lastDailyCheck = today)
        // A gift (curious cats find more).
        val giftChance = if (PetTrait.Curious in state.traits) 0.6 else 0.35
        if (r.nextDouble() < giftChance) {
            val gift = pickGift(r)
            p += PetPatch(gift = gift.id, counters = mapOf("gifts" to 1), diary = listOf(diaryGift(gift, r)))
        }
        // Getting poorly: more likely when the litter's dirty, it's hungry, or it hasn't been brushed.
        if (!state.sick) {
            var chance = 0.03
            if (l.getValue(PetNeed.Litter) < 25) chance += 0.15
            if (l.getValue(PetNeed.Hunger) < 15) chance += 0.1
            if (r.nextDouble() < chance) {
                p += PetPatch(sickSince = now, vetDone = false, medicineLeft = 0, diary = listOf(diarySick(r)))
            }
        }
        if (!state.muddy && r.nextDouble() < 0.08) {
            p += PetPatch(muddy = true, diary = listOf("Went exploring behind the plant. Came back a different colour. Someone said the word \"bath\"."))
        }
        return p
    }

    // --- Actions ---

    /** What [action] does, or null with a reason it can't happen right now. */
    fun act(state: PetState, action: PetAction, now: Instant, zone: ZoneId, random: Random, myName: String, partnerName: String): PetOutcome {
        val l = levels(state, now)
        val today = now.atZone(zone).toLocalDate()
        val name = state.name ?: "The cat"
        fun care(useful: Boolean, bond: Int = 1): PetPatch {
            val streak = when (state.streakDay) {
                today -> null
                today.minusDays(1) -> (state.streak + 1) to today
                else -> 1 to today
            }
            val earnedToday = if (state.myCoinDay == today) state.myCoinToday else 0
            val coins = if (useful && earnedToday < DAILY_COIN_CAP) COIN_PER_CARE.coerceAtMost(DAILY_COIN_CAP - earnedToday) else 0
            return PetPatch(
                coins = coins,
                bond = bond,
                streak = streak,
                coinDay = if (coins > 0) today to (earnedToday + coins) else null,
            )
        }
        return when (action) {
            is PetAction.Feed -> {
                val food = action.food
                if (!food.free && (state.inventory[food.key] ?: 0) <= 0) return PetOutcome(PetPatch(), "You're out of ${food.label.lowercase()}. The shop has more.")
                val hunger = l.getValue(PetNeed.Hunger)
                val disliked = food == state.dislikedFood
                val favourite = food == state.favoriteFood
                val gain = food.hunger * (if (disliked) 0.4f else 1f) * (if (favourite) 1.2f else 1f)
                val overfed = food.hunger > 0 && hunger > 80
                val weight = state.weight + (if (overfed) 0.12f else 0f) + (if (food == Food.Salmon) 0.03f else 0f)
                val needs = buildMap {
                    if (food.hunger > 0) put(PetNeed.Hunger, (hunger + gain).coerceAtMost(100f))
                    if (food.play > 0) put(PetNeed.Fun, (l.getValue(PetNeed.Fun) + food.play).coerceAtMost(100f))
                    if (food.love > 0 || favourite) put(PetNeed.Affection, (l.getValue(PetNeed.Affection) + food.love + if (favourite) 5f else 0f).coerceAtMost(100f))
                }
                val diary = buildList {
                    if (hunger < 15) add("$myName fed me at last. I had been starving for at least four minutes. I sat on their foot to make a point.")
                    if (favourite && random.nextInt(3) == 0) add("${food.label}. My favourite. $myName is learning.")
                    if (disliked) add("$myName gave me ${food.label.lowercase()}. I ate two bites to be polite and pushed the rest onto the floor.")
                    if (food == Food.Catnip) add("Catnip. I saw sounds. I ran up a wall. Do not ask me about the curtains.")
                }
                val message = when {
                    disliked -> "$name sniffs it, eats a little, and walks off. Not a fan of ${food.label.lowercase()}."
                    favourite -> "$name inhales it. That's the favourite."
                    food == Food.Catnip -> "$name has gone completely feral."
                    overfed -> "$name ate it anyway. Careful, it's getting round."
                    else -> "$name eats it all."
                }
                PetOutcome(
                    care(hunger < 90 || food.play > 0) + PetPatch(
                        needs = needs,
                        weight = weight,
                        counters = mapOf("feeds" to 1),
                        inventory = if (food.free) emptyMap() else mapOf(food.key to -1),
                        diary = diary,
                    ),
                    message,
                )
            }
            PetAction.Water -> PetOutcome(
                care(l.getValue(PetNeed.Thirst) < 90) + PetPatch(needs = mapOf(PetNeed.Thirst to 100f), counters = mapOf("waters" to 1)),
                "Fresh water. $name pretends not to care, then drinks for ages.",
            )
            PetAction.CleanLitter -> PetOutcome(
                care(l.getValue(PetNeed.Litter) < 90) + PetPatch(
                    needs = mapOf(PetNeed.Litter to 100f),
                    counters = mapOf("litters" to 1),
                    diary = if (l.getValue(PetNeed.Litter) < 20) listOf("$myName finally cleaned my box. I used it immediately, to say thank you.") else emptyList(),
                ),
                "All clean. $name inspects it like a hotel critic.",
            )
            is PetAction.Pet -> {
                val love = l.getValue(PetNeed.Affection)
                val bitey = if (PetTrait.Sassy in state.traits) 0.55 else 0.25
                if (action.zone == 2 && random.nextDouble() < bitey) {
                    return PetOutcome(
                        PetPatch(
                            needs = mapOf(PetNeed.Affection to (love - 3f).coerceAtLeast(0f)),
                            counters = mapOf("bites" to 1),
                            diary = listOf("$myName touched the belly. The belly is a trap. Everyone knows this."),
                        ),
                        "It was a trap. $name bites, gently, and looks very pleased with itself.",
                    )
                }
                val gain = when (action.zone) {
                    0 -> 9f
                    1 -> 6f
                    else -> 12f
                }
                val double = state.partnerPettingAt?.let { Duration.between(it, now).abs() <= DOUBLE_PURR_WINDOW } == true
                val bonus = if (double) 15f else 0f
                PetOutcome(
                    care(love < 90, bond = 2) + PetPatch(
                        needs = mapOf(PetNeed.Affection to (love + gain + bonus).coerceAtMost(100f)),
                        counters = mapOf("pets" to 1) + if (double) mapOf("double_purrs" to 1) else emptyMap(),
                        pettingAt = now,
                        diary = if (double) listOf("Both of them petted me at the same time. I purred so hard the floor shook. Best day.") else emptyList(),
                    ),
                    when {
                        double -> "Double purr! You're both petting $name at once."
                        action.zone == 0 -> "$name leans into it, purring."
                        action.zone == 2 -> "$name allows the belly rub. This time."
                        else -> "$name arches into your hand."
                    },
                )
            }
            PetAction.Brush -> PetOutcome(
                care(true) + PetPatch(
                    needs = mapOf(
                        PetNeed.Affection to (l.getValue(PetNeed.Affection) + 8f).coerceAtMost(100f),
                        PetNeed.Fun to (l.getValue(PetNeed.Fun) + 4f).coerceAtMost(100f),
                    ),
                    counters = mapOf("brushes" to 1),
                    diary = if (random.nextInt(5) == 0) listOf("$myName brushed me. Enough fur came off to build a second cat.") else emptyList(),
                ),
                "So much fur. $name is shiny now.",
            )
            is PetAction.Play -> {
                val energy = l.getValue(PetNeed.Energy)
                if (energy < 12) return PetOutcome(PetPatch(), "$name is too sleepy to play. Let it nap.")
                val gain = 15f + action.catches * 6f
                val found = random.nextDouble() < 0.1
                val gift = if (found) pickGift(random) else null
                PetOutcome(
                    care(l.getValue(PetNeed.Fun) < 95) + PetPatch(
                        needs = mapOf(
                            PetNeed.Fun to (l.getValue(PetNeed.Fun) + gain).coerceAtMost(100f),
                            PetNeed.Energy to (energy - 10f - action.catches * 2f).coerceAtLeast(0f),
                        ),
                        weight = (state.weight - 0.03f).coerceAtLeast(BASE_WEIGHT - 0.4f),
                        counters = mapOf("plays" to 1) + if (gift != null) mapOf("gifts" to 1) else emptyMap(),
                        gift = gift?.id,
                        diary = listOfNotNull(
                            if (action.catches >= 5) "Caught the ${toyName(action.toy)} ${action.catches} times. I am a fearsome hunter. $myName clapped." else null,
                            gift?.let { "Found ${it.name.lowercase()} while hunting. Gave it to $myName. You're welcome." },
                        ),
                    ),
                    when {
                        gift != null -> "$name found something mid-chase and dropped it at your feet: ${gift.name.lowercase()}."
                        action.catches == 0 -> "$name watched the ${toyName(action.toy)} with total contempt."
                        else -> "$name caught it ${action.catches} time${if (action.catches == 1) "" else "s"}."
                    },
                )
            }
            PetAction.TrimNails -> {
                val last = state.nailsAt
                if (last != null && Duration.between(last, now) < NAILS_EVERY) return PetOutcome(PetPatch(), "Nails are still short. Next trim in ${Duration.between(now, last.plus(NAILS_EVERY)).toDays() + 1} days.")
                PetOutcome(
                    care(true) + PetPatch(
                        nailsAt = now,
                        counters = mapOf("nails" to 1),
                        needs = mapOf(PetNeed.Affection to (l.getValue(PetNeed.Affection) - 5f).coerceAtLeast(0f)),
                        diary = listOf("$myName cut my claws. I have filed a complaint. With myself. It was dismissed."),
                    ),
                    "Snip, snip. $name is offended but tidy.",
                )
            }
            PetAction.GiveMedicine -> {
                if (!state.sick || !state.vetDone) return PetOutcome(PetPatch(), "No medicine to give right now.")
                if (state.lastMedicineDay == today) return PetOutcome(PetPatch(), "$name already had today's medicine.")
                val left = state.medicineLeft - 1
                PetOutcome(
                    care(true) + PetPatch(
                        medicineLeft = left,
                        lastMedicineDay = today,
                        clearSick = left <= 0,
                        vetDone = if (left <= 0) false else null,
                        counters = mapOf("medicines" to 1),
                        diary = listOf(if (left <= 0) "I am better. I never doubted it. I was very brave." else "Medicine hidden in a treat. I found it. I ate it anyway, for them."),
                    ),
                    if (left <= 0) "$name is all better!" else "Medicine down. $left more day${if (left == 1) "" else "s"} to go.",
                )
            }
            is PetAction.StartTeamCare -> {
                val needed = when (action.kind) {
                    TeamCare.Vet -> state.sick && !state.vetDone
                    TeamCare.Bath -> state.muddy
                }
                if (!needed) return PetOutcome(PetPatch(), if (action.kind == TeamCare.Vet) "$name doesn't need the vet." else "$name is clean enough.")
                PetOutcome(
                    PetPatch(pending = PendingCare(action.kind, byMe = true, at = now)),
                    "You've got the ${if (action.kind == TeamCare.Vet) "carrier" else "towel"} ready. $partnerName needs to help.",
                )
            }
            PetAction.JoinTeamCare -> {
                val pending = state.pending ?: return PetOutcome(PetPatch(), "Nothing to help with.")
                if (pending.byMe) return PetOutcome(PetPatch(), "Waiting for $partnerName to help.")
                when (pending.kind) {
                    TeamCare.Vet -> PetOutcome(
                        care(true, bond = 3) + PetPatch(
                            clearPending = true,
                            vetDone = true,
                            medicineLeft = 3,
                            counters = mapOf("vets" to 1),
                            diary = listOf("They put me in the box with the door. The vet touched my ears. I was sick on nobody. I was so brave."),
                        ),
                        "Vet trip done. One medicine a day for three days.",
                    )
                    TeamCare.Bath -> PetOutcome(
                        care(true, bond = 3) + PetPatch(
                            clearPending = true,
                            muddy = false,
                            counters = mapOf("baths" to 1),
                            needs = mapOf(PetNeed.Affection to (l.getValue(PetNeed.Affection) - 10f).coerceAtLeast(0f)),
                            diary = listOf("BATH. Both of them. A conspiracy. I am now 40% smaller and 100% betrayed."),
                        ),
                        "Bath done. $name is damp, furious, and clean.",
                    )
                }
            }
            is PetAction.Buy -> {
                val item = action.item
                if (state.myCoins < item.price) return PetOutcome(PetPatch(), "Not enough coins (${item.price} needed).")
                if (item.pack == 0 && state.has(item.id)) return PetOutcome(PetPatch(), "You already have that.")
                PetOutcome(
                    PetPatch(
                        coins = -item.price,
                        own = if (item.pack == 0) item.id else null,
                        inventory = if (item.pack > 0) mapOf(item.id to item.pack) else emptyMap(),
                        counters = mapOf("purchases" to 1),
                        diary = if (item.category == ShopCategory.Furniture) listOf("New ${item.name.lowercase()}. I will ignore it for three days, then never leave it.") else emptyList(),
                    ),
                    if (item.pack > 0) "Bought ${item.name.lowercase()} (${item.pack})." else "Bought ${item.name.lowercase()}.",
                )
            }
            is PetAction.Wear -> PetOutcome(
                PetPatch(
                    outfit = action.outfit ?: "",
                    diary = if (action.outfit != null && random.nextInt(2) == 0) listOf("$myName put me in ${outfitName(action.outfit).lowercase().let { if (it.endsWith("s")) it else "a $it" }}. I look incredible and I know it.") else emptyList(),
                ),
                if (action.outfit == null) "$name is au naturel." else "$name is wearing the ${outfitName(action.outfit).lowercase()}.",
            )
        }
    }

    /** Starting traits and tastes, picked at adoption. */
    fun adoptionTraits(random: Random): Triple<List<PetTrait>, Food, Food> {
        val traits = PetTrait.entries.shuffled(random).take(2)
        val foods = Food.entries.filter { it != Food.Kibble && it != Food.Catnip }.shuffled(random)
        return Triple(traits, foods[0], foods[1])
    }

    fun cleanName(input: String): String? = input.replace(Regex("\\s+"), " ").trim().take(MAX_NAME).ifEmpty { null }

    // --- Shop ---

    val SHOP: List<ShopItem> = listOf(
        ShopItem("wet_food", "Wet food", ShopCategory.Food, 15, "Fills it up more than kibble.", pack = 3),
        ShopItem("tuna", "Tuna", ShopCategory.Food, 20, "Stinky, beloved.", pack = 3),
        ShopItem("salmon", "Salmon treats", ShopCategory.Food, 12, "A little food, a lot of love.", pack = 4),
        ShopItem("catnip", "Catnip", ShopCategory.Food, 25, "Instant fun. Side effects: chaos.", pack = 2),
        ShopItem("laser", "Laser pointer", ShopCategory.Toys, 60, "The red dot it will never catch."),
        ShopItem("yarn", "Yarn ball", ShopCategory.Toys, 30, "Rolls, unravels, is destroyed."),
        ShopItem("mouse", "Toy mouse", ShopCategory.Toys, 25, "Squeaks. Lives under the fridge."),
        ShopItem("box", "Cardboard box", ShopCategory.Toys, 20, "Better than anything else you could buy."),
        ShopItem("tree", "Tall cat tree", ShopCategory.Furniture, 150, "Three levels of judging you from above. Less boredom."),
        ShopItem("heated_bed", "Heated bed", ShopCategory.Furniture, 120, "Naps restore energy faster."),
        ShopItem("perch", "Window perch", ShopCategory.Furniture, 80, "Bird TV. Less boredom."),
        ShopItem("hammock", "Hammock", ShopCategory.Furniture, 90, "Swings. Very serious."),
        ShopItem("fountain", "Water fountain", ShopCategory.Furniture, 70, "Running water: it gets thirsty slower."),
        ShopItem("bow_tie", "Bow tie", ShopCategory.Outfits, 40, "For formal occasions, like dinner."),
        ShopItem("bell_collar", "Bell collar", ShopCategory.Outfits, 35, "Jingles. No more sneaking up on you."),
        ShopItem("spiked_collar", "Spiked collar", ShopCategory.Outfits, 50, "Goth cat."),
        ShopItem("bandana", "Bandana", ShopCategory.Outfits, 30, "Off to a barn dance."),
        ShopItem("scarf", "Scarf", ShopCategory.Outfits, 45, "Knitted, wine red, too long."),
        ShopItem("moon_pendant", "Moon pendant", ShopCategory.Outfits, 55, "A little crescent on a chain."),
        ShopItem("witch_hat", "Witch hat", ShopCategory.Outfits, 60, "The obvious choice for a black cat."),
        ShopItem("pumpkin_hat", "Pumpkin hat", ShopCategory.Outfits, 60, "Seasonal. Every season."),
        ShopItem("flower_crown", "Flower crown", ShopCategory.Outfits, 50, "Soft era."),
        ShopItem("beret", "Beret", ShopCategory.Outfits, 45, "Painter, poet, menace."),
        ShopItem("sunglasses", "Sunglasses", ShopCategory.Outfits, 55, "Too cool to look at you."),
        ShopItem("headphones", "Headphones", ShopCategory.Outfits, 50, "Can't hear you. Vibing."),
        ShopItem("bat_wings", "Bat wings", ShopCategory.Outfits, 80, "Cannot fly. Will try."),
        ShopItem("cape", "Velvet cape", ShopCategory.Outfits, 75, "Count Catula."),
        ShopItem("crown", "Crown", ShopCategory.Outfits, 120, "It already thought it was royalty."),
        ShopItem("fairy_lights", "Fairy lights", ShopCategory.Decor, 40, "Warm little lights along the wall."),
        ShopItem("plants", "House plants", ShopCategory.Decor, 30, "For it to knock over."),
        ShopItem("rug", "Round rug", ShopCategory.Decor, 50, "Soft, for belly-up naps."),
        ShopItem("candles", "Candles", ShopCategory.Decor, 35, "Flickering, on the shelf, safely high."),
        ShopItem("poster", "Moth poster", ShopCategory.Decor, 25, "Art."),
        ShopItem("moon_lamp", "Moon lamp", ShopCategory.Decor, 45, "A glowing moon in the corner."),
    )

    fun item(id: String): ShopItem? = SHOP.firstOrNull { it.id == id }

    fun outfitName(id: String?): String = SHOP.firstOrNull { it.id == id }?.name ?: "Nothing"

    fun toyName(id: String): String = when (id) {
        "laser" -> "red dot"
        "yarn" -> "yarn ball"
        "mouse" -> "toy mouse"
        else -> "feather"
    }

    // --- Gifts ---

    data class GiftKind(val id: String, val name: String, val rarity: Int)

    val GIFTS = listOf(
        GiftKind("leaf", "A crunchy leaf", 10),
        GiftKind("sock", "One of your socks", 9),
        GiftKind("feather", "A black feather", 8),
        GiftKind("bottle_cap", "A bottle cap", 8),
        GiftKind("hair_tie", "A hair tie", 9),
        GiftKind("button", "A button", 7),
        GiftKind("moth_wing", "A moth's wing", 5),
        GiftKind("pebble", "A smooth pebble", 8),
        GiftKind("quartz", "A quartz crystal", 4),
        GiftKind("obsidian", "A piece of obsidian", 3),
        GiftKind("geode", "Half a geode", 2),
        GiftKind("ammonite", "A tiny ammonite fossil", 1),
        GiftKind("trilobite", "A trilobite", 1),
        GiftKind("ribbon", "A wine-red ribbon", 6),
        GiftKind("key", "An old key", 3),
    )

    fun gift(id: String): GiftKind? = GIFTS.firstOrNull { it.id == id }

    fun pickGift(random: Random): GiftKind {
        val total = GIFTS.sumOf { it.rarity }
        var roll = random.nextInt(total)
        for (g in GIFTS) {
            roll -= g.rarity
            if (roll < 0) return g
        }
        return GIFTS.first()
    }

    private fun diaryGift(g: GiftKind, r: Random): String = listOf(
        "Brought them ${g.name.lowercase()}. Left it on the pillow. They screamed with joy, I assume.",
        "Found ${g.name.lowercase()}. Hunted it for hours. It is theirs now.",
        "Gift delivered: ${g.name.lowercase()}. No need to thank me. (Thank me.)",
    )[r.nextInt(3)]

    private fun diarySick(r: Random) = listOf(
        "I do not feel like myself. I sneezed on the windowsill. Someone should call a vet. Or my lawyer.",
        "Lay in bed all day. Not even the red dot interests me. This is serious.",
    )[r.nextInt(2)]

    // --- Trophies ---

    data class Trophy(val id: String, val name: String, val description: String, val unlocked: (PetState, Instant) -> Boolean)

    val TROPHIES = listOf(
        Trophy("adopted", "Welcome home", "Adopted the cat together.") { s, _ -> s.adopted },
        Trophy("first_meal", "First meal", "Fed it for the first time.") { s, _ -> s.count("feeds") >= 1 },
        Trophy("first_purr", "First purr", "Petted it until it purred.") { s, _ -> s.count("pets") >= 1 },
        Trophy("hunter", "Fearsome hunter", "Played with it 10 times.") { s, _ -> s.count("plays") >= 10 },
        Trophy("clean_box", "Clean freak", "Cleaned the litter 20 times.") { s, _ -> s.count("litters") >= 20 },
        Trophy("week", "A whole week", "Looked after it 7 days in a row.") { s, _ -> s.streak >= 7 },
        Trophy("month", "Devoted", "Looked after it 30 days in a row.") { s, _ -> s.streak >= 30 },
        Trophy("first_gift", "For you", "It brought you its first gift.") { s, _ -> s.gifts.isNotEmpty() },
        Trophy("collector", "Collector", "10 gifts on the shelf.") { s, _ -> s.gifts.size >= 10 },
        Trophy("rockhound", "Rockhound", "It found a fossil.") { s, _ -> s.gifts.any { it.id == "ammonite" || it.id == "trilobite" } },
        Trophy("vet", "Brave little cat", "Took it to the vet together.") { s, _ -> s.count("vets") >= 1 },
        Trophy("bath", "Survived bath time", "Bathed it together.") { s, _ -> s.count("baths") >= 1 },
        Trophy("fashion", "Fashion icon", "Owns three outfits.") { s, _ -> s.owned.count { id -> item(id)?.category == ShopCategory.Outfits } >= 3 },
        Trophy("double_purr", "Double purr", "Petted it at the same time.") { s, _ -> s.count("double_purrs") >= 1 },
        Trophy("bitten", "Belly trap", "Fell for the belly.") { s, _ -> s.count("bites") >= 1 },
        Trophy("grown", "All grown up", "It's not a kitten any more.") { s, now -> s.adopted && ageDays(s, now) >= KITTEN_DAYS },
        Trophy("chonk", "Absolute unit", "It got chonky. Oops.") { s, _ -> isChonky(s) },
        Trophy("best_friends", "Best friends", "Both of you have a bond of 100.") { s, _ -> s.myBond >= 100 && s.partnerBond >= 100 },
    )

    /** Bond levels, by bond points. */
    fun bondLabel(points: Int): String = when {
        points >= 300 -> "soulmates"
        points >= 150 -> "best friends"
        points >= 60 -> "close"
        points >= 20 -> "friendly"
        else -> "getting to know you"
    }

    /** Whether one pending team job is still open (it lapses after a few hours). */
    fun pendingOpen(p: PendingCare?, now: Instant) = p != null && Duration.between(p.at, now) < TEAM_CARE_EXPIRES
}

/** [patch] applied to this state, as of [now] (used where there's no server: on this phone alone, and in tests). */
fun PetState.applied(patch: PetPatch, now: Instant): PetState {
    val g = patch.gift?.let { PetGift(it, now, forMe = true) }
    return copy(
        needs = needs + patch.needs.mapValues { NeedLevel(it.value, now) },
        weight = patch.weight ?: weight,
        myCoins = myCoins + patch.coins,
        myBond = myBond + patch.bond,
        counters = (counters.keys + patch.counters.keys).associateWith { (counters[it] ?: 0) + (patch.counters[it] ?: 0) },
        inventory = (inventory.keys + patch.inventory.keys).associateWith { (inventory[it] ?: 0) + (patch.inventory[it] ?: 0) },
        owned = owned + listOfNotNull(patch.own),
        outfit = when (patch.outfit) {
            null -> outfit
            "" -> null
            else -> patch.outfit
        },
        sickSince = if (patch.clearSick) null else patch.sickSince ?: sickSince,
        medicineLeft = patch.medicineLeft ?: medicineLeft,
        lastMedicineDay = patch.lastMedicineDay ?: lastMedicineDay,
        vetDone = patch.vetDone ?: vetDone,
        muddy = patch.muddy ?: muddy,
        nailsAt = patch.nailsAt ?: nailsAt,
        pending = if (patch.clearPending) null else patch.pending ?: pending,
        gifts = gifts + listOfNotNull(g),
        streak = patch.streak?.first ?: streak,
        streakDay = patch.streak?.second ?: streakDay,
        lastDailyCheck = patch.lastDailyCheck ?: lastDailyCheck,
        mySeenDay = patch.seenDay ?: mySeenDay,
        myCoinDay = patch.coinDay?.first ?: myCoinDay,
        myCoinToday = patch.coinDay?.second ?: myCoinToday,
    )
}
