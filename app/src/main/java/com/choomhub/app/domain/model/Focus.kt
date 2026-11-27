package com.choomhub.app.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Where someone is with the focus room. */
enum class FocusState(val wire: String) {
    Away("away"),
    Focusing("focus"),
    OnBreak("break"),
    ;

    companion object {
        fun fromWire(wire: String?): FocusState = entries.firstOrNull { it.wire == wire } ?: Away
    }
}

/**
 * One person's session as stored. The stopwatch is [bankedMs] (focus before this stretch)
 * plus the time since [focusSince] while focusing; during a break it holds still.
 */
data class FocusSession(
    val state: FocusState = FocusState.Away,
    val bankedMs: Long = 0,
    /** Start of the current focus stretch, while [FocusState.Focusing]. */
    val focusSince: Instant? = null,
    /** When the break is meant to end, while [FocusState.OnBreak]. */
    val breakEndsAt: Instant? = null,
    /** Breaks taken this session, not counting the one in progress. */
    val breakMs: Long = 0,
    /** When the current break started, while [FocusState.OnBreak]. */
    val breakSince: Instant? = null,
    /** Your all-time focus when this session started (to count the points it earned). */
    val startTotalMs: Long = 0,
    /**
     * Your last few finished focus stretches (epoch ms, start to end), so the other phone can
     * work out how much of its own focus overlapped with yours (together time).
     */
    val recent: List<LongRange> = emptyList(),
)

/** What a session came to, shown before you leave. */
data class SessionSummary(
    val focusMs: Long,
    val breakMs: Long,
    /** Points earned this session (counting the stretch you're leaving now). */
    val points: Int,
    /** Focus still needed for your next point. */
    val toNextPointMs: Long,
)

/** Someone's focus totals. [weekMs] belongs to the week starting [weekStart] (a Monday). */
data class FocusTotals(
    val totalMs: Long = 0,
    val weekMs: Long = 0,
    val weekStart: LocalDate? = null,
    /** Whole hours already turned into points. */
    val pointHours: Int = 0,
    /** Focus time while the other one was focusing too. Counted as well as [totalMs], for the bonus. */
    val togetherMs: Long = 0,
    /** Whole together-hours already turned into bonus points. */
    val togetherHours: Int = 0,
) {
    fun weekMsAt(today: LocalDate): Long = if (weekStart == FocusRules.weekStart(today)) weekMs else 0
}

/** A music-app jam link left in the record player. */
data class JamLink(val url: String, val fromMe: Boolean, val at: Instant?)

/** Everything the focus room shows. */
data class FocusRoomData(
    val me: FocusSession = FocusSession(),
    val partner: FocusSession = FocusSession(),
    val myTotals: FocusTotals = FocusTotals(),
    val partnerTotals: FocusTotals = FocusTotals(),
    val jam: JamLink? = null,
    /** Where the cat went when last tapped (null: asleep in its bed). */
    val cat: CatOuting? = null,
    /** Points per focus hour, and any change waiting to be agreed. */
    val rate: FocusRate = FocusRate(),
)

/** The result of a change to your session: what to store, and what it adds up to. */
data class FocusChange(
    val session: FocusSession,
    /** Your totals after this change (unchanged unless a focus stretch just ended). */
    val totals: FocusTotals,
    /** Points to award yourself for newly completed hours (0 for none). */
    val points: Int,
    /** Newly completed focus hours (what the cat's coins go by, whatever the points rate). */
    val hours: Int = points,
)

/**
 * Points per focus hour, agreed by both of you (0 turns them off), and a change one of you
 * has suggested, waiting for the other.
 */
data class FocusRate(val rate: Int = FocusRules.DEFAULT_RATE, val proposed: Int? = null, val proposedByMe: Boolean = false)

/** What banking focus needs besides your own session: the rate, and their session for together time. */
data class FocusContext(val rate: Int = FocusRules.DEFAULT_RATE, val partner: FocusSession = FocusSession())

/** The focus room's arithmetic, kept free of storage so it can be tested. */
object FocusRules {
    private val HOUR = Duration.ofHours(1).toMillis()

    /** One focus stretch counts for at most this long (a forgotten session can't run up days). */
    val MAX_STRETCH_MS: Long = Duration.ofHours(12).toMillis()

    val BREAK_CHOICES = listOf(5, 10, 15, 20, 30)
    const val MAX_BREAK_MINUTES = 180
    const val EXTEND_MINUTES = 5
    const val MAX_JAM_URL = 500

    /** Reason on the points you get for focusing (the rules only allow this one for yourself). */
    const val POINTS_REASON = "focus hours"

    const val DEFAULT_RATE = 1
    const val MAX_RATE = 10

    /** Finished stretches kept for together time. */
    const val RECENT_STRETCHES = 6

    /** How together time pays, in words for Settings. */
    fun rateRules(rate: Int): List<String> = if (rate == 0) {
        listOf("Focus hours don't earn points right now.", "Hours still count on the scoreboard, and for the cat's coins.")
    } else {
        listOf(
            "Every full hour in the focus room earns $rate point${if (rate == 1) "" else "s"}.",
            "Hours add up across sessions and breaks; only time at the desk counts.",
            "Together time: while you're both at your desks, it counts double, ${rate * 2} points an hour.",
            "Points land when you take a break or leave the room.",
        )
    }

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The current focus stretch, capped. */
    fun stretchMs(s: FocusSession, now: Instant): Long {
        if (s.state != FocusState.Focusing) return 0
        val since = s.focusSince ?: return 0
        return (now.toEpochMilli() - since.toEpochMilli()).coerceIn(0, MAX_STRETCH_MS)
    }

    /** The stopwatch: this session's focus so far. */
    fun sessionMs(s: FocusSession, now: Instant): Long = s.bankedMs + stretchMs(s, now)

    /** Time left on the break; negative once it's over. Null when not on a break. */
    fun breakLeftMs(s: FocusSession, now: Instant): Long? {
        if (s.state != FocusState.OnBreak) return null
        val end = s.breakEndsAt ?: return null
        return end.toEpochMilli() - now.toEpochMilli()
    }

    fun enter(s: FocusSession, totals: FocusTotals, now: Instant): FocusChange = when (s.state) {
        FocusState.Away -> FocusChange(FocusSession(FocusState.Focusing, 0, now, startTotalMs = totals.totalMs, recent = s.recent), totals, 0)
        else -> FocusChange(s, totals, 0)
    }

    /** Breaks so far this session, including one in progress. */
    fun breakTotalMs(s: FocusSession, now: Instant): Long {
        val current = if (s.state == FocusState.OnBreak && s.breakSince != null) {
            (now.toEpochMilli() - s.breakSince.toEpochMilli()).coerceAtLeast(0)
        } else {
            0
        }
        return s.breakMs + current
    }

    /** The session so far, as if you left now (points at [rate], not counting the together bonus). */
    fun summary(s: FocusSession, totals: FocusTotals, now: Instant, rate: Int = DEFAULT_RATE): SessionSummary {
        val total = totals.totalMs + stretchMs(s, now)
        val start = s.startTotalMs.coerceAtMost(total)
        return SessionSummary(
            focusMs = sessionMs(s, now),
            breakMs = breakTotalMs(s, now),
            points = (total / HOUR - start / HOUR).toInt() * rate,
            toNextPointMs = HOUR - total % HOUR,
        )
    }

    fun startBreak(s: FocusSession, totals: FocusTotals, minutes: Int, now: Instant, today: LocalDate, ctx: FocusContext = FocusContext()): FocusChange {
        val m = minutes.coerceIn(1, MAX_BREAK_MINUTES)
        if (s.state != FocusState.Focusing) return FocusChange(s, totals, 0)
        val stretch = stretchMs(s, now)
        val banked = bank(totals, s, stretch, now, today, ctx)
        return banked.copy(
            session = s.copy(
                state = FocusState.OnBreak, bankedMs = s.bankedMs + stretch, focusSince = null,
                breakEndsAt = now.plusSeconds(m * 60L), breakSince = now, recent = remember(s, stretch, now),
            ),
        )
    }

    /** A longer break: [minutes] more from the planned end, or from now if it's already over. */
    fun extendBreak(s: FocusSession, totals: FocusTotals, minutes: Int, now: Instant): FocusChange {
        if (s.state != FocusState.OnBreak) return FocusChange(s, totals, 0)
        val end = s.breakEndsAt?.takeIf { it.isAfter(now) } ?: now
        return FocusChange(s.copy(breakEndsAt = end.plusSeconds(minutes.coerceIn(1, MAX_BREAK_MINUTES) * 60L)), totals, 0)
    }

    /** Back to the desk: the stopwatch picks up where it stopped. */
    fun resume(s: FocusSession, totals: FocusTotals, now: Instant): FocusChange = when (s.state) {
        FocusState.OnBreak -> FocusChange(
            s.copy(state = FocusState.Focusing, focusSince = now, breakEndsAt = null, breakMs = breakTotalMs(s, now), breakSince = null),
            totals,
            0,
        )
        else -> FocusChange(s, totals, 0)
    }

    fun leave(s: FocusSession, totals: FocusTotals, now: Instant, today: LocalDate, ctx: FocusContext = FocusContext()): FocusChange {
        val stretch = stretchMs(s, now)
        val banked = bank(totals, s, stretch, now, today, ctx)
        return banked.copy(session = FocusSession(recent = remember(s, stretch, now)))
    }

    /** The stretch that just ended, added to the ones the other phone looks at. */
    private fun remember(s: FocusSession, stretch: Long, now: Instant): List<LongRange> {
        if (stretch <= 0) return s.recent
        val end = now.toEpochMilli()
        return (s.recent + listOf((end - stretch)..end)).takeLast(RECENT_STRETCHES)
    }

    /** How much of [from]..[to] (epoch ms) they were focusing too: their finished stretches plus the current one. */
    fun togetherMs(from: Long, to: Long, partner: FocusSession, now: Instant): Long {
        if (to <= from) return 0
        val theirs = partner.recent.toMutableList()
        if (partner.state == FocusState.Focusing && partner.focusSince != null) {
            theirs += partner.focusSince.toEpochMilli()..now.toEpochMilli()
        }
        // Merge first, so overlapping entries never count twice.
        val merged = mutableListOf<LongRange>()
        theirs.sortedBy { it.first }.forEach { r ->
            val last = merged.lastOrNull()
            if (last != null && r.first <= last.last) merged[merged.lastIndex] = last.first..maxOf(last.last, r.last) else merged += r
        }
        return merged.sumOf { r -> (minOf(to, r.last) - maxOf(from, r.first)).coerceAtLeast(0) }
    }

    /**
     * Adds a finished stretch (ending [now]) to the totals and works out the points: [FocusContext.rate]
     * per newly completed hour, and the same again per completed hour of together time.
     */
    private fun bank(totals: FocusTotals, s: FocusSession, ms: Long, now: Instant, today: LocalDate, ctx: FocusContext): FocusChange {
        if (ms <= 0) return FocusChange(s, totals, 0, 0)
        val week = weekStart(today)
        val weekMs = (if (totals.weekStart == week) totals.weekMs else 0) + ms
        val end = now.toEpochMilli()
        val together = togetherMs(end - ms, end, ctx.partner, now).coerceIn(0, ms)
        val next = totals.copy(totalMs = totals.totalMs + ms, weekMs = weekMs, weekStart = week, togetherMs = totals.togetherMs + together)
        val due = ((next.totalMs / HOUR).toInt() - next.pointHours).coerceIn(0, 10)
        val dueTogether = ((next.togetherMs / HOUR).toInt() - next.togetherHours).coerceIn(0, 10)
        val rate = ctx.rate.coerceIn(0, MAX_RATE)
        return FocusChange(
            s,
            next.copy(pointHours = next.pointHours + due, togetherHours = next.togetherHours + dueTogether),
            points = rate * (due + dueTogether),
            hours = due,
        )
    }

    /** "1:02:33" */
    fun stopwatch(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    }

    /** "4:12" left, or "+1:05" over. */
    fun countdown(leftMs: Long): String {
        val over = leftMs < 0
        // Round up while counting down, so it never shows 0:00 with time still left.
        val s = if (over) (-leftMs) / 1000 else (leftMs + 999) / 1000
        val text = if (s >= 3600) {
            String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
        } else {
            String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)
        }
        return if (over) "+$text" else text
    }

    /** "1 h 30 min", "45 min", "under a minute". */
    fun duration(ms: Long): String {
        val minutes = ms / 60_000
        return when {
            minutes < 1 -> "under a minute"
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0L -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }

    /** Hours for the scoreboard: "6.5" under ten, "41" from ten up. */
    fun hours(ms: Long): String {
        val h = ms.toDouble() / HOUR
        return if (h < 10) String.format(Locale.ROOT, "%.1f", h) else h.toLong().toString()
    }

    /** A jam link as pasted, cleaned up; null if it isn't a web link. */
    fun cleanJamUrl(input: String): String? {
        val t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() } || t.length > MAX_JAM_URL) return null
        val url = if (t.startsWith("https://") || t.startsWith("http://")) t else "https://$t"
        val host = url.substringAfter("://").substringBefore('/')
        return url.takeIf { host.contains('.') && host.length > 3 }
    }
}

/** Where the cat goes when someone taps it. It spends a minute there, then goes back to sleep. */
enum class CatActivity(val wire: String) {
    WatchingFish("fish"),
    PlayingWithPost("post"),
    PlayingGuitar("guitar"),
    Windowsill("window"),
    ;

    companion object {
        fun fromWire(wire: String?): CatActivity? = entries.firstOrNull { it.wire == wire }
    }
}

/** A trip the cat is on, started at [since]. */
data class CatOuting(val activity: CatActivity, val since: Instant)

object CatRules {
    /** Walking there (and back) takes this long. */
    const val WALK_MS = 2_500L

    /** How long it stays once it's there. */
    const val STAY_MS = 60_000L

    const val TOTAL_MS = WALK_MS + STAY_MS + WALK_MS

    /** Whether the cat is still out (or on its way back). */
    fun isOut(outing: CatOuting?, nowMs: Long): Boolean =
        outing != null && nowMs - outing.since.toEpochMilli() in 0 until TOTAL_MS
}
