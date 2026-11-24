package com.choomhub.app.domain.model

import java.time.Duration
import java.time.Instant

/** The next time the two of you are meeting. Shared: either person can set, change or clear it. */
data class Meetup(
    val at: Instant,
)

/** How the countdown should feel, from far away to "it's today". */
enum class CountdownMood { Sad, Bored, Hopeful, Happy, Excited, Ecstatic }

/** What the countdown section shows at a given moment. */
sealed interface CountdownState {
    /** Nothing planned (or the last meetup is over). */
    data object NoneScheduled : CountdownState

    /** A meetup is ahead; [remaining] ticks down every second. */
    data class Upcoming(val at: Instant, val remaining: Duration, val mood: CountdownMood) : CountdownState

    /** The meetup time has arrived and is within the "together" window. */
    data class Happening(val at: Instant) : CountdownState
}

/** Pure countdown rules, kept separate from UI so they can be tested exactly. */
object MeetupCountdown {
    /** Thresholds (time remaining, at most) for each mood; anything further away is [CountdownMood.Sad]. */
    val BORED_WITHIN: Duration = Duration.ofDays(30)
    val HOPEFUL_WITHIN: Duration = Duration.ofDays(14)
    val HAPPY_WITHIN: Duration = Duration.ofDays(7)
    val EXCITED_WITHIN: Duration = Duration.ofDays(3)
    val ECSTATIC_WITHIN: Duration = Duration.ofDays(1)

    /** After the meetup time, show "together" for this long before going back to "none scheduled". */
    val TOGETHER_FOR: Duration = Duration.ofHours(12)

    fun stateAt(meetup: Meetup?, now: Instant): CountdownState {
        if (meetup == null) return CountdownState.NoneScheduled
        val remaining = Duration.between(now, meetup.at)
        return when {
            !remaining.isNegative && !remaining.isZero -> CountdownState.Upcoming(meetup.at, remaining, moodFor(remaining))
            now < meetup.at.plus(TOGETHER_FOR) -> CountdownState.Happening(meetup.at)
            else -> CountdownState.NoneScheduled
        }
    }

    fun moodFor(remaining: Duration): CountdownMood = when {
        remaining <= ECSTATIC_WITHIN -> CountdownMood.Ecstatic
        remaining <= EXCITED_WITHIN -> CountdownMood.Excited
        remaining <= HAPPY_WITHIN -> CountdownMood.Happy
        remaining <= HOPEFUL_WITHIN -> CountdownMood.Hopeful
        remaining <= BORED_WITHIN -> CountdownMood.Bored
        else -> CountdownMood.Sad
    }

    /** Splits a duration into whole days, hours, minutes and seconds for display. */
    fun parts(remaining: Duration): CountdownParts {
        val total = remaining.seconds.coerceAtLeast(0)
        return CountdownParts(
            days = total / 86_400,
            hours = (total % 86_400 / 3_600).toInt(),
            minutes = (total % 3_600 / 60).toInt(),
            seconds = (total % 60).toInt(),
        )
    }
}

data class CountdownParts(val days: Long, val hours: Int, val minutes: Int, val seconds: Int)
