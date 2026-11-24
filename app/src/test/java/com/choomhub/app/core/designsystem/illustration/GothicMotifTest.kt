package com.choomhub.app.core.designsystem.illustration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class GothicMotifTest {

    @Test
    fun `motif holds for a few days then changes`() {
        val start = LocalDate.ofEpochDay(0)
        val first = GothicMotif.forDate(start)
        repeat(GothicMotif.DAYS_PER_MOTIF) { offset ->
            assertEquals(first, GothicMotif.forDate(start.plusDays(offset.toLong())))
        }
        assertNotEquals(first, GothicMotif.forDate(start.plusDays(GothicMotif.DAYS_PER_MOTIF.toLong())))
    }

    @Test
    fun `every motif comes round in one full cycle`() {
        val start = LocalDate.of(2026, 11, 24)
        val seen = (0 until GothicMotif.entries.size * GothicMotif.DAYS_PER_MOTIF)
            .map { GothicMotif.forDate(start.plusDays(it.toLong())) }
            .toSet()
        assertEquals(GothicMotif.entries.toSet(), seen)
    }
}
