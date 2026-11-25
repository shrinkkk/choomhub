package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ShopRulesTest {
    @Test
    fun `the shop starts with the twelve rewards, cheapest first`() {
        val shop = ShopRules.catalogue(emptyList())
        assertEquals(12, shop.size)
        assertEquals("selfie", shop.first().id)
        assertEquals(shop.map { it.price }.sorted(), shop.map { it.price })
    }

    @Test
    fun `agreed changes set prices, take rewards out and add new ones`() {
        val shop = ShopRules.catalogue(
            listOf(
                RewardChange("selfie", null, 7, custom = false, removed = false),
                RewardChange("drawing", null, 25, custom = false, removed = true),
                RewardChange("p9", "Back scratch", 18, custom = true, removed = false),
                RewardChange("p10", "Gone", 3, custom = true, removed = true),
            ),
        )
        assertEquals(7, shop.first { it.id == "selfie" }.price)
        assertTrue(shop.none { it.id == "drawing" || it.id == "p10" })
        assertTrue(shop.first { it.id == "p9" }.custom)
        assertEquals(12, shop.size)
    }

    @Test
    fun `spending comes off your points, given back things don't count`() {
        val purchases = listOf(
            Purchase("a", "selfie", "Selfie", 5, byMe = true, at = Instant.EPOCH, state = PurchaseState.Used),
            Purchase("b", "drawing", "Drawing", 25, byMe = true, at = Instant.EPOCH, state = PurchaseState.Returned),
            Purchase("c", "letter", "Letter", 40, byMe = false, at = Instant.EPOCH, state = PurchaseState.Owned),
        )
        assertEquals(5, ShopRules.spent(purchases, byMe = true))
        assertEquals(45, ShopRules.available(50, purchases))
        val awards = listOf(PointAward("1", toMe = true, amount = 10, reason = null, at = null), PointAward("2", toMe = false, amount = 50, reason = null, at = null))
        assertEquals(PointBalances(goodBoy = 5, goodGirl = 10), PointRules.balances(awards, PointsRole.GoodBoy, purchases))
    }

    @Test
    fun `prices are whole numbers in range`() {
        assertEquals(20, ShopRules.parsePrice(" 20 ").getOrNull())
        assertTrue(ShopRules.parsePrice("0").isFailure)
        assertTrue(ShopRules.parsePrice("501").isFailure)
        assertTrue(ShopRules.parsePrice("abc").isFailure)
        assertNull(ShopRules.cleanName("   "))
    }
}
