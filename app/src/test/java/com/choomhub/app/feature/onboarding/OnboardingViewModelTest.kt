package com.choomhub.app.feature.onboarding

import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.testing.FakeProfileRepository
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
class OnboardingViewModelTest {

    private val repository = FakeProfileRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank name shows an error and saves nothing`() {
        val viewModel = OnboardingViewModel(repository)
        viewModel.onInputChange("   ")
        viewModel.onContinue()

        assertEquals("Give them a name first.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSaving)
        assertNull(repository.names.value.partnerName)
    }

    @Test
    fun `typing clears the error`() {
        val viewModel = OnboardingViewModel(repository)
        viewModel.onContinue()
        viewModel.onInputChange("W")
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `valid name is cleaned and saved`() {
        val viewModel = OnboardingViewModel(repository)
        viewModel.onInputChange("  Wren   the  Bold ")
        viewModel.onContinue()
        assertEquals("Pick which one you are.", viewModel.state.value.error)

        viewModel.onRoleSelected(PointsRole.GoodBoy)
        viewModel.onContinue()
        assertTrue(viewModel.state.value.isSaving)
        assertEquals("Wren the Bold", repository.names.value.partnerName)
        assertEquals(PointsRole.GoodBoy, repository.names.value.myRole)
    }

    @Test
    fun `when the partner already picked, your role is decided`() {
        repository.names.value = repository.names.value.copy(myRole = PointsRole.GoodGirl, roleChosenByPartner = true)
        val viewModel = OnboardingViewModel(repository)
        assertEquals(PointsRole.GoodGirl, viewModel.state.value.lockedRole)
        viewModel.onInputChange("Rook")
        viewModel.onContinue()
        assertEquals("Rook", repository.names.value.partnerName)
    }
}
