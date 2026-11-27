package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.SnapCollection
import com.choomhub.app.feature.snaps.KeepPicker
import com.choomhub.app.feature.world.MemoryLaneViewModel
import com.choomhub.app.feature.world.OurWorldScreen
import com.choomhub.app.testing.FakeSnapRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

/**
 * The pop-ups with a text field (naming collections, the keep picker). Under Robolectric,
 * a text field in a dialog never lets Compose go idle when the screen size is set with
 * width/height qualifiers (a Robolectric quirk; real phones are fine), so these run
 * on the default screen and outside MainActivity.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi")
class MemoryLaneDialogsTest {
    @get:Rule val rule = createComposeRule()

    private val snaps = FakeSnapRepository()

    private fun showOurWorld() {
        val viewModel = MemoryLaneViewModel(snaps, SavedStateHandle(), com.choomhub.app.testing.FakeNotifier(), com.choomhub.app.testing.FakeProfileRepository())
        rule.setContent { ChoomHubTheme { OurWorldScreen(PaddingValues(), onOpenSnap = {}, viewModel = viewModel) } }
        rule.waitForIdle()
    }

    private fun captureDialog(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun anEmptyCollectionCanBeMadeAndRenamed() {
        showOurWorld()
        rule.onNodeWithTag("collection_new").performClick()
        rule.onNodeWithTag("dialog_name").performTextInput("rainy days")
        captureDialog("lane_new_collection")
        rule.onNodeWithTag("dialog_confirm").performClick()
        rule.waitForIdle()
        assertEquals(listOf("rainy days"), snaps.collections.value.map { it.name })

        rule.onNodeWithTag("collection_card_rainy days").performTouchInput { longClick() }
        rule.onNodeWithTag("collection_rename").performClick()
        rule.onNodeWithTag("dialog_name").performTextReplacement("puddles")
        rule.onNodeWithTag("dialog_confirm").performClick()
        rule.waitForIdle()
        assertEquals(listOf("puddles"), snaps.collections.value.map { it.name })
    }

    @Test
    fun aBlankNameIsNotSaved() {
        showOurWorld()
        rule.onNodeWithTag("collection_new").performClick()
        rule.onNodeWithTag("dialog_name").performTextInput("   ")
        rule.onNodeWithTag("dialog_confirm").performClick()
        rule.waitForIdle()
        assertTrue(snaps.collections.value.isEmpty())
    }

    @Test
    fun theKeepPickerTogglesCollectionsAndNamesNewOnes() {
        val toggled = mutableListOf<String>()
        var created = ""
        val collections = listOf(SnapCollection("k1", "late walks", Instant.now()), SnapCollection("k2", "food we ate", Instant.now()))
        rule.setContent {
            ChoomHubTheme {
                var name by remember { mutableStateOf("") }
                KeepPicker(
                    collections = collections,
                    keptIn = listOf("k1"),
                    newName = name,
                    onNewNameChange = { name = it },
                    onCreate = { created = name },
                    onToggle = { toggled += it.id },
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithTag("collection_food we ate").performClick()
        rule.onNodeWithTag("snap_new_collection").performTextInput("rainy days")
        captureDialog("phone_keep_picker")
        rule.onNodeWithTag("snap_new_collection_add").performClick()
        assertEquals(listOf("k2"), toggled)
        assertEquals("rainy days", created)
    }
}
