package com.choomhub.app.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Emits the current time once per second, aligned to the start of each second. */
interface SecondTicker {
    fun ticks(): Flow<Instant>
}

class ClockSecondTicker @Inject constructor(private val clock: Clock) : SecondTicker {
    override fun ticks(): Flow<Instant> = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(1_000L - now.toEpochMilli() % 1_000L)
        }
    }
}
