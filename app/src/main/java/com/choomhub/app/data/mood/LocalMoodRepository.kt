package com.choomhub.app.data.mood

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.GestureResult
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Device-only builds: your mood is kept on this phone; there's no partner to see it. */
class LocalMoodRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : MoodRepository {

    override fun observeMoods(): Flow<CoupleMoods> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val mine = Mood.fromKey(prefs[MOOD])?.let { MoodUpdate(it, prefs[NOTE], prefs[AT]?.let(Instant::ofEpochMilli)) }
            CoupleMoods(mine, partner = null)
        }

    override suspend fun setMood(mood: Mood, note: String?) {
        dataStore.edit {
            it[MOOD] = mood.key
            if (note == null) it.remove(NOTE) else it[NOTE] = note
            it[AT] = clock.millis()
        }
    }

    private companion object {
        val MOOD = stringPreferencesKey("my_mood")
        val NOTE = stringPreferencesKey("my_mood_note")
        val AT = longPreferencesKey("my_mood_at")
    }
}

/** Device-only builds have no one to send gestures to, and say so. */
class LocalGestureRepository @Inject constructor() : GestureRepository {
    override fun observeRecent(): Flow<List<Gesture>> = flowOf(emptyList())

    override suspend fun send(kind: AffectionKind): GestureResult =
        GestureResult.NotSent("Gestures need the shared space. This build keeps everything on this phone.")
}
