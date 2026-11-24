package com.choomhub.app.feature.mood

import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodRules
import com.choomhub.app.testing.FakeMoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoodOptionsViewModelTest {

    private val repo = FakeMoodRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `add, refuse duplicates and long names, remove, restore`() {
        val vm = MoodOptionsViewModel(repo)
        vm.onInputChange("  Angry  bcs   parents ")
        vm.add()
        val added = repo.options.value.last()
        assertEquals("Angry bcs parents", added.label)
        assertTrue(added.key.startsWith("c_"))
        assertEquals("", vm.state.value.input)

        vm.onInputChange("angry BCS parents")
        vm.add()
        assertEquals("That one's already in the list.", vm.state.value.error)

        vm.onInputChange("x".repeat(MoodRules.MAX_LABEL + 3))
        vm.add()
        assertEquals("Keep it to 24 characters.", vm.state.value.error)

        vm.remove(Mood.Cozy.key)
        assertTrue(repo.options.value.none { it.key == Mood.Cozy.key })

        vm.restoreDefaults()
        assertEquals(Mood.defaults, repo.options.value)
    }

    @Test
    fun `the last mood can't be removed`() {
        repo.options.value = listOf(Mood.Calm.option)
        val vm = MoodOptionsViewModel(repo)
        vm.remove(Mood.Calm.key)
        assertEquals("Keep at least one.", vm.state.value.error)
        assertEquals(listOf(Mood.Calm.option), repo.options.value)
    }
}
