package com.afterglow.app.ui

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afterglow.app.MainActivity
import com.afterglow.app.core.navigation.TopLevelDestination
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.annotation.Config

/**
 * Launches the real MainActivity (with Hilt and the sample repository) on the
 * JVM via Robolectric and walks the bottom navigation.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DisableAmbientAnimationsRule()).around(composeRule)

    @Test
    fun appLaunchesOnHomeWithAllSections() {
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithText("Afterglow").assertIsDisplayed()
        composeRule.onNodeWithTag("partner_mood_card").assertIsDisplayed()
        composeRule.onNodeWithTag(TopLevelDestination.Home.testTag).assertIsSelected()

        listOf("latest_snap_card", "affection_Hug", "points_good_girl", "shared_cat_card").forEach { tag ->
            composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
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
    fun affectionButtonsAreHonestPrototypes() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("affection_Hug"))
        composeRule.onNodeWithTag("affection_Hug").performClick()
        composeRule.onNodeWithText("Hugs can't travel yet", substring = true).assertIsDisplayed()
    }
}

/** Mirrors the system "remove animations" setting so infinite ambient effects stay off in tests. */
class DisableAmbientAnimationsRule : TestRule {
    override fun apply(base: Statement, description: org.junit.runner.Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val resolver = ApplicationProvider.getApplicationContext<android.app.Application>().contentResolver
                Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
                base.evaluate()
            }
        }
}
