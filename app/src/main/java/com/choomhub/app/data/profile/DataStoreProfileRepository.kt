package com.choomhub.app.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Device-local storage for the couple's names.
 *
 * Interim home until the shared space syncs: these values move into Room
 * (members table) with the backend milestone. `my_name` is written only by
 * that future sync, never by this device's UI.
 */
class DataStoreProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ProfileRepository {

    override fun observeNames(): Flow<CoupleNames> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            CoupleNames(
                partnerName = prefs[PARTNER_NAME],
                myName = prefs[MY_NAME],
                myRole = prefs[MY_ROLE]?.let { runCatching { PointsRole.valueOf(it) }.getOrNull() },
            )
        }
        .distinctUntilChanged()

    override suspend fun setPartnerName(name: String) {
        dataStore.edit { it[PARTNER_NAME] = name }
    }

    override suspend fun setMyRole(role: PointsRole): Boolean {
        dataStore.edit { it[MY_ROLE] = role.name }
        return true
    }

    private companion object {
        val MY_ROLE = stringPreferencesKey("my_points_role")
        val PARTNER_NAME = stringPreferencesKey("partner_name")
        val MY_NAME = stringPreferencesKey("my_name")
    }
}
