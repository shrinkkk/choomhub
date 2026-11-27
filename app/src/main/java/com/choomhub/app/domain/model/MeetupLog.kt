package com.choomhub.app.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** What you did, ticked on a meetup. */
enum class MeetupActivity(val wire: String, val label: String) {
    Food("food", "Food"),
    Walk("walk", "Walk"),
    JustTalked("talked", "Just talked"),
    Cigs("cigs", "Cigs"),
    Drinks("drinks", "Drinks"),
    Movie("movie", "Movie"),
    Other("other", "Other"),
    ;

    companion object {
        fun fromWire(wire: String): MeetupActivity? = entries.firstOrNull { it.wire == wire }
    }
}

/** Somewhere you went that day: its name, and its spot on the map if it has one. */
data class MeetupPlace(val name: String, val lat: Double? = null, val lon: Double? = null) {
    val hasPin: Boolean get() = lat != null && lon != null

    /** Typed nothing and pinned nothing: not worth keeping. */
    val isBlank: Boolean get() = name.isBlank() && !hasPin
}

/** A photo in a meetup: [thumb] is small, for lists; the full picture loads separately. */
data class MeetupPhoto(val id: String, val thumb: ByteArray?) {
    override fun equals(other: Any?): Boolean = other is MeetupPhoto && other.id == id && (thumb?.size ?: 0) == (other.thumb?.size ?: 0)
    override fun hashCode(): Int = id.hashCode()
}

/**
 * One time you met, as pinned in the specimen cabinet. Only [date] is required; everything
 * else can be filled in later, by either of you. While [live], the time together is still counting
 * from [startedAt].
 */
data class MeetupLog(
    val id: String,
    val date: LocalDate,
    val minutes: Int? = null,
    val live: Boolean = false,
    val startedAt: Instant? = null,
    /** Where you went, in the order you went (or added them). The first is the one previews show. */
    val places: List<MeetupPlace> = emptyList(),
    val note: String? = null,
    val did: Set<MeetupActivity> = emptySet(),
    val first: String? = null,
    /** In order: the first one is the thumbnail. */
    val photos: List<MeetupPhoto> = emptyList(),
    val byMe: Boolean = false,
    val createdAt: Instant? = null,
) {
    /** Time together: still counting while live. */
    fun minutesAt(now: Instant): Int? = if (live && startedAt != null) {
        Duration.between(startedAt, now).toMinutes().toInt().coerceIn(0, MeetupRules.MAX_MINUTES)
    } else {
        minutes
    }

    /** The first place's name, for previews. */
    val place: String? get() = places.firstOrNull { it.name.isNotBlank() }?.name

    /** The places that are on the map, in order. */
    val pins: List<MeetupPlace> get() = places.filter { it.hasPin }

    val hasPin: Boolean get() = pins.isNotEmpty()

    val photoIds: List<String> get() = photos.map { it.id }

    /** A small picture of the first photo, for lists, the sky and the map. */
    val thumb: ByteArray? get() = photos.firstOrNull()?.thumb

    override fun equals(other: Any?): Boolean = other is MeetupLog && other.id == id && other.date == date && other.minutes == minutes &&
        other.live == live && other.startedAt == startedAt && other.places == places &&
        other.note == note && other.did == did && other.first == first && other.photos == photos

    override fun hashCode(): Int = id.hashCode()
}

/** A change to a meetup's details (null fields stay as they are; [clear] lists the ones to empty). */
data class MeetupEdit(
    val date: LocalDate? = null,
    val minutes: Int? = null,
    /** The whole list, replaced (blank entries are dropped). */
    val places: List<MeetupPlace>? = null,
    val note: String? = null,
    val did: Set<MeetupActivity>? = null,
    val first: String? = null,
    val clear: Set<String> = emptySet(),
)

object MeetupRules {
    const val MAX_PHOTOS = 12
    const val MAX_PLACE = 80
    const val MAX_PLACES = 6
    const val MAX_NOTE = 500
    const val MAX_FIRST = 80
    const val MAX_MINUTES = 24 * 60

    fun clean(text: String?, max: Int): String? = text?.replace(Regex("[\\t ]+"), " ")?.trim()?.take(max)?.ifEmpty { null }

    /** "5h 40m", "45m", "6h". */
    fun duration(minutes: Int): String = when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    /** Moves photo [id] by [by] places (clamped); moving it to the front makes it the thumbnail. */
    fun move(order: List<String>, id: String, by: Int): List<String> {
        val i = order.indexOf(id)
        if (i < 0) return order
        val j = (i + by).coerceIn(0, order.lastIndex)
        if (i == j) return order
        return order.toMutableList().apply { removeAt(i); add(j, id) }
    }

    /** Places within about 300 m of each other count as one pin. */
    fun samePlace(a: MeetupPlace, b: MeetupPlace): Boolean {
        if (!a.hasPin || !b.hasPin) return false
        return kotlin.math.abs(a.lat!! - b.lat!!) < 0.003 && kotlin.math.abs(a.lon!! - b.lon!!) < 0.003
    }

    /** The places worth saving: trimmed, blanks dropped, at most [MAX_PLACES]. */
    fun cleanPlaces(places: List<MeetupPlace>): List<MeetupPlace> = places
        .map { it.copy(name = clean(it.name, MAX_PLACE).orEmpty()) }
        .filterNot { it.isBlank }
        .take(MAX_PLACES)

    /** Meetups in date order (oldest first), for the sky. */
    fun inOrder(all: List<MeetupLog>): List<MeetupLog> = all.sortedWith(compareBy<MeetupLog> { it.date }.thenBy { it.createdAt })
}
