package com.choomhub.app.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.choomhub.app.data.pet.PetSeen
import com.choomhub.app.data.pet.PetSeenState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/** A DataStore that holds nothing, for fakes that keep their own state. */
object NoDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = emptyPreferences()
}

/** What's been seen in the Pet House, kept in memory. */
class FakePetSeen(start: PetSeenState = PetSeenState()) : PetSeen(NoDataStore) {
    val state = MutableStateFlow(start)
    override fun observe(): Flow<PetSeenState> = state
    override suspend fun diary(at: Instant) = state.update { it.copy(diaryAt = maxOf(it.diaryAt ?: Instant.EPOCH, at)) }
    override suspend fun gifts(count: Int) = state.update { it.copy(gifts = count) }
    override suspend fun trophies(ids: Set<String>) = state.update { it.copy(trophies = (it.trophies ?: emptySet()) + ids) }
}
