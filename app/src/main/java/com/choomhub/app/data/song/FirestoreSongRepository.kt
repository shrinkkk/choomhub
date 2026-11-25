package com.choomhub.app.data.song

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.SharedSong
import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongPair
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.SongRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** couples/{id}/songs/{member id}: the song each of you last sent, and its rating. Writes aren't awaited. */
@Singleton
class FirestoreSongRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : SongRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun songs(coupleId: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection(SONGS)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSongs(): Flow<SongPair> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(SongPair(loaded = true))
        val mine = songs(coupleId).document(me).observe().map { it?.let(::songFrom) }
        val theirs = (space as? SpaceState.Paired)?.let { songs(coupleId).document(it.partnerUid).observe().map { d -> d?.let(::songFrom) } } ?: flowOf(null)
        combine(mine, theirs) { m, t -> SongPair(m, t, loaded = true) }
    }.distinctUntilChanged()

    override suspend fun send(song: SongDraft) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        // A full replace: the old song's rating goes with it.
        songs(coupleId).document(me).set(
            buildMap {
                put("trackId", song.trackId)
                put("title", song.title)
                if (song.artist != null) put("artist", song.artist)
                if (song.cover != null) put("cover", Blob.fromBytes(song.cover))
                put("sentAt", FieldValue.serverTimestamp())
            },
        )
    }

    override suspend fun rate(rating: Int) {
        val space = spaceRepository.observeSpace().first() as? SpaceState.Paired ?: return
        songs(space.coupleId).document(space.partnerUid)
            .update(mapOf("rating" to rating, "ratedAt" to FieldValue.serverTimestamp()))
    }

    companion object {
        const val SONGS = "songs"

        fun songFrom(doc: DocumentSnapshot): SharedSong? {
            if (!doc.exists()) return null
            return SharedSong(
                trackId = doc.getString("trackId") ?: return null,
                title = doc.getString("title") ?: return null,
                artist = doc.getString("artist"),
                cover = doc.getBlob("cover")?.toBytes(),
                sentAt = doc.getTimestamp("sentAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
                rating = doc.getLong("rating")?.toInt(),
            )
        }
    }
}
