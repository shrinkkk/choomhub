package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactionRulesTest {
    @Test
    fun `any one emoji is a reaction, text never is`() {
        listOf("🖤", "😢", "❤️", "👩‍💻", "🇮🇳", "👍🏽", "1️⃣")
            .forEach { assertTrue(it, SnapRules.isReaction(it)) }
        listOf("", "1", "lol", "🖤a", "🖤 ", "🖤".repeat(9)).forEach { assertFalse(it, SnapRules.isReaction(it)) }
    }

    @Test
    fun `the quick four start with the black heart`() {
        assertEquals(listOf("🖤", "😭", "😢", "🔥"), SnapRules.QUICK_DEFAULT)
        assertTrue(SnapRules.QUICK_DEFAULT.all(SnapRules::isReaction))
    }
}
