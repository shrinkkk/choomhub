package com.choomhub.app.testing

import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.GestureResult
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeMoodRepository(initial: CoupleMoods = CoupleMoods(null, null)) : MoodRepository {
    val moods = MutableStateFlow(initial)
    val options = MutableStateFlow(Mood.defaults)
    var saves = 0

    override fun observeMoods(): Flow<CoupleMoods> = moods

    override fun observeOptions(): Flow<List<MoodOption>> = options

    override suspend fun setOptions(options: List<MoodOption>) {
        this.options.value = options
    }

    override suspend fun setMood(mood: MoodOption, note: String?) {
        saves++
        moods.update { it.copy(mine = MoodUpdate(mood, note, Instant.EPOCH)) }
    }
}

/** Sends succeed (and appear in the history) unless [refusal] is set. */
class FakeGestureRepository(var refusal: String? = null) : GestureRepository {
    val history = MutableStateFlow<List<Gesture>>(emptyList())

    override fun observeRecent(): Flow<List<Gesture>> = history

    override suspend fun send(kind: AffectionKind): GestureResult {
        refusal?.let { return GestureResult.NotSent(it) }
        history.update { listOf(Gesture("g${it.size}", kind, fromMe = true, sentAt = Instant.EPOCH)) + it }
        return GestureResult.Sent
    }
}

/** Awards go straight into the ledger unless [refusal] is set. */
class FakePointsRepository(var refusal: String? = null) : com.choomhub.app.domain.repository.PointsRepository {
    val ledger = MutableStateFlow<List<com.choomhub.app.domain.model.PointAward>>(emptyList())

    override fun observeAwards(): Flow<List<com.choomhub.app.domain.model.PointAward>> = ledger

    override suspend fun award(amount: Int, reason: String?): com.choomhub.app.domain.model.AwardResult {
        refusal?.let { return com.choomhub.app.domain.model.AwardResult.NotGiven(it) }
        ledger.update { listOf(com.choomhub.app.domain.model.PointAward("p${it.size}", toMe = false, amount, reason, Instant.EPOCH)) + it }
        return com.choomhub.app.domain.model.AwardResult.Given
    }
}

class FakeSnapPromptSettings(initial: Boolean = true) : com.choomhub.app.feature.snaps.prompt.SnapPromptSettings {
    val enabled = MutableStateFlow(initial)
    override fun observeEnabled(): Flow<Boolean> = enabled
    override suspend fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}
