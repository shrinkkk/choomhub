package com.choomhub.app.data.pet

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this phone has already looked at in the Pet House: the newest diary entry, how many gifts,
 * which trophies. Each of you has your own, so a dot stays on theirs until they look. Null means
 * this phone hasn't looked yet (then everything there now counts as seen).
 */
data class PetSeenState(val diaryAt: Instant? = null, val gifts: Int? = null, val trophies: Set<String>? = null)

@Singleton
open class PetSeen @Inject constructor(private val dataStore: DataStore<Preferences>) {
    open fun observe(): Flow<PetSeenState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { PetSeenState(it[DIARY]?.let(Instant::ofEpochMilli), it[GIFTS], it[TROPHIES]) }

    open suspend fun diary(at: Instant) {
        dataStore.edit { it[DIARY] = maxOf(it[DIARY] ?: 0L, at.toEpochMilli()) }
    }

    open suspend fun gifts(count: Int) {
        dataStore.edit { it[GIFTS] = count }
    }

    open suspend fun trophies(ids: Set<String>) {
        dataStore.edit { it[TROPHIES] = (it[TROPHIES] ?: emptySet()) + ids }
    }

    private companion object {
        val DIARY = longPreferencesKey("pet_seen_diary_at")
        val GIFTS = intPreferencesKey("pet_seen_gifts")
        val TROPHIES = stringSetPreferencesKey("pet_seen_trophies")
    }
}
