package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MeetupCountdownTest {

    private val now = Instant.parse("2026-11-24T18:00:00Z")
    private fun stateIn(d: Duration) = MeetupCountdown.stateAt(Meetup(now.plus(d)), now)

    @Test
    fun `no meetup means nothing scheduled`() {
        assertEquals(CountdownState.NoneScheduled, MeetupCountdown.stateAt(null, now))
    }

    @Test
    fun `mood moves from sad to happy to excited as the day approaches`() {
        assertEquals(CountdownMood.Sad, (stateIn(Duration.ofDays(30)) as CountdownState.Upcoming).mood)
        assertEquals(CountdownMood.Sad, (stateIn(Duration.ofDays(14).plusSeconds(1)) as CountdownState.Upcoming).mood)
        assertEquals(CountdownMood.Happy, (stateIn(Duration.ofDays(14)) as CountdownState.Upcoming).mood)
        assertEquals(CountdownMood.Happy, (stateIn(Duration.ofDays(3).plusSeconds(1)) as CountdownState.Upcoming).mood)
        assertEquals(CountdownMood.Excited, (stateIn(Duration.ofDays(3)) as CountdownState.Upcoming).mood)
        assertEquals(CountdownMood.Excited, (stateIn(Duration.ofSeconds(1)) as CountdownState.Upcoming).mood)
    }

    @Test
    fun `after the time it is happening for a while, then nothing scheduled`() {
        assertTrue(stateIn(Duration.ZERO) is CountdownState.Happening)
        assertTrue(stateIn(Duration.ofHours(-11)) is CountdownState.Happening)
        assertEquals(CountdownState.NoneScheduled, stateIn(Duration.ofHours(-12)))
    }

    @Test
    fun `parts split exactly to the second`() {
        val d = Duration.ofDays(12).plusHours(4).plusMinutes(33).plusSeconds(9)
        assertEquals(CountdownParts(12, 4, 33, 9), MeetupCountdown.parts(d))
        assertEquals(CountdownParts(0, 0, 0, 0), MeetupCountdown.parts(Duration.ofSeconds(-5)))
    }
}
