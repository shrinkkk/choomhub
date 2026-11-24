package com.choomhub.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
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
@UninstallModules(ProfileModule::class, MeetupModule::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationTest {

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

    @Test
    fun appLaunchesOnHomeWithAllSections() {
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
    fun gestureButtonsAreHonestPrototypes() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("affection_Hug"))
        // Invoke the click action directly: after scrolling, the tile can sit under the bottom bar.
        composeRule.onNodeWithTag("affection_Hug").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Hugs can't travel yet", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Hugs can't travel yet", substring = true).assertExists()
    }
}
