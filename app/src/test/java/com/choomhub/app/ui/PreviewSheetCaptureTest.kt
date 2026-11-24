package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.choomhub.app.core.designsystem.component.GestureButton
import com.choomhub.app.core.designsystem.icon.GestureIcons
import com.choomhub.app.domain.model.AffectionKind
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.CatFace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Large renders of the cat's moods and the raven/spider, for design review before shipping. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w760dp-h620dp-xhdpi")
class PreviewSheetCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun designPreview() {
        composeRule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    Column(
                        Modifier.background(ChoomHubTheme.colors.night).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CatExpression.entries.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { e ->
                                    Column {
                                        CatFace(e, Modifier.size(170.dp).background(ChoomHubTheme.colors.surfaceElevated))
                                        Text(e.name, color = ChoomHubTheme.colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        save("preview_cats")
    }

    @Test
    fun ravenAndSpider() {
        composeRule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    Row(
                        Modifier.background(ChoomHubTheme.colors.night).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        listOf(GothicMotif.Raven, GothicMotif.Cobweb).forEach {
                            GothicMotifArt(it, Modifier.size(300.dp).background(ChoomHubTheme.colors.surface))
                        }
                    }
                }
            }
        }
        save("preview_raven_spider")
    }

    @Test
    fun gestureIcons() {
        composeRule.setContent {
            ChoomHubTheme {
                Column(
                    Modifier.background(ChoomHubTheme.colors.night).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        listOf(GestureIcons.Hug, GestureIcons.ForeheadKiss, GestureIcons.Paw, GestureIcons.Tomato).forEach {
                            Icon(it, contentDescription = null, tint = ChoomHubTheme.colors.textMain, modifier = Modifier.size(120.dp))
                        }
                    }
                    Row(Modifier.width(420.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AffectionKind.entries.forEach { kind ->
                            GestureButton(
                                label = kind.label,
                                icon = when (kind) {
                                    AffectionKind.Hug -> GestureIcons.Hug
                                    AffectionKind.ForeheadKiss -> GestureIcons.ForeheadKiss
                                    AffectionKind.HeadPats -> GestureIcons.Paw
                                    AffectionKind.TomatoDelivery -> GestureIcons.Tomato
                                },
                                tint = if (kind == AffectionKind.TomatoDelivery) ChoomHubTheme.colors.tomato else ChoomHubTheme.colors.textMain,
                                onClick = {},
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
        save("preview_icons")
    }

    private fun save(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
