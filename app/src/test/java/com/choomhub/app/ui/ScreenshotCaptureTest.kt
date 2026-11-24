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
import com.choomhub.app.data.di.ProfileModule
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.testing.DisableAmbientAnimationsRule
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
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders each top-level screen on the JVM and writes PNGs to
 * app/build/outputs/screenshots/ so the UI can be reviewed without a device.
 * This is a capture tool, not a pixel-comparison test: it fails only if a
 * screen cannot be rendered.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(ProfileModule::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {

    private val fake = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null))

    @BindValue
    @JvmField
    val profileRepository: ProfileRepository = fake

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

    /** Small, older-phone size to check nothing clips. */
    @Test
    @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun smallPhoneHome() = capture("small_home")
}

internal fun screenshotDir(): File = File("build/outputs/screenshots").apply { mkdirs() }
