package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.GestureResult
import com.choomhub.app.domain.model.MoodOption
import kotlinx.coroutines.flow.Flow

/** Your mood and your partner's. Saving works offline and syncs later. */
interface MoodRepository {
    fun observeMoods(): Flow<CoupleMoods>

    /** [note] must already be cleaned (see [com.choomhub.app.domain.model.MoodRules]). */
    suspend fun setMood(mood: MoodOption, note: String?)

    /** The space's mood list (the defaults until someone edits it). */
    fun observeOptions(): Flow<List<MoodOption>>

    suspend fun setOptions(options: List<MoodOption>)
}

/** Hugs, forehead kisses, head pats and tomatoes, in both directions. */
interface GestureRepository {
    /** The most recent gestures, newest first. */
    fun observeRecent(): Flow<List<Gesture>>

    suspend fun send(kind: AffectionKind): GestureResult
}
