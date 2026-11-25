package com.choomhub.app.feature.focus

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalSkyPhase
import com.choomhub.app.core.designsystem.theme.SkyPhase
import com.choomhub.app.ui.screenshotDir
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the focus room in a few states to build/outputs/screenshots (a capture tool, not a check). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class FocusRoomPreviewTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, sky: SkyPhase, scene: RoomScene) {
        rule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalSkyPhase provides sky) {
                    androidx.compose.foundation.layout.Column(Modifier.background(ChoomHubTheme.colors.night).padding(12.dp)) {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            val me = scene.him
                            com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton(
                                text = if (me.spot == Spot.Beanbag) "Back to focus" else "Take a break",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                            )
                            com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton(text = "Leave", onClick = {})
                        }
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        ) {
                            androidx.compose.material3.Text("♪ fire + rain: on", style = com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
                            androidx.compose.material3.Text("NUDGE WREN ›", style = com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.pink)
                        }
                        FocusRoomArt(scene, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val rook = RoomPerson("Rook", Spot.Desk, "1:02:33", weekHours = "6.5", totalHours = "41", weekMinutes = 390)
    private val wren = RoomPerson("Wren", Spot.Desk, "0:47:10", weekHours = "9.2", totalHours = "58", weekMinutes = 552)

    /** Frames of the animation (fire, rain, the cat's z's) for a GIF preview. */
    @Test fun animationFrames() {
        val scene = RoomScene(rook.copy(spot = Spot.Beanbag, tvTime = "4:12"), wren, together = false)
        repeat(24) { f ->
            val bmp = renderRoomFrame(scene, SkyPhase.Night, f)
            File(screenshotDir(), "focus_anim_%02d.png".format(f)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun bothFocusing() = shot("focus_both", SkyPhase.Night, RoomScene(rook, wren, together = true))

    @Test fun oneOnBreak() = shot(
        "focus_break",
        SkyPhase.Dusk,
        RoomScene(rook.copy(spot = Spot.Beanbag, tvTime = "4:12"), wren, together = false),
    )

    @Test fun bothOnBreak() = shot(
        "focus_both_break",
        SkyPhase.Dawn,
        RoomScene(rook.copy(spot = Spot.Beanbag, tvTime = "2:40"), wren.copy(spot = Spot.Beanbag, tvTime = "7:05"), together = false),
    )

    @Test fun overrunAndAway() = shot(
        "focus_overrun",
        SkyPhase.Day,
        RoomScene(rook.copy(spot = Spot.Away, tvTime = "--:--"), wren.copy(spot = Spot.Beanbag, tvTime = "+1:05", tvOverrun = true), together = false),
    )
}
