package com.choomhub.app.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodRules
import com.choomhub.app.domain.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoodOptionsUiState(
    val options: List<MoodOption> = emptyList(),
    val input: String = "",
    val error: String? = null,
)

/** Editing the space's mood list: add your own, remove any, or go back to the defaults. */
@HiltViewModel
class MoodOptionsViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MoodOptionsUiState())
    val state: StateFlow<MoodOptionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            moodRepository.observeOptions().collect { options -> _state.update { it.copy(options = options) } }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value.take(MoodRules.MAX_LABEL + 8), error = null) }

    fun add() {
        val s = _state.value
        val label = MoodRules.cleanLabel(s.input) ?: return
        val error = when {
            s.input.trim().length > MoodRules.MAX_LABEL -> "Keep it to ${MoodRules.MAX_LABEL} characters."
            s.options.any { it.label.equals(label, ignoreCase = true) } -> "That one's already in the list."
            s.options.size >= MoodRules.MAX_OPTIONS -> "That's the most the list can hold (${MoodRules.MAX_OPTIONS})."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        save(s.options + MoodOption(MoodRules.newKey(), label))
        _state.update { it.copy(input = "") }
    }

    fun remove(key: String) {
        val options = _state.value.options
        if (options.size <= 1) {
            _state.update { it.copy(error = "Keep at least one.") }
            return
        }
        save(options.filterNot { it.key == key })
    }

    fun restoreDefaults() = save(Mood.defaults)

    private fun save(options: List<MoodOption>) {
        _state.update { it.copy(options = options, error = null) }
        viewModelScope.launch { moodRepository.setOptions(options) }
    }
}
