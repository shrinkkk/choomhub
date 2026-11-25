package com.choomhub.app.feature.song

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SongSectionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun playRateAndSend() {
        val log = mutableListOf<String>()
        rule.setContent {
            ChoomHubTheme {
                SongSection(
                    song = SongUi("Velvet Hours", "The Paper Moths", null, "2h ago", isNew = false, myRating = 6),
                    sent = SentSongUi("Static Bloom", 7),
                    partnerName = "Jas",
                    onPlay = { log += "play" },
                    onRate = { log += "rate $it" },
                    onSend = { log += "send" },
                )
            }
        }
        rule.onNodeWithTag("song_stamp").assertIsDisplayed()
        rule.onNodeWithTag("song_play").performClick()
        rule.onNodeWithTag("song_rate_9").performClick()
        rule.onNodeWithTag("song_send").performClick()
        assertEquals(listOf("play", "rate 9", "send"), log)
    }
}
