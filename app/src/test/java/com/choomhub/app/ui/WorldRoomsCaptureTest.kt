package com.choomhub.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Size
import com.choomhub.app.feature.world.constellation.starPoints
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.MoodModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.data.di.PushModule
import com.choomhub.app.data.di.SnapModule
import com.choomhub.app.data.meetuplog.LocalMeetupLogRepository
import com.choomhub.app.data.song.LocalSongRepository
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPhoto
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.SharedSong
import com.choomhub.app.domain.model.SongPair
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MeetupLogRepository
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.PointsRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.domain.repository.SongRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeMoodRepository
import com.choomhub.app.testing.FakeNotificationRepository
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakePointsRepository
import com.choomhub.app.testing.FakeProfileRepository
import com.choomhub.app.testing.FakeSnapRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.choomhub.app.domain.model.MeetupActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Renders Our World's rooms with sample dates and songs, for review (writes PNGs, asserts nothing). */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorldRoomsCaptureTest {
    @BindValue @JvmField val pointsRepository: PointsRepository = FakePointsRepository()
    @BindValue @JvmField val snapRepository: SnapRepository = FakeSnapRepository()
    @BindValue @JvmField val moodRepository: MoodRepository = FakeMoodRepository()
    @BindValue @JvmField val gestureRepository: GestureRepository = FakeGestureRepository()
    @BindValue @JvmField val notifier: Notifier = FakeNotifier()
    @BindValue @JvmField val notificationRepository: NotificationRepository = FakeNotificationRepository()
    @BindValue @JvmField val profileRepository: ProfileRepository = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null, myRole = PointsRole.GoodBoy))
    @BindValue @JvmField val meetupRepository: MeetupRepository = FakeMeetupRepository()

    @Inject lateinit var logs: MeetupLogRepository
    @Inject lateinit var songs: SongRepository

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun picture(seed: Int, edge: Int = 240): ByteArray {
        val palette = listOf(0xFF5C2233, 0xFF2E3A4A, 0xFF6B5A2E, 0xFF3B3349, 0xFF7A3446, 0xFF2F4A3A, 0xFFB9A98A).map { it.toInt() }
        val b = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint().apply { shader = LinearGradient(0f, 0f, edge.toFloat(), edge.toFloat(), palette[seed % palette.size], palette[(seed + 3) % palette.size], Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, edge.toFloat(), edge.toFloat(), p)
        c.drawCircle(edge * 0.65f, edge * 0.35f, edge * 0.18f, Paint().apply { color = 0x55FFFFFF })
        return ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    private fun seed() {
        val now = Instant.now()
        val places = listOf(
            Triple("Lodhi Garden", 28.5931, 77.2197), Triple("Hauz Khas Village", 28.5494, 77.2001), Triple("Connaught Place", 28.6315, 77.2167),
            Triple("Sunder Nursery", 28.5933, 77.2445), Triple("Majnu ka Tilla", 28.7008, 77.2274), Triple("Lodhi Garden", 28.5935, 77.2201),
        )
        val list = (0 until 13).map { i ->
            val p = places[i % places.size]
            MeetupLog(
                "m$i", LocalDate.of(2026, 6, 1).plusDays(i * 13L), minutes = 60 + (i * 37) % 300,
                places = listOf(com.choomhub.app.domain.model.MeetupPlace(p.first, p.second, p.third)) +
                    if (i == 7) listOf(com.choomhub.app.domain.model.MeetupPlace("Khan Market", 28.6003, 77.2270), com.choomhub.app.domain.model.MeetupPlace("India Gate", 28.6129, 77.2295), com.choomhub.app.domain.model.MeetupPlace("a chai stall somewhere")) else emptyList(),
                note = if (i == 7) "fed the ducks, argued about the best momos, walked till the guard whistled" else null,
                did = setOf(MeetupActivity.entries[i % MeetupActivity.entries.size], MeetupActivity.Walk) + if (i == 7) setOf(MeetupActivity.Other) else emptySet(),
                firsts = if (i == 7) listOf("rowing boat", "sushi at that tiny place") else emptyList(),
                extras = if (i == 7) listOf("karaoke") else emptyList(),
                stamps = if (i == 7) mapOf("p70" to "21:43") else emptyMap(),
                photos = (0 until (i % 5)).map { k -> MeetupPhoto("p$i$k", picture(i + k, 160)) },
                createdAt = now,
            )
        } + MeetupLog("live", LocalDate.now(), live = true, startedAt = now.minusSeconds(134 * 60), createdAt = now)
        (logs as LocalMeetupLogRepository).meetups.value = list
        (songs as LocalSongRepository).songs.value = SongPair(
            mine = SharedSong("4uLU6hMCjMI75M1A2tKUQC", "Pink + White", "Frank Ocean", picture(2), Instant.parse("2026-11-20T10:00:00Z"), 9, com.choomhub.app.domain.model.SongKind.Album),
            theirs = SharedSong("7GhIk7Il098yCjg4BQjzvb", "Rainy Day Drives", "Wren", picture(5), Instant.parse("2026-11-21T10:00:00Z"), null, com.choomhub.app.domain.model.SongKind.Playlist),
            loaded = true,
        )
    }

    private fun waitFor(tag: String) = composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun ourWorld() {
        hiltRule.inject()
        waitFor("home_list")
        seed()
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        waitFor("screen_our_world")
        capture("world_rooms")
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    fun rooms() {
        hiltRule.inject()
        waitFor("home_list")
        seed()
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        waitFor("screen_our_world")
        composeRule.onNodeWithTag("world_sky").performClick()
        waitFor("screen_constellation")
        capture("world_sky")
        val sky = composeRule.onNodeWithTag("constellation_sky").fetchSemanticsNode().size
        val star = starPoints(14, Size(sky.width.toFloat(), sky.height.toFloat()))[7]
        composeRule.onNodeWithTag("constellation_sky").performTouchInput { click(star) }
        composeRule.mainClock.advanceTimeBy(1_500)
        capture("world_sky_star")
        composeRule.onNodeWithTag("star_open").performClick()
        waitFor("screen_meetup_view")
        capture("world_meetup_view")
        composeRule.onNodeWithTag("meetup_edit").performClick()
        waitFor("screen_meetup")
        capture("world_meetup")
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("screen_meetup_view")
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("constellation_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("constellation_back").performClick()
        waitFor("screen_our_world")

        composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag("world_cabinet"))
        composeRule.onNodeWithTag("world_cabinet").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("screen_cabinet")
        capture("world_cabinet")
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("screen_our_world")

        composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag("world_shelf"))
        composeRule.onNodeWithTag("world_shelf").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("screen_shelf")
        capture("world_shelf")
        composeRule.onNodeWithTag("shelf_theirs").performClick()
        capture("world_shelf_theirs")
        composeRule.onNodeWithTag("shelf_back").performClick()
        waitFor("screen_our_world")

        composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag("world_atlas"))
        composeRule.onNodeWithTag("world_atlas").performSemanticsAction(SemanticsActions.OnClick)
        waitFor("screen_atlas")
        capture("world_atlas")
    }
}
