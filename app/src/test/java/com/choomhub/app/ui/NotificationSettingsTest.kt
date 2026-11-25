package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.data.di.PushModule
import com.choomhub.app.data.di.MoodModule
import com.choomhub.app.data.di.SnapModule
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.testing.FakeSnapRepository
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.testing.FakeGestureRepository
import com.choomhub.app.testing.FakeMoodRepository
import com.choomhub.app.data.di.SpaceModule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.TestPushResult
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
import com.choomhub.app.testing.FakeMeetupRepository
import com.choomhub.app.testing.FakeNotificationRepository
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakeProfileRepository
import com.choomhub.app.testing.FakeSpaceRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Settings → Notifications with pushes available, signed in and paired. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, SpaceModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NotificationSettingsTest {

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

    private val fakeNotifier = FakeNotifier(PushAvailability.Ready, TestPushResult.Sent)
    private val prefs = FakeNotificationRepository()
    private val spaces = FakeSpaceRepository().also { it.space.value = SpaceState.Paired(it.me, "c1", "partner") }

    @BindValue @JvmField val notifier: Notifier = fakeNotifier
    @BindValue @JvmField val notificationRepository: NotificationRepository = prefs
    @BindValue @JvmField val spaceRepository: SpaceRepository = spaces
    @BindValue @JvmField val meetupRepository: MeetupRepository = FakeMeetupRepository()
    @BindValue @JvmField val profileRepository: ProfileRepository =
        FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = "Rook", myRole = PointsRole.GoodBoy))

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    private fun openSettings() {
        composeRule.onNodeWithTag(TopLevelDestination.Settings.testTag).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun categoriesCanBeTurnedOffAndTestSends() {
        openSettings()
        composeRule.onNodeWithTag("notifications_meetup").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertFalse(prefs.prefs.value.isEnabled(NotificationCategory.Meetup))

        composeRule.onNodeWithTag("notifications_test").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Sent. It should show up in a few seconds.").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, fakeNotifier.tests)
    }

    @Test
    fun signingOutForgetsThisPhone() {
        openSettings()
        composeRule.onNodeWithText("Signed in").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(1, fakeNotifier.forgotten)
    }

    /** A dialog with a text field only settles in Robolectric without size qualifiers. */
    @Test
    @Config(qualifiers = "xxhdpi")
    fun leavingTheSpaceNeedsThePhraseTypedOut() {
        openSettings()
        composeRule.onNodeWithTag("settings_leave").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("dialog_phrase").performTextInput("I hate")
        composeRule.onNodeWithTag("dialog_confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("dialog_phrase").performTextInput(" wren")
        composeRule.onNodeWithTag("dialog_confirm").assertIsEnabled()
        val dialog = composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(screenshotDir(), "phone_settings_leave.png").outputStream().use { dialog.compress(Bitmap.CompressFormat.PNG, 100, it) }
        composeRule.onNodeWithTag("dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertEquals(1, spaces.leaves)
    }

    /** Writes the section to app/build/outputs/screenshots/ for review. */
    @Test
    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    fun captureSettings() {
        openSettings()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "phone_settings_notifications.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
