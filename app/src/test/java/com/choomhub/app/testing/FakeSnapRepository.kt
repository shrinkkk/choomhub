package com.choomhub.app.testing

import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapRules
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

    override suspend fun send(snap: PreparedSnap, caption: String?, keepIn: List<String>): SnapSendResult {
        refusal?.let { return SnapSendResult.NotSent(it) }
        val id = "mine${next++}"
        images[id] = snap.full
        snaps.update { list ->
            list.filterNot { it.fromMe && !it.isKept } + SnapInfo(id, true, caption, Instant.now(), snap.width, snap.height, snap.thumb, keepIn, addedByMe = keepIn.toSet())
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

    override suspend fun addToCollection(snapId: String, collectionId: String) {
        snaps.update { list ->
            list.map { if (it.id == snapId && collectionId !in it.collections) it.copy(collections = it.collections + collectionId, addedByMe = it.addedByMe + collectionId) else it }
        }
    }

    override suspend fun renameCollection(collectionId: String, name: String) {
        collections.update { list -> list.map { if (it.id == collectionId) it.copy(name = name) else it } }
    }

    override suspend fun setCover(collectionId: String, snapId: String) {
        collections.update { list -> list.map { if (it.id == collectionId) it.copy(cover = snapId) else it } }
    }

    override suspend fun removeFromCollection(snap: SnapInfo, collectionId: String) {
        if (!snaps.value.first { it.id == snap.id }.canRemoveFrom(collectionId)) return
        val recent = snaps.value
        val goes = SnapRules.deletesForGood(snaps.value.first { it.id == snap.id }, collectionId, recent)
        snaps.update { list ->
            list.mapNotNull { if (it.id == snap.id) it.copy(collections = it.collections - collectionId, addedByMe = it.addedByMe - collectionId).takeUnless { goes } else it }
        }
    }

    override suspend fun askToDeleteCollection(collectionId: String) {
        collections.update { list -> list.map { if (it.id == collectionId) it.copy(deleteAskedByMe = true) else it } }
    }

    override suspend fun takeBackDeleteCollection(collectionId: String) {
        collections.update { list -> list.map { if (it.id == collectionId) it.copy(deleteAskedByMe = null) else it } }
    }

    override suspend fun answerDeleteCollection(collectionId: String, agree: Boolean) {
        if (agree) deleteCollection(collectionId) else takeBackDeleteCollection(collectionId)
    }

    /** They ask to delete [collectionId] (it waits for you to agree). */
    fun partnerAsksToDelete(collectionId: String) {
        collections.update { list -> list.map { if (it.id == collectionId) it.copy(deleteAskedByMe = false) else it } }
    }

    private fun deleteCollection(collectionId: String) {
        val recent = snaps.value
        collections.update { list -> list.filterNot { it.id == collectionId } }
        snaps.update { list ->
            list.mapNotNull {
                when {
                    collectionId !in it.collections -> it
                    SnapRules.deletesForGood(it, collectionId, recent) -> null
                    else -> it.copy(collections = it.collections - collectionId, addedByMe = it.addedByMe - collectionId)
                }
            }
        }
    }

    /** A kept snap from them, already in [collectionIds]. */
    fun keep(id: String, caption: String?, jpeg: ByteArray, at: Instant, vararg collectionIds: String, fromMe: Boolean = false, addedByMe: Boolean = true) {
        images[id] = jpeg
        snaps.update { it + SnapInfo(id, fromMe, caption, at, 4, 3, jpeg, collectionIds.toList(), addedByMe = if (addedByMe) collectionIds.toSet() else emptySet()) }
    }

    /** A snap from them, with a real tiny JPEG so it can be drawn. */
    fun receive(id: String, caption: String?, jpeg: ByteArray, at: Instant = Instant.now()) {
        images[id] = jpeg
        snaps.update { it + SnapInfo(id, false, caption, at, 4, 3, jpeg, emptyList()) }
    }
}
