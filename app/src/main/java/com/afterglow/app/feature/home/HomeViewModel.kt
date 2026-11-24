package com.afterglow.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglow.app.domain.model.AffectionKind
import com.afterglow.app.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = homeRepository.observeHome()
        .map { it.toUiState(clock) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    private val _pendingNotice = MutableStateFlow<AffectionKind?>(null)

    /**
     * Affection gesture the user just tapped, waiting to be acknowledged in the
     * UI. Gestures are not sent anywhere yet; the UI tells the user so.
     */
    val pendingNotice: StateFlow<AffectionKind?> = _pendingNotice.asStateFlow()

    fun onAffectionTapped(kind: AffectionKind) {
        _pendingNotice.value = kind
    }

    fun onNoticeShown() {
        _pendingNotice.value = null
    }
}
