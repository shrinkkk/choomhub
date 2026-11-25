package com.choomhub.app.feature.shop

import com.choomhub.app.data.shop.LocalShopRepository
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointAward
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.model.ShopRules
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakePointsRepository
import com.choomhub.app.testing.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
    private val shop = LocalShopRepository()
    private val points = FakePointsRepository().also {
        it.ledger.value = listOf(PointAward("a", toMe = true, amount = 30, reason = null, at = Instant.EPOCH))
    }
    private val names = FakeProfileRepository(CoupleNames(partnerName = "Jas", myName = "Shri", myRole = PointsRole.GoodBoy))
    private val notifier = FakeNotifier()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ShopViewModel(shop, points, names, notifier)

    @Test
    fun `buying takes your points and tells them, and you can't overspend`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(30, vm.state.value.myPoints)
        val massage = vm.state.value.catalogue.first { it.id == "head_massage" }
        assertTrue(vm.buy(massage))
        assertEquals(15, vm.state.value.myPoints)
        assertEquals(PushEvent.Shop, notifier.sent.last().first)
        assertEquals("bought", notifier.kinds.last())
        assertFalse(vm.buy(vm.state.value.catalogue.first { it.id == "drawing" }))
        assertNotNull(vm.message.value)
        // Using it, then giving back is no longer possible in the UI; giving back an unused one refunds.
        val bought = vm.state.value.purchases.single()
        vm.giveBack(bought)
        assertEquals(30, vm.state.value.myPoints)
    }

    @Test
    fun `suggestions wait for the other one and are checked`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals("What's the reward?", vm.suggestReward("  ", "10"))
        assertEquals("Between 1 and 500 points.", vm.suggestReward("Back scratch", "900"))
        assertEquals(null, vm.suggestReward("Back scratch", "18"))
        assertEquals(ProposalKind.Add, vm.state.value.proposals.single().kind)
        assertTrue(vm.state.value.catalogue.none { it.name == "Back scratch" })
        assertEquals("suggest_add", notifier.kinds.last())

        val selfie = vm.state.value.catalogue.first { it.id == "selfie" }
        assertEquals("That's already the price.", vm.suggestPrice(selfie, "5"))
        assertEquals(null, vm.suggestPrice(selfie, "8"))
        assertEquals(selfie, ShopRules.catalogue(vm.state.value.changes).first { it.id == "selfie" })
    }

    @Test
    fun `agreeing to their suggestion changes the shop and tells them`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val theirs = ShopProposal("p1", ProposalKind.Price, "selfie", null, 9, byMe = false, at = Instant.EPOCH)
        shop.data.update { it.copy(proposals = listOf(theirs)) }
        vm.accept(theirs)
        assertEquals(9, vm.state.value.catalogue.first { it.id == "selfie" }.price)
        assertTrue(vm.state.value.proposals.isEmpty())
        assertEquals("agreed", notifier.kinds.last())

        val removal = ShopProposal("p2", ProposalKind.Remove, "selfie", null, null, byMe = false, at = Instant.EPOCH)
        shop.data.update { it.copy(proposals = listOf(removal)) }
        vm.dismiss(removal)
        assertEquals("declined", notifier.kinds.last())
        assertTrue(vm.state.value.catalogue.any { it.id == "selfie" })
    }

    @Test
    fun `cashing in and marking done`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.buy(vm.state.value.catalogue.first { it.id == "selfie" })
        vm.use(vm.state.value.purchases.single())
        assertEquals(PurchaseState.Used, vm.state.value.purchases.single().state)
        assertEquals("used", notifier.kinds.last())
        vm.markDone(vm.state.value.purchases.single())
        assertEquals(PurchaseState.Done, vm.state.value.purchases.single().state)
        assertEquals(25, vm.state.value.myPoints)
    }
}
