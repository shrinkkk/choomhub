package com.choomhub.app.feature.pet

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetBuild
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose
import com.choomhub.app.ui.screenshotDir
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Design sheets for the Pet House, written to build/outputs/screenshots (a capture tool). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1400dp-xxhdpi")
class PetDesignPreviewTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private data class Cell(val label: String, val pose: PetPose, val build: PetBuild = PetBuild.Cat, val outfit: PetOutfit = PetOutfit.None, val expression: CatExpression = CatExpression.Happy, val frame: Int = 0)

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun catSheet() {
        val cells = listOf(
            Cell("sitting", PetPose.Sit),
            Cell("loaf", PetPose.Loaf),
            Cell("asleep", PetPose.Asleep),
            Cell("walking", PetPose.Walk),
            Cell("walking (step)", PetPose.Walk, frame = 2),
            Cell("eating", PetPose.Eat),
            Cell("about to pounce", PetPose.Pounce),
            Cell("being petted", PetPose.Petted),
            Cell("belly up", PetPose.BellyUp),
            Cell("grooming", PetPose.Grooming),
            Cell("sick", PetPose.Sick),
            Cell("bored", PetPose.Sit, expression = CatExpression.Bored),
            Cell("sad", PetPose.Sit, expression = CatExpression.Sad),
            Cell("excited", PetPose.Sit, expression = CatExpression.Excited),
            Cell("kitten", PetPose.Sit, PetBuild.Kitten, expression = CatExpression.Excited),
            Cell("kitten asleep", PetPose.Asleep, PetBuild.Kitten),
            Cell("chonky", PetPose.Sit, PetBuild.Chonky),
            Cell("chonky loaf", PetPose.Loaf, PetBuild.Chonky),
            Cell("bow tie", PetPose.Sit, outfit = PetOutfit.BowTie),
            Cell("witch hat", PetPose.Sit, outfit = PetOutfit.WitchHat),
            Cell("bat wings", PetPose.Sit, outfit = PetOutfit.BatWings),
            Cell("scarf", PetPose.Sit, outfit = PetOutfit.Scarf),
            Cell("crown", PetPose.Sit, outfit = PetOutfit.Crown),
            Cell("hat + walking", PetPose.Walk, outfit = PetOutfit.WitchHat),
        )
        rule.setContent {
            ChoomHubTheme {
                FlowRow(
                    Modifier.background(ChoomHubTheme.colors.night).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cells.forEach { c ->
                        Column(Modifier.width(132.dp).background(ChoomHubTheme.colors.surface).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PetCat(c.pose, Modifier.size(120.dp), expression = c.expression, build = c.build, outfit = c.outfit, frame = c.frame)
                            Text(c.label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
        capture("pet_cat_sheet")
    }

    private val t0 = java.time.Instant.parse("2026-11-25T20:00:00Z")

    private fun cat(vararg levels: Pair<com.choomhub.app.domain.model.PetNeed, Float>, build: com.choomhub.app.domain.model.PetState.() -> com.choomhub.app.domain.model.PetState = { this }) =
        com.choomhub.app.domain.model.PetState(
            loaded = true,
            name = "Mochi",
            adoptedAt = t0.minus(java.time.Duration.ofDays(40)),
            needs = com.choomhub.app.domain.model.PetNeed.entries.associateWith { n ->
                com.choomhub.app.domain.model.NeedLevel(levels.firstOrNull { it.first == n }?.second ?: 80f, t0)
            },
        ).build()

    @Test
    fun apartmentSheet() {
        val n = com.choomhub.app.domain.model.PetNeed.entries
        val all = setOf("tree", "heated_bed", "perch", "hammock", "fountain", "fairy_lights", "plants", "rug", "candles", "poster", "moon_lamp", "box", "yarn", "mouse")
        val states = listOf(
            "bare, content" to cat(),
            "hungry" to cat(n[0] to 10f),
            "tired" to cat(n[2] to 10f),
            "lonely (hiding)" to cat(n[5] to 5f),
            "dirty litter" to cat(n[3] to 8f),
            "sick" to cat { copy(sickSince = t0) },
            "everything bought" to cat { copy(owned = all, gifts = listOf("quartz", "feather", "geode", "ammonite", "sock", "trilobite", "obsidian", "key").mapIndexed { i, g -> com.choomhub.app.domain.model.PetGift(g, t0, i % 2 == 0) }) },
            "muddy + crown" to cat { copy(muddy = true, outfit = "crown", owned = setOf("rug")) },
        )
        rule.setContent {
            ChoomHubTheme {
                Column(Modifier.background(ChoomHubTheme.colors.night).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    states.chunked(2).forEach { row ->
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, s) ->
                                Column(Modifier.width(280.dp)) {
                                    PetApartment(s, t0, Modifier.width(280.dp))
                                    Text(label.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        capture("pet_apartment_sheet")
    }
}
