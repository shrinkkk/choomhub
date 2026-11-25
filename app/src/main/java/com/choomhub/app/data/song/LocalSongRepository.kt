package com.choomhub.app.data.song

import com.choomhub.app.domain.model.SharedSong
import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongPair
import com.choomhub.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Without Firebase: your own song stays on this phone (there's nobody to send it to). */
@Singleton
open class LocalSongRepository @Inject constructor() : SongRepository {
    val songs = MutableStateFlow(SongPair(loaded = true))

    override fun observeSongs(): Flow<SongPair> = songs

    override suspend fun send(song: SongDraft) {
        songs.update { it.copy(mine = SharedSong(song.trackId, song.title, song.artist, song.cover, Instant.now(), rating = null)) }
    }

    override suspend fun rate(rating: Int) {
        songs.update { p -> p.copy(theirs = p.theirs?.copy(rating = rating)) }
    }
}
