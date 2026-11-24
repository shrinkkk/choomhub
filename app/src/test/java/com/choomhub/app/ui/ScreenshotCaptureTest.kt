package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.data.di.MeetupModule
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.data.di.SpaceModule
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.SpaceRepository
import com.choomhub.app.testing.FakeSpaceRepository
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
import org.robolectric.annotation.GraphicsMode
import java.io.File
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodUpdate
import androidx.compose.ui.test.performSemanticsAction

/**
 * Renders each top-level screen on the JVM and writes PNGs to
 * app/build/outputs/screenshots/ so the UI can be reviewed without a device.
 * This is a capture tool, not a pixel-comparison test: it fails only if a
 * screen cannot be rendered.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, SpaceModule::class, PushModule::class, MoodModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {

    private val fakeMoods = FakeMoodRepository()
    private val fakeGestures = FakeGestureRepository()

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

    private val fake = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null, myRole = PointsRole.GoodBoy))

    @BindValue
    @JvmField
    val profileRepository: ProfileRepository = fake

    private val meetups = FakeMeetupRepository()

    @BindValue
    @JvmField
    val meetupRepository: MeetupRepository = meetups

    private val spaces = FakeSpaceRepository()

    @BindValue
    @JvmField
    val spaceRepository: SpaceRepository = spaces

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(DisableAmbientAnimationsRule()).around(composeRule)

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** A tall canvas so the whole scrolling home screen fits in one image. */
    @Test
    @Config(qualifiers = "w411dp-h2000dp-xxhdpi")
    fun homeFullLength() = capture("home_full")

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun everyTopLevelScreen() {
        capture("phone_home")
        TopLevelDestination.entries.drop(1).forEach { destination ->
            composeRule.onNodeWithTag(destination.testTag).performClick()
            capture("phone_${destination.name.lowercase()}")
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun onboarding() {
        fake.names.value = CoupleNames(partnerName = null, myName = null)
        capture("phone_onboarding")
    }

    /** The countdown in each mood, captured at the top of Home. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun countdownMoods() {
        mapOf("sad" to 40L, "bored" to 20L, "hopeful" to 10L, "happy" to 5L, "excited" to 2L).forEach { (name, days) ->
            meetups.meetup.value = Meetup(Instant.now().plus(Duration.ofDays(days)).plusSeconds(3 * 3600 + 25 * 60 + 7))
            capture("countdown_$name")
        }
    }

    /** Sign-in, pairing, and Settings while waiting for the other person. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun accountScreens() {
        spaces.space.value = SpaceState.SignedOut
        capture("phone_sign_in")
        spaces.space.value = SpaceState.NoSpace(spaces.me)
        capture("phone_pairing")
        spaces.space.value = SpaceState.Waiting(spaces.me, "c1", "RVN4KEY7")
        composeRule.onNodeWithTag(TopLevelDestination.Settings.testTag).performClick()
        capture("phone_settings_waiting")
    }

    /** Home with moods and gestures in both directions, and the mood picker. */
    @Test
    @Config(qualifiers = "w411dp-h2000dp-xxhdpi")
    fun moodsAndGestures() {
        val now = Instant.now()
        fakeMoods.moods.value = CoupleMoods(
            mine = MoodUpdate(Mood.Calm.option, "tea and a long playlist", now),
            partner = MoodUpdate(Mood.MissingYou.option, "rain again, crows on the wire", now.minusSeconds(720)),
        )
        fakeGestures.history.value = listOf(
            Gesture("2", AffectionKind.TomatoDelivery, fromMe = true, sentAt = now.minusSeconds(60)),
            Gesture("1", AffectionKind.Hug, fromMe = false, sentAt = now.minusSeconds(240)),
        )
        capture("phone_home_live")
        composeRule.onNodeWithTag("set_my_mood").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        capture("phone_mood")
    }

    /** Small, older-phone size to check nothing clips. */
    @Test
    @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun smallPhoneHome() = capture("small_home")
}

internal fun screenshotDir(): File = File("build/outputs/screenshots").apply { mkdirs() }
