package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.FocusModule
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.MoodModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.data.di.PushModule
import com.choomhub.app.data.di.SnapModule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
import com.choomhub.app.testing.FakeFocusRepository
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeMoodRepository
import com.choomhub.app.testing.FakeNotificationRepository
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakeProfileRepository
import com.choomhub.app.testing.FakeSnapRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

/**
 * Activities → the focus room: entering tells them, a break moves you to the beanbag and back,
 * the jam link goes into the record player, and leaving empties your desk. Runs on the default
 * screen size: under Robolectric a text field in a dialog never goes idle at a set size.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class, FocusModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi")
class FocusRoomFlowTest {

    private val focus = FakeFocusRepository()
    private val notifier = FakeNotifier()

    @BindValue @JvmField val focusRepository: FocusRepository = focus
    @BindValue @JvmField val snapRepository: SnapRepository = FakeSnapRepository()
    @BindValue @JvmField val moodRepository: MoodRepository = FakeMoodRepository()
    @BindValue @JvmField val gestureRepository: GestureRepository = FakeGestureRepository()
    @BindValue @JvmField val pointsRepository: com.choomhub.app.domain.repository.PointsRepository = com.choomhub.app.testing.FakePointsRepository()
    @BindValue @JvmField val notifierBinding: Notifier = notifier
    @BindValue @JvmField val notificationRepository: NotificationRepository = FakeNotificationRepository()
    @BindValue @JvmField val meetupRepository: MeetupRepository = FakeMeetupRepository()
    @BindValue @JvmField val profileRepository: ProfileRepository =
        FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = "Rook", myRole = PointsRole.GoodBoy))

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    @Before
    fun openActivities() {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TopLevelDestination.Activities.testTag).performClick()
        composeRule.waitForIdle()
    }

    private fun capture(name: String, dialog: Boolean = false) {
        composeRule.waitForIdle()
        val node = if (dialog) composeRule.onNode(isDialog()) else composeRule.onRoot()
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun enterRoom() {
        composeRule.onNodeWithTag("focus_enter").performScrollTo().performClick()
        composeRule.waitForIdle()
        // First time in: the Do Not Disturb question.
        composeRule.onNodeWithTag("focus_dnd_ask").assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun enteringStartsTheStopwatchAndTellsThem() {
        capture("activities_focus_card")
        enterRoom()
        composeRule.onNodeWithTag("screen_focus_room").assertIsDisplayed()
        composeRule.onNodeWithTag(TopLevelDestination.Home.testTag).assertDoesNotExist()
        assertEquals(FocusState.Focusing, focus.room.value.me.state)
        assertEquals(listOf(PushEvent.FocusEntered), notifier.sent.map { it.first })
        capture("focus_room_in")
    }

    @Test
    fun aBreakMovesYouToTheBeanbagAndBack() {
        enterRoom()
        composeRule.onNodeWithTag("focus_break").performClick()
        composeRule.onNodeWithTag("break_10").performClick()
        composeRule.waitForIdle()
        val onBreak = focus.room.value.me
        assertEquals(FocusState.OnBreak, onBreak.state)
        composeRule.onNodeWithTag("focus_resume").performClick()
        composeRule.waitForIdle()
        assertEquals(FocusState.Focusing, focus.room.value.me.state)
        assertEquals(onBreak.bankedMs, focus.room.value.me.bankedMs)
    }

    @Test
    fun anOverBreakAsksWhatNext() {
        enterRoom()
        focus.room.update { it.copy(me = FocusSession(FocusState.OnBreak, 600_000, null, Instant.now().minusSeconds(65))) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("focus_break_over").assertIsDisplayed()
        capture("focus_break_over", dialog = true)
        composeRule.onNodeWithTag("over_extend_5").performClick()
        composeRule.waitForIdle()
        val end = focus.room.value.me.breakEndsAt!!
        assertTrue(end.isAfter(Instant.now().plusSeconds(200)))
    }

    @Test
    fun aJamLinkGoesInTheRecordPlayer() {
        enterRoom()
        composeRule.onNodeWithTag("focus_room_art").performClick() // centre of the room: nothing
        composeRule.onNodeWithTag("focus_jam").assertDoesNotExist()
        // The record player is on the left, about 67 room pixels down (120 room pixels are shown across).
        composeRule.onNodeWithTag("focus_room_art").performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 10f / 120f, width * 67f / 120f)) }
        composeRule.onNodeWithTag("jam_input").performTextInput("open.spotify.com/socialsession/abc")
        capture("focus_jam", dialog = true)
        composeRule.onNodeWithTag("jam_leave").performClick()
        composeRule.waitForIdle()
        assertEquals("https://open.spotify.com/socialsession/abc", focus.room.value.jam?.url)
        assertTrue(notifier.sent.any { it.first == PushEvent.JamLeft })
    }

    @Test
    fun tappingTheSleepingCatSendsItOff() {
        enterRoom()
        // Its bed is by the fire, left of the beanbags (room x 53, y 54).
        composeRule.onNodeWithTag("focus_room_art").performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 49f / 120f, width * 52f / 120f)) }
        composeRule.waitForIdle()
        assertTrue(focus.room.value.cat != null)
    }

    @Test
    fun leavingShowsTheSessionThenEmptiesYourDesk() {
        enterRoom()
        composeRule.onNodeWithTag("focus_leave").performClick()
        composeRule.onNodeWithTag("focus_leave_confirm").assertIsDisplayed()
        capture("focus_leave_summary", dialog = true)
        composeRule.onNodeWithTag("dialog_confirm").performClick() // Stay
        assertEquals(FocusState.Focusing, focus.room.value.me.state)
        composeRule.onNodeWithTag("focus_leave").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick() // Leave
        composeRule.waitForIdle()
        assertEquals(FocusState.Away, focus.room.value.me.state)
        composeRule.onNodeWithTag("screen_activities").assertIsDisplayed()
    }
}
