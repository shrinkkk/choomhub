package com.choomhub.app.feature.world

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
    private val places: PlaceFinder,
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

    /** Saves after a short pause, so typing doesn't write on every key. */
    private fun later(key: String, edit: MeetupEdit) {
        pending.remove(key)?.cancel()
        pending[key] = viewModelScope.launch {
            delay(700)
            meetups.edit(id, edit)
        }
    }

    private fun now(edit: MeetupEdit) {
        viewModelScope.launch { meetups.edit(id, edit) }
    }

    fun setDate(date: LocalDate) = now(MeetupEdit(date = date))

    fun setMinutes(minutes: Int?) = if (minutes == null) now(MeetupEdit(clear = setOf("minutes"))) else now(MeetupEdit(minutes = minutes.coerceIn(0, MeetupRules.MAX_MINUTES)))

    fun stopLive() {
        val m = meetup.value ?: return
        viewModelScope.launch { meetups.stopLive(m) }
    }

    fun setPlace(text: String) = later("place", if (text.isBlank()) MeetupEdit(clear = setOf("place", "lat", "lon")) else MeetupEdit(place = text))

    /** Finds the typed place on the map (Delhi), so it gets a pin. */
    fun findPlace(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = "Finding it on the map…"
            val hit = places.search(text)
            _busy.value = null
            if (hit == null) {
                _message.value = "Couldn't find that on the map. Pin it yourself instead."
            } else {
                meetups.edit(id, MeetupEdit(place = text, lat = hit.lat, lon = hit.lon))
            }
        }
    }

    val canFindHere: Boolean get() = places.hasLocationPermission()

    /** Pins it where this phone is now, and names the place. */
    fun useHere() {
        viewModelScope.launch {
            _busy.value = "Finding where you are…"
            val here = places.here()
            _busy.value = null
            if (here == null) {
                _message.value = "Couldn't get your location. Turn on location, or pin it yourself."
            } else {
                val name = here.name ?: meetup.value?.place
                meetups.edit(id, MeetupEdit(place = name, lat = here.lat, lon = here.lon, clear = if (name == null) setOf("place") else emptySet()))
            }
        }
    }

    /** A pin dropped on the map by hand; names it if the place has no name yet. */
    fun pinAt(lat: Double, lon: Double) {
        viewModelScope.launch {
            val name = meetup.value?.place ?: places.nameOf(lat, lon)
            meetups.edit(id, MeetupEdit(lat = lat, lon = lon, place = name))
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
    fun pinned() {
        val m = meetup.value ?: return
        viewModelScope.launch {
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
