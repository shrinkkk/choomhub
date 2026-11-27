package com.choomhub.app.feature.world

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.SnapInfo
import com.choomhub.app.domain.model.SnapRules
import com.choomhub.app.domain.repository.SnapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A collection with its snaps (newest first) and the one shown on top of its stack. */
data class CollectionUi(
    val id: String,
    val name: String,
    val snaps: List<SnapInfo>,
    val cover: SnapInfo?,
    /** A delete waiting for a yes: true if you asked, false if they did, null if nobody. */
    val deleteAskedByMe: Boolean? = null,
)

/** What deleting a collection would do, for its confirmation. */
data class CollectionDeletion(val goneForGood: Int, val stayElsewhere: Int, val stayCurrent: Int)

/** Memory Lane: every collection with the snaps kept in it, and what you can do to them. */
@HiltViewModel
class MemoryLaneViewModel @Inject constructor(
    private val snapRepository: SnapRepository,
    private val savedState: SavedStateHandle,
    private val notifier: com.choomhub.app.domain.repository.Notifier,
    profile: com.choomhub.app.domain.repository.ProfileRepository,
) : ViewModel() {

    /** Their name, for "only Jas can take it out". */
    val partnerName: StateFlow<String> = profile.observeNames().map { it.partnerName ?: "they" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "they")

    val collections: StateFlow<List<CollectionUi>?> =
        combine(snapRepository.observeCollections(), snapRepository.observeKept()) { collections, kept ->
            collections.map { c ->
                val snaps = kept.filter { c.id in it.collections }
                CollectionUi(c.id, c.name, snaps, SnapRules.coverOf(c, snaps), c.deleteAskedByMe)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val recent: StateFlow<List<SnapInfo>> = snapRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Open collections (kept across rotation). */
    val expanded: StateFlow<Set<String>> = savedState.getStateFlow<List<String>>(EXPANDED, emptyList())
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setExpanded(collectionId: String, open: Boolean) {
        val now = savedState.get<List<String>>(EXPANDED).orEmpty().toSet()
        savedState[EXPANDED] = (if (open) now + collectionId else now - collectionId).toList()
    }

    fun image(snapId: String): Flow<ByteArray?> = snapRepository.observeImage(snapId)

    fun create(name: String) {
        val clean = SnapRules.cleanCollectionName(name) ?: return
        viewModelScope.launch { snapRepository.createCollection(clean) }
    }

    fun rename(collectionId: String, name: String) {
        val clean = SnapRules.cleanCollectionName(name) ?: return
        viewModelScope.launch { snapRepository.renameCollection(collectionId, clean) }
    }

    fun setCover(collectionId: String, snapId: String) {
        viewModelScope.launch { snapRepository.setCover(collectionId, snapId) }
    }

    /** Whether taking [snap] out of [collectionId] deletes it for good. */
    fun deletesForGood(snap: SnapInfo, collectionId: String): Boolean = SnapRules.deletesForGood(snap, collectionId, recent.value)

    fun isCurrent(snap: SnapInfo): Boolean = SnapRules.isCurrent(snap, recent.value)

    fun remove(snap: SnapInfo, collectionId: String) {
        viewModelScope.launch { snapRepository.removeFromCollection(snap, collectionId) }
    }

    fun deletion(collection: CollectionUi): CollectionDeletion {
        val elsewhere = collection.snaps.count { (it.collections - collection.id).isNotEmpty() }
        val forGood = collection.snaps.count { SnapRules.deletesForGood(it, collection.id, recent.value) }
        return CollectionDeletion(forGood, elsewhere, collection.snaps.size - elsewhere - forGood)
    }

    /** Asks them to agree to deleting it. */
    fun askToDelete(collection: CollectionUi) {
        viewModelScope.launch {
            snapRepository.askToDeleteCollection(collection.id)
            notifier.notifyPartner(com.choomhub.app.domain.model.PushEvent.Collection, detail = collection.name, kind = "delete_ask")
        }
    }

    fun takeBackDelete(collection: CollectionUi) {
        viewModelScope.launch { snapRepository.takeBackDeleteCollection(collection.id) }
    }

    /** Answers their ask: agreeing deletes it now. */
    fun answerDelete(collection: CollectionUi, agree: Boolean) {
        if (agree) setExpanded(collection.id, false)
        viewModelScope.launch {
            snapRepository.answerDeleteCollection(collection.id, agree)
            notifier.notifyPartner(com.choomhub.app.domain.model.PushEvent.Collection, detail = collection.name, kind = if (agree) "delete_agreed" else "delete_declined")
        }
    }

    private companion object {
        const val EXPANDED = "expanded"
    }
}
