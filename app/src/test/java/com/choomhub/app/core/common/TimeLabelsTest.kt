package com.choomhub.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class TimeLabelsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-11-24T15:52:00Z")

    private fun ago(duration: Duration) = TimeLabels.relative(now.minus(duration), now, zone, Locale.UK)

    @Test
    fun `relative labels step through minutes, hours and days`() {
        assertEquals("just now", ago(Duration.ofSeconds(20)))
        assertEquals("12 min ago", ago(Duration.ofMinutes(12)))
        assertEquals("3 h ago", ago(Duration.ofHours(3)))
        assertEquals("yesterday", ago(Duration.ofHours(30)))
        assertEquals("4 days ago", ago(Duration.ofDays(4)))
        assertEquals("10 Nov", ago(Duration.ofDays(14)))
    }

    @Test
    fun `future timestamps from clock skew read as just now`() {
        assertEquals("just now", TimeLabels.relative(now.plusSeconds(90), now, zone))
    }

    @Test
    fun `greeting follows the hour`() {
        assertEquals("Good morning", TimeLabels.greeting(LocalTime.of(7, 0)))
        assertEquals("Good afternoon", TimeLabels.greeting(LocalTime.of(13, 30)))
        assertEquals("Good evening", TimeLabels.greeting(LocalTime.of(21, 22)))
        assertEquals("Still awake?", TimeLabels.greeting(LocalTime.of(2, 10)))
    }

    @Test
    fun `long date and clock time format`() {
        val time = ZonedDateTime.of(2026, 11, 24, 20, 41, 0, 0, zone)
        assertEquals("Tuesday, 24 November", TimeLabels.longDate(time, Locale.UK))
        assertEquals("20:41", TimeLabels.clockTime(time, use24Hour = true, locale = Locale.UK))
        assertEquals("8:41 pm", TimeLabels.clockTime(time, use24Hour = false, locale = Locale.UK))
    }
}
