package com.choomhub.app.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** When a bounty should be done by. */
enum class BountyDue(val wire: String, val label: String, val noticeLabel: String?) {
    Tonight("tonight", "Tonight", "by tonight"),
    Week("week", "This week", "this week"),
    Whenever("whenever", "Whenever", null),
    ;

    companion object {
        fun fromWire(wire: String?): BountyDue = entries.firstOrNull { it.wire == wire } ?: Whenever
    }
}

enum class BountyStatus(val wire: String) {
    Open("open"),
    /** The other one says it's done; waiting for the poster to pay. */
    Claimed("claimed"),
    Paid("paid"),
    ;

    companion object {
        fun fromWire(wire: String?): BountyStatus = entries.firstOrNull { it.wire == wire } ?: Open
    }
}

/** A job one of you pins up for the other, paid in points once the poster agrees it's done. */
data class Bounty(
    val id: String,
    val task: String,
    val reward: Int,
    val due: BountyDue,
    /** You posted it (so it's for them to do). */
    val byMe: Boolean,
    val status: BountyStatus,
    val postedAt: Instant?,
    val paidAt: Instant? = null,
)

object BountyRules {
    const val MAX_TASK = 80
    const val MIN_REWARD = 1
    const val MAX_REWARD = 20

    /** Paid ones stay up this long, then come down. */
    val PAID_SHOWN: Duration = Duration.ofDays(3)

    fun cleanTask(input: String): String? = input.replace(Regex("\\s+"), " ").trim().take(MAX_TASK).ifEmpty { null }

    /** The reason on the points it pays. */
    fun reason(task: String) = "Bounty: $task".take(140)

    /** Past its "by" and still not done. */
    fun lapsed(b: Bounty, now: Instant, zone: ZoneId): Boolean {
        if (b.status != BountyStatus.Open) return false
        val posted = b.postedAt ?: return false
        return when (b.due) {
            BountyDue.Tonight -> now.isAfter(posted.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant())
            BountyDue.Week -> now.isAfter(posted.plus(Duration.ofDays(7)))
            BountyDue.Whenever -> false
        }
    }

    /** What's on the board: everything open or waiting, and recently paid ones. */
    fun shown(all: List<Bounty>, now: Instant): List<Bounty> = all.filter {
        it.status != BountyStatus.Paid || (it.paidAt ?: now).isAfter(now.minus(PAID_SHOWN))
    }
}
