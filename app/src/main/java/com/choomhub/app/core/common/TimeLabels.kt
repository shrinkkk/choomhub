package com.choomhub.app.core.common

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Small, pure helpers for the human-friendly time strings used across the app. */
object TimeLabels {

    /** "just now", "12 min ago", "3 h ago", "yesterday", "4 days ago", then a date. */
    fun relative(then: Instant, now: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String {
        val elapsed = Duration.between(then, now)
        return when {
            elapsed.isNegative || elapsed < Duration.ofMinutes(1) -> "just now"
            elapsed < Duration.ofHours(1) -> "${elapsed.toMinutes()} min ago"
            elapsed < Duration.ofHours(24) -> "${elapsed.toHours()} h ago"
            elapsed < Duration.ofDays(2) -> "yesterday"
            elapsed < Duration.ofDays(7) -> "${elapsed.toDays()} days ago"
            else -> DateTimeFormatter.ofPattern("d MMM", locale).format(then.atZone(zone))
        }
    }

    /** e.g. "Tuesday, 24 November". */
    fun longDate(now: ZonedDateTime, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(now)

    /** e.g. "8:41 pm" or "20:41" depending on [use24Hour]. */
    fun clockTime(time: ZonedDateTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale).format(time).lowercase(locale)
    }

    /** e.g. "Sat 14 Dec, 7:30 pm": the meetup time as it appears in a notification. */
    fun meetupMoment(time: ZonedDateTime, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE d MMM", locale).format(time) + ", " + clockTime(time, use24Hour = false, locale = locale)

    /** Plain greeting for the hour. */
    fun greeting(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..22 -> "Evening"
        else -> "Late one"
    }
}
