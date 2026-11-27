package com.choomhub.app.data.bounty

import com.choomhub.app.domain.model.Bounty
import com.choomhub.app.domain.model.BountyDue
import com.choomhub.app.domain.repository.BountyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Device-only builds: there's no one to post jobs for. */
class LocalBountyRepository @Inject constructor() : BountyRepository {
    override fun observe(): Flow<List<Bounty>> = flowOf(emptyList())
    override suspend fun post(task: String, reward: Int, due: BountyDue) = Unit
    override suspend fun claim(bounty: Bounty) = Unit
    override suspend fun notYet(bounty: Bounty) = Unit
    override suspend fun pay(bounty: Bounty) = Unit
    override suspend fun takeDown(bounty: Bounty) = Unit
}
