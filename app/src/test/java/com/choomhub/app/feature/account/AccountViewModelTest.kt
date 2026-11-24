package com.choomhub.app.feature.account

import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.testing.FakeSpaceRepository
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
class AccountViewModelTest {
    private val repo = FakeSpaceRepository(SpaceState.SignedOut)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `code input is normalised and capped`() {
        val vm = AccountViewModel(repo)
        vm.onCodeChange("abcd-2345-extra")
        assertEquals("ABCD2345", vm.state.value.codeInput)
    }

    @Test
    fun `starting a space moves to waiting`() {
        val vm = AccountViewModel(repo)
        vm.startSpace()
        assertTrue(repo.space.value is SpaceState.Waiting)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun `join errors are shown and cleared on typing`() {
        repo.joinError = "That code has already been used."
        val vm = AccountViewModel(repo)
        vm.onCodeChange("ABCD2345")
        vm.join()
        assertEquals("That code has already been used.", vm.state.value.error)
        vm.onCodeChange("ABCD2346")
        assertNull(vm.state.value.error)
    }
}
