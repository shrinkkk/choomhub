package com.choomhub.app.feature.world

import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MemoryLaneRulesTest {
    private val t0 = Instant.parse("2026-11-01T10:00:00Z")
    private fun snap(id: String, mine: Boolean, minutes: Long, vararg collections: String) =
        SnapInfo(id, mine, null, t0.plusSeconds(minutes * 60), 4, 3, null, collections.toList())

    @Test
    fun theNewestSnapOfEachPersonIsCurrent() {
        val oldTheirs = snap("a", false, 0, "k")
        val newTheirs = snap("b", false, 10)
        val mine = snap("c", true, 5, "k")
        val recent = listOf(oldTheirs, newTheirs, mine)
        assertFalse(SnapRules.isCurrent(oldTheirs, recent))
        assertTrue(SnapRules.isCurrent(newTheirs, recent))
        assertTrue(SnapRules.isCurrent(mine, recent))
    }

    @Test
    fun whenWeCantTellItCountsAsCurrent() {
        val old = snap("a", false, 0, "k")
        // Nothing of theirs among the recent ones: unknown, so it's kept safe.
        assertTrue(SnapRules.isCurrent(old, listOf(snap("m", true, 50))))
        // Still on its way (no server time yet).
        assertTrue(SnapRules.isCurrent(old.copy(sentAt = null), emptyList()))
    }

    @Test
    fun onlyAnOldSnapInNoOtherCollectionIsDeletedForGood() {
        val old = snap("a", false, 0, "k")
        val shared = snap("s", false, 1, "k", "j")
        val newest = snap("b", false, 10, "k")
        val recent = listOf(old, shared, newest)
        assertTrue(SnapRules.deletesForGood(old, "k", recent))
        assertFalse(SnapRules.deletesForGood(shared, "k", recent))
        assertFalse(SnapRules.deletesForGood(newest, "k", recent))
    }

    @Test
    fun theCoverIsTheChosenSnapOrTheNewest() {
        val a = snap("a", false, 0, "k")
        val b = snap("b", true, 10, "k")
        assertEquals("b", SnapRules.coverOf(SnapCollection("k", "crows", null), listOf(a, b))?.id)
        assertEquals("a", SnapRules.coverOf(SnapCollection("k", "crows", null, cover = "a"), listOf(a, b))?.id)
        assertEquals("b", SnapRules.coverOf(SnapCollection("k", "crows", null, cover = "gone"), listOf(a, b))?.id)
    }
}
