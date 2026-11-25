package com.choomhub.app.domain.model

import com.choomhub.app.data.firebase.FirebaseSpaceRepository.Companion.spaceStateFrom
import com.choomhub.app.ui.AppGate
import com.choomhub.app.ui.AppViewModel.Companion.gateFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceTest {
    private val me = Account("me", null)

    @Test
    fun `invite codes are 8 unambiguous characters and survive sloppy typing`() {
        repeat(200) {
            val code = InviteCodes.generate()
            assertTrue(code, InviteCodes.isWellFormed(code))
            assertFalse(code.any { it in "01ILO" })
        }
        assertEquals("ABCD2345", InviteCodes.normalize(" abcd-2345 "))
        assertEquals("ABCD 2345", InviteCodes.display("ABCD2345"))
        assertFalse(InviteCodes.isWellFormed("ABC"))
    }

    @Test
    fun `couple document maps to waiting, paired or no space`() {
        assertEquals(SpaceState.Waiting(me, "c1", "CODE2345"), spaceStateFrom(me, "c1", listOf("me"), "CODE2345"))
        assertEquals(SpaceState.Paired(me, "c1", "her"), spaceStateFrom(me, "c1", listOf("me", "her"), "CODE2345"))
        assertEquals(SpaceState.NoSpace(me), spaceStateFrom(me, "c1", null, null))
        assertEquals(SpaceState.NoSpace(me), spaceStateFrom(me, "c1", listOf("someone"), null))
    }

    @Test
    fun `someone who came back on another account is known by their old member id`() {
        // "her" left and rejoined as "her2"; I'm on my original account.
        val ids = mapOf("her2" to "her")
        assertEquals(SpaceState.Paired(me, "c1", "her", memberId = "me"), spaceStateFrom(me, "c1", listOf("me", "her2"), "", ids))
        // I'm the one who came back.
        val newMe = Account("me2", null)
        val mine = spaceStateFrom(newMe, "c1", listOf("her", "me2"), "", mapOf("me2" to "me"))
        assertEquals(SpaceState.Paired(newMe, "c1", "her", memberId = "me"), mine)
        assertEquals("me", mine.memberIdOrNull)
        // Alone after they left: waiting, with no code until a fresh one is made.
        assertEquals(SpaceState.Waiting(me, "c1", ""), spaceStateFrom(me, "c1", listOf("me"), ""))
    }

    @Test
    fun `the leave phrase must be typed out`() {
        assertTrue(com.choomhub.app.core.designsystem.component.typedPhraseMatches("i  hate jas ", "I hate Jas"))
        assertFalse(com.choomhub.app.core.designsystem.component.typedPhraseMatches("I hate", "I hate Jas"))
        assertFalse(com.choomhub.app.core.designsystem.component.typedPhraseMatches("", ""))
    }

    @Test
    fun `roles follow the first pick and can't be stolen`() {
        val repo = com.choomhub.app.data.firebase.FirestoreProfileRepository
        assertEquals(PointsRole.GoodBoy to false, repo.rolesFor("me", "me", null))
        assertEquals(PointsRole.GoodGirl to true, repo.rolesFor("me", "her", null))
        assertEquals(null to false, repo.rolesFor("me", null, null))
        assertEquals("me" to null, repo.claimRole("me", PointsRole.GoodBoy, null, null))
        assertEquals(null, repo.claimRole("me", PointsRole.GoodBoy, "her", null))
        assertEquals(null to "me", repo.claimRole("me", PointsRole.GoodGirl, "me", null))
        assertEquals(CoupleNames("Wren", null, null).needsOnboarding, true)
    }

    @Test
    fun `gate goes sign in, pairing, naming, main`() {
        val unnamed = CoupleNames(partnerName = null, myName = null)
        val named = CoupleNames(partnerName = "Wren", myName = null, myRole = PointsRole.GoodGirl)
        assertEquals(AppGate.SignIn, gateFor(SpaceState.SignedOut, named))
        assertEquals(AppGate.Pairing, gateFor(SpaceState.NoSpace(me), named))
        assertEquals(AppGate.Onboarding, gateFor(SpaceState.Waiting(me, "c", "X"), unnamed))
        assertEquals(AppGate.Main, gateFor(SpaceState.Waiting(me, "c", "X"), named))
        assertEquals(AppGate.Main, gateFor(SpaceState.Paired(me, "c", "her"), named))
        assertEquals(AppGate.Onboarding, gateFor(SpaceState.DeviceOnly, unnamed))
    }
}
