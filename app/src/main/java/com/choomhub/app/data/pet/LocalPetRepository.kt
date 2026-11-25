package com.choomhub.app.data.pet

import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.NeedLevel
import com.choomhub.app.domain.model.PetDiaryEntry
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetPatch
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.domain.model.applied
import com.choomhub.app.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Without Firebase: the cat lives on this phone only, until the app is closed. */
@Singleton
open class LocalPetRepository @Inject constructor() : PetRepository {
    val pet = MutableStateFlow(PetState(loaded = true))
    val diary = MutableStateFlow<List<PetDiaryEntry>>(emptyList())
    private var next = 0

    override fun observePet(): Flow<PetState> = pet

    override fun observeDiary(): Flow<List<PetDiaryEntry>> = diary

    override suspend fun proposeName(name: String) {
        pet.update { it.copy(proposedName = name, proposedByMe = true) }
    }

    override suspend fun adopt(name: String, traits: List<PetTrait>, favorite: Food, disliked: Food, now: Instant) {
        pet.update {
            it.copy(
                name = name,
                adoptedAt = now,
                proposedName = null,
                traits = traits,
                favoriteFood = favorite,
                dislikedFood = disliked,
                needs = PetNeed.entries.associateWith { NeedLevel(80f, now) },
            )
        }
        addDiary(listOf("They brought me home and called me $name. I have decided to allow it."), now)
    }

    override suspend fun apply(patch: PetPatch) {
        val now = Instant.now()
        pet.update { it.applied(patch, now) }
        addDiary(patch.diary, now)
    }

    private fun addDiary(lines: List<String>, now: Instant) {
        if (lines.isEmpty()) return
        diary.update { old -> lines.map { PetDiaryEntry("d${next++}", it, now) }.reversed() + old }
    }
}
