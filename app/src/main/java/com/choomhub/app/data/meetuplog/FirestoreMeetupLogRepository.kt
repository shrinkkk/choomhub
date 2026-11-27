package com.choomhub.app.data.meetuplog

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.MeetupActivity
import com.choomhub.app.domain.model.MeetupEdit
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPhoto
import com.choomhub.app.domain.model.MeetupPlace
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.MeetupLogRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * couples/{id}/meetups/{mid}  { date "2026-11-27", minutes?, live, startedAt?, places? [{ name, lat?, lon? }], note?,
 *                               did [..], first?, photos [ids, in order], thumbs {id: small JPEG},
 *                               by, createdAt, updatedBy, updatedAt }
 * couples/{id}/meetups/{mid}/photos/{pid}  { data, by, at }  the full picture
 *
 * The small pictures live on the meetup itself, so the cabinet, the sky and the map never load
 * a full photo until one is opened.
 */
@Singleton
class FirestoreMeetupLogRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val clock: Clock,
) : MeetupLogRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun meetups(coupleId: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(coupleId).collection(COLLECTION)

    private suspend fun ids(): Pair<String, String>? {
        val space = spaceRepository.observeSpace().first()
        return (space.coupleIdOrNull ?: return null) to (space.memberIdOrNull ?: return null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<MeetupLog>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(emptyList())
        meetups(coupleId).observe().map { snap ->
            snap?.documents.orEmpty().mapNotNull { meetupFrom(it, me) }.sortedWith(compareByDescending<MeetupLog> { it.date }.thenByDescending { it.createdAt })
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(id: String): Flow<MeetupLog?> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(null)
        meetups(coupleId).document(id).observe().map { it?.let { doc -> meetupFrom(doc, me) } }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePhoto(meetupId: String, photoId: String): Flow<ByteArray?> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull ?: return@flatMapLatest flowOf(null)
        meetups(coupleId).document(meetupId).collection(PHOTOS).document(photoId).observe().map { it?.getBlob("data")?.toBytes() }
    }

    private fun stamp(me: String) = mapOf("updatedBy" to me, "updatedAt" to FieldValue.serverTimestamp())

    private suspend fun create(fields: Map<String, Any?>): String? {
        val (coupleId, me) = ids() ?: return null
        val ref = meetups(coupleId).document()
        ref.set(
            fields + mapOf(
                "did" to emptyList<String>(),
                "photos" to emptyList<String>(),
                "thumbs" to emptyMap<String, Any>(),
                "by" to me,
                "createdAt" to FieldValue.serverTimestamp(),
            ) + stamp(me),
        )
        return ref.id
    }

    override suspend fun startLive(): String? = create(
        mapOf("date" to LocalDate.now(clock).toString(), "live" to true, "startedAt" to Timestamp(Date.from(clock.instant()))),
    )

    override suspend fun logPast(date: LocalDate): String? = create(mapOf("date" to date.toString(), "live" to false))

    override suspend fun stopLive(meetup: MeetupLog) {
        val (coupleId, me) = ids() ?: return
        meetups(coupleId).document(meetup.id).update(
            mapOf("live" to false, "minutes" to (meetup.minutesAt(clock.instant()) ?: 0)) + stamp(me),
        )
    }

    override suspend fun edit(meetupId: String, edit: MeetupEdit) {
        val (coupleId, me) = ids() ?: return
        val changes = buildMap<String, Any?> {
            edit.date?.let { put("date", it.toString()) }
            edit.minutes?.let { put("minutes", it.coerceIn(0, MeetupRules.MAX_MINUTES)); put("live", false) }
            edit.places?.let { list ->
                put("places", MeetupRules.cleanPlaces(list).map { p -> buildMap { put("name", p.name); p.lat?.let { put("lat", it) }; p.lon?.let { put("lon", it) } } })
                // The single place older versions kept goes; the list replaces it.
                put("place", FieldValue.delete()); put("lat", FieldValue.delete()); put("lon", FieldValue.delete())
            }
            edit.note?.let { put("note", MeetupRules.clean(it, MeetupRules.MAX_NOTE)) }
            edit.did?.let { put("did", it.map(MeetupActivity::wire)) }
            edit.first?.let { put("first", MeetupRules.clean(it, MeetupRules.MAX_FIRST)) }
            edit.clear.forEach { put(it, FieldValue.delete()) }
        }
        if (changes.isEmpty()) return
        meetups(coupleId).document(meetupId).update(changes + stamp(me))
    }

    override suspend fun addPhoto(meetup: MeetupLog, photo: PreparedSnap) {
        val (coupleId, me) = ids() ?: return
        if (meetup.photoIds.size >= MeetupRules.MAX_PHOTOS) return
        val doc = meetups(coupleId).document(meetup.id)
        val ref = doc.collection(PHOTOS).document()
        val batch = db.batch()
        batch.set(ref, mapOf("data" to Blob.fromBytes(photo.full), "by" to me, "at" to FieldValue.serverTimestamp()))
        batch.update(doc, mapOf("photos" to FieldValue.arrayUnion(ref.id), "thumbs.${ref.id}" to Blob.fromBytes(photo.thumb)) + stamp(me))
        batch.commit()
    }

    override suspend fun removePhoto(meetup: MeetupLog, photoId: String) {
        val (coupleId, me) = ids() ?: return
        val doc = meetups(coupleId).document(meetup.id)
        val batch = db.batch()
        batch.update(doc, mapOf("photos" to FieldValue.arrayRemove(photoId), "thumbs.$photoId" to FieldValue.delete()) + stamp(me))
        batch.delete(doc.collection(PHOTOS).document(photoId))
        batch.commit()
    }

    override suspend fun reorder(meetup: MeetupLog, order: List<String>) {
        val (coupleId, me) = ids() ?: return
        if (order.toSet() != meetup.photoIds.toSet()) return
        meetups(coupleId).document(meetup.id).update(mapOf("photos" to order) + stamp(me))
    }

    override suspend fun delete(meetup: MeetupLog) {
        val (coupleId, _) = ids() ?: return
        val doc = meetups(coupleId).document(meetup.id)
        val photos = runCatching { doc.collection(PHOTOS).get().await().documents }.getOrDefault(emptyList())
        val batch = db.batch()
        photos.forEach { batch.delete(it.reference) }
        meetup.photoIds.forEach { batch.delete(doc.collection(PHOTOS).document(it)) }
        batch.delete(doc)
        batch.commit()
    }

    companion object {
        const val COLLECTION = "meetups"
        const val PHOTOS = "photos"

        /** The places list, or (logged before there could be several) the one place. */
        fun placesFrom(doc: DocumentSnapshot): List<MeetupPlace> {
            val list = (doc.get("places") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                MeetupPlace((map["name"] as? String).orEmpty(), (map["lat"] as? Number)?.toDouble(), (map["lon"] as? Number)?.toDouble())
            }
            if (list != null) return list
            val old = MeetupPlace(doc.getString("place").orEmpty(), doc.getDouble("lat"), doc.getDouble("lon"))
            return if (old.isBlank) emptyList() else listOf(old)
        }

        fun meetupFrom(doc: DocumentSnapshot, me: String): MeetupLog? {
            val date = doc.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val photos = (doc.get("photos") as? List<*>).orEmpty().filterIsInstance<String>()
            val thumbs = (doc.get("thumbs") as? Map<*, *>).orEmpty()
            return MeetupLog(
                id = doc.id,
                date = date,
                minutes = doc.getLong("minutes")?.toInt(),
                live = doc.getBoolean("live") == true,
                startedAt = doc.getTimestamp("startedAt")?.toDate()?.toInstant(),
                places = placesFrom(doc),
                note = doc.getString("note"),
                did = (doc.get("did") as? List<*>).orEmpty().mapNotNull { (it as? String)?.let(MeetupActivity::fromWire) }.toSet(),
                first = doc.getString("first"),
                photos = photos.map { id -> MeetupPhoto(id, (thumbs[id] as? Blob)?.toBytes()) },
                byMe = doc.getString("by") == me,
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant(),
            )
        }
    }
}
