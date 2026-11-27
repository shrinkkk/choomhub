package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.MeetupEdit
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.PreparedSnap
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The specimen cabinet: every meetup you've logged, and their photos. Either of you can change anything. */
interface MeetupLogRepository {
    /** Newest first. */
    fun observe(): Flow<List<MeetupLog>>

    fun observe(id: String): Flow<MeetupLog?>

    /** The full picture, as JPEG bytes; null while loading or if it's gone. */
    fun observePhoto(meetupId: String, photoId: String): Flow<ByteArray?>

    /** Starts logging one that's happening now (time together counts from now). Returns its id. */
    suspend fun startLive(): String?

    /** Logs one from before. Returns its id. */
    suspend fun logPast(date: LocalDate): String?

    /** Stops the clock on a live one (its time together is kept). */
    suspend fun stopLive(meetup: MeetupLog)

    suspend fun edit(meetupId: String, edit: MeetupEdit)

    suspend fun addPhoto(meetup: MeetupLog, photo: PreparedSnap)

    suspend fun removePhoto(meetup: MeetupLog, photoId: String)

    /** New order; the first photo becomes the thumbnail. */
    suspend fun reorder(meetup: MeetupLog, order: List<String>)

    suspend fun delete(meetup: MeetupLog)
}
