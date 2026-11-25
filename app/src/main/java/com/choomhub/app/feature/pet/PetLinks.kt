package com.choomhub.app.feature.pet

import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetPatch
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.repository.PetRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Where the rest of the app touches the cat: being nice to each other, and focus hours. */
@Singleton
class PetLinks @Inject constructor(
    private val pets: PetRepository,
    private val clock: Clock,
) {
    /** It likes it when you're nice to each other: a gesture gives it a little love too. */
    suspend fun gestureSent() {
        val s = pets.observePet().first()
        if (!s.adopted) return
        val love = PetRules.level(s, PetNeed.Affection, clock.instant())
        pets.apply(PetPatch(needs = mapOf(PetNeed.Affection to (love + 4f).coerceAtMost(100f))))
    }

    /** Coins for focus hours (the same hours that earn points). */
    suspend fun focusHours(hours: Int) {
        if (hours <= 0) return
        val s = pets.observePet().first()
        if (!s.adopted) return
        pets.apply(PetPatch(coins = hours * PetRules.COINS_PER_FOCUS_HOUR))
    }
}
