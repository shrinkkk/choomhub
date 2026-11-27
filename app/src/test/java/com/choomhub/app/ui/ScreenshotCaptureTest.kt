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
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToNode
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.CoupleMoods
import com.choomhub.app.domain.model.Gesture
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodUpdate
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput

/**
 * Renders each top-level screen on the JVM and writes PNGs to
 * app/build/outputs/screenshots/ so the UI can be reviewed without a device.
 * This is a capture tool, not a pixel-comparison test: it fails only if a
 * screen cannot be rendered.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class, MeetupModule::class, SpaceModule::class, PushModule::class, MoodModule::class, SnapModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {

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

    /** A received snap on Home, opened and kept, then Memory Lane. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun snaps() {
        val w = 640
        val h = 480
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawPaint(android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF2B2438.toInt(), 0xFF6B5A6E.toInt(), android.graphics.Shader.TileMode.CLAMP)
        })
        val ink = android.graphics.Paint().apply { color = 0xFF101014.toInt(); isAntiAlias = true }
        canvas.drawRect(0f, h * 0.72f, w.toFloat(), h.toFloat(), ink)
        canvas.drawCircle(w * 0.72f, h * 0.28f, 46f, android.graphics.Paint().apply { color = 0xFFE9DFCF.toInt(); isAntiAlias = true })
        canvas.drawLine(0f, h * 0.5f, w.toFloat(), h * 0.46f, android.graphics.Paint().apply { color = 0xFF101014.toInt(); strokeWidth = 3f })
        val bytes = java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        fakeSnaps.receive("s1", "the crows are back on the wire", bytes)
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("latest_snap_open").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("home_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("latest_snap_card"))
        capture("phone_snap_home")
        composeRule.onNodeWithTag("latest_snap_open").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        kotlinx.coroutines.runBlocking { fakeSnaps.setCollections("s1", listOf(fakeSnaps.createCollection("crows"))) }
        kotlinx.coroutines.runBlocking { fakeSnaps.createCollection("rainy days") }
        capture("phone_snap_viewer")
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        capture("phone_memory_lane")
    }

    /** A little scene to stand in for a photo: sky, a moon or lamp, a horizon. */
    private fun scene(seed: Int): ByteArray {
        val w = 800
        val h = 800
        val skies = intArrayOf(0xFF3A2F45.toInt(), 0xFF4A2B33.toInt(), 0xFF2D3A36.toInt(), 0xFF6B5A6E.toInt(), 0xFF45392B.toInt())
        val grounds = intArrayOf(0xFF141218.toInt(), 0xFF2B1D16.toInt(), 0xFF1D2622.toInt(), 0xFF231A25.toInt())
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawPaint(android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), skies[seed % skies.size], 0xFFC9A98E.toInt(), android.graphics.Shader.TileMode.CLAMP)
        })
        val light = android.graphics.Paint().apply { color = 0xFFF3E7CF.toInt(); isAntiAlias = true }
        canvas.drawCircle(w * (0.25f + 0.13f * (seed % 4)), h * 0.3f, 70f + 10f * (seed % 3), light)
        val ground = android.graphics.Paint().apply { color = grounds[seed % grounds.size]; isAntiAlias = true }
        val path = android.graphics.Path().apply {
            moveTo(0f, h * 0.68f)
            quadTo(w * 0.4f, h * (0.58f + 0.04f * (seed % 3)), w.toFloat(), h * 0.66f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(path, ground)
        return java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
    }

    /** The focus room filling a phone screen. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun focusRoom() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TopLevelDestination.Activities.testTag).performClick()
        composeRule.onNodeWithTag("focus_enter").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.onNodeWithTag("focus_go_in").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        capture("phone_focus_room")
        composeRule.onNodeWithTag("focus_leave").performClick()
        captureDialog("phone_focus_leave")
    }

    /** Memory Lane: closed stacks, one collection unfolded, and its pop-ups. */
    @Test
    @Config(qualifiers = "w411dp-h2400dp-xxhdpi")
    fun memoryLane() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        val zone = java.time.ZoneId.systemDefault()
        val t0 = java.time.LocalDateTime.of(2026, 11, 24, 21, 47).atZone(zone).toInstant()
        val walks = kotlinx.coroutines.runBlocking { fakeSnaps.createCollection("late walks") }
        val food = kotlinx.coroutines.runBlocking { fakeSnaps.createCollection("food we ate") }
        kotlinx.coroutines.runBlocking { fakeSnaps.createCollection("rainy days") }
        val captions = listOf(
            "the long way home",
            null,
            "street cat judged us the entire time and honestly fair enough, we deserved it",
            "too cold for this",
            null,
        )
        captions.forEachIndexed { i, caption ->
            fakeSnaps.keep("w$i", caption, scene(i), t0.minus(java.time.Duration.ofHours(30L * i + 3L * i * i)), walks, fromMe = i % 2 == 0)
        }
        fakeSnaps.keep("f0", "3am maggi", scene(7), t0.minus(java.time.Duration.ofDays(9)), food)
        fakeSnaps.keep("f1", "burnt it a little", scene(8), t0.minus(java.time.Duration.ofDays(12)), food)
        composeRule.onNodeWithTag(TopLevelDestination.OurWorld.testTag).performClick()
        capture("lane_closed")
        composeRule.onNodeWithTag("collection_card_late walks").performClick()
        composeRule.mainClock.advanceTimeBy(2_000)
        capture("lane_open")
        composeRule.onNodeWithTag("collection_snap_w2").performTouchInput { longClick() }
        captureDialog("lane_snap_menu")
        composeRule.onNodeWithTag("snap_remove").performClick()
        captureDialog("lane_snap_delete")
        composeRule.onNodeWithTag("dialog_confirm").performClick()
        composeRule.onNodeWithTag("collection_card_late walks").performTouchInput { longClick() }
        composeRule.onNodeWithTag("collection_delete").performClick()
        captureDialog("lane_collection_delete")
    }

    /** Points (stacked, giving form open) and both "cancel the meetup?" pop-ups. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun pointsAndConfirmations() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("home_list").fetchSemanticsNodes().isNotEmpty() }
        kotlinx.coroutines.runBlocking { fakePoints.award(15, "for the playlist") }
        composeRule.onNodeWithTag("home_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("points_good_girl"))
        composeRule.onNodeWithTag("points_award").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.onNodeWithTag("home_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("award_give"))
        capture("phone_points")
        meetups.meetup.value = Meetup(Instant.now().plus(Duration.ofDays(3)))
        composeRule.onNodeWithTag("home_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("meetup_card"))
        composeRule.onNodeWithTag("meetup_clear").performClick()
        captureDialog("dialog_cancel_1")
        composeRule.onNodeWithTag("dialog_dismissive").performClick()
        captureDialog("dialog_cancel_2")
    }

    private fun captureDialog(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onNode(androidx.compose.ui.test.isDialog()).captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Small, older-phone size to check nothing clips. */
    @Test
    @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun smallPhoneHome() = capture("small_home")
}

internal fun screenshotDir(): File = File("build/outputs/screenshots").apply { mkdirs() }
