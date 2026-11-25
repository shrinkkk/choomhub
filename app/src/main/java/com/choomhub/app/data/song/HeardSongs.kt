package com.choomhub.app.data.song

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers, on this phone, the last of their songs you played (so it stops showing as new). */
@Singleton
class HeardSongs @Inject constructor(private val dataStore: DataStore<Preferences>) {
    fun observe(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HEARD] }

    suspend fun markHeard(key: String) {
        dataStore.edit { it[HEARD] = key }
    }

    private companion object {
        val HEARD = stringPreferencesKey("heard_song_key")
    }
}
