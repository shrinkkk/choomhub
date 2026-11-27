package com.choomhub.app.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.MoodRules
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoodUiState(
    /** The space's mood list (plus your current mood if it has since been removed from the list). */
    val options: List<MoodOption> = emptyList(),
    val selected: MoodOption? = null,
    val note: String = "",
    val error: String? = null,
    /** Set once saved (or nothing changed); the screen closes. */
    val done: Boolean = false,
)

/** Picking your mood and an optional line about it. */
@HiltViewModel
class MoodViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val _state = MutableStateFlow(MoodUiState())
    val state: StateFlow<MoodUiState> = _state.asStateFlow()

    private var current: Pair<MoodOption, String?>? = null

    init {
        viewModelScope.launch {
            val mine = moodRepository.observeMoods().first().mine
            if (mine != null) {
                current = mine.mood to mine.note
                // The line about it always starts empty: a new mood gets a new line.
                _state.update { s -> if (s.selected == null) s.copy(selected = mine.mood) else s }
            }
            moodRepository.observeOptions().collect { options ->
                _state.update { s ->
                    val selected = s.selected
                    val shown = if (selected != null && options.none { it.key == selected.key }) listOf(selected) + options else options
                    s.copy(options = shown)
                }
            }
        }
    }

    fun select(mood: MoodOption) = _state.update { it.copy(selected = mood, error = null) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(MoodRules.MAX_NOTE), error = null) }

    fun save() {
        val s = _state.value
        val mood = s.selected ?: run {
            _state.update { it.copy(error = "Pick one first.") }
            return
        }
        val note = MoodRules.cleanNote(s.note)
        if (current?.first?.key == mood.key && current?.second == note) {
            _state.update { it.copy(done = true) }
            return
        }
        val changedMood = current?.first?.key != mood.key
        current = mood to note
        viewModelScope.launch {
            moodRepository.setMood(mood, note)
            // Only clearing the line on the same mood isn't worth a notification.
            if (changedMood || note != null) notifier.notifyPartner(PushEvent.MoodChanged, detail = note, kind = mood.label)
            _state.update { it.copy(done = true) }
        }
    }
}
