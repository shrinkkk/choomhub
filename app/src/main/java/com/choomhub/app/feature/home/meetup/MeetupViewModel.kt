package com.choomhub.app.feature.home.meetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.domain.model.CountdownState
import com.choomhub.app.domain.model.MeetupCountdown
import com.choomhub.app.domain.repository.MeetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** Drives the live countdown to the next meetup and scheduling it. */
@HiltViewModel
class MeetupViewModel @Inject constructor(
    private val repository: MeetupRepository,
    private val clock: Clock,
    ticker: SecondTicker,
) : ViewModel() {

    /** Null until the stored meetup has been read, so the card doesn't flash "none". */
    val countdown: StateFlow<CountdownState?> =
        combine(repository.observeMeetup(), ticker.ticks()) { meetup, now -> MeetupCountdown.stateAt(meetup, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val zone: ZoneId get() = clock.zone

    /**
     * Schedules the meetup at [date] [time] in the phone's time zone.
     * Returns an error message, or null when accepted.
     */
    fun schedule(date: LocalDate, time: LocalTime): String? {
        val at = date.atTime(time).atZone(clock.zone).toInstant()
        if (!at.isAfter(clock.instant())) return "Pick a time that hasn't happened yet."
        viewModelScope.launch { repository.schedule(at) }
        return null
    }

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}
