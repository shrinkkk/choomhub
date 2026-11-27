package com.choomhub.app.data.meetuplog

import com.choomhub.app.domain.model.MeetupEdit
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPhoto
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.repository.MeetupLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Without Firebase (and in tests): the cabinet lives in memory on this phone. */
@Singleton
open class LocalMeetupLogRepository @Inject constructor(private val clock: Clock) : MeetupLogRepository {
    val meetups = MutableStateFlow<List<MeetupLog>>(emptyList())
    private val pictures = HashMap<String, ByteArray>()
    private var next = 1

    override fun observe(): Flow<List<MeetupLog>> = meetups.map { list -> list.sortedWith(compareByDescending<MeetupLog> { it.date }.thenByDescending { it.createdAt }) }
    override fun observe(id: String): Flow<MeetupLog?> = meetups.map { list -> list.firstOrNull { it.id == id } }
    override fun observePhoto(meetupId: String, photoId: String): Flow<ByteArray?> = meetups.map { pictures[photoId] }

    private fun add(m: MeetupLog): String {
        meetups.update { it + m }
        return m.id
    }

    private fun change(id: String, block: (MeetupLog) -> MeetupLog) = meetups.update { list -> list.map { if (it.id == id) block(it) else it } }

    override suspend fun startLive(): String = add(MeetupLog("m${next++}", LocalDate.now(clock), live = true, startedAt = clock.instant(), byMe = true, createdAt = clock.instant()))

    override suspend fun logPast(date: LocalDate): String = add(MeetupLog("m${next++}", date, byMe = true, createdAt = clock.instant()))

    override suspend fun stopLive(meetup: MeetupLog) = change(meetup.id) { it.copy(live = false, minutes = it.minutesAt(clock.instant()) ?: 0) }

    override suspend fun edit(meetupId: String, edit: MeetupEdit) = change(meetupId) { m ->
        var r = m.copy(
            date = edit.date ?: m.date,
            minutes = edit.minutes?.coerceIn(0, MeetupRules.MAX_MINUTES) ?: m.minutes,
            live = if (edit.minutes != null) false else m.live,
            places = edit.places?.let(MeetupRules::cleanPlaces) ?: m.places,
            note = edit.note?.let { MeetupRules.clean(it, MeetupRules.MAX_NOTE) } ?: m.note,
            did = edit.did ?: m.did,
            first = edit.first?.let { MeetupRules.clean(it, MeetupRules.MAX_FIRST) } ?: m.first,
        )
        edit.clear.forEach { field ->
            r = when (field) {
                "minutes" -> r.copy(minutes = null)
                "places" -> r.copy(places = emptyList())
                "note" -> r.copy(note = null)
                "first" -> r.copy(first = null)
                else -> r
            }
        }
        r
    }

    override suspend fun addPhoto(meetup: MeetupLog, photo: PreparedSnap) {
        val id = "p${next++}"
        pictures[id] = photo.full
        change(meetup.id) { if (it.photos.size >= MeetupRules.MAX_PHOTOS) it else it.copy(photos = it.photos + MeetupPhoto(id, photo.thumb)) }
    }

    override suspend fun removePhoto(meetup: MeetupLog, photoId: String) = change(meetup.id) { it.copy(photos = it.photos.filterNot { p -> p.id == photoId }) }

    override suspend fun reorder(meetup: MeetupLog, order: List<String>) = change(meetup.id) { m ->
        if (order.toSet() != m.photoIds.toSet()) m else m.copy(photos = order.map { id -> m.photos.first { it.id == id } })
    }

    override suspend fun delete(meetup: MeetupLog) = meetups.update { list -> list.filterNot { it.id == meetup.id } }

}
