package com.choomhub.app.domain.model

import java.time.Instant

/**
 * A photo one of you sent. Only its small thumbnail lives in this record; the
 * full picture is loaded separately (see [com.choomhub.app.domain.repository.SnapRepository.observeImage]).
 */
data class SnapInfo(
    val id: String,
    val fromMe: Boolean,
    val caption: String?,
    /** Null only for a moment, while a just-sent snap waits for the server's clock. */
    val sentAt: Instant?,
    val width: Int,
    val height: Int,
    /** Small JPEG for lists and grids. */
    val thumb: ByteArray?,
    /** Memory Lane collections it's kept in. Kept snaps are never replaced. */
    val collections: List<String>,
    /** Reactions, one each: yours and theirs (any emoji, see [SnapRules.isReaction]). */
    val myReaction: String? = null,
    val partnerReaction: String? = null,
    /** The collections you put it in. Only whoever put it in a collection can take it out. */
    val addedByMe: Set<String> = emptySet(),
) {
    val isKept: Boolean get() = collections.isNotEmpty()

    /** Whether you can take it out of [collectionId] (you're the one who put it there). */
    fun canRemoveFrom(collectionId: String): Boolean = collectionId in addedByMe

    // ByteArray has identity equality; compare by id and the fields that change.
    override fun equals(other: Any?): Boolean =
        other is SnapInfo && other.id == id && other.collections == collections && other.sentAt == sentAt &&
            other.caption == caption && other.myReaction == myReaction && other.partnerReaction == partnerReaction &&
            other.addedByMe == addedByMe
    override fun hashCode(): Int = id.hashCode()
}

/** A named group of kept snaps, shown in Our World → Memory Lane. [cover] is the snap shown on top of its stack. */
data class SnapCollection(
    val id: String,
    val name: String,
    val createdAt: Instant?,
    val cover: String? = null,
    /** Someone asked to delete it: true if it was you, false if them, null if nobody. It goes only when the other agrees. */
    val deleteAskedByMe: Boolean? = null,
)

/** A photo ready to send: compressed full image and thumbnail. */
class PreparedSnap(val full: ByteArray, val thumb: ByteArray, val width: Int, val height: Int)

sealed interface SnapSendResult {
    data object Sent : SnapSendResult
    data class NotSent(val message: String) : SnapSendResult
}

object SnapRules {
    /** The reactions on offer; the rules and the push Worker accept only these. */
    /** The four quick reactions you start with; each of you can swap them for any emoji. */
    val QUICK_DEFAULT = listOf("\uD83D\uDDA4", "\uD83D\uDE2D", "\uD83D\uDE22", "\uD83D\uDD25")
    const val QUICK_COUNT = 4

    /** At most this many UTF-16 units: room for flags, skin tones and joined emoji. */
    const val MAX_REACTION_LENGTH = 16

    /**
     * Whether [text] is one emoji: symbols plus the joiners, variation selectors, skin tones
     * and keycap marks that build them, with at least one picture in it. Mirrors the Worker
     * and the Firestore rules.
     */
    fun isReaction(text: String): Boolean {
        if (text.isEmpty() || text.length > MAX_REACTION_LENGTH) return false
        var picture = false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            when {
                PICTURES.any { cp in it } || cp == 0x20E3 -> picture = true
                cp == 0x200D || cp == 0xFE0E || cp == 0xFE0F || cp in 0xE0020..0xE007F -> Unit
                cp in 0x30..0x39 || cp == '#'.code || cp == '*'.code -> Unit
                else -> return false
            }
            i += Character.charCount(cp)
        }
        return picture
    }

    /** Code points that are emoji pictures (the same list as the rules and the Worker). */
    private val PICTURES = listOf(
        0xA9..0xA9,
        0xAE..0xAE,
        0x203C..0x203C,
        0x2049..0x2049,
        0x2122..0x2122,
        0x2139..0x2139,
        0x2194..0x2199,
        0x21A9..0x21AA,
        0x231A..0x231B,
        0x2328..0x2328,
        0x23CF..0x23CF,
        0x23E9..0x23F3,
        0x23F8..0x23FA,
        0x24C2..0x24C2,
        0x25AA..0x25AB,
        0x25B6..0x25B6,
        0x25C0..0x25C0,
        0x25FB..0x25FE,
        0x2600..0x27BF,
        0x2934..0x2935,
        0x2B05..0x2B07,
        0x2B1B..0x2B1C,
        0x2B50..0x2B50,
        0x2B55..0x2B55,
        0x3030..0x3030,
        0x303D..0x303D,
        0x3297..0x3297,
        0x3299..0x3299,
        0x1F000..0x1FAFF,
    )

    const val MAX_CAPTION = 140
    const val MAX_COLLECTION_NAME = 40
    const val MAX_COLLECTIONS_PER_SNAP = 20

    /**
     * Longest side of the full image, in pixels. Kept snaps are the pictures in Memory Lane, so
     * this is also the size collection photos are stored at (sharp on a phone, ~150 KB each).
     */
    const val FULL_EDGE = 1080

    /** Longest side of the thumbnail, in pixels. */
    const val THUMB_EDGE = 320

    /** Firestore documents max out at 1 MiB; stay well under it. */
    const val MAX_FULL_BYTES = 850_000
    const val MAX_THUMB_BYTES = 60_000

    fun cleanCaption(input: String?): String? =
        input?.replace(Regex("\\s+"), " ")?.trim()?.take(MAX_CAPTION)?.ifEmpty { null }

    fun cleanCollectionName(input: String): String? =
        input.replace(Regex("\\s+"), " ").trim().take(MAX_COLLECTION_NAME).ifEmpty { null }

    /**
     * Whether [snap] is its sender's current snap (their newest), judged from [recent], the
     * newest snaps of you both. If the sender has nothing in [recent] we can't tell, so it
     * counts as current: a current snap is never deleted.
     */
    fun isCurrent(snap: SnapInfo, recent: List<SnapInfo>): Boolean {
        val sentAt = snap.sentAt ?: return true
        val newest = recent.filter { it.fromMe == snap.fromMe }.maxByOrNull { it.sentAt ?: Instant.MAX } ?: return true
        val newestAt = newest.sentAt ?: return false
        return newest.id == snap.id || !newestAt.isAfter(sentAt)
    }

    /** Taking [snap] out of [collectionId] deletes it for good: it's in no other collection and isn't anyone's current snap. */
    fun deletesForGood(snap: SnapInfo, collectionId: String, recent: List<SnapInfo>): Boolean =
        (snap.collections - collectionId).isEmpty() && !isCurrent(snap, recent)

    /** The snap shown on top of a collection's stack: its chosen cover if it's still in there, else the newest. */
    fun coverOf(collection: SnapCollection, snaps: List<SnapInfo>): SnapInfo? =
        snaps.firstOrNull { it.id == collection.cover } ?: snaps.maxByOrNull { it.sentAt ?: Instant.MAX }
}
