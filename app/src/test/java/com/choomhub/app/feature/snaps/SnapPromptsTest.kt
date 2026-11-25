package com.choomhub.app.feature.snaps

import com.choomhub.app.domain.model.SnapPrompts
import com.choomhub.app.feature.snaps.prompt.SnapPromptScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SnapPromptsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 25)

    @Test
    fun `both phones get the same prompt and time for a day, between noon and 8 pm`() {
        assertEquals(SnapPrompts.forDate(day), SnapPrompts.forDate(day))
        (0L until 60L).map { day.plusDays(it) }.forEach { d ->
            val t = SnapPrompts.timeFor(d)
            assertTrue("$t", !t.isBefore(LocalTime.NOON) && t.isBefore(LocalTime.of(20, 0)))
        }
        assertTrue("prompts vary", (0L until 30L).map { SnapPrompts.forDate(day.plusDays(it)) }.toSet().size > 10)
    }

    @Test
    fun `the next prompt is today's if it's still ahead, otherwise tomorrow's`() {
        val morning = day.atTime(8, 0).atZone(zone)
        assertEquals(day.atTime(SnapPrompts.timeFor(day)).atZone(zone), morning.plus(SnapPromptScheduler.delayUntilNext(morning)))
        val late = day.atTime(22, 0).atZone(zone)
        val tomorrow = day.plusDays(1)
        assertEquals(tomorrow.atTime(SnapPrompts.timeFor(tomorrow)).atZone(zone), late.plus(SnapPromptScheduler.delayUntilNext(late)))
    }
}
