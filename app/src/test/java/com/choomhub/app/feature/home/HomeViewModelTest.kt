package com.choomhub.app.feature.home

import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Member
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.PartnerMood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.domain.model.Snap
import com.choomhub.app.domain.repository.HomeRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-11-24T15:52:00Z") // 21:22 IST
    private val clock = Clock.fixed(now, zone)
    private val partner = Member("p", "Wren")
    private val gestures = FakeGestureRepository()
    private val notifier = FakeNotifier()

    private val snapshot = HomeSnapshot(
        viewerName = "Rook",
        partnerMood = PartnerMood(partner, Mood.Cozy, note = "  ", updatedAt = now.minus(Duration.ofMinutes(5))),
        latestSnap = Snap("s1", partner, "rain again", now.minus(Duration.ofHours(2)), isUnseen = true),
        points = PointBalances(goodBoy = 3, goodGirl = 9),
        cat = SharedCatStatus(isAdopted = false, name = null),
        isSampleData = false,
    )

    private class FakeHomeRepository(initial: HomeSnapshot) : HomeRepository {
        val data = MutableStateFlow(initial)
        override fun observeHome(): Flow<HomeSnapshot> = data
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `maps repository data into ready state`() = runTest {
        val viewModel = HomeViewModel(FakeHomeRepository(snapshot), gestures, notifier, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value as HomeUiState.Ready
        assertEquals("Evening", state.greeting)
        assertEquals("Rook", state.viewerName)
        assertEquals("Wren", state.partner?.name)
        assertEquals(Mood.Cozy, state.partner?.mood)
        assertNull("blank notes are hidden", state.partner?.note)
        assertEquals("5 min ago", state.partner?.updatedLabel)
        assertEquals("2 h ago", state.snap?.relativeLabel)
        assertTrue(state.snap!!.isNew)
        assertEquals(9, state.points.goodGirl)
    }

    @Test
    fun `newer repository data replaces older state`() = runTest {
        val repository = FakeHomeRepository(snapshot)
        val viewModel = HomeViewModel(repository, gestures, notifier, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        repository.data.value = snapshot.copy(
            partnerMood = snapshot.partnerMood!!.copy(mood = Mood.MissingYou, updatedAt = now),
        )

        val state = viewModel.uiState.value as HomeUiState.Ready
        assertEquals(Mood.MissingYou, state.partner?.mood)
        assertEquals("just now", state.partner?.updatedLabel)
    }

    @Test
    fun `a gesture is sent, the partner is notified, and repeat taps are ignored`() {
        val viewModel = HomeViewModel(FakeHomeRepository(snapshot), gestures, notifier, clock)
        viewModel.onAffectionTapped(AffectionKind.Hug)
        assertEquals("Hug sent.", viewModel.pendingNotice.value)
        assertEquals(listOf<Pair<PushEvent, String?>>(PushEvent.Gesture to null), notifier.sent)
        assertEquals(listOf<String?>("hug"), notifier.kinds)

        viewModel.onNoticeShown()
        viewModel.onAffectionTapped(AffectionKind.Hug) // same instant: inside the cooldown
        assertNull(viewModel.pendingNotice.value)
        assertEquals(1, gestures.history.value.size)

        viewModel.onAffectionTapped(AffectionKind.TomatoDelivery)
        assertEquals("Tomato on its way.", viewModel.pendingNotice.value)
    }

    @Test
    fun `a refused gesture says why and notifies no one`() {
        gestures.refusal = "They haven't joined your space yet. Gestures go out once they do."
        val viewModel = HomeViewModel(FakeHomeRepository(snapshot), gestures, notifier, clock)
        viewModel.onAffectionTapped(AffectionKind.ForeheadKiss)
        assertEquals(gestures.refusal, viewModel.pendingNotice.value)
        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun `latest gestures each way are shown with names`() = runTest {
        val withGestures = snapshot.copy(
            partnerName = "Wren",
            recentGestures = listOf(
                Gesture("2", AffectionKind.HeadPats, fromMe = true, sentAt = now.minus(Duration.ofMinutes(1))),
                Gesture("1", AffectionKind.Hug, fromMe = false, sentAt = now.minus(Duration.ofMinutes(3))),
            ),
        )
        val viewModel = HomeViewModel(FakeHomeRepository(withGestures), gestures, notifier, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value as HomeUiState.Ready
        assertEquals("Wren sent a hug", state.lastReceived?.text)
        assertEquals("3 min ago", state.lastReceived?.whenLabel)
        assertEquals("You gave head pats", state.lastSent?.text)
    }
}
