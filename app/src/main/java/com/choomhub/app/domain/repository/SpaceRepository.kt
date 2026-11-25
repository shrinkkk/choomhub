package com.choomhub.app.domain.repository

import android.content.Context
import com.choomhub.app.domain.model.SpaceState
import kotlinx.coroutines.flow.Flow

/** Sign-in and the shared space (creating it, joining it with an invite code). */
interface SpaceRepository {
    fun observeSpace(): Flow<SpaceState>

    /** Shows Google's sign-in sheet. [activityContext] must be an Activity. */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit>

    suspend fun signOut()

    /** Starts a new space with only you in it. */
    suspend fun startSpace(): Result<Unit>

    /** Joins your partner's space using their invite code. */
    suspend fun joinSpace(code: String): Result<Unit>

    /** Replaces the invite code while you're still waiting (e.g. it expired). */
    suspend fun refreshInvite(): Result<Unit>

    /**
     * Leaves the space. If your partner is still in it, your place is kept and you can come back
     * with a code from them (on any Google account) and carry on where you were. If you're the
     * last one in it, everything in the space is deleted for good.
     */
    suspend fun leaveSpace(): Result<Unit>
}

/** A failure the person can understand and act on. */
class SpaceException(message: String) : Exception(message)
