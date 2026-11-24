package com.choomhub.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.GestureResult
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.HomeRepository
import com.choomhub.app.domain.repository.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository,
    private val gestureRepository: GestureRepository,
    private val notifier: Notifier,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = homeRepository.observeHome()
        .map { it.toUiState(clock) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    private val _pendingNotice = MutableStateFlow<String?>(null)

    /** A short message for the snackbar (gesture sent, or why it wasn't). */
    val pendingNotice: StateFlow<String?> = _pendingNotice.asStateFlow()

    private val lastSent = mutableMapOf<AffectionKind, Instant>()

    /** Sends the gesture and lets them know. Repeat taps within a few seconds are ignored. */
    fun onAffectionTapped(kind: AffectionKind) {
        val now = clock.instant()
        val previous = lastSent[kind]
        if (previous != null && Duration.between(previous, now) < COOLDOWN) return
        lastSent[kind] = now
        viewModelScope.launch {
            _pendingNotice.value = when (val result = gestureRepository.send(kind)) {
                GestureResult.Sent -> {
                    notifier.notifyPartner(PushEvent.Gesture, kind = kind.key)
                    sentText(kind)
                }
                is GestureResult.NotSent -> {
                    lastSent.remove(kind)
                    result.message
                }
            }
        }
    }

    fun onNoticeShown() {
        _pendingNotice.value = null
    }

    companion object {
        val COOLDOWN: Duration = Duration.ofSeconds(3)

        fun sentText(kind: AffectionKind): String = when (kind) {
            AffectionKind.Hug -> "Hug sent."
            AffectionKind.ForeheadKiss -> "Forehead kiss sent."
            AffectionKind.HeadPats -> "Head pats sent."
            AffectionKind.TomatoDelivery -> "Tomato on its way."
        }
    }
}
