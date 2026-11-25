package com.choomhub.app.feature.song

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.component.InkPanelCard
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.ui.screenshotDir
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1700dp-h1500dp-xxhdpi")
class SongDesignPreviewTest {
    @get:Rule val rule = createComposeRule()

    /** A made-up album cover: a gradient with a moon. */
    private fun cover(top: Int, bottom: Int, moon: Int): ImageBitmap {
        val b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(b)
        c.drawRect(0f, 0f, 300f, 300f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, 300f, top, bottom, Shader.TileMode.CLAMP) })
        c.drawCircle(200f, 110f, 60f, Paint().apply { shader = RadialGradient(200f, 110f, 60f, moon, 0x00000000, Shader.TileMode.CLAMP) })
        c.drawCircle(200f, 110f, 34f, Paint().apply { color = moon })
        val hills = Paint().apply { color = 0xFF0C0A0E.toInt() }
        c.drawOval(-60f, 220f, 200f, 380f, hills)
        c.drawOval(120f, 240f, 380f, 400f, hills)
        return b.asImageBitmap()
    }

    @Composable
    private fun Label(text: String) {
        Text(text.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.candle, modifier = Modifier.padding(bottom = 6.dp))
    }

    @Composable
    private fun Notification(title: String, body: String) {
        val colors = ChoomHubTheme.colors
        Column(Modifier.width(360.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2B2830)).padding(14.dp)) {
            Text("ChoomHub · Songs · now", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFEDE7F0))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDB6C2))
        }
    }

    @Test
    fun songSheet() {
        val violet = cover(0xFF3B2A5E.toInt(), 0xFF7A3446.toInt(), 0xFFF2E3C6.toInt())
        val teal = cover(0xFF0F3B44.toInt(), 0xFF1F6E63.toInt(), 0xFFE8D27A.toInt())
        rule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    Row(Modifier.background(ChoomHubTheme.colors.night).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Label("1 · Nothing received yet")
                            SongSection(null, null, "Jas", {}, {}, {})
                            Label("2 · New song, not rated yet")
                            SongSection(SongUi("Velvet Hours", "The Paper Moths", violet, "2h ago", isNew = true, myRating = null), SentSongUi("Static Bloom", null), "Jas", {}, {}, {})
                        }
                        Column(Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Label("3 · Rated: the stamp stays till the next song")
                            SongSection(SongUi("Velvet Hours", "The Paper Moths", violet, "yesterday", isNew = false, myRating = 8), SentSongUi("Static Bloom", 7), "Jas", {}, {}, {})
                            Label("4 · No cover available")
                            SongSection(SongUi("Static Bloom", null, null, "5m ago", isNew = true, myRating = null), SentSongUi("Velvet Hours", 8), "Jas", {}, {}, {})
                            Label("Notification to the sender")
                            Notification("Jas rated your song 8/10", "Velvet Hours")
                            Notification("Jas sent you a song", "Velvet Hours · The Paper Moths")
                        }
                        Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Label("5 · Send pop-up: paste a link")
                            InkPanelCard { SendSongContent(SendSongState.Link(), "Jas", {}, {}, {}, {}, {}) }
                            Label("6 · Not a song link")
                            InkPanelCard { SendSongContent(SendSongState.Link("https://open.spotify.com/playlist/37i9", "That's a playlist. Share a single song."), "Jas", {}, {}, {}, {}, {}) }
                        }
                        Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Label("7 · Found (also what Share → ChoomHub opens)")
                            InkPanelCard { SendSongContent(SendSongState.Found("Static Bloom", "Kestrel & Co", teal), "Jas", {}, {}, {}, {}, {}) }
                            Label("8 · Looking it up")
                            InkPanelCard { SendSongContent(SendSongState.Looking, "Jas", {}, {}, {}, {}, {}) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "song_mockup.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
