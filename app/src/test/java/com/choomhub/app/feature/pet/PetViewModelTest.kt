package com.choomhub.app.feature.pet

import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.data.pet.LocalPetRepository
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.TeamCare
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-11-25T09:30:00Z")
    private val clock = Clock.fixed(now, zone)
    private val notifier = FakeNotifier()
    private val names = FakeProfileRepository(CoupleNames(partnerName = "Jas", myName = "Shri"))
    private val noTicks = object : SecondTicker {
        override fun ticks(): Flow<Instant> = emptyFlow()
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: LocalPetRepository) = PetViewModel(repo, names, noTicks, notifier, clock).also { it.random = Random(3) }

    @Test
    fun `suggesting a name stores it and tells your partner`() = runTest {
        val repo = LocalPetRepository()
        val vm = viewModel(repo)
        vm.proposeName("  Mochi ")
        assertEquals("Mochi", repo.pet.value.proposedName)
        assertEquals(PushEvent.PetNamed to "Mochi", notifier.sent.single())
    }

    @Test
    fun `adopting gives it traits, a birthday, and the first day's coins`() = runTest {
        val repo = LocalPetRepository()
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.adopt("Mochi")
        val pet = repo.pet.value
        assertEquals("Mochi", pet.name)
        assertEquals(now, pet.adoptedAt)
        assertEquals(2, pet.traits.size)
        assertTrue(pet.favoriteFood != pet.dislikedFood)
        assertTrue(pet.myCoins >= PetRules.DAILY_BONUS)
        assertTrue(repo.diary.value.isNotEmpty())
        assertEquals("Welcome home, Mochi.", vm.message.value?.text)
    }

    @Test
    fun `actions are stored, and starting a vet trip asks your partner for help`() = runTest {
        val repo = LocalPetRepository()
        repo.adopt("Mochi", emptyList(), Food.Tuna, Food.WetFood, now)
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.act(PetAction.Water)
        assertEquals(100f, repo.pet.value.needs.getValue(PetNeed.Thirst).value)

        repo.pet.value = repo.pet.value.copy(sickSince = now, vetDone = false)
        vm.act(PetAction.StartTeamCare(TeamCare.Vet))
        assertEquals(TeamCare.Vet, repo.pet.value.pending?.kind)
        assertEquals(PushEvent.PetHelp, notifier.sent.last().first)
        assertEquals("vet", notifier.kinds.last())
    }

    @Test
    fun `something that can't be done isn't stored`() = runTest {
        val repo = LocalPetRepository()
        repo.adopt("Mochi", emptyList(), Food.Tuna, Food.WetFood, now)
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val before = repo.pet.value
        val o = vm.act(PetAction.Feed(Food.Salmon))
        assertTrue(o.patch.isEmpty())
        assertEquals(before.needs, repo.pet.value.needs)
        assertTrue(notifier.sent.isEmpty())
    }
}
