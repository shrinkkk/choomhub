package com.choomhub.app.feature.world

import kotlinx.coroutines.flow.combine
import com.choomhub.app.data.place.FoundPlace
import com.choomhub.app.domain.model.MeetupPlace
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.data.place.PlaceFinder
import com.choomhub.app.data.snap.SnapImages
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.MeetupActivity
import com.choomhub.app.domain.model.MeetupEdit
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.ShelfSong
import com.choomhub.app.domain.repository.MeetupLogRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Everything in Our World besides Memory Lane: the cabinet, the sky, the shelf and the map. */
@HiltViewModel
class WorldViewModel @Inject constructor(
    private val meetups: MeetupLogRepository,
    songs: SongRepository,
    profile: ProfileRepository,
    ticker: SecondTicker,
    private val notifier: Notifier,
    private val clock: Clock,
) : ViewModel() {
    val logs: StateFlow<List<MeetupLog>?> = meetups.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val shelf: StateFlow<List<ShelfSong>?> = songs.observeShelf().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val names: StateFlow<CoupleNames> = profile.observeNames().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoupleNames(null, null))

    /** For the live meetup's clock. */
    val now: StateFlow<Instant> = ticker.ticks().onStart { emit(clock.instant()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())

    fun today(): LocalDate = LocalDate.now(clock)

    /** Starts logging one that's happening now; [then] gets its id. */
    fun startLive(then: (String) -> Unit) {
        viewModelScope.launch {
            val id = meetups.startLive() ?: return@launch
            notifier.notifyPartner(PushEvent.MeetupLog, kind = "started")
            then(id)
        }
    }

    fun logPast(date: LocalDate, then: (String) -> Unit) {
        viewModelScope.launch { meetups.logPast(date)?.let(then) }
    }

    fun photo(meetupId: String, photoId: String): Flow<ByteArray?> = meetups.observePhoto(meetupId, photoId)
}

@Serializable
data class MeetupRoute(val id: String)

/** One meetup, being logged or edited. Text fields save themselves a moment after you stop typing. */
@HiltViewModel
class MeetupViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val meetups: MeetupLogRepository,
    private val images: SnapImages,
    private val placesFinder: PlaceFinder,
    private val notifier: Notifier,
    ticker: SecondTicker,
    private val clock: Clock,
) : ViewModel() {
    val id: String = savedState.toRoute<MeetupRoute>().id

    val meetup: StateFlow<MeetupLog?> = meetups.observe(id).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val now: StateFlow<Instant> = ticker.ticks().onStart { emit(clock.instant()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())

    private val _busy = MutableStateFlow<String?>(null)
    /** What's happening right now ("Adding photos…", "Finding you…"), or null. */
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() = _message.update { null }

    fun photo(photoId: String): Flow<ByteArray?> = meetups.observePhoto(id, photoId)

    private val pending = HashMap<String, Job>()
    private val unsaved = HashMap<String, MeetupEdit>()

    /** Saves after a short pause, so typing doesn't write on every key. */
    private fun later(key: String, edit: MeetupEdit, then: () -> Unit = {}) {
        pending.remove(key)?.cancel()
        unsaved[key] = edit
        pending[key] = viewModelScope.launch {
            delay(700)
            unsaved.remove(key)
            meetups.edit(id, edit)
            then()
        }
    }

    override fun onCleared() {
        // Left the screen mid-typing: what was typed still gets saved.
        val left = unsaved.values.toList()
        unsaved.clear()
        if (left.isNotEmpty()) Flush.scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { left.forEach { meetups.edit(id, it) } }
    }

    private fun now(edit: MeetupEdit) {
        viewModelScope.launch { meetups.edit(id, edit) }
    }

    fun setDate(date: LocalDate) = now(MeetupEdit(date = date))

    fun setMinutes(minutes: Int?) = if (minutes == null) now(MeetupEdit(clear = setOf("minutes"))) else now(MeetupEdit(minutes = minutes.coerceIn(0, MeetupRules.MAX_MINUTES)))

    /** Hours and minutes as they're typed: saved after a pause, no button needed. */
    fun typeMinutes(minutes: Int?) = later("minutes", if (minutes == null) MeetupEdit(clear = setOf("minutes")) else MeetupEdit(minutes = minutes.coerceIn(0, MeetupRules.MAX_MINUTES)))

    fun stopLive() {
        val m = meetup.value ?: return
        viewModelScope.launch { meetups.stopLive(m) }
    }

    // --- Places: a working list, so quick edits to two of them don't undo each other ---

    private val draft = MutableStateFlow<List<MeetupPlace>?>(null)

    /** The places as the screen shows them: what's saved, plus edits still on their way. */
    val places: StateFlow<List<MeetupPlace>> = combine(meetup, draft) { m, d -> d ?: m?.places.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun changePlaces(typing: Boolean = false, block: (MutableList<MeetupPlace>) -> Unit) {
        val next = places.value.toMutableList().also(block)
        draft.value = next
        val edit = MeetupEdit(places = next)
        if (typing) {
            later("places", edit) { if (draft.value == next && next.none { it.isBlank }) draft.value = null }
        } else {
            pending.remove("places")?.cancel()
            unsaved.remove("places")
            viewModelScope.launch {
                meetups.edit(id, edit)
                if (draft.value == next && next.none { it.isBlank }) draft.value = null
            }
        }
    }

    /** A new, empty place at the end (kept on screen; saved once it has a name or a pin). */
    fun addPlace() {
        if (places.value.size >= MeetupRules.MAX_PLACES) return
        draft.value = places.value + MeetupPlace("")
    }

    fun removePlace(index: Int) = changePlaces { if (index in it.indices) it.removeAt(index) }

    /** Room for [index] (the screen always shows at least one place, even before it's saved). */
    private fun MutableList<MeetupPlace>.reach(index: Int): Boolean {
        if (index < 0 || index >= MeetupRules.MAX_PLACES) return false
        while (size <= index) add(MeetupPlace(""))
        return true
    }

    /** Typing a name: the pin it had stays until it's found again or moved. */
    fun setPlaceName(index: Int, text: String) = changePlaces(typing = true) { if (it.reach(index)) it[index] = it[index].copy(name = text.take(MeetupRules.MAX_PLACE)) }

    private fun pin(index: Int, lat: Double, lon: Double, name: String? = null) = changePlaces {
        if (it.reach(index)) it[index] = it[index].copy(lat = lat, lon = lon, name = it[index].name.ifBlank { name.orEmpty() })
    }

    /** Several spots share the typed name: which one it was. */
    data class PlaceChoices(val index: Int, val typed: String, val options: List<FoundPlace>)

    private val _choices = MutableStateFlow<PlaceChoices?>(null)
    val choices: StateFlow<PlaceChoices?> = _choices.asStateFlow()

    /** Finds the typed place on the map (Delhi) and asks which of the matches it was. */
    fun findPlace(index: Int) {
        val text = places.value.getOrNull(index)?.name?.trim().orEmpty()
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = "Finding it on the map…"
            val hits = placesFinder.searchAll(text)
            _busy.value = null
            // Always shown to check, even one match: a name can belong to more than one spot
            // (Sheesh Mahal in Shalimar Bagh, Sheesh Mahal near Jama Masjid), and the phone's
            // first guess isn't always yours.
            if (hits.isEmpty()) _message.value = "Couldn't find that on the map. Add the area (\"$text, Shalimar Bagh\") or pin it yourself."
            else _choices.value = PlaceChoices(index, text, hits)
        }
    }

    fun choose(option: FoundPlace?) {
        val c = _choices.value ?: return
        _choices.value = null
        if (option != null) pin(c.index, option.lat, option.lon)
    }

    val canFindHere: Boolean get() = placesFinder.hasLocationPermission()

    /** Pins place [index] where this phone is now, and names it if it has no name. */
    fun useHere(index: Int) {
        viewModelScope.launch {
            _busy.value = "Finding where you are…"
            val here = placesFinder.here()
            _busy.value = null
            if (here == null) {
                _message.value = "Couldn't get your location. Turn on location, or pin it yourself."
            } else {
                pin(index, here.lat, here.lon, here.name)
            }
        }
    }

    /** A pin dropped on the map by hand; names it if it has no name yet. */
    fun pinAt(index: Int, lat: Double, lon: Double) {
        viewModelScope.launch {
            val name = if (places.value.getOrNull(index)?.name.isNullOrBlank()) placesFinder.nameOf(lat, lon) else null
            pin(index, lat, lon, name)
        }
    }

    fun toggle(activity: MeetupActivity) {
        val did = meetup.value?.did ?: return
        now(MeetupEdit(did = if (activity in did) did - activity else did + activity))
    }

    fun setFirst(text: String) = later("first", if (text.isBlank()) MeetupEdit(clear = setOf("first")) else MeetupEdit(first = text))

    fun setNote(text: String) = later("note", if (text.isBlank()) MeetupEdit(clear = setOf("note")) else MeetupEdit(note = text))

    /** Adds photos (camera or gallery), up to twelve in all, each made small (1080 px) first. */
    fun addPhotos(uris: List<Uri>) {
        val m = meetup.value ?: return
        val room = MeetupRules.MAX_PHOTOS - m.photos.size
        if (room <= 0) {
            _message.value = "That's twelve already. Take one out to add another."
            return
        }
        if (uris.size > room) _message.value = "Only $room more fit, so the first $room were added."
        viewModelScope.launch {
            _busy.value = "Adding photos…"
            uris.take(room).forEach { uri ->
                val prepared = images.prepare(uri)
                val current = meetup.value ?: return@forEach
                if (prepared != null) meetups.addPhoto(current, prepared)
            }
            _busy.value = null
        }
    }

    fun removePhoto(photoId: String) {
        val m = meetup.value ?: return
        viewModelScope.launch { meetups.removePhoto(m, photoId) }
    }

    /** Moves a photo earlier (negative) or later; to the very front makes it the thumbnail. */
    fun movePhoto(photoId: String, by: Int) {
        val m = meetup.value ?: return
        val order = MeetupRules.move(m.photoIds, photoId, by)
        if (order != m.photoIds) viewModelScope.launch { meetups.reorder(m, order) }
    }

    fun makeFirst(photoId: String) = movePhoto(photoId, -MeetupRules.MAX_PHOTOS)

    /** Done for now: tells them it's pinned (once). */
    /** Writes whatever was typed and is still waiting for its pause, right now. */
    private suspend fun saveNow() {
        val left = unsaved.values.toList()
        unsaved.keys.toList().forEach { pending.remove(it)?.cancel() }
        unsaved.clear()
        left.forEach { meetups.edit(id, it) }
    }

    fun pinned() {
        // Pinning takes the time (and anything else) as typed, no need to wait or press anything.
        // It outlives the screen, which closes straight after.
        Flush.scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            saveNow()
            val m = meetup.value ?: return@launch
            notifier.notifyPartner(PushEvent.MeetupLog, detail = listOfNotNull(m.place, m.minutes?.let(MeetupRules::duration)).joinToString(" · ").ifEmpty { null }, kind = "logged")
        }
    }

    fun delete(then: () -> Unit) {
        val m = meetup.value ?: return
        viewModelScope.launch {
            meetups.delete(m)
            then()
        }
    }
}

/** Outlives a screen, for saving what was typed just before leaving it. */
private object Flush {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
}
