package com.choomhub.app.data.snap

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

/** Remembers, on this phone, the last of their snaps you opened. */
@Singleton
class SeenSnaps @Inject constructor(private val dataStore: DataStore<Preferences>) {
    fun observe(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SEEN] }

    suspend fun markSeen(snapId: String) {
        dataStore.edit { it[SEEN] = snapId }
    }

    private companion object {
        val SEEN = stringPreferencesKey("seen_snap_id")
    }
}
