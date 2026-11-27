package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.Bounty
import com.choomhub.app.domain.model.BountyDue
import kotlinx.coroutines.flow.Flow

/** The bounty board: jobs for each other, paid in points. */
interface BountyRepository {
    fun observe(): Flow<List<Bounty>>

    /** Pins up a job for them. */
    suspend fun post(task: String, reward: Int, due: BountyDue)

    /** Says you did their job. */
    suspend fun claim(bounty: Bounty)

    /** Sends a claimed one back: it isn't done yet. */
    suspend fun notYet(bounty: Bounty)

    /** Agrees it's done: marks it paid and gives them the points, in one go. */
    suspend fun pay(bounty: Bounty)

    /** Takes down one of yours that nobody's claimed (or that's been paid). */
    suspend fun takeDown(bounty: Bounty)
}
