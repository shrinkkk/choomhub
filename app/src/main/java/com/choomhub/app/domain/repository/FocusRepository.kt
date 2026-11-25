package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.FocusChange
import com.choomhub.app.domain.model.FocusRoomData
import kotlinx.coroutines.flow.Flow

/**
 * The focus room: both of your sessions and totals, and the jam link in the record player.
 * Changes to your own session go through [apply], which also stores your totals and any points
 * you've earned, in one go.
 */
interface FocusRepository {
    fun observeRoom(): Flow<FocusRoomData>

    /** Stores [change]: your session, your totals, and [FocusChange.points] points for focus hours. */
    suspend fun apply(change: FocusChange)

    /** Leaves a jam link in the record player, or takes it out (null). */
    suspend fun setJam(url: String?)
}
