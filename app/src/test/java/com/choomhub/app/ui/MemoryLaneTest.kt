package com.choomhub.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.time.Instant

/** Our World → Memory Lane: unfolding a collection, covers, renaming, and deleting with confirmation. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2000dp-xxhdpi")
class MemoryLaneTest {

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

    private val t0 = Instant.parse("2026-11-01T15:00:00Z")
    private lateinit var walks: String
    private lateinit var food: String

    @Before
    fun openOurWorld() {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            walks = snaps.createCollection("late walks")
            food = snaps.createCollection("food")
        }
        // Old ones of theirs, one shared with "food", and their newest (current) one.
        snaps.keep("old", "the long way home", jpeg(), t0, walks)
        snaps.keep("shared", "3am maggi", jpeg(), t0.plusSeconds(60), walks, food)
        snaps.keep("newest", null, jpeg(), t0.plusSeconds(120), walks)
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        composeRule.waitForIdle()
    }

    private fun jpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.DKGRAY) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    private fun scrollTo(tag: String) = composeRule.onNodeWithTag("our_world_list").performScrollToNode(hasTestTag(tag))

    @Test
    fun aCollectionUnfoldsWithDatesAndFoldsBackUp() {
        composeRule.onNodeWithTag("collection_snap_old").assertDoesNotExist()
        composeRule.onNodeWithTag("collection_card_late walks").performClick()
        composeRule.waitForIdle()
        scrollTo("collection_snap_old")
        composeRule.onNodeWithTag("collection_snap_old").assertIsDisplayed()
        composeRule.onNodeWithText("the long way home").assertIsDisplayed()
        composeRule.onAllNodesWithText("1 nov 2026").assertCountEquals(3)

        scrollTo("collection_card_late walks")
        composeRule.onNodeWithTag("collection_card_late walks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collection_snap_old").assertDoesNotExist()
    }

    @Test
    fun anyPolaroidCanBecomeTheCover() {
        composeRule.onNodeWithTag("collection_card_late walks").performClick()
        composeRule.waitForIdle()
        scrollTo("collection_snap_old")
        composeRule.onNodeWithTag("collection_snap_old").performTouchInput { longClick() }
        composeRule.onNodeWithTag("snap_make_cover").performClick()
        composeRule.waitForIdle()
        assertEquals("old", snaps.collections.value.first { it.id == walks }.cover)
    }

    @Test
    fun anOldSnapInNoOtherCollectionIsDeletedForGoodAfterAsking() {
        composeRule.onNodeWithTag("collection_card_late walks").performClick()
        composeRule.waitForIdle()
        scrollTo("collection_snap_old")
        composeRule.onNodeWithTag("collection_snap_old").performTouchInput { longClick() }
        composeRule.onNodeWithTag("snap_remove").performClick()
        composeRule.onNodeWithText("Delete it for good?").assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_confirm").performClick() // "Keep it"
        assertTrue(snaps.snaps.value.any { it.id == "old" })

        composeRule.onNodeWithTag("collection_snap_old").performTouchInput { longClick() }
        composeRule.onNodeWithTag("snap_remove").performClick()
        composeRule.onNodeWithTag("dialog_dismissive").performClick() // "Delete for good"
        composeRule.waitForIdle()
        assertNull(snaps.snaps.value.firstOrNull { it.id == "old" })
        composeRule.onNodeWithTag("collection_snap_old").assertDoesNotExist()
        composeRule.onNodeWithTag("collection_snap_shared").assertExists()
    }

    @Test
    fun deletingACollectionKeepsSnapsThatAreElsewhereOrCurrent() {
        composeRule.onNodeWithTag("collection_card_late walks").performTouchInput { longClick() }
        composeRule.onNodeWithTag("collection_delete").performClick()
        composeRule.onNodeWithText("1 snap only in this collection will be deleted for good, for both of you.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("food"), snaps.collections.value.map { it.name })
        assertEquals(setOf("shared", "newest"), snaps.snaps.value.map { it.id }.toSet())
        assertEquals(listOf(food), snaps.snaps.value.first { it.id == "shared" }.collections)
    }
}

/** Types into a field without focusing it: focusing a field inside a dialog never goes idle under Robolectric. */
internal fun androidx.compose.ui.test.SemanticsNodeInteraction.setText(text: String) {
    performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetText) { it(androidx.compose.ui.text.AnnotatedString(text)) }
}
