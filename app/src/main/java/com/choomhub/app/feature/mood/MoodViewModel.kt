package com.choomhub.app.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.Mood
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
    val selected: Mood? = null,
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

    private var current: Pair<Mood, String?>? = null

    init {
        viewModelScope.launch {
            val mine = moodRepository.observeMoods().first().mine ?: return@launch
            current = mine.mood to mine.note
            _state.update { s -> if (s.selected == null && s.note.isEmpty()) s.copy(selected = mine.mood, note = mine.note.orEmpty()) else s }
        }
    }

    fun select(mood: Mood) = _state.update { it.copy(selected = mood, error = null) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(MoodRules.MAX_NOTE), error = null) }

    fun save() {
        val s = _state.value
        val mood = s.selected ?: run {
            _state.update { it.copy(error = "Pick one first.") }
            return
        }
        val note = MoodRules.cleanNote(s.note)
        if (current == mood to note) {
            _state.update { it.copy(done = true) }
            return
        }
        current = mood to note
        viewModelScope.launch {
            moodRepository.setMood(mood, note)
            notifier.notifyPartner(PushEvent.MoodChanged, detail = note, kind = mood.key)
            _state.update { it.copy(done = true) }
        }
    }
}
