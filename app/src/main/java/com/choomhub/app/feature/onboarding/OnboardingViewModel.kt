package com.choomhub.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.NameRules
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
)

/**
 * First launch: the person names their partner. Once saved, the app-level
 * gate ([com.choomhub.app.ui.AppViewModel]) moves on to the main UI by itself.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onInputChange(value: String) {
        // Allow a little overflow so the "too long" message can show instead of silently truncating.
        _state.update { it.copy(input = value.take(NameRules.MAX_LENGTH + 8), error = null) }
    }

    fun onContinue() {
        if (_state.value.isSaving) return
        when (val result = NameRules.validate(_state.value.input)) {
            is NameRules.Result.Valid -> {
                _state.update { it.copy(isSaving = true, error = null) }
                viewModelScope.launch { profileRepository.setPartnerName(result.name) }
            }
            else -> _state.update { it.copy(error = NameRules.messageFor(result)) }
        }
    }
}
