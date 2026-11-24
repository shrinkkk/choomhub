package com.choomhub.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which top-level experience to show. */
enum class AppGate { Loading, Onboarding, Main }

/** Decides between first-run onboarding and the main app. */
@HiltViewModel
class AppViewModel @Inject constructor(
    profileRepository: ProfileRepository,
) : ViewModel() {

    val gate: StateFlow<AppGate> = profileRepository.observeNames()
        .map { if (it.needsOnboarding) AppGate.Onboarding else AppGate.Main }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppGate.Loading)
}
