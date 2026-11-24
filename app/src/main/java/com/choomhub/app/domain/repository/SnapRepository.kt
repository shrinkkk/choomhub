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

    suspend fun send(snap: PreparedSnap, caption: String?): SnapSendResult

    fun observeCollections(): Flow<List<SnapCollection>>

    /** Every kept snap (for Memory Lane), newest first. */
    fun observeKept(): Flow<List<SnapInfo>>

    /** Creates a collection and returns its id, or null if there's no space. */
    suspend fun createCollection(name: String): String?

    /** Which collections a snap is kept in; an empty list un-keeps it. */
    suspend fun setCollections(snapId: String, collectionIds: List<String>)
}
