package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
import kotlinx.coroutines.flow.Flow

/**
 * Names (and later avatars) for the two members.
 *
 * Today this is device-local. When the shared space exists, the name you
 * give your partner is uploaded to their member profile, and the name they
 * gave you arrives through sync; the interface stays the same.
 */
interface ProfileRepository {
    fun observeNames(): Flow<CoupleNames>

    /** Stores an already-validated name for your partner (see [com.choomhub.app.domain.model.NameRules]). */
    suspend fun setPartnerName(name: String)

    /**
     * Claims your points role. Fails (returns false) if your partner already
     * holds that role; then you have the other one.
     */
    suspend fun setMyRole(role: PointsRole): Boolean
}
