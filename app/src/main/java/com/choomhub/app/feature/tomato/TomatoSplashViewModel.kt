package com.choomhub.app.feature.tomato

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.data.home.TomatoesSeen
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Tomatoes they threw that this phone hasn't shown yet: how many, from whom, the newest. */
data class TomatoVolley(val count: Int, val from: String, val newest: Instant)

object TomatoRules {
    /** At most this many land on screen, however many piled up. */
    const val MAX_SHOWN = 14

    /**
     * The tomatoes from them newer than [seen]. When this phone has never looked ([seen] null),
     * nothing: old tomatoes from before don't all land at once on the first open.
     */
    fun unseen(gestures: List<Gesture>, seen: Instant?): List<Gesture> {
        if (seen == null) return emptyList()
        return gestures.filter { !it.fromMe && it.kind == AffectionKind.TomatoDelivery && it.sentAt != null && it.sentAt > seen }
    }
}

@HiltViewModel
class TomatoSplashViewModel @Inject constructor(
    private val gestures: GestureRepository,
    private val seen: TomatoesSeen,
    profile: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {

    val volley: StateFlow<TomatoVolley?> = combine(gestures.observeRecent(), seen.observe(), profile.observeNames()) { list, seenAt, names ->
        if (seenAt == null) {
            // First look on this phone: count from now (the list may not have loaded yet, so
            // "the newest one in it" isn't safe).
            viewModelScope.launch { seen.markSeen(clock.instant()) }
            return@combine null
        }
        val fresh = TomatoRules.unseen(list, seenAt)
        if (fresh.isEmpty()) null else TomatoVolley(fresh.size, names.partnerName ?: "They", fresh.maxOf { it.sentAt!! })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Called as the splash starts, so it shows once. */
    fun shown(volley: TomatoVolley) {
        viewModelScope.launch { seen.markSeen(volley.newest) }
    }
}
