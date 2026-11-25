package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.PetDiaryEntry
import com.choomhub.app.domain.model.PetPatch
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PetTrait
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The shared cat: its state, its diary, and changes either of you make. */
interface PetRepository {
    fun observePet(): Flow<PetState>

    /** Newest first. */
    fun observeDiary(): Flow<List<PetDiaryEntry>>

    /** Suggests a name; the other one agrees to it (with [adopt]) or suggests another. */
    suspend fun proposeName(name: String)

    /** Adopts the cat under [name], with its personality and tastes. */
    suspend fun adopt(name: String, traits: List<PetTrait>, favorite: Food, disliked: Food, now: Instant)

    /** Stores a change you made (needs, coins, purchases, diary lines...). */
    suspend fun apply(patch: PetPatch)
}
