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
    val mood: Mood,
    /** Optional short status, e.g. "rewatching ghibli under a blanket". */
    val note: String?,
    /** Server-assigned time of the update (authoritative ordering). */
    val updatedAt: Instant,
)

/** A photograph shared between the two members. */
data class Snap(
    val id: String,
    val sender: Member,
    val caption: String?,
    val sentAt: Instant,
    /** True until the receiving member has opened it. */
    val isUnseen: Boolean,
    /** Remote/private-storage reference. Null while only preview artwork exists. */
    val imageRef: String? = null,
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
    val viewer: Member,
    val partnerMood: PartnerMood?,
    val latestSnap: Snap?,
    val points: PointBalances,
    val cat: SharedCatStatus,
    /** True when the data is local sample data rather than a synced space. */
    val isSampleData: Boolean,
)

/** Affection gestures available from the home screen. */
enum class AffectionKind(val label: String) {
    Hug("Hug"),
    Kiss("Kiss"),
    HeadPats("Head pats"),
    TomatoDelivery("Tomato"),
}
