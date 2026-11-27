package com.choomhub.app.domain.model

import java.time.Instant

/** What was sent: a single song, a whole album, or a playlist. */
enum class SongKind(val wire: String, val label: String) {
    Track("track", "song"),
    Album("album", "album"),
    Playlist("playlist", "playlist"),
    ;

    /** "a song", "an album", "a playlist". */
    val withArticle: String get() = if (this == Album) "an $label" else "a $label"

    companion object {
        fun fromWire(wire: String?): SongKind = entries.firstOrNull { it.wire == wire } ?: Track
    }
}

/** A song one of you sent the other (each of you has one at a time). Or an album, or a playlist. */
data class SharedSong(
    /** Spotify's id for the track, album or playlist (22 letters and numbers). */
    val trackId: String,
    val title: String,
    val artist: String?,
    /** A small JPEG of the album cover, if there was one. */
    val cover: ByteArray?,
    val sentAt: Instant?,
    /** What the other one rated it, 1-10. */
    val rating: Int?,
    val kind: SongKind = SongKind.Track,
) {
    /** Identifies this sending of the song (the same song sent twice counts as new). */
    val key: String get() = "$trackId@${sentAt?.toEpochMilli() ?: 0}"

    override fun equals(other: Any?): Boolean = other is SharedSong && trackId == other.trackId && kind == other.kind && title == other.title &&
        artist == other.artist && sentAt == other.sentAt && rating == other.rating && (cover?.contentEquals(other.cover) ?: (other.cover == null))

    override fun hashCode(): Int = listOf(trackId, title, artist, sentAt, rating, cover?.size).hashCode()
}

/** Your song and theirs. */
data class SongPair(val mine: SharedSong? = null, val theirs: SharedSong? = null, val loaded: Boolean = false)

/** A song (album, playlist) found from a link, ready to send. For a playlist, artist is who made it. */
data class SongDraft(val trackId: String, val title: String, val artist: String?, val cover: ByteArray?, val kind: SongKind = SongKind.Track)

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

    /** Why a link of this kind can't be sent, or null for a song, an album or a playlist. */
    fun refusal(kind: String): String? = when (kind) {
        "track", "album", "playlist" -> null
        "artist" -> "That's an artist. Share a song, album or playlist of theirs."
        "episode", "show" -> "That's a podcast. Share a song, album or playlist."
        else -> "That isn't a Spotify song, album or playlist link."
    }

    /** Opens in the Spotify app if it's installed (it claims these links), else in the browser. */
    fun playUrl(id: String, kind: SongKind = SongKind.Track): String = "https://open.spotify.com/${kind.wire}/$id"

    /** "Never Gonna Give You Up · Rick Astley" for notifications. */
    fun describe(title: String, artist: String?): String = if (artist.isNullOrBlank()) title else "$title · $artist"

    /** The artist from a song page's preview description: "Rick Astley · Whenever You Need Somebody · Song · 1987". */
    fun artistFromDescription(description: String?): String? =
        description?.split(" · ")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_ARTIST }

    /**
     * Who made an album or playlist, from Spotify's embed page: the entity's own "subtitle"
     * (the artist for an album, the owner for a playlist).
     */
    fun makerFromEmbed(page: String, kind: SongKind, id: String): String? {
        val m = Regex(""""uri":"spotify:${kind.wire}:$id","id":"$id","title":"(?:[^"\\]|\\.)*","subtitle":"((?:[^"\\]|\\.)*)"""").find(page) ?: return null
        return unescapeJson(m.groupValues[1]).trim().takeIf { it.isNotEmpty() && it.length <= MAX_ARTIST }
    }

    private fun unescapeJson(s: String): String = Regex("""\\(u[0-9a-fA-F]{4}|.)""").replace(s) { r ->
        val e = r.groupValues[1]
        when {
            e.startsWith("u") && e.length == 5 -> e.substring(1).toInt(16).toChar().toString()
            e == "n" || e == "t" -> " "
            else -> e
        }
    }
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
    val kind: SongKind = SongKind.Track,
) {
    override fun equals(other: Any?): Boolean = other is ShelfSong && key == other.key && rating == other.rating && title == other.title && kind == other.kind
    override fun hashCode(): Int = key.hashCode()
}
