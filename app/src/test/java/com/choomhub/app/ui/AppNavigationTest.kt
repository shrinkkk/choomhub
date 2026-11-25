package com.choomhub.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.Meetup
import java.time.Duration
import java.time.Instant
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeProfileRepository
import dagger.hilt.android.testing.BindValue
import com.choomhub.app.data.di.PushModule
import com.choomhub.app.data.di.MoodModule
import com.choomhub.app.data.di.SnapModule
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.testing.FakeSnapRepository
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeMoodRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.testing.FakeNotificationRepository
import com.choomhub.app.testing.FakeNotifier
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Launches the real MainActivity (Hilt, sample home data) on the JVM via
 * Robolectric, with onboarding already done, and walks the bottom navigation.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationTest {

    private val fakeMoods = FakeMoodRepository()
    private val fakeGestures = FakeGestureRepository()
    private val fakePoints = com.choomhub.app.testing.FakePointsRepository()

    @BindValue
    @JvmField
    val pointsRepository: com.choomhub.app.domain.repository.PointsRepository = fakePoints
    private val fakeSnaps = FakeSnapRepository()

    @BindValue
    @JvmField
    val snapRepository: SnapRepository = fakeSnaps

    @BindValue
    @JvmField
    val moodRepository: MoodRepository = fakeMoods

    @BindValue
    @JvmField
    val gestureRepository: GestureRepository = fakeGestures

    @BindValue
    @JvmField
    val notifier: Notifier = FakeNotifier()

    @BindValue
    @JvmField
    val notificationRepository: NotificationRepository = FakeNotificationRepository()

    @BindValue
    @JvmField
    val profileRepository: ProfileRepository = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null, myRole = PointsRole.GoodBoy))

    private val meetups = FakeMeetupRepository()

    @BindValue
    @JvmField
    val meetupRepository: MeetupRepository = meetups

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    /** Home draws once its first data (names, moods, gestures, snaps, points) is in. */
    @org.junit.Before
    fun waitForHome() {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun appLaunchesOnHomeWithAllSections() {
        // Home waits for its first data (names, moods, gestures, snaps) before drawing.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("screen_home").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithText("ChoomHub").assertIsDisplayed()
        composeRule.onNodeWithText("WREN").assertIsDisplayed()
        composeRule.onNodeWithTag(TopLevelDestination.Home.testTag).assertIsSelected()

        listOf("latest_snap_card", "affection_Hug", "points_good_girl", "shared_cat_card").forEach { tag ->
            composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun meetupCountdownShowsNothingPlannedThenCountsDown() {
        composeRule.onNodeWithTag("meetup_card").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing planned").assertIsDisplayed()
        composeRule.onNodeWithTag("cat_Crying").assertIsDisplayed()
        composeRule.onNodeWithTag("meetup_schedule").assertIsDisplayed()

        meetups.meetup.value = Meetup(Instant.now().plus(Duration.ofDays(2)))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("meetup_countdown").assertIsDisplayed()
        composeRule.onNodeWithTag("cat_Excited").assertIsDisplayed()
        composeRule.onNodeWithTag("meetup_reschedule").assertIsDisplayed()

        composeRule.onNodeWithTag("meetup_clear").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nothing planned").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationReachesEveryDestination() {
        val screens = mapOf(
            TopLevelDestination.Messages to "screen_messages",
            TopLevelDestination.Activities to "screen_activities",
            TopLevelDestination.OurWorld to "screen_our_world",
            TopLevelDestination.Settings to "screen_settings",
            TopLevelDestination.Home to "screen_home",
        )
        screens.forEach { (destination, screenTag) ->
            composeRule.onNodeWithTag(destination.testTag).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(screenTag).assertIsDisplayed()
            composeRule.onNodeWithTag(destination.testTag).assertIsSelected()
        }
    }

    @Test
    fun gesturesAreSentAndShown() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("affection_Hug"))
        // Invoke the click action directly: after scrolling, the tile can sit under the bottom bar.
        composeRule.onNodeWithTag("affection_Hug").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Hug sent.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("gesture_sent"))
        composeRule.onNodeWithText("You sent a hug").assertExists()
    }

    @Test
    fun moodCanBeSetFromHome() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("set_my_mood"))
        composeRule.onNodeWithTag("set_my_mood").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_mood").assertIsDisplayed()
        composeRule.onNodeWithTag("mood_sleepy").performClick()
        composeRule.onNodeWithTag("mood_note").performTextInput("rain again")
        composeRule.onNodeWithTag("mood_save").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        assertEquals(Mood.Sleepy.option, fakeMoods.moods.value.mine?.mood)
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("set_my_mood"))
        composeRule.onNodeWithText("Sleepy \u00B7 rain again").assertExists()
    }

    @Test
    fun pointsStackWithYoursFirstAndCanBeGiven() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("points_good_girl"))
        val boy = composeRule.onNodeWithTag("points_good_boy").fetchSemanticsNode().boundsInRoot
        val girl = composeRule.onNodeWithTag("points_good_girl").fetchSemanticsNode().boundsInRoot
        assertTrue("your (Good Boy) box is on top", boy.bottom <= girl.top)
        assertEquals(boy.height, girl.height, 1f)

        composeRule.onNodeWithTag("points_give").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("award_amount_10").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("award_reason").performTextInput("the playlist")
        composeRule.onNodeWithTag("award_give").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("10 points given.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(10, fakePoints.ledger.value.single().amount)
        assertEquals("the playlist", fakePoints.ledger.value.single().reason)
    }

    @Test
    fun clearingTheMeetupAsksTwice() {
        meetups.meetup.value = Meetup(Instant.now().plus(Duration.ofDays(3)))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("meetup_clear").performClick()
        composeRule.onNodeWithText("Are you sure you want to cancel?").assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.onNodeWithText("Are you REALLY sure you want to cancel?").assertIsDisplayed()
        // Backing out at the second step keeps it.
        composeRule.onNodeWithTag("dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertTrue(meetups.meetup.value != null)

        composeRule.onNodeWithTag("meetup_clear").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.waitForIdle()
        assertNull(meetups.meetup.value)
    }

    @Test
    fun restoringDefaultMoodsAsksFirst() {
        composeRule.onNodeWithTag(TopLevelDestination.Settings.testTag).performClick()
        composeRule.onNodeWithTag("settings_mood_list").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mood_option_input").performTextInput("very sad")
        composeRule.onNodeWithTag("mood_option_add").performClick()
        composeRule.onNodeWithTag("mood_options_defaults").performScrollTo().performClick()
        composeRule.onNodeWithText("Restore the defaults?").assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertTrue(fakeMoods.options.value.any { it.label == "very sad" })
        composeRule.onNodeWithTag("mood_options_defaults").performScrollTo().performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.waitForIdle()
        assertTrue(fakeMoods.options.value.none { it.label == "very sad" })
    }
}
