package com.choomhub.app.feature.world

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.repository.SnapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CollectionUi(val id: String, val name: String, val snaps: List<SnapInfo>)

/** Memory Lane: every collection with the snaps kept in it. */
@HiltViewModel
class MemoryLaneViewModel @Inject constructor(snapRepository: SnapRepository) : ViewModel() {
    val collections: StateFlow<List<CollectionUi>?> =
        combine(snapRepository.observeCollections(), snapRepository.observeKept()) { collections, kept ->
            collections.map { c -> CollectionUi(c.id, c.name, kept.filter { c.id in it.collections }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** One collection's snaps. */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    snapRepository: SnapRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<CollectionRoute>().id

    val collection: StateFlow<CollectionUi?> =
        combine(snapRepository.observeCollections(), snapRepository.observeKept()) { collections, kept ->
            collections.firstOrNull { it.id == id }?.let { c -> CollectionUi(c.id, c.name, kept.filter { id in it.collections }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
