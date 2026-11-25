package com.choomhub.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.NameRules
import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.NotificationPrefs
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.TestPushResult
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The test-notification button's state. */
sealed interface TestSendState {
    data object Idle : TestSendState
    data object Sending : TestSendState
    data class Done(val ok: Boolean, val message: String) : TestSendState
}

data class NotificationsUi(
    val availability: PushAvailability = PushAvailability.DeviceOnly,
    val prefs: NotificationPrefs = NotificationPrefs(),
    val test: TestSendState = TestSendState.Idle,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val spaceRepository: SpaceRepository,
    private val notificationRepository: NotificationRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val testState = MutableStateFlow<TestSendState>(TestSendState.Idle)

    val notifications: StateFlow<NotificationsUi> =
        combine(notificationRepository.observePrefs(), testState) { prefs, test -> NotificationsUi(notifier.availability, prefs, test) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUi(notifier.availability))

    fun setNotificationsEnabled(category: NotificationCategory, enabled: Boolean) {
        viewModelScope.launch { notificationRepository.setEnabled(category, enabled) }
    }

    fun sendTestNotification() {
        if (testState.value == TestSendState.Sending) return
        testState.value = TestSendState.Sending
        viewModelScope.launch {
            testState.value = when (val result = notifier.sendTest()) {
                TestPushResult.Sent -> TestSendState.Done(ok = true, message = "Sent. It should show up in a few seconds.")
                is TestPushResult.NotSent -> TestSendState.Done(ok = false, message = result.message)
            }
        }
    }

    val space: StateFlow<SpaceState?> = spaceRepository.observeSpace()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun newInviteCode() {
        viewModelScope.launch { spaceRepository.refreshInvite() }
    }

    fun signOut() {
        viewModelScope.launch {
            notifier.forgetThisPhone()
            spaceRepository.signOut()
        }
    }

    val names: StateFlow<CoupleNames> = profileRepository.observeNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoupleNames(partnerName = null, myName = null))

    /**
     * Renames your partner. Returns an error message to show, or null if the
     * name was accepted and is being saved.
     */
    fun renamePartner(input: String): String? {
        val result = NameRules.validate(input)
        if (result !is NameRules.Result.Valid) return NameRules.messageFor(result)
        if (result.name == names.value.partnerName) return null
        viewModelScope.launch {
            profileRepository.setPartnerName(result.name)
            notifier.notifyPartner(PushEvent.Renamed)
        }
        return null
    }
}
