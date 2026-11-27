package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class PetRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val t0 = Instant.parse("2026-11-25T08:30:00Z") // 14:00 IST
    private val today = LocalDate.of(2026, 11, 25)

    private fun cat(level: Float = 50f, traits: List<PetTrait> = emptyList(), block: PetState.() -> PetState = { this }) = PetState(
        loaded = true,
        name = "Mochi",
        adoptedAt = t0.minus(Duration.ofDays(30)),
        traits = traits,
        favoriteFood = Food.Tuna,
        dislikedFood = Food.WetFood,
        needs = PetNeed.entries.associateWith { NeedLevel(level, t0) },
    ).block()

    private fun act(state: PetState, action: PetAction, random: Random = Random(1), at: Instant = t0) =
        PetRules.act(state, action, at, zone, random, "Shri", "Jas")

    @Test
    fun `needs drift from their stored value over time and stay in range`() {
        val s = cat(50f)
        val later = t0.plus(Duration.ofHours(2))
        assertEquals(50f - 12f, PetRules.level(s, PetNeed.Hunger, later), 0.01f)
        assertEquals(50f + 18f, PetRules.level(s, PetNeed.Energy, later), 0.01f)
        assertEquals(0f, PetRules.level(s, PetNeed.Thirst, t0.plus(Duration.ofDays(2))), 0f)
        assertEquals(100f, PetRules.level(s, PetNeed.Energy, t0.plus(Duration.ofDays(2))), 0f)
    }

    @Test
    fun `traits and furniture change how fast needs drift`() {
        val plain = cat()
        assertTrue(PetRules.rate(PetNeed.Affection, cat(traits = listOf(PetTrait.Clingy))) < PetRules.rate(PetNeed.Affection, plain))
        assertTrue(PetRules.rate(PetNeed.Thirst, cat { copy(owned = setOf("fountain")) }) > PetRules.rate(PetNeed.Thirst, plain))
        assertTrue(PetRules.rate(PetNeed.Energy, cat { copy(owned = setOf("heated_bed")) }) > PetRules.rate(PetNeed.Energy, plain))
    }

    @Test
    fun `feeding fills hunger, the favourite gives love and coins come with it`() {
        val s = cat(30f) { copy(inventory = mapOf("tuna" to 2)) }
        val o = act(s, PetAction.Feed(Food.Tuna))
        assertTrue(o.patch.needs.getValue(PetNeed.Hunger) > 80f)
        assertTrue(PetNeed.Affection in o.patch.needs)
        assertEquals(-1, o.patch.inventory["tuna"])
        assertEquals(PetRules.COIN_PER_CARE, o.patch.coins)
        assertEquals(1 to today, o.patch.streak)
        assertTrue(o.message.contains("favourite"))
    }

    @Test
    fun `food you don't have can't be served, kibble always can`() {
        val s = cat(30f)
        assertTrue(act(s, PetAction.Feed(Food.Salmon)).patch.isEmpty())
        assertFalse(act(s, PetAction.Feed(Food.Kibble)).patch.isEmpty())
    }

    @Test
    fun `disliked food fills less and overfeeding puts on weight`() {
        val s = cat(30f) { copy(inventory = mapOf("wet_food" to 1)) }
        val disliked = act(s, PetAction.Feed(Food.WetFood)).patch.needs.getValue(PetNeed.Hunger)
        assertEquals(30f + Food.WetFood.hunger * 0.4f, disliked, 0.01f)
        val full = act(cat(95f), PetAction.Feed(Food.Kibble)).patch
        assertTrue(full.weight!! > PetRules.BASE_WEIGHT)
        assertEquals("no coins for feeding a full cat", 0, full.coins)
    }

    @Test
    fun `coins stop at the daily cap`() {
        val s = cat(30f) { copy(myCoinDay = today, myCoinToday = PetRules.DAILY_COIN_CAP - 1) }
        assertEquals(1, act(s, PetAction.Water).patch.coins)
        val capped = s.copy(myCoinToday = PetRules.DAILY_COIN_CAP)
        assertEquals(0, act(capped, PetAction.Water).patch.coins)
    }

    @Test
    fun `the streak grows on consecutive days and resets after a gap`() {
        assertEquals(5 to today, act(cat { copy(streak = 4, streakDay = today.minusDays(1)) }, PetAction.Water).patch.streak)
        assertNull("already counted today", act(cat { copy(streak = 4, streakDay = today) }, PetAction.Water).patch.streak)
        assertEquals(1 to today, act(cat { copy(streak = 4, streakDay = today.minusDays(3)) }, PetAction.Water).patch.streak)
    }

    @Test
    fun `the belly is sometimes a trap`() {
        val s = cat(50f, traits = listOf(PetTrait.Sassy))
        val outcomes = (0 until 40).map { act(s, PetAction.Pet(2), Random(it)) }
        val bites = outcomes.count { (it.patch.counters["bites"] ?: 0) > 0 }
        assertTrue("some bites", bites > 5)
        assertTrue("not always", bites < 40)
        // The head never bites.
        assertTrue((0 until 20).none { (act(s, PetAction.Pet(0), Random(it)).patch.counters["bites"] ?: 0) > 0 })
    }

    @Test
    fun `petting together gives a double purr`() {
        val s = cat(40f) { copy(partnerPettingAt = t0.minusSeconds(10)) }
        val o = act(s, PetAction.Pet(0))
        assertEquals(1, o.patch.counters["double_purrs"])
        assertEquals(40f + 9f + 15f, o.patch.needs.getValue(PetNeed.Affection), 0.01f)
        val alone = act(cat(40f) { copy(partnerPettingAt = t0.minusSeconds(60)) }, PetAction.Pet(0))
        assertNull(alone.patch.counters["double_purrs"])
    }

    @Test
    fun `too tired to play, and playing tires it out`() {
        assertTrue(act(cat(5f), PetAction.Play("feather", 3)).patch.isEmpty())
        val o = act(cat(60f), PetAction.Play("feather", 4))
        assertEquals(60f + 15f + 24f, o.patch.needs.getValue(PetNeed.Fun), 0.01f)
        assertTrue(o.patch.needs.getValue(PetNeed.Energy) < 60f)
    }

    @Test
    fun `nails can be trimmed once a week`() {
        val done = act(cat(), PetAction.TrimNails).patch
        assertEquals(t0, done.nailsAt)
        val s = cat { copy(nailsAt = t0.minus(Duration.ofDays(2))) }
        assertTrue(act(s, PetAction.TrimNails).patch.isEmpty())
        assertFalse(act(s.copy(nailsAt = t0.minus(Duration.ofDays(8))), PetAction.TrimNails).patch.isEmpty())
    }

    @Test
    fun `the vet takes two, then three days of medicine makes it better`() {
        val sick = cat { copy(sickSince = t0.minus(Duration.ofHours(5)), vetDone = false) }
        val start = act(sick, PetAction.StartTeamCare(TeamCare.Vet)).patch
        assertEquals(TeamCare.Vet, start.pending?.kind)
        // The same person can't finish it.
        val mine = sick.copy(pending = PendingCare(TeamCare.Vet, byMe = true, at = t0))
        assertTrue(act(mine, PetAction.JoinTeamCare).patch.isEmpty())
        // The other one can.
        val theirs = sick.copy(pending = PendingCare(TeamCare.Vet, byMe = false, at = t0))
        val vet = act(theirs, PetAction.JoinTeamCare).patch
        assertEquals(true, vet.vetDone)
        assertEquals(3, vet.medicineLeft)
        assertTrue(vet.clearPending)

        var s = theirs.applied(vet, t0)
        assertTrue(s.sick)
        for (day in 0 until 3) {
            val at = t0.plus(Duration.ofDays(day.toLong()))
            val o = act(s, PetAction.GiveMedicine, at = at)
            assertFalse(o.patch.isEmpty())
            s = s.applied(o.patch, at)
            assertTrue("only once a day", act(s, PetAction.GiveMedicine, at = at).patch.isEmpty())
        }
        assertFalse(s.sick)
        assertFalse(s.vetDone)
    }

    @Test
    fun `no vet or bath when it doesn't need one`() {
        assertTrue(act(cat(), PetAction.StartTeamCare(TeamCare.Vet)).patch.isEmpty())
        assertTrue(act(cat(), PetAction.StartTeamCare(TeamCare.Bath)).patch.isEmpty())
        val bath = act(cat { copy(muddy = true, pending = PendingCare(TeamCare.Bath, false, t0)) }, PetAction.JoinTeamCare).patch
        assertEquals(false, bath.muddy)
    }

    @Test
    fun `the shop takes your own coins and things are bought once`() {
        val laser = PetRules.item("laser")!!
        assertTrue(act(cat { copy(myCoins = 10) }, PetAction.Buy(laser)).patch.isEmpty())
        val bought = act(cat { copy(myCoins = 100) }, PetAction.Buy(laser)).patch
        assertEquals(-laser.price, bought.coins)
        assertEquals("laser", bought.own)
        assertTrue(act(cat { copy(myCoins = 100, owned = setOf("laser")) }, PetAction.Buy(laser)).patch.isEmpty())
        val tuna = act(cat { copy(myCoins = 100) }, PetAction.Buy(PetRules.item("tuna")!!)).patch
        assertEquals(3, tuna.inventory["tuna"])
    }

    @Test
    fun `wearing a look, one thing per slot, and taking it all off`() {
        val dresser = cat { copy(owned = setOf("crown", "scarf", "striped_socks", "laser")) }
        val on = dresser.applied(act(dresser, PetAction.Wear(listOf("crown"), "crown")).patch, t0)
        assertEquals("crown", on.outfit)
        val more = on.applied(act(on, PetAction.Wear(listOf("crown", "scarf", "striped_socks"), "striped_socks")).patch, t0)
        assertEquals(listOf("crown", "scarf", "striped_socks"), more.wearing())
        // Things it doesn't own, or that aren't clothes, are left out.
        val odd = more.applied(act(more, PetAction.Wear(listOf("crown", "beanie", "laser"))).patch, t0)
        assertEquals(listOf("crown"), odd.wearing())
        val off = odd.applied(act(odd, PetAction.Wear(emptyList())).patch, t0)
        assertNull(off.outfit)
        assertEquals(emptyList<String>(), off.wearing())
    }

    @Test
    fun `the daily check is the same on both phones and runs once`() {
        val s = cat()
        val a = PetRules.daily(s, t0, zone)
        val b = PetRules.daily(s, t0.plus(Duration.ofHours(3)), zone)
        assertEquals(a.gift, b.gift)
        assertEquals(a.sickSince != null, b.sickSince != null)
        assertEquals(today, a.lastDailyCheck)
        assertEquals(PetRules.DAILY_BONUS, a.coins)
        val after = s.applied(a, t0)
        assertTrue(PetRules.daily(after, t0.plus(Duration.ofHours(1)), zone).isEmpty())
    }

    @Test
    fun `gifts turn up over a few weeks`() {
        var s = cat()
        for (d in 0 until 30) {
            val at = t0.plus(Duration.ofDays(d.toLong()))
            s = s.applied(PetRules.daily(s, at, zone), at)
        }
        assertTrue(s.gifts.size in 5..25)
        assertNotNull(s.lastDailyCheck)
    }

    @Test
    fun `status points at what's wrong`() {
        val hungry = cat(80f) { copy(needs = needs + (PetNeed.Hunger to NeedLevel(10f, t0))) }
        assertTrue(PetRules.status(hungry, t0).contains("bowl"))
        assertTrue(PetRules.status(cat(90f), t0).contains("content"))
        assertTrue(PetRules.status(cat { copy(sickSince = t0) }, t0).contains("vet"))
    }

    @Test
    fun `names are tidied and capped`() {
        assertEquals("Mochi Bean", PetRules.cleanName("  Mochi   Bean "))
        assertNull(PetRules.cleanName("   "))
        assertEquals(PetRules.MAX_NAME, PetRules.cleanName("x".repeat(40))!!.length)
    }

    @Test
    fun `kittens grow up and chonk is tracked`() {
        val kitten = cat { copy(adoptedAt = t0.minus(Duration.ofDays(3))) }
        assertTrue(PetRules.isKitten(kitten, t0))
        assertFalse(PetRules.isKitten(cat(), t0))
        assertTrue(PetRules.isChonky(cat { copy(weight = 5.8f) }))
        assertTrue(PetRules.TROPHIES.first { it.id == "grown" }.unlocked(cat(), t0))
    }
}
