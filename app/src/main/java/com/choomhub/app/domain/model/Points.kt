package com.choomhub.app.domain.model

import java.time.Instant

/**
 * One entry in the points ledger: someone awarded the other points (positive) or
 * took some away (negative), maybe with a reason. Totals can go below zero.
 */
data class PointAward(
    val id: String,
    /** True if you received it, false if you gave it. */
    val toMe: Boolean,
    /** Positive when awarded, negative when taken away. */
    val amount: Int,
    val reason: String?,
    /** Null only for a moment, while a just-saved entry waits for the server's clock. */
    val at: Instant?,
)

sealed interface AwardResult {
    data object Awarded : AwardResult
    data class NotAwarded(val message: String) : AwardResult
}

object PointRules {
    const val MIN = 1
    const val MAX = 10
    const val MAX_REASON = 140

    /** Parses what was typed (always a positive number; the button decides award or take away). */
    fun parseAmount(input: String): Result<Int> {
        val n = input.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("How many points?"))
        if (n < MIN || n > MAX) return Result.failure(IllegalArgumentException("Between $MIN and $MAX at a time."))
        return Result.success(n)
    }

    fun cleanReason(input: String?): String? =
        input?.replace(Regex("\\s+"), " ")?.trim()?.take(MAX_REASON)?.ifEmpty { null }

    /** Totals from the ledger: what you've received, and what they have (either can be negative). */
    fun balances(awards: List<PointAward>, myRole: PointsRole?): PointBalances {
        val mine = awards.filter { it.toMe }.sumOf { it.amount }
        val theirs = awards.filterNot { it.toMe }.sumOf { it.amount }
        return when (myRole) {
            PointsRole.GoodBoy -> PointBalances(goodBoy = mine, goodGirl = theirs)
            PointsRole.GoodGirl -> PointBalances(goodBoy = theirs, goodGirl = mine)
            null -> PointBalances(0, 0)
        }
    }
}
