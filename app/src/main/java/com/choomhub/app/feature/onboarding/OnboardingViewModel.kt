package com.choomhub.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.NameRules
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val input: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    /** Your pick, when you get to choose. */
    val selectedRole: PointsRole? = null,
    /** Set when your partner already chose, so your role is decided. */
    val lockedRole: PointsRole? = null,
)

/**
 * Setup: name your partner and say whether you're the Good Boy or the Good
 * Girl (who receives which points). If your partner already picked, your
 * role is shown instead of asked. Once both are saved, the app gate moves on.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** The name already saved for your partner, to tell whether it changed. */
    private var savedName: String? = null

    init {
        viewModelScope.launch {
            profileRepository.observeNames().collect { names ->
                savedName = names.partnerName
                _state.update { s ->
                    s.copy(
                        input = if (s.input.isEmpty()) names.partnerName.orEmpty() else s.input,
                        lockedRole = if (names.roleChosenByPartner) names.myRole else null,
                        selectedRole = s.selectedRole ?: names.myRole.takeIf { !names.roleChosenByPartner },
                    )
                }
            }
        }
    }

    fun onInputChange(value: String) {
        // Allow a little overflow so the "too long" message can show instead of silently truncating.
        _state.update { it.copy(input = value.take(NameRules.MAX_LENGTH + 8), error = null) }
    }

    fun onRoleSelected(role: PointsRole) {
        _state.update { it.copy(selectedRole = role, error = null) }
    }

    fun onContinue() {
        val s = _state.value
        if (s.isSaving) return
        val name = NameRules.validate(s.input)
        if (name !is NameRules.Result.Valid) {
            _state.update { it.copy(error = NameRules.messageFor(name)) }
            return
        }
        val role = s.lockedRole ?: s.selectedRole
        if (role == null) {
            _state.update { it.copy(error = "Pick which one you are.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            if (name.name != savedName) {
                profileRepository.setPartnerName(name.name)
                notifier.notifyPartner(PushEvent.Renamed)
            }
            val claimed = s.lockedRole != null || profileRepository.setMyRole(role)
            if (!claimed) {
                _state.update {
                    it.copy(isSaving = false, error = "They already picked ${role.label}, so you're the ${role.other.label}.")
                }
            }
        }
    }
}
