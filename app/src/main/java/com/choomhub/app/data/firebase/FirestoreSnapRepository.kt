package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapRules
import com.choomhub.app.domain.model.SnapSendResult
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * couples/{id}/snaps/{snapId}           metadata + thumbnail (small, cheap to list)
 * couples/{id}/snaps/{snapId}/image/full the full JPEG (read only when shown)
 * couples/{id}/collections/{cid}        Memory Lane collections
 *
 * Pictures are stored inside Firestore because the free plan has no Cloud
 * Storage (ADR-022). Only equality and single-field queries are used, so no
 * composite indexes have to be created by hand.
 */
class FirestoreSnapRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : SnapRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun couple(coupleId: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId)
    private fun snaps(coupleId: String) = couple(coupleId).collection("snaps")
    private fun image(coupleId: String, snapId: String) = snaps(coupleId).document(snapId).collection("image").document("full")

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> inSpace(empty: T, block: (coupleId: String, me: String) -> Flow<T>): Flow<T> =
        spaceRepository.observeSpace().flatMapLatest { space ->
            val coupleId = space.coupleIdOrNull
            val me = space.memberIdOrNull
            if (coupleId == null || me == null) flowOf(empty) else block(coupleId, me)
        }

    override fun observeRecent(): Flow<List<SnapInfo>> = inSpace(emptyList()) { coupleId, me ->
        snaps(coupleId).orderBy("sentAt", Query.Direction.DESCENDING).limit(RECENT).observe()
            .map { snap -> snap?.documents.orEmpty().mapNotNull { snapFrom(it, me) } }
    }.distinctUntilChanged()

    override fun observeSnap(id: String): Flow<SnapInfo?> = inSpace(null) { coupleId, me ->
        snaps(coupleId).document(id).observe().map { it?.let { doc -> snapFrom(doc, me) } }
    }.distinctUntilChanged()

    override fun observeImage(id: String): Flow<ByteArray?> = inSpace(null) { coupleId, _ ->
        image(coupleId, id).observe().map { it?.getBlob("data")?.toBytes() }
    }

    override suspend fun send(snap: PreparedSnap, caption: String?): SnapSendResult {
        val space = spaceRepository.observeSpace().first()
        if (space !is SpaceState.Paired) return SnapSendResult.NotSent(NOT_PAIRED)
        val coupleId = space.coupleId
        val me = space.memberId
        // Your previous snaps that nobody kept are removed once the new one is written.
        val previous = runCatching {
            snaps(coupleId).whereEqualTo("from", me).whereEqualTo("kept", false).get(Source.CACHE).await().documents
        }.getOrDefault(emptyList())
        val ref = snaps(coupleId).document()
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "from" to me,
                "caption" to caption,
                "sentAt" to FieldValue.serverTimestamp(),
                "width" to snap.width,
                "height" to snap.height,
                "thumb" to Blob.fromBytes(snap.thumb),
                "collections" to emptyList<String>(),
                "kept" to false,
            ),
        )
        batch.set(image(coupleId, ref.id), mapOf("data" to Blob.fromBytes(snap.full)))
        // Not awaited: it shows at once and uploads when there's a connection.
        batch.commit()
        // Removing the old ones is a separate batch: if one was kept in the meantime the rules
        // refuse that delete, and the new snap must not fail with it.
        if (previous.isNotEmpty()) {
            val cleanup = db.batch()
            previous.forEach { old ->
                cleanup.delete(image(coupleId, old.id))
                cleanup.delete(old.reference)
            }
            cleanup.commit()
        }
        return SnapSendResult.Sent
    }

    override fun observeCollections(): Flow<List<SnapCollection>> = inSpace(emptyList()) { coupleId, _ ->
        couple(coupleId).collection("collections").orderBy("createdAt", Query.Direction.ASCENDING).observe()
            .map { snap ->
                snap?.documents.orEmpty().mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    SnapCollection(
                        doc.id,
                        name,
                        doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
                        cover = doc.getString("cover"),
                    )
                }
            }
    }.distinctUntilChanged()

    override fun observeKept(): Flow<List<SnapInfo>> = inSpace(emptyList()) { coupleId, me ->
        // Equality only (sorted here), so no composite index is needed.
        snaps(coupleId).whereEqualTo("kept", true).observe()
            .map { snap -> snap?.documents.orEmpty().mapNotNull { snapFrom(it, me) }.sortedByDescending { it.sentAt } }
    }.distinctUntilChanged()

    override suspend fun createCollection(name: String): String? {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return null
        val me = space.memberIdOrNull ?: return null
        val ref = couple(coupleId).collection("collections").document()
        ref.set(mapOf("name" to name, "createdBy" to me, "createdAt" to FieldValue.serverTimestamp()))
        return ref.id
    }

    override suspend fun react(snapId: String, reaction: String?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        snaps(coupleId).document(snapId).update("reactions.$me", reaction ?: FieldValue.delete())
    }

    override suspend fun setCollections(snapId: String, collectionIds: List<String>) {
        val coupleId = spaceRepository.observeSpace().first().coupleIdOrNull ?: return
        snaps(coupleId).document(snapId).update(mapOf("collections" to collectionIds, "kept" to collectionIds.isNotEmpty()))
    }

    override suspend fun renameCollection(collectionId: String, name: String) {
        val coupleId = spaceRepository.observeSpace().first().coupleIdOrNull ?: return
        couple(coupleId).collection("collections").document(collectionId).update("name", name)
    }

    override suspend fun setCover(collectionId: String, snapId: String) {
        val coupleId = spaceRepository.observeSpace().first().coupleIdOrNull ?: return
        couple(coupleId).collection("collections").document(collectionId).update("cover", snapId)
    }

    override suspend fun removeFromCollection(snap: SnapInfo, collectionId: String) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        val remaining = snap.collections - collectionId
        snaps(coupleId).document(snap.id).update(mapOf("collections" to remaining, "kept" to remaining.isNotEmpty()))
        if (remaining.isEmpty() && !SnapRules.isCurrent(snap, recent(coupleId, me))) deleteForGood(coupleId, listOf(snap.id))
    }

    override suspend fun deleteCollection(collectionId: String) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        val inIt = runCatching { snaps(coupleId).whereArrayContains("collections", collectionId).get().await().documents }
            .getOrDefault(emptyList())
            .mapNotNull { snapFrom(it, me) }
        // First un-keep (so the rules allow the deletes), then delete what nothing keeps any more.
        val batch = db.batch()
        batch.delete(couple(coupleId).collection("collections").document(collectionId))
        inIt.forEach { snap ->
            val remaining = snap.collections - collectionId
            batch.update(snaps(coupleId).document(snap.id), mapOf("collections" to remaining, "kept" to remaining.isNotEmpty()))
        }
        batch.commit()
        val recent = recent(coupleId, me)
        deleteForGood(coupleId, inIt.filter { SnapRules.deletesForGood(it, collectionId, recent) }.map { it.id })
    }

    /** The newest snaps of you both; empty if they can't be read (then nothing counts as deletable). */
    private suspend fun recent(coupleId: String, me: String): List<SnapInfo> = runCatching {
        snaps(coupleId).orderBy("sentAt", Query.Direction.DESCENDING).limit(RECENT).get().await()
            .documents.mapNotNull { snapFrom(it, me) }
    }.getOrDefault(emptyList())

    private fun deleteForGood(coupleId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        // Not awaited, like every write here: it applies at once and syncs when there's a connection.
        ids.chunked(200).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { id ->
                batch.delete(image(coupleId, id))
                batch.delete(snaps(coupleId).document(id))
            }
            batch.commit()
        }
    }

    companion object {
        const val RECENT = 12L
        const val NOT_PAIRED = "They haven't joined your space yet. Snaps go out once they do."

        @Suppress("UNCHECKED_CAST")
        fun snapFrom(doc: DocumentSnapshot, me: String): SnapInfo? {
            val from = doc.getString("from") ?: return null
            val reactions = (doc.get("reactions") as? Map<*, *>).orEmpty()
                .mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }.toMap()
            return SnapInfo(
                id = doc.id,
                fromMe = from == me,
                caption = doc.getString("caption")?.ifBlank { null },
                sentAt = doc.getTimestamp("sentAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
                width = doc.getLong("width")?.toInt() ?: 0,
                height = doc.getLong("height")?.toInt() ?: 0,
                thumb = doc.getBlob("thumb")?.toBytes(),
                collections = (doc.get("collections") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                myReaction = reactions[me],
                partnerReaction = reactions.entries.firstOrNull { it.key != me }?.value,
            )
        }
    }
}
