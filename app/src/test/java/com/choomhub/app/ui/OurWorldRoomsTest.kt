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
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Our World's rooms in the real app: the cabinet, the sky, the shelf and the atlas. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OurWorldRoomsTest {
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

    private val local get() = logs as LocalMeetupLogRepository

    @Before
    fun openOurWorld() {
        hiltRule.inject()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("screen_our_world").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun open(tag: String) {
        composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    private fun seed(vararg m: MeetupLog) {
        local.meetups.value = m.toList()
        composeRule.waitForIdle()
    }

    private fun waitFor(tag: String) = composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun aLiveMeetupStartsCountingAndStops() {
        open("world_cabinet")
        waitFor("screen_cabinet")
        composeRule.onNodeWithTag("cabinet_log").performClick()
        composeRule.onNodeWithTag("cabinet_live").performClick()
        waitFor("screen_meetup")
        composeRule.onNodeWithTag("meetup_stop").assertIsDisplayed()
        assertTrue(local.meetups.value.single().live)

        composeRule.onNodeWithTag("meetup_stop").performClick()
        composeRule.waitUntil(5_000) { !local.meetups.value.single().live }
        composeRule.onNodeWithTag("meetup_hours").assertIsDisplayed()
    }

    @Test
    fun aPastMeetupTakesTypedInTime() {
        open("world_cabinet")
        waitFor("screen_cabinet")
        composeRule.onNodeWithTag("cabinet_log").performClick()
        composeRule.onNodeWithTag("cabinet_past").performClick()
        waitFor("date_panel")
        composeRule.onNodeWithTag("date_ok").performClick()
        waitFor("screen_meetup")
        val m = local.meetups.value.single()
        assertEquals(false, m.live)

        composeRule.onNodeWithTag("meetup_hours").performTextInput("3")
        composeRule.onNodeWithTag("meetup_minutes").performTextInput("20")
        composeRule.onNodeWithTag("meetup_time_save").performClick()
        composeRule.waitUntil(5_000) { local.meetups.value.single().minutes == 200 }
    }

    @Test
    fun photosReorderAndTheFrontOneIsTheCover() {
        seed(MeetupLog("m1", LocalDate.of(2026, 11, 2), minutes = 90, photos = listOf(MeetupPhoto("a", null), MeetupPhoto("b", null), MeetupPhoto("c", null)), createdAt = Instant.now()))
        open("world_cabinet")
        waitFor("specimen_m1")
        composeRule.onNodeWithTag("specimen_m1").performClick()
        waitFor("screen_meetup")

        composeRule.onNodeWithTag("meetup_photo_2").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("photo_first").performClick()
        composeRule.waitUntil(5_000) { local.meetups.value.single().photoIds == listOf("c", "a", "b") }

        composeRule.onNodeWithTag("meetup_photo_1").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("photo_later").performClick()
        composeRule.waitUntil(5_000) { local.meetups.value.single().photoIds == listOf("c", "b", "a") }
    }

    @Test
    fun theSkyOpensFromThePreviewAndAStarZoomsToItsDate() {
        seed(
            MeetupLog("m1", LocalDate.of(2026, 10, 4), minutes = 150, place = "Lodhi Garden", note = "the ducks", createdAt = Instant.now()),
            MeetupLog("m2", LocalDate.of(2026, 11, 2), minutes = 60, createdAt = Instant.now()),
        )
        composeRule.onNodeWithTag("world_sky").performClick()
        waitFor("screen_constellation")
        // No tab bar over the sky.
        assertTrue(composeRule.onAllNodesWithTag(TopLevelDestination.OurWorld.testTag).fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("2 dates · 3 hours together").assertIsDisplayed()

        // Tap the first (oldest) star where it's drawn.
        val sky = composeRule.onNodeWithTag("constellation_sky").fetchSemanticsNode().size
        val star = starPoints(2, Size(sky.width.toFloat(), sky.height.toFloat()))[0]
        composeRule.onNodeWithTag("constellation_sky").performTouchInput { click(star) }
        waitFor("star_overview")
        composeRule.onNodeWithText("4 October 2026").assertIsDisplayed()
        composeRule.onNodeWithText("2h 30m together · Lodhi Garden").assertIsDisplayed()
        composeRule.onNodeWithText("the ducks").assertIsDisplayed()

        // Back zooms out first, then leaves.
        composeRule.onNodeWithTag("constellation_back").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("star_overview").fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithTag("screen_constellation").assertIsDisplayed()

        composeRule.onNodeWithTag("constellation_sky").performTouchInput { click(star) }
        waitFor("star_open")
        composeRule.onNodeWithTag("star_open").performClick()
        waitFor("screen_meetup")
        composeRule.onNodeWithText("Lodhi Garden").assertIsDisplayed()
    }

    @Test
    fun theShelfShowsBothSongsAndFiltersBySender() {
        (songs as LocalSongRepository).songs.value = SongPair(
            mine = SharedSong("4uLU6hMCjMI75M1A2tKUQC", "Sent by me", "Someone", null, Instant.parse("2026-11-20T10:00:00Z"), 9),
            theirs = SharedSong("7GhIk7Il098yCjg4BQjzvb", "Sent by Wren", "Somebody", null, Instant.parse("2026-11-21T10:00:00Z"), null),
            loaded = true,
        )
        open("world_shelf")
        waitFor("screen_shelf")
        composeRule.onNodeWithText("Sent by me").assertIsDisplayed()
        composeRule.onNodeWithText("Sent by Wren").assertIsDisplayed()

        composeRule.onNodeWithTag("shelf_theirs").performClick()
        composeRule.onNodeWithTag("shelf_theirs").assertIsSelected()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Sent by me").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Sent by Wren").assertIsDisplayed()

        composeRule.onNodeWithTag("shelf_mine").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Sent by Wren").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theAtlasShowsPinnedDates() {
        seed(MeetupLog("m1", LocalDate.of(2026, 10, 4), minutes = 150, place = "Lodhi Garden", lat = 28.5931, lon = 77.2197, createdAt = Instant.now()))
        composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag("world_atlas"))
        composeRule.onNodeWithText("1 place in Delhi").assertIsDisplayed()
        open("world_atlas")
        waitFor("screen_atlas")
        composeRule.onNodeWithTag("atlas_map").assertIsDisplayed()
    }
}
