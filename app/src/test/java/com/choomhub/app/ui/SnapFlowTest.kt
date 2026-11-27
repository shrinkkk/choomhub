package com.choomhub.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.MoodModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.data.di.PushModule
import com.choomhub.app.data.di.SnapModule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.repository.GestureRepository
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.MoodRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/** Their snap on Home → open it → keep it in a new collection → it's in Memory Lane. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SnapFlowTest {

    private val snaps = FakeSnapRepository()

    @BindValue @JvmField val snapRepository: SnapRepository = snaps
    @BindValue @JvmField val moodRepository: MoodRepository = FakeMoodRepository()
    @BindValue @JvmField val gestureRepository: GestureRepository = FakeGestureRepository()
    @BindValue @JvmField val pointsRepository: com.choomhub.app.domain.repository.PointsRepository = com.choomhub.app.testing.FakePointsRepository()
    @BindValue @JvmField val notifier: Notifier = FakeNotifier()
    @BindValue @JvmField val notificationRepository: NotificationRepository = FakeNotificationRepository()
    @BindValue @JvmField val meetupRepository: MeetupRepository = FakeMeetupRepository()
    @BindValue @JvmField val profileRepository: ProfileRepository =
        FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = "Rook", myRole = PointsRole.GoodBoy))

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    /** Home draws once its first data (names, moods, gestures, snaps, points) is in. */
    @org.junit.Before
    fun waitForHome() {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun jpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.DKGRAY) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    @Test
    fun theirSnapCanBeOpenedAndKept() {
        snaps.receive("s1", "crows on the wire", jpeg())
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("latest_snap_open"))
        composeRule.onNodeWithText("crows on the wire").assertIsDisplayed()
        composeRule.onNodeWithTag("latest_snap_open").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_snap").assertIsDisplayed()

        // The picker itself has a text field in a dialog, which Robolectric can't idle at this
        // screen size; MemoryLaneDialogsTest covers it. Here the snap is kept through the repository.
        composeRule.onNodeWithTag("snap_keep").assertExists()
        kotlinx.coroutines.runBlocking { snaps.addToCollection("s1", snaps.createCollection("crows")) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Kept in crows").assertExists()

        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collection_card_crows").assertIsDisplayed()
        composeRule.onNodeWithTag("collection_card_crows").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collection_snap_s1").assertIsDisplayed()
    }

    @Test
    fun withNoSnapsHomeSaysSoAndOffersTheCamera() {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("snap_camera"))
        composeRule.onNodeWithText("Nothing from Wren yet").assertExists()
        composeRule.onNodeWithTag("snap_camera").assertExists()
    }
}
