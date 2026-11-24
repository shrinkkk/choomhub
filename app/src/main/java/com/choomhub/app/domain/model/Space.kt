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

    /** You've started a space and are waiting for your partner to join with [inviteCode]. */
    data class Waiting(val account: Account, val coupleId: String, val inviteCode: String) : SpaceState

    data class Paired(val account: Account, val coupleId: String, val partnerUid: String) : SpaceState
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
