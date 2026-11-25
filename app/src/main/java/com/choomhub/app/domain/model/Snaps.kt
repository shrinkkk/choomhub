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
    /** Reactions, one each: yours and theirs (see [SnapRules.REACTIONS]). */
    val myReaction: String? = null,
    val partnerReaction: String? = null,
) {
    val isKept: Boolean get() = collections.isNotEmpty()

    // ByteArray has identity equality; compare by id and the fields that change.
    override fun equals(other: Any?): Boolean =
        other is SnapInfo && other.id == id && other.collections == collections && other.sentAt == sentAt &&
            other.caption == caption && other.myReaction == myReaction && other.partnerReaction == partnerReaction
    override fun hashCode(): Int = id.hashCode()
}

/** A named group of kept snaps, shown in Our World → Memory Lane. */
data class SnapCollection(val id: String, val name: String, val createdAt: Instant?)

/** A photo ready to send: compressed full image and thumbnail. */
class PreparedSnap(val full: ByteArray, val thumb: ByteArray, val width: Int, val height: Int)

sealed interface SnapSendResult {
    data object Sent : SnapSendResult
    data class NotSent(val message: String) : SnapSendResult
}

object SnapRules {
    /** The reactions on offer; the rules and the push Worker accept only these. */
    val REACTIONS = listOf("\uD83D\uDDA4", "\uD83E\uDD7A", "\uD83D\uDE2D", "\uD83D\uDE02", "\uD83D\uDD25", "\uD83C\uDF45")

    const val MAX_CAPTION = 140
    const val MAX_COLLECTION_NAME = 40
    const val MAX_COLLECTIONS_PER_SNAP = 20

    /** Longest side of the full image, in pixels. */
    const val FULL_EDGE = 1280

    /** Longest side of the thumbnail, in pixels. */
    const val THUMB_EDGE = 320

    /** Firestore documents max out at 1 MiB; stay well under it. */
    const val MAX_FULL_BYTES = 850_000
    const val MAX_THUMB_BYTES = 60_000

    fun cleanCaption(input: String?): String? =
        input?.replace(Regex("\\s+"), " ")?.trim()?.take(MAX_CAPTION)?.ifEmpty { null }

    fun cleanCollectionName(input: String): String? =
        input.replace(Regex("\\s+"), " ").trim().take(MAX_COLLECTION_NAME).ifEmpty { null }
}
