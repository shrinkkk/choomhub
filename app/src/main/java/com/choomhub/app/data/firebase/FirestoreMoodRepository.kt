package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
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
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(CoupleMoods(null, null))
        val partner = (space as? SpaceState.Paired)?.partnerUid
        val partnerFlow = if (partner == null) flowOf(null) else moodDoc(coupleId, partner).observe().map(::moodFrom)
        combine(moodDoc(coupleId, me).observe().map(::moodFrom), partnerFlow, ::CoupleMoods)
    }.distinctUntilChanged()

    override suspend fun setMood(mood: MoodOption, note: String?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        // The label travels with the mood, so it still reads right if the list changes later.
        // Not awaited: the cache updates at once and the write syncs when it can.
        moodDoc(coupleId, me).set(
            mapOf("mood" to mood.key, "label" to mood.label, "note" to note, "updatedAt" to FieldValue.serverTimestamp()),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeOptions(): Flow<List<MoodOption>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull ?: return@flatMapLatest flowOf(Mood.defaults)
        optionsDoc(coupleId).observe().map(::optionsFrom)
    }.distinctUntilChanged()

    override suspend fun setOptions(options: List<MoodOption>) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        optionsDoc(coupleId).set(
            mapOf(
                "options" to options.map { mapOf("key" to it.key, "label" to it.label) },
                "updatedBy" to me,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    private fun optionsDoc(coupleId: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("shared").document("moodOptions")

    private fun moodDoc(coupleId: String, uid: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("moods").document(uid)

    companion object {
        fun moodFrom(snapshot: DocumentSnapshot?): MoodUpdate? {
            val key = snapshot?.getString("mood") ?: return null
            val label = snapshot.getString("label")?.ifBlank { null } ?: Mood.fromKey(key)?.label ?: return null
            val at = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            return MoodUpdate(MoodOption(key, label), snapshot.getString("note")?.ifBlank { null }, at?.toDate()?.toInstant())
        }

        /** The stored list, or the defaults if nobody has edited it (or it's unreadable). */
        fun optionsFrom(snapshot: DocumentSnapshot?): List<MoodOption> {
            val raw = snapshot?.get("options") as? List<*> ?: return Mood.defaults
            val options = raw.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val key = map["key"] as? String ?: return@mapNotNull null
                val label = map["label"] as? String ?: return@mapNotNull null
                MoodOption(key, label)
            }
            return options.ifEmpty { Mood.defaults }
        }
    }
}
