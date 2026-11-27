package com.choomhub.app.feature.focus

import com.choomhub.app.domain.model.CatActivity
import com.choomhub.app.domain.model.CatOuting
import com.choomhub.app.domain.model.CatRules
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.FocusContext
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
    fun theAgreedRateSetsThePointsAndZeroTurnsThemOff() {
        val s = FocusSession(FocusState.Focusing, 0, t0)
        val three = FocusRules.leave(s, FocusTotals(), t0.plusSeconds(2 * 3600), wed, FocusContext(rate = 3))
        assertEquals(6, three.points)
        assertEquals(2, three.hours)
        val off = FocusRules.leave(s, FocusTotals(), t0.plusSeconds(2 * 3600), wed, FocusContext(rate = 0))
        assertEquals(0, off.points)
        assertEquals(2, off.hours) // the cat's coins still come
        assertEquals(2, off.totals.pointHours) // and the hours don't pile up for later
    }

    @Test
    fun togetherTimeCountsDouble() {
        // You focus 10:00-12:00; they were at their desk 9:30-10:45 (finished) and again from 11:30.
        val them = FocusSession(
            FocusState.Focusing,
            focusSince = t0.plusSeconds(90 * 60),
            recent = listOf(t0.minusSeconds(30 * 60).toEpochMilli()..t0.plusSeconds(45 * 60).toEpochMilli()),
        )
        val me = FocusSession(FocusState.Focusing, 0, t0)
        val end = t0.plusSeconds(2 * 3600)
        assertEquals(mins(75), FocusRules.togetherMs(t0.toEpochMilli(), end.toEpochMilli(), them, end))
        val left = FocusRules.leave(me, FocusTotals(togetherMs = mins(20)), end, wed, FocusContext(rate = 2, partner = them))
        // Two hours at 2, plus one completed together hour (20 + 75 min) at 2 more.
        assertEquals(mins(95), left.totals.togetherMs)
        assertEquals(1, left.totals.togetherHours)
        assertEquals(2 * 2 + 2, left.points)
        // Your finished stretch is kept for their phone to see.
        assertEquals(listOf(t0.toEpochMilli()..end.toEpochMilli()), left.session.recent)
        assertEquals(FocusState.Away, left.session.state)
    }

    @Test
    fun overlappingStretchesNeverCountTwice() {
        val them = FocusSession(recent = listOf(0L..100L, 50L..150L, 140L..160L))
        assertEquals(160L, FocusRules.togetherMs(0, 1000, them, Instant.ofEpochMilli(1000)))
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
    fun theCatIsOutForAMinuteAndTheWalkBothWays() {
        val trip = CatOuting(CatActivity.Windowsill, t0)
        assertTrue(CatRules.isOut(trip, t0.toEpochMilli() + 30_000))
        assertTrue(CatRules.isOut(trip, t0.toEpochMilli() + 63_000))
        assertTrue(!CatRules.isOut(trip, t0.toEpochMilli() + 66_000))
        assertTrue(!CatRules.isOut(null, t0.toEpochMilli()))
    }

    @Test
    fun leavingSaysWhatTheSessionCameTo() {
        // A first session: 20 minutes banked before a break, then 70 more. 1.5 h is 1 point, 30 min to the next.
        val s = FocusSession(FocusState.Focusing, mins(20), t0, breakMs = mins(12), startTotalMs = 0)
        val sum = FocusRules.summary(s, FocusTotals(totalMs = mins(20)), t0.plusSeconds(70 * 60))
        assertEquals(mins(90), sum.focusMs)
        assertEquals(mins(12), sum.breakMs)
        assertEquals(1, sum.points)
        assertEquals(mins(30), sum.toNextPointMs)
        assertEquals("1 h 30 min", FocusRules.duration(mins(90)))
        assertEquals("12 min", FocusRules.duration(mins(12)))
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
        assertEquals(CatSpot.HisLap, catSpot(scene, 0))
        assertEquals(CatSpot.HerLap, catSpot(scene.copy(him = scene.him.copy(spot = Spot.Desk)), 0))
        assertEquals(CatSpot.Bed, catSpot(scene.copy(him = scene.him.copy(spot = Spot.Desk), her = scene.her.copy(spot = Spot.Desk)), 0))
    }

    @Test
    fun fireAndRainStayInRange() {
        val synth = FireAndRain(Random(1))
        repeat(50_000) { val v = synth.next(); assertTrue(v in -1f..1f) }
    }
}
