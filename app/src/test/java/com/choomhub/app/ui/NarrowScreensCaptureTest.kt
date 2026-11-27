package com.choomhub.app.ui

import com.choomhub.app.data.pet.LocalPetRepository
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PetGift
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.SongKind
import com.choomhub.app.domain.repository.PetRepository
import java.time.Duration

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
import androidx.compose.ui.test.performScrollTo
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

/**
 * Every main screen on a narrow phone (360dp wide), for checking that labels stay on one line.
 * Writes PNGs to build/outputs/screenshots/narrow_*.png; asserts nothing.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h2600dp-xxhdpi")
class NarrowScreensCaptureTest {
    @BindValue @JvmField val pointsRepository: PointsRepository = FakePointsRepository()
    @BindValue @JvmField val snapRepository: SnapRepository = FakeSnapRepository()
    @BindValue @JvmField val moodRepository: MoodRepository = FakeMoodRepository()
    @BindValue @JvmField val gestureRepository: GestureRepository = FakeGestureRepository()
    @BindValue @JvmField val notifier: Notifier = FakeNotifier()
    @BindValue @JvmField val notificationRepository: NotificationRepository = FakeNotificationRepository()
    @BindValue @JvmField val profileRepository: ProfileRepository = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = "Shri", myRole = PointsRole.GoodBoy))
    @BindValue @JvmField val meetupRepository: MeetupRepository = FakeMeetupRepository()

    @Inject lateinit var songs: SongRepository
    @Inject lateinit var pets: PetRepository

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "narrow_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitFor(tag: String) = composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun tab(d: TopLevelDestination, screen: String) {
        composeRule.onNodeWithTag(d.testTag).performClick()
        waitFor(screen)
    }

    private fun back() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun everyScreen() {
        hiltRule.inject()
        waitFor("home_list")
        val now = Instant.now()
        (songs as LocalSongRepository).songs.value = SongPair(
            mine = SharedSong("37i9dQZF1DXcBWIGoYBM5M", "Today's Top Hits", "Spotify", null, now.minusSeconds(3600), 7, SongKind.Playlist),
            theirs = SharedSong("4aawyAB9vmqN3uQ7FjRGTy", "Global Warming", "Pitbull", null, now.minusSeconds(600), 8, SongKind.Album),
            loaded = true,
        )
        (pets as LocalPetRepository).pet.value = PetState(
            loaded = true, name = "Mochi", adoptedAt = now.minus(Duration.ofDays(40)),
            traits = listOf(PetTrait.Curious, PetTrait.Sassy), favoriteFood = Food.Tuna, dislikedFood = Food.WetFood,
            myCoins = 1240, partnerCoins = 87, myBond = 140, partnerBond = 90,
            gifts = (1..12).map { PetGift("g$it", now.minusSeconds(it * 3600L), it % 2 == 0) },
        )
        capture("home")

        tab(TopLevelDestination.PetHouse, "screen_pet")
        capture("pet")
        listOf("shop", "diary", "gifts", "trophies").forEach { page ->
            composeRule.onNodeWithTag("pet_page_$page").performScrollTo().performClick()
            composeRule.waitForIdle()
            capture("pet_$page")
            if (page == "shop") {
                listOf("Toys", "Furniture", "Outfits", "Decor").forEach { c ->
                    composeRule.onNodeWithTag("shop_tab_$c").performClick()
                    capture("pet_shop_$c")
                }
            }
            back()
        }

        tab(TopLevelDestination.Activities, "screen_activities")
        capture("activities")
        composeRule.onNodeWithTag("bounty_card").performScrollTo().performClick()
        waitFor("screen_bounty_board")
        capture("bounties")
        back()
        composeRule.onNodeWithTag("shop_open").performScrollTo().performClick()
        waitFor("screen_shop")
        capture("points_shop")
        back()

        tab(TopLevelDestination.OurWorld, "screen_our_world")
        capture("world")

        tab(TopLevelDestination.Settings, "screen_settings")
        capture("settings")
    }
}
