package com.choomhub.app.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.component.SkyBackground
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.core.designsystem.theme.SkyPhase
import com.choomhub.app.core.navigation.ChoomHubBottomBar
import com.choomhub.app.domain.model.CountdownMood
import com.choomhub.app.domain.model.CountdownState
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.feature.home.HomeContent
import com.choomhub.app.feature.home.HomeUiState
import com.choomhub.app.feature.home.PartnerMoodUi
import com.choomhub.app.feature.home.SnapUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** Home at each time of day, for design review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SkyPhaseCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyPhase() {
        val phase = mutableStateOf(SkyPhase.Night)
        composeRule.setContent {
            ChoomHubTheme(phase.value) {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    SkyBackground {
                        Box(Modifier.fillMaxSize()) {
                            HomeContent(
                                state = state(phase.value),
                                contentPadding = PaddingValues(top = 28.dp, bottom = 90.dp),
                                onAffection = {},
                                countdown = CountdownState.Upcoming(Instant.now().plus(Duration.ofDays(5)), Duration.ofDays(5).plusHours(3).plusMinutes(25), CountdownMood.Happy),
                            )
                            ChoomHubBottomBar(currentDestination = null, onNavigate = {}, modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
        SkyPhase.entries.forEach {
            phase.value = it
            composeRule.waitForIdle()
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            File(screenshotDir(), "sky_${it.name.lowercase()}.png").outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }
    }

    private fun state(phase: SkyPhase) = HomeUiState.Ready(
        greeting = when (phase) {
            SkyPhase.Dawn -> "Morning"
            SkyPhase.Day -> "Afternoon"
            SkyPhase.Dusk -> "Evening"
            SkyPhase.Night -> "Late one"
        },
        dateLabel = "Wednesday, 25 November",
        viewerName = null,
        partner = PartnerMoodUi("Wren", Mood.Sleepy.option, "headphones on, ignoring the rain", "12 min ago"),
        snap = SnapUi("s1", "Wren", "the crows are back on the wire", ZonedDateTime.now(), "47 min ago", isNew = true),
        points = PointBalances(goodBoy = 128, goodGirl = 142),
        myRole = PointsRole.GoodBoy,
        partnerName = "Wren",
        cat = SharedCatStatus(isAdopted = false, name = null),
        isSampleData = true,
    )
}
