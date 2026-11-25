package com.choomhub.app.data.snap

import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapSendResult
import com.choomhub.app.domain.repository.SnapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Device-only builds have no one to send snaps to, and say so. */
class LocalSnapRepository @Inject constructor() : SnapRepository {
    override fun observeRecent(): Flow<List<SnapInfo>> = flowOf(emptyList())
    override fun observeSnap(id: String): Flow<SnapInfo?> = flowOf(null)
    override fun observeImage(id: String): Flow<ByteArray?> = flowOf(null)
    override suspend fun send(snap: PreparedSnap, caption: String?): SnapSendResult =
        SnapSendResult.NotSent("Snaps need the shared space. This build keeps everything on this phone.")
    override fun observeCollections(): Flow<List<SnapCollection>> = flowOf(emptyList())
    override fun observeKept(): Flow<List<SnapInfo>> = flowOf(emptyList())
    override suspend fun createCollection(name: String): String? = null
    override suspend fun react(snapId: String, reaction: String?) = Unit
    override suspend fun setCollections(snapId: String, collectionIds: List<String>) = Unit
}
