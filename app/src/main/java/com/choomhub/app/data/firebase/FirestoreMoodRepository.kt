package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.SpaceRepository
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

/** couples/{id}/moods/{uid}: one document per person, overwritten on each change. */
class FirestoreMoodRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : MoodRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMoods(): Flow<CoupleMoods> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.accountOrNull?.uid
        if (coupleId == null || me == null) return@flatMapLatest flowOf(CoupleMoods(null, null))
        val partner = (space as? SpaceState.Paired)?.partnerUid
        val partnerFlow = if (partner == null) flowOf(null) else moodDoc(coupleId, partner).observe().map(::moodFrom)
        combine(moodDoc(coupleId, me).observe().map(::moodFrom), partnerFlow, ::CoupleMoods)
    }.distinctUntilChanged()

    override suspend fun setMood(mood: Mood, note: String?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.accountOrNull?.uid ?: return
        // Not awaited: the cache updates at once and the write syncs when it can.
        moodDoc(coupleId, me).set(mapOf("mood" to mood.key, "note" to note, "updatedAt" to FieldValue.serverTimestamp()))
    }

    private fun moodDoc(coupleId: String, uid: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("moods").document(uid)

    companion object {
        fun moodFrom(snapshot: DocumentSnapshot?): MoodUpdate? {
            val mood = Mood.fromKey(snapshot?.getString("mood")) ?: return null
            val at = snapshot?.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            return MoodUpdate(mood, snapshot?.getString("note")?.ifBlank { null }, at?.toDate()?.toInstant())
        }
    }
}
