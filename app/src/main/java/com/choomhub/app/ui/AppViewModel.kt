package com.choomhub.app.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which top-level experience to show. */
enum class AppGate { Loading, SignIn, Pairing, Onboarding, Main }

/** Decides between sign-in, pairing, first-run naming and the main app. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    profileRepository: ProfileRepository,
    notifier: Notifier,
    private val store: DataStore<Preferences>,
) : ViewModel() {

    val gate: StateFlow<AppGate> = combine(spaceRepository.observeSpace(), profileRepository.observeNames(), ::gateFor)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppGate.Loading)

    init {
        // When your partner leaves, their code goes with them: make a fresh one for them to come
        // back with (once per space while this app is open; Settings has "New code" too).
        val tried = mutableSetOf<String>()
        viewModelScope.launch {
            spaceRepository.observeSpace().collect { space ->
                if (space is SpaceState.Waiting && space.inviteCode.isBlank() && tried.add(space.coupleId)) {
                    spaceRepository.refreshInvite()
                }
            }
        }
    }

    /**
     * True once, on reaching the main app, if this build can send pushes and
     * we haven't asked for the notification permission yet. Settings has the
     * toggle for later.
     */
    val shouldAskForNotifications: StateFlow<Boolean> =
        (if (notifier.availability == PushAvailability.DeviceOnly) flowOf(false) else store.data.map { it[ASKED_NOTIFICATIONS] != true })
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onNotificationsAsked() {
        viewModelScope.launch { store.edit { it[ASKED_NOTIFICATIONS] = true } }
    }

    companion object {
        private val ASKED_NOTIFICATIONS = booleanPreferencesKey("asked_notification_permission")

        fun gateFor(space: SpaceState, names: CoupleNames): AppGate = when (space) {
            SpaceState.SignedOut -> AppGate.SignIn
            is SpaceState.NoSpace -> AppGate.Pairing
            SpaceState.DeviceOnly, is SpaceState.Waiting, is SpaceState.Paired ->
                if (names.needsOnboarding) AppGate.Onboarding else AppGate.Main
        }
    }
}
