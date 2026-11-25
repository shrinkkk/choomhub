package com.choomhub.app.feature.focus

import com.choomhub.app.domain.model.CatActivity
import com.choomhub.app.domain.model.CatSchedule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.FocusTotals
import com.choomhub.app.domain.model.PointsRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class FocusRulesTest {
    private val t0 = Instant.parse("2026-11-25T10:00:00Z")
    private val wed = LocalDate.of(2026, 11, 25)
    private fun mins(m: Long) = Duration.ofMinutes(m).toMillis()

    @Test
    fun theStopwatchHoldsStillOnABreakAndPicksUpAfter() {
        val entered = FocusRules.enter(FocusSession(), FocusTotals(), t0).session
        assertEquals(FocusState.Focusing, entered.state)
        val onBreak = FocusRules.startBreak(entered, FocusTotals(), 10, t0.plusSeconds(1500), wed)
        assertEquals(mins(25), onBreak.session.bankedMs)
        assertEquals(t0.plusSeconds(2100), onBreak.session.breakEndsAt)
        // Twenty minutes on the beanbag change nothing.
        assertEquals(mins(25), FocusRules.sessionMs(onBreak.session, t0.plusSeconds(2700)))
        assertEquals(-mins(5), FocusRules.breakLeftMs(onBreak.session, t0.plusSeconds(2400)))
        val back = FocusRules.resume(onBreak.session, onBreak.totals, t0.plusSeconds(2700)).session
        assertEquals(mins(30), FocusRules.sessionMs(back, t0.plusSeconds(3000)))
    }

    @Test
    fun totalsCountEachStretchAndTheWeekStartsOnMonday() {
        val s = FocusSession(FocusState.Focusing, 0, t0)
        val last = FocusTotals(totalMs = mins(600), weekMs = mins(120), weekStart = LocalDate.of(2026, 11, 16))
        val left = FocusRules.leave(s, last, t0.plusSeconds(1800), wed)
        assertEquals(mins(630), left.totals.totalMs)
        assertEquals(mins(30), left.totals.weekMs) // last week's 120 don't carry over
        assertEquals(LocalDate.of(2026, 11, 23), left.totals.weekStart)
        assertEquals(FocusState.Away, left.session.state)
        assertEquals(0L, FocusTotals(weekMs = 5, weekStart = LocalDate.of(2026, 11, 16)).weekMsAt(wed))
    }

    @Test
    fun everyFullHourIsOnePoint() {
        val s = FocusSession(FocusState.Focusing, 0, t0)
        val first = FocusRules.startBreak(s, FocusTotals(), 5, t0.plusSeconds(50 * 60), wed)
        assertEquals(0, first.points)
        val again = FocusRules.leave(FocusSession(FocusState.Focusing, mins(50), t0), first.totals, t0.plusSeconds(25 * 60), wed)
        assertEquals(1, again.points)
        assertEquals(1, again.totals.pointHours)
        val long = FocusRules.leave(FocusSession(FocusState.Focusing, 0, t0), again.totals, t0.plusSeconds(3 * 3600), wed)
        assertEquals(3, long.points)
    }

    @Test
    fun aForgottenSessionCountsTwelveHoursAtMost() {
        val s = FocusSession(FocusState.Focusing, 0, t0)
        assertEquals(Duration.ofHours(12).toMillis(), FocusRules.sessionMs(s, t0.plus(Duration.ofDays(3))))
    }

    @Test
    fun extendingAnOverBreakCountsFromNow() {
        val b = FocusSession(FocusState.OnBreak, mins(20), null, t0)
        assertEquals(t0.plusSeconds(1000 + 300), FocusRules.extendBreak(b, FocusTotals(), 5, t0.plusSeconds(1000)).session.breakEndsAt)
        assertEquals(t0.plusSeconds(600), FocusRules.extendBreak(b, FocusTotals(), 10, t0.minusSeconds(60)).session.breakEndsAt)
    }

    @Test
    fun labels() {
        assertEquals("1:02:33", FocusRules.stopwatch(3_753_000))
        assertEquals("4:12", FocusRules.countdown(251_500))
        assertEquals("+1:05", FocusRules.countdown(-65_000))
        assertEquals("6.5", FocusRules.hours(mins(390)))
        assertEquals("41", FocusRules.hours(mins(41 * 60 + 20)))
        assertNull(FocusRules.breakLeftMs(FocusSession(), t0))
    }

    @Test
    fun jamLinksAreCleanedUp() {
        assertEquals("https://open.spotify.com/socialsession/x", FocusRules.cleanJamUrl("  open.spotify.com/socialsession/x "))
        assertEquals("https://music.youtube.com/watch?v=1", FocusRules.cleanJamUrl("https://music.youtube.com/watch?v=1"))
        assertNull(FocusRules.cleanJamUrl("not a link"))
        assertNull(FocusRules.cleanJamUrl("hello"))
    }

    @Test
    fun theCatMostlySleeps() {
        var asleep = 0
        val day = 24 * 60
        repeat(day) { m -> if (CatSchedule.at(t0.toEpochMilli() + m * 60_000L) == CatActivity.Sleeping) asleep++ }
        assertTrue("asleep $asleep of $day minutes", asleep > day * 0.8)
        assertTrue(asleep < day)
    }

    @Test
    fun theCatPicksTheLapOfWhoeverFocusedMoreThisWeek() {
        val names = CoupleNames(partnerName = "Wren", myName = "Rook", myRole = PointsRole.GoodBoy)
        val totals = FocusTotals(weekMs = mins(600), weekStart = LocalDate.of(2026, 11, 23))
        val room = FocusRoomData(
            me = FocusSession(FocusState.OnBreak, 0, null, t0.plusSeconds(300)),
            partner = FocusSession(FocusState.OnBreak, 0, null, t0.plusSeconds(300)),
            myTotals = totals,
        )
        val scene = roomScene(room, names, t0, wed)
        assertEquals("Rook", scene.him.name)
        assertEquals(CatSpot.HisLap, catSpot(scene))
        assertEquals(CatSpot.HerLap, catSpot(scene.copy(him = scene.him.copy(spot = Spot.Desk))))
    }

    @Test
    fun fireAndRainStayInRange() {
        val synth = FireAndRain(Random(1))
        repeat(50_000) { val v = synth.next(); assertTrue(v in -1f..1f) }
    }
}
