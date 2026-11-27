package com.choomhub.app.data.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers, on this phone, the newest tomato from them you've already been splattered with.
 * Missing means this phone hasn't looked yet (then whatever's there counts as seen).
 */
@Singleton
open class TomatoesSeen @Inject constructor(private val dataStore: DataStore<Preferences>) {
    open fun observe(): Flow<Instant?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[SEEN]?.let(Instant::ofEpochMilli) ?: if (prefs[STARTED] == true) Instant.EPOCH else null }

    open suspend fun markSeen(at: Instant) {
        dataStore.edit {
            it[SEEN] = maxOf(it[SEEN] ?: 0L, at.toEpochMilli())
            it[STARTED] = true
        }
    }

    private companion object {
        val SEEN = longPreferencesKey("tomato_seen_at")
        val STARTED = androidx.datastore.preferences.core.booleanPreferencesKey("tomato_seen_started")
    }
}
