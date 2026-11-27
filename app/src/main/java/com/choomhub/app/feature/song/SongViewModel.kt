package com.choomhub.app.feature.song

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.data.song.HeardSongs
import com.choomhub.app.data.song.SongInbox
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.SharedSong
import com.choomhub.app.domain.model.SongKind
import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongRules
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SongLookup
import com.choomhub.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** The song card on Home. */
data class SongHomeUi(
    val loaded: Boolean = false,
    val song: SongUi? = null,
    val sent: SentSongUi? = null,
    val partnerName: String? = null,
)

@HiltViewModel
class SongViewModel @Inject constructor(
    private val songs: SongRepository,
    private val lookup: SongLookup,
    private val heard: HeardSongs,
    profile: ProfileRepository,
    private val notifier: Notifier,
    private val clock: Clock,
) : ViewModel() {

    /** Covers decoded once per song, not on every update. */
    private var coverCache: Pair<String, ImageBitmap?>? = null

    private fun coverOf(song: SharedSong): ImageBitmap? {
        coverCache?.let { (key, bitmap) -> if (key == song.key) return bitmap }
        val bitmap = song.cover?.let { bytes -> runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull() }
        coverCache = song.key to bitmap
        return bitmap
    }

    /** The latest of their songs, for rating and the notification. */
    private var latest: SharedSong? = null

    val state: StateFlow<SongHomeUi> = combine(songs.observeSongs(), heard.observe(), profile.observeNames()) { pair, heardKey, names ->
        latest = pair.theirs
        val now = clock.instant()
        SongHomeUi(
            loaded = pair.loaded,
            song = pair.theirs?.let { s ->
                SongUi(
                    title = s.title,
                    artist = s.artist,
                    cover = coverOf(s),
                    sentLabel = TimeLabels.relative(s.sentAt ?: now, now, clock.zone),
                    isNew = s.rating == null && heardKey != s.key,
                    myRating = s.rating,
                    kind = s.kind,
                )
            },
            sent = pair.mine?.let { SentSongUi(it.title, it.rating, it.kind) },
            partnerName = names.partnerName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SongHomeUi())

    private val _sending = MutableStateFlow<SendSongState?>(null)

    /** The send pop-up's state; null while it's closed. */
    val sending: StateFlow<SendSongState?> = _sending.asStateFlow()

    private var found: SongDraft? = null
    private var looking: Job? = null

    init {
        // Shared in from Spotify: open the pop-up and look it up straight away.
        viewModelScope.launch {
            SongInbox.link.filterNotNull().collect { link ->
                SongInbox.link.value = null
                _sending.value = SendSongState.Link(link)
                find()
            }
        }
    }

    /** The link to open in Spotify, and the song stops being new. */
    fun play(): String? {
        val song = latest ?: return null
        viewModelScope.launch { heard.markHeard(song.key) }
        return SongRules.playUrl(song.trackId, song.kind)
    }

    fun rate(rating: Int) {
        val song = latest ?: return
        if (rating !in 1..10 || rating == song.rating) return
        viewModelScope.launch {
            songs.rate(rating)
            heard.markHeard(song.key)
        }
        notifier.notifyPartner(PushEvent.SongRated, detail = song.title, kind = if (song.kind == SongKind.Track) "$rating" else "${song.kind.wire}:$rating")
    }

    fun openSend() {
        found = null
        _sending.value = SendSongState.Link()
    }

    fun close() {
        looking?.cancel()
        _sending.value = null
    }

    fun linkChanged(text: String) {
        _sending.value = SendSongState.Link(text.take(500))
    }

    fun find() {
        val text = (_sending.value as? SendSongState.Link)?.text ?: return
        if (text.isBlank()) return
        _sending.value = SendSongState.Looking
        looking?.cancel()
        looking = viewModelScope.launch {
            lookup.lookup(text).fold(
                onSuccess = { draft ->
                    found = draft
                    val cover = draft.cover?.let { b -> runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull() }
                    _sending.value = SendSongState.Found(draft.title, draft.artist, cover, draft.kind)
                },
                onFailure = { _sending.value = SendSongState.Link(text, it.message ?: "Couldn't look that up.") },
            )
        }
    }

    fun different() {
        found = null
        _sending.value = SendSongState.Link()
    }

    fun send() {
        val draft = found ?: return
        viewModelScope.launch { songs.send(draft) }
        notifier.notifyPartner(PushEvent.SongSent, detail = SongRules.describe(draft.title, draft.artist), kind = draft.kind.takeIf { it != SongKind.Track }?.wire)
        found = null
        _sending.value = null
    }
}
