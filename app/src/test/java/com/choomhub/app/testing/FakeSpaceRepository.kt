package com.choomhub.app.testing

import android.content.Context
import com.choomhub.app.domain.model.Account
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.SpaceException
import com.choomhub.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSpaceRepository(initial: SpaceState = SpaceState.DeviceOnly) : SpaceRepository {
    val space = MutableStateFlow(initial)
    var joinError: String? = null
    val me = Account("me", "me@example.com")

    override fun observeSpace(): Flow<SpaceState> = space
    override suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        space.value = SpaceState.NoSpace(me); return Result.success(Unit)
    }
    override suspend fun signOut() { space.value = SpaceState.SignedOut }
    override suspend fun startSpace(): Result<Unit> {
        space.value = SpaceState.Waiting(me, "c1", "ABCD2345"); return Result.success(Unit)
    }
    override suspend fun joinSpace(code: String): Result<Unit> {
        joinError?.let { return Result.failure(SpaceException(it)) }
        space.value = SpaceState.Paired(me, "c1", "partner"); return Result.success(Unit)
    }
    override suspend fun refreshInvite(): Result<Unit> = Result.success(Unit)
}
