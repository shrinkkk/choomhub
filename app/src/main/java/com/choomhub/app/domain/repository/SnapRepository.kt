package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapSendResult
import kotlinx.coroutines.flow.Flow

/**
 * Snaps: each of you has one current snap. Sending a new one removes your
 * previous one unless it was kept in a Memory Lane collection.
 */
interface SnapRepository {
    /** The newest snaps (both of you), newest first. */
    fun observeRecent(): Flow<List<SnapInfo>>

    fun observeSnap(id: String): Flow<SnapInfo?>

    /** The full picture as JPEG bytes; null while loading or if it's gone. */
    fun observeImage(id: String): Flow<ByteArray?>

    /** Sends it, already kept in [keepIn] (you count as the one who put it there). */
    suspend fun send(snap: PreparedSnap, caption: String?, keepIn: List<String> = emptyList()): SnapSendResult

    fun observeCollections(): Flow<List<SnapCollection>>

    /** Every kept snap (for Memory Lane), newest first. */
    fun observeKept(): Flow<List<SnapInfo>>

    /** Creates a collection and returns its id, or null if there's no space. */
    suspend fun createCollection(name: String): String?

    /** Your reaction to a snap (any one emoji, see [com.choomhub.app.domain.model.SnapRules.isReaction]); null removes it. */
    suspend fun react(snapId: String, reaction: String?)

    /** Keeps a snap in a collection, with you as the one who put it there. */
    suspend fun addToCollection(snapId: String, collectionId: String)

    suspend fun renameCollection(collectionId: String, name: String)

    /** The snap shown on top of the collection's stack. */
    suspend fun setCover(collectionId: String, snapId: String)

    /**
     * Takes [snap] out of one collection (only if you put it there). If that leaves it in none and
     * it isn't anyone's current snap, it's deleted for good (see [com.choomhub.app.domain.model.SnapRules.deletesForGood]).
     */
    suspend fun removeFromCollection(snap: SnapInfo, collectionId: String)

    /** Asks to delete a collection; it goes only once they agree. */
    suspend fun askToDeleteCollection(collectionId: String)

    /** Takes back your own ask. */
    suspend fun takeBackDeleteCollection(collectionId: String)

    /**
     * Answers their ask. Agreeing deletes the collection, and for good every snap that was only in it
     * (except current snaps); declining keeps it.
     */
    suspend fun answerDeleteCollection(collectionId: String, agree: Boolean)
}
