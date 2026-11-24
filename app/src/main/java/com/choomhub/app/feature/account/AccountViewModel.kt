package com.choomhub.app.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.InviteCodes
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val codeInput: String = "",
)

/**
 * Sign-in and pairing actions. Success needs no explicit navigation: the
 * app gate observes the space and moves on by itself.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    fun signIn(activityContext: Context) = run { spaceRepository.signInWithGoogle(activityContext) }

    fun startSpace() = run { spaceRepository.startSpace() }

    fun onCodeChange(value: String) {
        _state.update { it.copy(codeInput = InviteCodes.normalize(value).take(InviteCodes.LENGTH), error = null) }
    }

    fun join() = run {
        spaceRepository.joinSpace(_state.value.codeInput).onSuccess { notifier.notifyPartner(PushEvent.PartnerJoined) }
    }

    fun signOut() {
        viewModelScope.launch {
            notifier.forgetThisPhone()
            spaceRepository.signOut()
        }
    }

    private fun run(action: suspend () -> Result<Unit>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = action()
            _state.update { it.copy(busy = false, error = result.exceptionOrNull()?.message) }
        }
    }
}
