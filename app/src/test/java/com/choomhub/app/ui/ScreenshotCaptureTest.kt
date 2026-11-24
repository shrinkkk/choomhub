package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.MainActivity
import com.choomhub.app.core.navigation.TopLevelDestination
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
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DisableAmbientAnimationsRule()).around(composeRule)

    private val outputDir = File("build/outputs/screenshots").apply { mkdirs() }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(outputDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** A tall canvas so the whole scrolling home screen fits in one image. */
    @Test
    @Config(qualifiers = "w411dp-h2050dp-xxhdpi")
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

    /** Small, older-phone size to check nothing clips. */
    @Test
    @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun smallPhoneHome() = capture("small_home")
}
