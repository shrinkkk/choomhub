package com.choomhub.app.feature.snaps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.choomhub.app.data.snap.SeenSnaps
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapRules
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SnapViewerUiState(
    val snap: SnapInfo? = null,
    val partnerName: String? = null,
    val collections: List<SnapCollection> = emptyList(),
    /** True once the snap has gone (replaced, and nobody kept it). */
    val gone: Boolean = false,
)

/** One snap, full size, with keeping it in Memory Lane collections. */
@HiltViewModel
class SnapViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val snapRepository: SnapRepository,
    profileRepository: ProfileRepository,
    private val seenSnaps: SeenSnaps,
) : ViewModel() {

    val snapId: String = savedStateHandle.toRoute<SnapViewerRoute>().id

    private val loaded = MutableStateFlow(false)

    val state: StateFlow<SnapViewerUiState> = combine(
        snapRepository.observeSnap(snapId),
        snapRepository.observeCollections(),
        profileRepository.observeNames(),
    ) { snap, collections, names ->
        if (snap != null) loaded.value = true
        SnapViewerUiState(snap, names.partnerName, collections, gone = snap == null && loaded.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SnapViewerUiState())

    val image: StateFlow<ByteArray?> = snapRepository.observeImage(snapId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _newCollection = MutableStateFlow("")
    val newCollection: StateFlow<String> = _newCollection

    init {
        viewModelScope.launch {
            val snap = snapRepository.observeSnap(snapId).filterNotNull().first()
            if (!snap.fromMe) seenSnaps.markSeen(snap.id)
        }
    }

    fun toggle(collectionId: String) {
        val snap = state.value.snap ?: return
        val next = if (collectionId in snap.collections) snap.collections - collectionId else snap.collections + collectionId
        viewModelScope.launch { snapRepository.setCollections(snap.id, next.take(SnapRules.MAX_COLLECTIONS_PER_SNAP)) }
    }

    fun onNewCollectionChange(value: String) = _newCollection.update { value.take(SnapRules.MAX_COLLECTION_NAME) }

    /** Makes a collection and keeps this snap in it. */
    fun createAndKeep() {
        val name = SnapRules.cleanCollectionName(_newCollection.value) ?: return
        val snap = state.value.snap ?: return
        _newCollection.value = ""
        viewModelScope.launch {
            val id = snapRepository.createCollection(name) ?: return@launch
            snapRepository.setCollections(snap.id, (snap.collections + id).take(SnapRules.MAX_COLLECTIONS_PER_SNAP))
        }
    }
}
