package com.choomhub.app.feature.mood

import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodRules
import com.choomhub.app.domain.model.MoodUpdate
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.testing.FakeMoodRepository
import com.choomhub.app.testing.FakeNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoodViewModelTest {

    private val notifier = FakeNotifier()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saving needs a mood, cleans the note and tells them`() {
        val repo = FakeMoodRepository()
        val vm = MoodViewModel(repo, notifier)
        vm.save()
        assertEquals("Pick one first.", vm.state.value.error)
        assertFalse(vm.state.value.done)

        vm.select(Mood.Sleepy)
        vm.onNoteChange("  headphones   on\n ignoring the rain ")
        vm.save()
        assertTrue(vm.state.value.done)
        assertEquals(MoodUpdate(Mood.Sleepy, "headphones on ignoring the rain", java.time.Instant.EPOCH), repo.moods.value.mine)
        assertEquals(listOf<Pair<PushEvent, String?>>(PushEvent.MoodChanged to "headphones on ignoring the rain"), notifier.sent)
        assertEquals(listOf<String?>("sleepy"), notifier.kinds)
    }

    @Test
    fun `opens on your current mood, and saving it unchanged notifies no one`() {
        val repo = FakeMoodRepository(CoupleMoods(MoodUpdate(Mood.Calm, "tea", null), null))
        val vm = MoodViewModel(repo, notifier)
        assertEquals(Mood.Calm, vm.state.value.selected)
        assertEquals("tea", vm.state.value.note)
        vm.save()
        assertTrue(vm.state.value.done)
        assertEquals(0, repo.saves)
        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun `note rules`() {
        assertNull(MoodRules.cleanNote("   "))
        assertEquals(MoodRules.MAX_NOTE, MoodRules.cleanNote("x".repeat(200))!!.length)
        val vm = MoodViewModel(FakeMoodRepository(), notifier)
        vm.onNoteChange("y".repeat(200))
        assertEquals(MoodRules.MAX_NOTE, vm.state.value.note.length)
    }
}
