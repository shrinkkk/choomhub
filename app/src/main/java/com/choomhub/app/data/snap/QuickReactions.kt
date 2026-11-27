package com.choomhub.app.data.snap

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choomhub.app.domain.model.SnapRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Your four quick reactions (yours alone, kept on this phone), and whether you've seen the long-press tip. */
@Singleton
class QuickReactions @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private val data = dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    fun observe(): Flow<List<String>> = data.map { prefs -> decode(prefs[QUICK]) }

    fun observeTipSeen(): Flow<Boolean> = data.map { it[TIP_SEEN] == true }

    /** Puts [emoji] in place [index]; if it's already one of the four, the two swap. */
    suspend fun replace(index: Int, emoji: String) {
        if (index !in 0 until SnapRules.QUICK_COUNT || !SnapRules.isReaction(emoji)) return
        dataStore.edit { prefs ->
            val now = decode(prefs[QUICK]).toMutableList()
            val already = now.indexOf(emoji)
            if (already >= 0) now[already] = now[index]
            now[index] = emoji
            prefs[QUICK] = now.joinToString(SEP)
        }
    }

    suspend fun markTipSeen() {
        dataStore.edit { it[TIP_SEEN] = true }
    }

    private fun decode(raw: String?): List<String> {
        val saved = raw?.split(SEP)?.filter(SnapRules::isReaction)
        return if (saved != null && saved.size == SnapRules.QUICK_COUNT) saved else SnapRules.QUICK_DEFAULT
    }

    private companion object {
        val QUICK = stringPreferencesKey("quick_reactions")
        val TIP_SEEN = booleanPreferencesKey("emoji_tip_seen")
        const val SEP = "\u001F"
    }
}
