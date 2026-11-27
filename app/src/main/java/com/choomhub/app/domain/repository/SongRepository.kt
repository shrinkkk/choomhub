package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongPair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The songs you share: one each, replaced when you send another. */
interface SongRepository {
    fun observeSongs(): Flow<SongPair>

    /** Sends [song] to your partner, replacing the one you sent before (and its rating). */
    suspend fun send(song: SongDraft)

    /** Rates the song they sent you, 1-10. */
    suspend fun rate(rating: Int)

    /** Every song either of you has sent, newest first (the record shelf). */
    fun observeShelf(): Flow<List<com.choomhub.app.domain.model.ShelfSong>> = observeSongs().map { pair ->
        listOfNotNull(
            pair.mine?.let { com.choomhub.app.domain.model.ShelfSong("me:${it.key}", it.trackId, it.title, it.artist, it.cover, true, it.sentAt, it.rating, it.kind) },
            pair.theirs?.let { com.choomhub.app.domain.model.ShelfSong("them:${it.key}", it.trackId, it.title, it.artist, it.cover, false, it.sentAt, it.rating, it.kind) },
        ).sortedByDescending { it.sentAt }
    }
}

/** Turns a Spotify link into a song you can send. */
interface SongLookup {
    suspend fun lookup(link: String): Result<SongDraft>
}

/** A failure the person can act on. */
class SongLookupException(message: String) : Exception(message)
