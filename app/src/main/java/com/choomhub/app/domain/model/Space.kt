package com.choomhub.app.domain.model

import java.security.SecureRandom

/** A signed-in person. */
data class Account(val uid: String, val email: String?)

/** Where this phone stands with the shared space. */
sealed interface SpaceState {
    /** No backend configured: everything stays on this phone. */
    data object DeviceOnly : SpaceState

    data object SignedOut : SpaceState

    data class NoSpace(val account: Account) : SpaceState

    /**
     * You're in a space on your own, waiting for your partner to join (or come back) with
     * [inviteCode]; blank while a fresh code is being made. [memberId] is who you are in the space.
     */
    data class Waiting(val account: Account, val coupleId: String, val inviteCode: String, val memberId: String = account.uid) : SpaceState

    /**
     * Both of you are in. [memberId] and [partnerUid] are member ids: the account each of you first
     * joined with, which everything in the space is filed under (someone who leaves and comes back
     * on another Google account keeps their old one).
     */
    data class Paired(val account: Account, val coupleId: String, val partnerUid: String, val memberId: String = account.uid) : SpaceState
}

/** Who you are inside the space (see [SpaceState.Paired.memberId]); null outside one. */
val SpaceState.memberIdOrNull: String?
    get() = when (this) {
        is SpaceState.Waiting -> memberId
        is SpaceState.Paired -> memberId
        else -> null
    }

val SpaceState.coupleIdOrNull: String?
    get() = when (this) {
        is SpaceState.Waiting -> coupleId
        is SpaceState.Paired -> coupleId
        else -> null
    }

val SpaceState.accountOrNull: Account?
    get() = when (this) {
        is SpaceState.NoSpace -> account
        is SpaceState.Waiting -> account
        is SpaceState.Paired -> account
        else -> null
    }

/** Invite codes: 8 characters without look-alikes (no 0/O, 1/I/L). */
object InviteCodes {
    const val LENGTH = 8
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(): String = buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Accepts lower case, spaces and dashes as typed or pasted. */
    fun normalize(input: String): String = input.uppercase().filter { it in ALPHABET }

    fun isWellFormed(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }

    /** "ABCD EFGH" for reading aloud. */
    fun display(code: String): String = if (code.length == LENGTH) code.chunked(4).joinToString(" ") else code
}
