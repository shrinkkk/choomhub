package com.choomhub.app.domain.model

import java.time.Instant

/** One of the two members of the shared space. */
data class Member(
    val id: String,
    val displayName: String,
)

/** The partner's most recent mood update. */
data class PartnerMood(
    val partner: Member,
    val mood: MoodOption,
    /** Optional short status, e.g. "rewatching ghibli under a blanket". */
    val note: String?,
    /** Server-assigned time of the update; null for a moment while it syncs. */
    val updatedAt: Instant?,
)

/**
 * Current Good Boy / Good Girl totals. In the real implementation these are
 * derived by summing an append-only transaction ledger, never stored as an
 * editable number.
 */
data class PointBalances(
    val goodBoy: Int,
    val goodGirl: Int,
)

/** What is known about the shared cat. Not adopted until a later milestone. */
data class SharedCatStatus(
    val isAdopted: Boolean,
    val name: String?,
)

/** Everything the home screen shows, assembled by [com.choomhub.app.domain.repository.HomeRepository]. */
data class HomeSnapshot(
    /** The name your partner gave you; null until it has synced. */
    val viewerName: String?,
    /** Which points you receive; null until chosen. */
    val myRole: PointsRole? = null,
    val partnerMood: PartnerMood?,
    /** What you call your partner; null until set. */
    val partnerName: String? = null,
    /** Your own current mood, if you've set one. */
    val myMood: MoodUpdate? = null,
    /** The points ledger, newest first. */
    val awards: List<PointAward> = emptyList(),
    /** Recent gestures in both directions, newest first. */
    val recentGestures: List<Gesture> = emptyList(),
    /** Their current snap, and yours. */
    val partnerSnap: SnapInfo? = null,
    val mySnap: SnapInfo? = null,
    /** Their snap's id once you've opened it, so it stops showing as new. */
    val seenSnapId: String? = null,
    val points: PointBalances,
    val cat: SharedCatStatus,
    /** True while points and the cat are still sample data. */
    val isSampleData: Boolean,
)

/**
 * Affection gestures available from the home screen. [key] is stored in
 * Firestore and sent to the push Worker; [sent] completes "Wren …".
 */
enum class AffectionKind(val label: String, val key: String, val sent: String) {
    Hug("Hug", "hug", "sent a hug"),
    ForeheadKiss("Forehead kiss", "forehead_kiss", "sent a forehead kiss"),
    HeadPats("Head pats", "head_pats", "gave head pats"),
    TomatoDelivery("Tomato", "tomato", "delivered a tomato"),
    ;

    companion object {
        fun fromKey(key: String?): AffectionKind? = entries.firstOrNull { it.key == key }
    }
}

/** One gesture in the shared history. */
data class Gesture(
    val id: String,
    val kind: AffectionKind,
    val fromMe: Boolean,
    /** Null only for a moment, while a just-sent gesture waits for the server's clock. */
    val sentAt: Instant?,
)

/** Whether a gesture went out. */
sealed interface GestureResult {
    data object Sent : GestureResult
    data class NotSent(val message: String) : GestureResult
}
