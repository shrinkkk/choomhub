package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.CoupleNames
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
}
