package com.choomhub.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which top-level experience to show. */
enum class AppGate { Loading, SignIn, Pairing, Onboarding, Main }

/** Decides between sign-in, pairing, first-run naming and the main app. */
@HiltViewModel
class AppViewModel @Inject constructor(
    spaceRepository: SpaceRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val gate: StateFlow<AppGate> = combine(spaceRepository.observeSpace(), profileRepository.observeNames(), ::gateFor)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppGate.Loading)

    companion object {
        fun gateFor(space: SpaceState, names: CoupleNames): AppGate = when (space) {
            SpaceState.SignedOut -> AppGate.SignIn
            is SpaceState.NoSpace -> AppGate.Pairing
            SpaceState.DeviceOnly, is SpaceState.Waiting, is SpaceState.Paired ->
                if (names.needsOnboarding) AppGate.Onboarding else AppGate.Main
        }
    }
}
