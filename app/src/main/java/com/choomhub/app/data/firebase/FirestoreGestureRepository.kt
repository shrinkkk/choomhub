package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.GestureResult
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * couples/{id}/gestures/{autoId}: an append-only history of gestures. Rules
 * only allow adding one as yourself; nothing can be edited or deleted.
 */
class FirestoreGestureRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : GestureRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRecent(): Flow<List<Gesture>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(emptyList())
        gestures(coupleId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(RECENT)
            .observe()
            .map { snap -> snap?.documents.orEmpty().mapNotNull { gestureFrom(it, me) } }
    }.distinctUntilChanged()

    override suspend fun send(kind: AffectionKind): GestureResult {
        val space = spaceRepository.observeSpace().first()
        if (space !is SpaceState.Paired) return GestureResult.NotSent(NOT_PAIRED)
        // Not awaited: it shows straight away and goes out as soon as there's a connection.
        gestures(space.coupleId).add(mapOf("kind" to kind.key, "from" to space.memberId, "sentAt" to FieldValue.serverTimestamp()))
        return GestureResult.Sent
    }

    private fun gestures(coupleId: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("gestures")

    companion object {
        const val RECENT = 20L
        const val NOT_PAIRED = "They haven't joined your space yet. Gestures go out once they do."

        fun gestureFrom(doc: DocumentSnapshot, me: String): Gesture? {
            val kind = AffectionKind.fromKey(doc.getString("kind")) ?: return null
            val at = doc.getTimestamp("sentAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            return Gesture(doc.id, kind, fromMe = doc.getString("from") == me, sentAt = at?.toDate()?.toInstant())
        }
    }
}
