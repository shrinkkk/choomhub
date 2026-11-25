package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.AwardResult
import com.choomhub.app.domain.model.PointAward
import kotlinx.coroutines.flow.Flow

/** The points ledger: append-only; balances are always summed from it, never stored. */
interface PointsRepository {
    /** Every award, newest first. */
    fun observeAwards(): Flow<List<PointAward>>

    /**
     * Awards your partner [amount] points, or takes them away if it's negative
     * (1-10 either way; validated by the caller and again by the rules).
     */
    suspend fun award(amount: Int, reason: String?): AwardResult
}
