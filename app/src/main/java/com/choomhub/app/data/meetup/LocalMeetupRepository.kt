package com.choomhub.app.data.meetup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.choomhub.app.domain.model.Meetup
import com.choomhub.app.domain.repository.MeetupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Device-local meetup, used until the shared space is connected; then the
 * Firestore-backed implementation takes over behind the same interface.
 */
class LocalMeetupRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MeetupRepository {

    override fun observeMeetup(): Flow<Meetup?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[MEETUP_AT]?.let { Meetup(Instant.ofEpochMilli(it)) } }
        .distinctUntilChanged()

    override suspend fun schedule(at: Instant) {
        dataStore.edit { it[MEETUP_AT] = at.toEpochMilli() }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(MEETUP_AT) }
    }

    private companion object {
        val MEETUP_AT = longPreferencesKey("meetup_at_epoch_ms")
    }
}
