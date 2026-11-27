package com.choomhub.app.feature.snaps

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.choomhub.app.data.snap.CaptureFiles
import com.choomhub.app.data.snap.SnapImages
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.SnapRules
import com.choomhub.app.domain.model.SnapSendResult
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.SnapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SnapComposeUiState(
    val preview: Bitmap? = null,
    val loading: Boolean = true,
    val caption: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    /** Collections it'll be kept in as it's sent (you count as the one who put it there). */
    val keepIn: List<String> = emptyList(),
    val collections: List<com.choomhub.app.domain.model.SnapCollection> = emptyList(),
    val newCollection: String = "",
)

/** Caption and send a picked or captured photo. */
@HiltViewModel
class SnapComposeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val images: SnapImages,
    private val snapRepository: SnapRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val uri: Uri = savedStateHandle.toRoute<SnapComposeRoute>().uri.toUri()

    private val _state = MutableStateFlow(SnapComposeUiState())
    val state: StateFlow<SnapComposeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            snapRepository.observeCollections().collect { list -> _state.update { it.copy(collections = list, keepIn = it.keepIn.filter { id -> list.any { c -> c.id == id } }) } }
        }
        viewModelScope.launch {
            val preview = images.preview(uri)
            _state.update {
                it.copy(preview = preview, loading = false, error = if (preview == null) "Couldn't open that photo." else null)
            }
        }
    }

    fun onCaptionChange(value: String) = _state.update { it.copy(caption = value.take(SnapRules.MAX_CAPTION), error = null) }

    /** Ticks or unticks a collection to keep it in. */
    fun toggleKeep(collectionId: String) = _state.update {
        it.copy(keepIn = if (collectionId in it.keepIn) it.keepIn - collectionId else (it.keepIn + collectionId).take(SnapRules.MAX_COLLECTIONS_PER_SNAP))
    }

    fun onNewCollectionChange(value: String) = _state.update { it.copy(newCollection = value.take(SnapRules.MAX_COLLECTION_NAME)) }

    /** Makes a collection and ticks it. */
    fun createAndKeep() {
        val name = SnapRules.cleanCollectionName(_state.value.newCollection) ?: return
        _state.update { it.copy(newCollection = "") }
        viewModelScope.launch {
            val id = snapRepository.createCollection(name) ?: return@launch
            _state.update { it.copy(keepIn = (it.keepIn + id).distinct().take(SnapRules.MAX_COLLECTIONS_PER_SNAP)) }
        }
    }

    fun send() {
        val s = _state.value
        if (s.sending || s.preview == null) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val prepared = images.prepare(uri)
            if (prepared == null) {
                _state.update { it.copy(sending = false, error = "Couldn't shrink that photo enough to send.") }
                return@launch
            }
            val caption = SnapRules.cleanCaption(s.caption)
            when (val result = snapRepository.send(prepared, caption, s.keepIn)) {
                SnapSendResult.Sent -> {
                    notifier.notifyPartner(PushEvent.SnapSent, detail = caption)
                    CaptureFiles.clear(context)
                    _state.update { it.copy(sending = false, done = true) }
                }
                is SnapSendResult.NotSent -> _state.update { it.copy(sending = false, error = result.message) }
            }
        }
    }
}
