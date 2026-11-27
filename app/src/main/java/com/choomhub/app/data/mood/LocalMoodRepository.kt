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
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.model.coloured
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
            val key = prefs[MOOD]
            val label = prefs[LABEL] ?: Mood.fromKey(key)?.label
            val mine = if (key != null && label != null) MoodUpdate(MoodOption(key, label), prefs[NOTE], prefs[AT]?.let(Instant::ofEpochMilli)) else null
            val options = prefs[OPTIONS]?.let(::decode)?.ifEmpty { null } ?: Mood.defaults
            CoupleMoods(mine, partner = null).coloured(options)
        }

    override fun observeOptions(): Flow<List<MoodOption>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[OPTIONS]?.let(::decode)?.ifEmpty { null } ?: Mood.defaults }

    override suspend fun setOptions(options: List<MoodOption>) {
        dataStore.edit { it[OPTIONS] = encode(options) }
    }

    override suspend fun setMood(mood: MoodOption, note: String?) {
        dataStore.edit {
            it[MOOD] = mood.key
            it[LABEL] = mood.label
            if (note == null) it.remove(NOTE) else it[NOTE] = note
            it[AT] = clock.millis()
        }
    }

    private companion object {
        val MOOD = stringPreferencesKey("my_mood")
        val NOTE = stringPreferencesKey("my_mood_note")
        val AT = longPreferencesKey("my_mood_at")
        val LABEL = stringPreferencesKey("my_mood_label")
        val OPTIONS = stringPreferencesKey("mood_options")

        // Unit/record separators can't be typed into a label, so they're safe delimiters.
        fun encode(options: List<MoodOption>) = options.joinToString("\u001E") { "${it.key}\u001F${it.label}" + (it.colour?.let { c -> "\u001F$c" } ?: "") }

        fun decode(text: String) = text.split('\u001E').mapNotNull { row ->
            val parts = row.split('\u001F')
            if (parts.size in 2..3 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) MoodOption(parts[0], parts[1], parts.getOrNull(2)?.ifEmpty { null }) else null
        }
    }
}

/** Device-only builds have no one to send gestures to, and say so. */
class LocalGestureRepository @Inject constructor() : GestureRepository {
    override fun observeRecent(): Flow<List<Gesture>> = flowOf(emptyList())

    override suspend fun send(kind: AffectionKind): GestureResult =
        GestureResult.NotSent("Gestures need the shared space. This build keeps everything on this phone.")
}

/** Device-only builds have no one to give points to. */
class LocalPointsRepository @Inject constructor() : com.choomhub.app.domain.repository.PointsRepository {
    override fun observeAwards(): Flow<List<com.choomhub.app.domain.model.PointAward>> = flowOf(emptyList())

    override suspend fun award(amount: Int, reason: String?): com.choomhub.app.domain.model.AwardResult =
        com.choomhub.app.domain.model.AwardResult.NotAwarded("Points need the shared space. This build keeps everything on this phone.")
}
