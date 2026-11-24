package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameRulesTest {

    @Test
    fun `names are trimmed and inner spaces collapsed`() {
        assertEquals(NameRules.Result.Valid("Little Crow"), NameRules.validate("   Little    Crow  "))
    }

    @Test
    fun `blank and overly long names are rejected`() {
        assertEquals(NameRules.Result.Empty, NameRules.validate("   "))
        assertEquals(NameRules.Result.TooLong, NameRules.validate("x".repeat(NameRules.MAX_LENGTH + 1)))
        assertEquals(NameRules.Result.Valid("x".repeat(NameRules.MAX_LENGTH)), NameRules.validate("x".repeat(NameRules.MAX_LENGTH)))
    }

    @Test
    fun `only invalid results carry a message`() {
        assertNull(NameRules.messageFor(NameRules.Result.Valid("Wren")))
        assertEquals("Give them a name first.", NameRules.messageFor(NameRules.Result.Empty))
    }

    @Test
    fun `onboarding is needed until the partner has a name`() {
        assert(CoupleNames(partnerName = null, myName = "Rook").needsOnboarding)
        assert(!CoupleNames(partnerName = "Wren", myName = null).needsOnboarding)
    }
}
