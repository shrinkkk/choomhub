package com.choomhub.app.feature.song

import com.choomhub.app.data.song.HeardSongs
import com.choomhub.app.data.song.LocalSongRepository
import com.choomhub.app.data.song.SongInbox
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.SharedSong
import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.repository.SongLookup
import com.choomhub.app.domain.repository.SongLookupException
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModelTest {
    private val now = Instant.parse("2026-11-25T14:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Kolkata"))
    private val songs = LocalSongRepository()
    private val notifier = FakeNotifier()
    private val names = FakeProfileRepository(CoupleNames(partnerName = "Jas", myName = "Shri"))
    private val lookup = object : SongLookup {
        override suspend fun lookup(link: String): Result<SongDraft> =
            if ("track" in link) Result.success(SongDraft("4uLU6hMCjMI75M1A2tKUQC", "Velvet Hours", "The Paper Moths", null))
            else Result.failure(SongLookupException("That's a playlist. Share a single song."))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SongInbox.link.value = null
    }

    /** Preferences kept in memory, so reads and writes happen straight away. */
    private class MemoryPrefs : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        val state = kotlinx.coroutines.flow.MutableStateFlow(androidx.datastore.preferences.core.emptyPreferences())
        override val data = state
        override suspend fun updateData(
            transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences,
        ): androidx.datastore.preferences.core.Preferences = transform(state.value).also { state.value = it }
    }

    @Suppress("UnusedReceiverParameter")
    private fun kotlinx.coroutines.test.TestScope.vm(): SongViewModel =
        SongViewModel(songs, lookup, HeardSongs(MemoryPrefs()), names, notifier, clock)

    @Test
    fun `sending a song looks it up, then sends it and tells them`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.openSend()
        vm.linkChanged("https://open.spotify.com/playlist/37i9")
        vm.find()
        advanceUntilIdle()
        assertEquals("That's a playlist. Share a single song.", (vm.sending.value as SendSongState.Link).error)
        vm.linkChanged("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC")
        vm.find()
        advanceUntilIdle()
        assertEquals("Velvet Hours", (vm.sending.value as SendSongState.Found).title)
        vm.send()
        advanceUntilIdle()
        assertNull(vm.sending.value)
        assertEquals("Velvet Hours", vm.state.value.sent?.title)
        assertEquals(PushEvent.SongSent to "Velvet Hours · The Paper Moths", notifier.sent.last())
    }

    @Test
    fun `a link shared in from Spotify opens the pop-up already looked up`() = runTest {
        val vm = vm()
        SongInbox.link.value = "Listen: https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=x"
        advanceUntilIdle()
        assertTrue(vm.sending.value is SendSongState.Found)
        assertNull(SongInbox.link.value)
    }

    @Test
    fun `their song is new until rated, and rating tells them`() = runTest {
        songs.songs.update { it.copy(theirs = SharedSong("4uLU6hMCjMI75M1A2tKUQC", "Static Bloom", null, null, now.minusSeconds(600), rating = null)) }
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertTrue(vm.state.value.song!!.isNew)
        vm.rate(8)
        advanceUntilIdle()
        assertEquals(8, vm.state.value.song!!.myRating)
        assertFalse(vm.state.value.song!!.isNew)
        assertEquals(PushEvent.SongRated to "Static Bloom", notifier.sent.last())
        assertEquals("8", notifier.kinds.last())
        // Same rating again: nothing new to tell.
        vm.rate(8)
        assertEquals(1, notifier.sent.size)
    }

    @Test
    fun `playing it opens Spotify and it stops being new`() = runTest {
        songs.songs.update { it.copy(theirs = SharedSong("4uLU6hMCjMI75M1A2tKUQC", "Static Bloom", null, null, now, rating = null)) }
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertEquals("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC", vm.play())
        advanceUntilIdle()
        assertFalse(vm.state.value.song!!.isNew)
    }
}
