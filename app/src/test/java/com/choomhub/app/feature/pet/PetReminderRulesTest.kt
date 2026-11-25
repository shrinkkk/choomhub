package com.choomhub.app.feature.pet

import com.choomhub.app.domain.model.NeedLevel
import com.choomhub.app.domain.model.PendingCare
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.TeamCare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class PetReminderRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val afternoon = Instant.parse("2026-11-25T09:30:00Z") // 15:00 IST
    private val night = Instant.parse("2026-11-25T18:30:00Z") // 00:00 IST

    private fun cat(hunger: Float = 80f, block: PetState.() -> PetState = { this }) = PetState(
        loaded = true,
        name = "Mochi",
        adoptedAt = afternoon.minus(Duration.ofDays(10)),
        needs = PetNeed.entries.associateWith { NeedLevel(if (it == PetNeed.Hunger) hunger else 80f, afternoon) },
    ).block()

    @Test
    fun `nothing when it's fine, or not adopted`() {
        assertNull(PetReminderRules.next(cat(), afternoon, zone, null, 0))
        assertNull(PetReminderRules.next(PetState(loaded = true), afternoon, zone, null, 0))
    }

    @Test
    fun `a low need gets a nudge`() {
        assertEquals("hunger", PetReminderRules.next(cat(5f), afternoon, zone, null, 0)?.key)
    }

    @Test
    fun `quiet at night, capped per day, spaced out`() {
        assertNull(PetReminderRules.next(cat(5f), night, zone, null, 0))
        assertNull(PetReminderRules.next(cat(5f), afternoon, zone, null, PetReminderRules.MAX_PER_DAY))
        assertNull(PetReminderRules.next(cat(5f), afternoon, zone, afternoon.minus(Duration.ofHours(1)), 1))
        assertEquals("hunger", PetReminderRules.next(cat(5f), afternoon, zone, afternoon.minus(Duration.ofHours(5)), 1)?.key)
    }

    @Test
    fun `being sick comes first, unless a vet trip is already under way`() {
        val sick = cat(5f) { copy(sickSince = afternoon) }
        assertEquals("sick", PetReminderRules.next(sick, afternoon, zone, null, 0)?.key)
        val started = sick.copy(pending = PendingCare(TeamCare.Vet, byMe = true, at = afternoon))
        assertEquals("hunger", PetReminderRules.next(started, afternoon, zone, null, 0)?.key)
        val medicine = sick.copy(vetDone = true, medicineLeft = 2)
        assertEquals("medicine", PetReminderRules.next(medicine, afternoon, zone, null, 0)?.key)
    }
}
