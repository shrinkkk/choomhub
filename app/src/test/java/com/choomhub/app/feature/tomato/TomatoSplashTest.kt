package com.choomhub.app.feature.tomato

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.data.home.TomatoesSeen
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeProfileRepository
import com.choomhub.app.ui.screenshotDir
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private object NoStore : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    override val data: Flow<androidx.datastore.preferences.core.Preferences> = MutableStateFlow(androidx.datastore.preferences.core.emptyPreferences())
    override suspend fun updateData(transform: suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences) = androidx.datastore.preferences.core.emptyPreferences()
}

private class MemorySeen(start: Instant?) : TomatoesSeen(NoStore) {
    val at = MutableStateFlow(start)
    override fun observe(): Flow<Instant?> = at
    override suspend fun markSeen(at: Instant) { this.at.value = maxOf(this.at.value ?: Instant.EPOCH, at) }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TomatoSplashTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun theSplashPlaysAndGoesAway() {
        var done = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides true) {
                    Box(Modifier.fillMaxSize().background(ChoomHubTheme.colors.night)) {
                        TomatoSplash(3, "Wren", onDone = { done++ })
                    }
                }
            }
        }
        // Frames through the volley, for looking at.
        listOf(0, 180, 420, 700, 1100, 1700, 2400).forEach { ms ->
            if (ms > 0) compose.mainClock.advanceTimeBy(if (ms == 180) 180 else ms - listOf(0, 180, 420, 700, 1100, 1700, 2400).let { it[it.indexOf(ms) - 1] }.toLong())
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(screenshotDir(), "tomato_$ms.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(1, done)
    }

    @Test
    fun aTapSkipsIt() {
        var done = 0
        compose.setContent {
            ChoomHubTheme { TomatoSplash(1, "Wren", onDone = { done++ }) }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("tomato_splash").performClick()
        assertTrue(done >= 1)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TomatoVolleyTest {
    private val now = Instant.parse("2026-11-27T15:00:00Z")
    private fun tomato(id: String, minutesAgo: Long, fromMe: Boolean = false) = Gesture(id, AffectionKind.TomatoDelivery, fromMe, now.minusSeconds(minutesAgo * 60))

    @org.junit.Before
    fun setUp() = kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())

    @org.junit.After
    fun tearDown() = kotlinx.coroutines.Dispatchers.resetMain()

    @Test
    fun onlyTheirTomatoesSinceLastLookCount() {
        val list = listOf(tomato("a", 5), tomato("b", 60), tomato("mine", 2, fromMe = true), Gesture("h", AffectionKind.Hug, false, now))
        assertEquals(listOf("a"), TomatoRules.unseen(list, now.minusSeconds(30 * 60)).map { it.id })
        assertEquals(listOf("a", "b"), TomatoRules.unseen(list, Instant.EPOCH).map { it.id })
        // A phone that has never looked doesn't get the old ones all at once.
        assertTrue(TomatoRules.unseen(list, null).isEmpty())
    }

    @Test
    fun aVolleyShowsOnceThenIsSeen() = runTest {
        val gestures = FakeGestureRepository().apply { history.value = listOf(tomato("a", 5), tomato("b", 3)) }
        val seen = MemorySeen(now.minusSeconds(3600))
        val vm = TomatoSplashViewModel(gestures, seen, FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null)), Clock.fixed(now, ZoneOffset.UTC))
        val v = vm.volley.filterNotNull().first()
        assertEquals(TomatoVolley(2, "Wren", now.minusSeconds(180)), v)
        vm.shown(v)
        assertEquals(now.minusSeconds(180), seen.at.value)
        assertNull(vm.volley.first { it == null })
    }

    @Test
    fun theFirstLookStartsCountingFromNow() = runTest {
        val gestures = FakeGestureRepository().apply { history.value = listOf(tomato("old", 600)) }
        val seen = MemorySeen(null)
        val vm = TomatoSplashViewModel(gestures, seen, FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null)), Clock.fixed(now, ZoneOffset.UTC))
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { vm.volley.collect {} }
        seen.at.first { it != null }
        assertEquals(now, seen.at.value)
        assertNull(vm.volley.value)
        job.cancel()
    }

}
