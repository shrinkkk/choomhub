package com.choomhub.app.data.space

import android.content.Context
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.SpaceException
import com.choomhub.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Used when Firebase isn't configured: no accounts, everything stays on this phone. */
class DeviceOnlySpaceRepository @Inject constructor() : SpaceRepository {
    private val unavailable = Result.failure<Unit>(SpaceException("The shared space isn't set up in this build."))

    override fun observeSpace(): Flow<SpaceState> = flowOf(SpaceState.DeviceOnly)
    override suspend fun signInWithGoogle(activityContext: Context) = unavailable
    override suspend fun signOut() = Unit
    override suspend fun startSpace() = unavailable
    override suspend fun joinSpace(code: String) = unavailable
    override suspend fun refreshInvite() = unavailable
    override suspend fun leaveSpace() = unavailable
}
