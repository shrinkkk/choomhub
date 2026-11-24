package com.choomhub.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.choomhub.app.domain.model.PointsRole
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeProfileRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** First launch: name your partner, land on Home, rename from Settings. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NamingFlowTest {

    private val fake = FakeProfileRepository()

    @BindValue
    @JvmField
    val profileRepository: ProfileRepository = fake

    private val meetups = FakeMeetupRepository()

    @BindValue
    @JvmField
    val meetupRepository: MeetupRepository = meetups

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    @Test
    fun firstLaunchAsksForPartnerNameThenShowsHome() {
        composeRule.onNodeWithTag("screen_onboarding").assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_continue").performClick()
        composeRule.onNodeWithText("Give them a name first.").assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_field").performTextInput("  Wren ")
        composeRule.onNodeWithTag("onboarding_continue").performClick()
        composeRule.onNodeWithText("Pick which one you are.").assertIsDisplayed()
        composeRule.onNodeWithTag("role_GoodBoy").performScrollTo().performClick()
        composeRule.onNodeWithTag("onboarding_continue").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Wren", fake.names.value.partnerName)
        assertEquals(PointsRole.GoodBoy, fake.names.value.myRole)
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithText("WREN").assertIsDisplayed()
    }

    @Test
    fun partnerCanBeRenamedFromSettings() {
        fake.names.value = fake.names.value.copy(partnerName = "Wren", myRole = PointsRole.GoodGirl)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TopLevelDestination.Settings.testTag).performClick()
        composeRule.onNodeWithText("Wren").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_partner_name").performClick()
        composeRule.onNodeWithTag("rename_field").performTextClearance()
        composeRule.onNodeWithTag("rename_field").performTextInput("Little Crow")
        composeRule.onNodeWithTag("rename_save").performClick()
        composeRule.waitForIdle()

        assertEquals("Little Crow", fake.names.value.partnerName)
        composeRule.onNodeWithText("Little Crow").assertIsDisplayed()
    }
}
