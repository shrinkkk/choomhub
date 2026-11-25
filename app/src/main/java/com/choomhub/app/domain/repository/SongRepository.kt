package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongPair
import kotlinx.coroutines.flow.Flow

/** The songs you share: one each, replaced when you send another. */
interface SongRepository {
    fun observeSongs(): Flow<SongPair>

    /** Sends [song] to your partner, replacing the one you sent before (and its rating). */
    suspend fun send(song: SongDraft)

    /** Rates the song they sent you, 1-10. */
    suspend fun rate(rating: Int)
}

/** Turns a Spotify link into a song you can send. */
interface SongLookup {
    suspend fun lookup(link: String): Result<SongDraft>
}

/** A failure the person can act on. */
class SongLookupException(message: String) : Exception(message)
