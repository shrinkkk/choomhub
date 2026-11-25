package com.choomhub.app.testing

import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapSendResult
import com.choomhub.app.domain.repository.SnapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** In-memory snaps that follow the real rules: a new snap replaces your unkept ones. */
class FakeSnapRepository(var refusal: String? = null) : SnapRepository {
    val snaps = MutableStateFlow<List<SnapInfo>>(emptyList())
    val images = mutableMapOf<String, ByteArray>()
    val collections = MutableStateFlow<List<SnapCollection>>(emptyList())
    private var next = 0

    override fun observeRecent(): Flow<List<SnapInfo>> = snaps.map { list -> list.sortedByDescending { it.sentAt } }
    override fun observeSnap(id: String): Flow<SnapInfo?> = snaps.map { list -> list.firstOrNull { it.id == id } }
    override fun observeImage(id: String): Flow<ByteArray?> = flowOf(images[id])

    override suspend fun send(snap: PreparedSnap, caption: String?): SnapSendResult {
        refusal?.let { return SnapSendResult.NotSent(it) }
        val id = "mine${next++}"
        images[id] = snap.full
        snaps.update { list ->
            list.filterNot { it.fromMe && !it.isKept } + SnapInfo(id, true, caption, Instant.now(), snap.width, snap.height, snap.thumb, emptyList())
        }
        return SnapSendResult.Sent
    }

    override fun observeCollections(): Flow<List<SnapCollection>> = collections
    override fun observeKept(): Flow<List<SnapInfo>> = snaps.map { list -> list.filter { it.isKept } }

    override suspend fun createCollection(name: String): String {
        val id = "c${next++}"
        collections.update { it + SnapCollection(id, name, Instant.now()) }
        return id
    }

    override suspend fun react(snapId: String, reaction: String?) {
        snaps.update { list -> list.map { if (it.id == snapId) it.copy(myReaction = reaction) else it } }
    }

    override suspend fun setCollections(snapId: String, collectionIds: List<String>) {
        snaps.update { list -> list.map { if (it.id == snapId) it.copy(collections = collectionIds) else it } }
    }

    /** A snap from them, with a real tiny JPEG so it can be drawn. */
    fun receive(id: String, caption: String?, jpeg: ByteArray, at: Instant = Instant.now()) {
        images[id] = jpeg
        snaps.update { it + SnapInfo(id, false, caption, at, 4, 3, jpeg, emptyList()) }
    }
}
