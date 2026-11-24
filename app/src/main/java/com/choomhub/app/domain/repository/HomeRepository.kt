package com.choomhub.app.domain.repository

import com.choomhub.app.domain.model.HomeSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Source of the home screen's data.
 *
 * Contract for implementations: emit from the local source of truth (Room,
 * once it exists) immediately and keep emitting as that data changes.
 * Implementations must never require a network round-trip before the first
 * emission. Remote sync happens behind the repository.
 *
 * Milestone 1 binds [com.choomhub.app.data.sample.SampleHomeRepository].
 */
interface HomeRepository {
    fun observeHome(): Flow<HomeSnapshot>
}
