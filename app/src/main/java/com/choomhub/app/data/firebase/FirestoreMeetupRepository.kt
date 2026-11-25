package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.Meetup
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.Date
import javax.inject.Inject

/** The shared meetup at couples/{id}/shared/meetup. Last write wins. */
class FirestoreMeetupRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : MeetupRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMeetup(): Flow<Meetup?> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull ?: return@flatMapLatest flowOf(null)
        meetupDoc(coupleId).observe().map { snap -> snap?.getTimestamp("at")?.let { Meetup(it.toDate().toInstant()) } }
    }.distinctUntilChanged()

    override suspend fun schedule(at: Instant) = write(Timestamp(Date.from(at)))

    override suspend fun clear() = write(null)

    private suspend fun write(at: Timestamp?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        meetupDoc(coupleId).set(mapOf("at" to at, "updatedBy" to me, "updatedAt" to FieldValue.serverTimestamp()))
    }

    private fun meetupDoc(coupleId: String) =
        db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection("shared").document("meetup")
}
