package com.choomhub.app.domain.model

import java.time.Instant

/** A song one of you sent the other (each of you has one at a time). */
data class SharedSong(
    /** Spotify's id for the track (22 letters and numbers). */
    val trackId: String,
    val title: String,
    val artist: String?,
    /** A small JPEG of the album cover, if there was one. */
    val cover: ByteArray?,
    val sentAt: Instant?,
    /** What the other one rated it, 1-10. */
    val rating: Int?,
) {
    /** Identifies this sending of the song (the same song sent twice counts as new). */
    val key: String get() = "$trackId@${sentAt?.toEpochMilli() ?: 0}"

    override fun equals(other: Any?): Boolean = other is SharedSong && trackId == other.trackId && title == other.title &&
        artist == other.artist && sentAt == other.sentAt && rating == other.rating && (cover?.contentEquals(other.cover) ?: (other.cover == null))

    override fun hashCode(): Int = listOf(trackId, title, artist, sentAt, rating, cover?.size).hashCode()
}

/** Your song and theirs. */
data class SongPair(val mine: SharedSong? = null, val theirs: SharedSong? = null, val loaded: Boolean = false)

/** A song found from a link, ready to send. */
data class SongDraft(val trackId: String, val title: String, val artist: String?, val cover: ByteArray?)

object SongRules {
    const val MAX_TITLE = 200
    const val MAX_ARTIST = 200
    private val TRACK_ID = Regex("^[A-Za-z0-9]{22}$")
    private val LINK = Regex("""(https?://(?:open\.spotify\.com|spotify\.link|spoti\.fi)/\S+|spotify:[a-z]+:[A-Za-z0-9]+)""")

    fun isTrackId(id: String): Boolean = TRACK_ID.matches(id)

    /** The Spotify link in what was shared or pasted (Spotify's share text can have words around it). */
    fun findLink(text: String): String? = LINK.find(text)?.value?.trimEnd('.', ',', ')', '"')

    /** What kind of thing an open.spotify.com link or spotify: URI points at, and its id. */
    fun parse(link: String): Pair<String, String>? {
        Regex("""^spotify:([a-z]+):([A-Za-z0-9]+)$""").find(link)?.let { return it.groupValues[1] to it.groupValues[2] }
        val m = Regex("""open\.spotify\.com/(?:intl-[a-z-]+/)?(?:embed/)?([a-z]+)/([A-Za-z0-9]+)""").find(link) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }

    /** Why a link of this kind can't be sent, or null for a song. */
    fun refusal(kind: String): String? = when (kind) {
        "track" -> null
        "playlist" -> "That's a playlist. Share a single song."
        "album" -> "That's an album. Share a single song from it."
        "artist" -> "That's an artist. Share one of their songs."
        "episode", "show" -> "That's a podcast. Share a song."
        else -> "That isn't a Spotify song link."
    }

    /** Opens in the Spotify app if it's installed (it claims these links), else in the browser. */
    fun playUrl(trackId: String): String = "https://open.spotify.com/track/$trackId"

    /** "Never Gonna Give You Up · Rick Astley" for notifications. */
    fun describe(title: String, artist: String?): String = if (artist.isNullOrBlank()) title else "$title · $artist"

    /** The artist from a song page's preview description: "Rick Astley · Whenever You Need Somebody · Song · 1987". */
    fun artistFromDescription(description: String?): String? =
        description?.split(" · ")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_ARTIST }
}

/** A song on the record shelf: every song either of you has sent. */
data class ShelfSong(
    val key: String,
    val trackId: String,
    val title: String,
    val artist: String?,
    val cover: ByteArray?,
    val fromMe: Boolean,
    val sentAt: Instant?,
    val rating: Int?,
) {
    override fun equals(other: Any?): Boolean = other is ShelfSong && key == other.key && rating == other.rating && title == other.title
    override fun hashCode(): Int = key.hashCode()
}
