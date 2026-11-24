package com.choomhub.app.feature.home

import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.domain.model.CountdownMood
import com.choomhub.app.domain.model.CountdownState
import com.choomhub.app.domain.model.Meetup
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.feature.home.meetup.MeetupViewModel
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MeetupViewModelTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-11-24T18:00:00Z") // 23:30 IST
    private val clock = Clock.fixed(now, zone)
    private val ticks = MutableStateFlow(now)
    private val ticker = object : SecondTicker {
        override fun ticks(): Flow<Instant> = ticks
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `countdown follows the stored meetup and the ticking clock`() = runTest {
        val repo = FakeMeetupRepository(Meetup(now.plus(Duration.ofDays(5))))
        val vm = MeetupViewModel(repo, clock, ticker, FakeNotifier())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.countdown.collect {} }

        assertEquals(CountdownMood.Happy, (vm.countdown.value as CountdownState.Upcoming).mood)
        ticks.value = now.plus(Duration.ofDays(3))
        val later = vm.countdown.value as CountdownState.Upcoming
        assertEquals(CountdownMood.Excited, later.mood)
        assertEquals(Duration.ofDays(2), later.remaining)

        repo.meetup.value = null
        assertEquals(CountdownState.NoneScheduled, vm.countdown.value)
    }

    @Test
    fun `scheduling in the past is refused, the future is saved in local time`() {
        val repo = FakeMeetupRepository()
        val notifier = FakeNotifier()
        val vm = MeetupViewModel(repo, clock, ticker, notifier)

        val error = vm.schedule(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0))
        assertEquals("Pick a time that hasn't happened yet.", error)
        assertNull(repo.meetup.value)
        assertEquals(emptyList<Pair<PushEvent, String?>>(), notifier.sent)

        assertNull(vm.schedule(LocalDate.of(2026, 12, 13), LocalTime.of(19, 30)))
        assertEquals(Instant.parse("2026-12-13T14:00:00Z"), repo.meetup.value?.at)
        assertEquals(PushEvent.MeetupSet to "Sun 13 Dec, 7:30 pm", notifier.sent.last())

        vm.clear()
        assertNull(repo.meetup.value)
        assertEquals(PushEvent.MeetupCleared to null, notifier.sent.last())
    }
}
