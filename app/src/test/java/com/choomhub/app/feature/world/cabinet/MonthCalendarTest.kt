package com.choomhub.app.feature.world.cabinet

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.ui.screenshotDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthCalendarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aMonthLinesUpUnderItsWeekdays() {
        // November 2026 starts on a Sunday.
        val sundayFirst = monthGrid(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), sundayFirst[0])
        assertEquals(35, sundayFirst.size)
        val mondayFirst = monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertNull(mondayFirst[5])
        assertEquals(LocalDate.of(2026, 11, 1), mondayFirst[6])
        assertEquals(0, mondayFirst.size % 7)
    }

    private fun show(width: String, picked: (LocalDate) -> Unit = {}) {
        compose.setContent {
            ChoomHubTheme {
                Box(Modifier.fillMaxSize().background(ChoomHubTheme.colors.night)) {
                    DatePanel(LocalDate.of(2026, 11, 22), onPick = picked, onDismiss = {})
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "calendar_$width.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun fitsANarrowPhone() = show("360")

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun fitsASmallPhone() = show("320")

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun pickADayFromAnotherMonth() {
        var picked: LocalDate? = null
        show("411") { picked = it }
        compose.onNodeWithTag("month_prev").performClick()
        compose.onNodeWithTag("month_title").assertTextEquals("October 2026")
        compose.onNodeWithTag("day_2026-10-03").performClick()
        compose.onNodeWithTag("date_ok").performClick()
        assertEquals(LocalDate.of(2026, 10, 3), picked)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun twoSheeshMahals() {
        var chosen: com.choomhub.app.data.place.FoundPlace? = null
        val options = listOf(
            com.choomhub.app.data.place.FoundPlace(28.6507, 77.2334, "Sheesh Mahal", "Sheesh Mahal, Chandni Chowk, near Jama Masjid, Delhi 110006"),
            com.choomhub.app.data.place.FoundPlace(28.7168, 77.1615, "Sheesh Mahal", "Sheesh Mahal, Shalimar Bagh, Delhi 110088"),
        )
        compose.setContent {
            ChoomHubTheme {
                Box(Modifier.fillMaxSize().background(ChoomHubTheme.colors.night)) {
                    PlaceChoicesPanel(com.choomhub.app.feature.world.MeetupViewModel.PlaceChoices(0, "Sheesh Mahal", options), onChoose = { chosen = it }, onPinMyself = {})
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "place_choices.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithTag("place_choice_1").performClick()
        assertEquals(options[1], chosen)
    }
}
