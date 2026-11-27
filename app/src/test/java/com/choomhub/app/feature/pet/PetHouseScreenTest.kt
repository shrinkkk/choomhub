package com.choomhub.app.feature.pet

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.NeedLevel
import com.choomhub.app.domain.model.PendingCare
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetDiaryEntry
import com.choomhub.app.domain.model.PetGift
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.domain.model.TeamCare
import com.choomhub.app.domain.model.applied
import com.choomhub.app.ui.screenshotDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PetHouseScreenTest {
    @get:Rule val rule = createComposeRule()

    private val now = Instant.parse("2026-11-25T14:30:00Z")
    private val zone = ZoneId.systemDefault()

    private fun cat(level: Float = 70f, block: PetState.() -> PetState = { this }) = PetState(
        loaded = true,
        name = "Mochi",
        adoptedAt = now.minus(Duration.ofDays(40)),
        traits = listOf(PetTrait.Curious, PetTrait.Sassy),
        favoriteFood = Food.Tuna,
        dislikedFood = Food.WetFood,
        needs = PetNeed.entries.associateWith { NeedLevel(level, now) },
        myCoins = 120,
        partnerCoins = 45,
        myBond = 70,
        partnerBond = 25,
        streak = 4,
        streakDay = now.atZone(zone).toLocalDate(),
    ).block()

    private var pet by mutableStateOf(PetState())
    private val proposed = mutableListOf<String>()
    private val adopted = mutableListOf<String>()

    private fun show(state: PetState, diary: List<PetDiaryEntry> = emptyList()) {
        pet = state
        rule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(ChoomHubTheme.colors.night)) {
                    PetHouseContent(
                        ui = PetUi(loaded = true, pet = pet, now = now, myName = "Shri", partnerName = "Jas", diary = diary),
                        message = null,
                        contentPadding = PaddingValues(),
                        onAct = { a: PetAction ->
                            val o = PetRules.act(pet, a, now, zone, Random(5), "Shri", "Jas")
                            pet = pet.applied(o.patch, now)
                            o
                        },
                        onProposeName = { proposed += it },
                        onAdopt = { adopted += it },
                        onDismissMessage = {},
                    )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun level(need: PetNeed) = PetRules.level(pet, need, now)

    private fun shot(name: String, node: SemanticsNodeInteraction = rule.onRoot()) {
        rule.waitForIdle()
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun dialog() = rule.onNode(isDialog())

    private fun open(action: String) {
        rule.onNodeWithTag("pet_action_$action").performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test
    fun suggestingAName() {
        show(PetState(loaded = true))
        shot("pet_adoption")
        rule.onNodeWithTag("pet_name_field").performTextInput("Mochi")
        rule.onNodeWithTag("pet_propose").performScrollTo().performClick()
        assertEquals(listOf("Mochi"), proposed)
    }

    @Test
    fun agreeingToTheirName() {
        show(PetState(loaded = true, proposedName = "Mochi", proposedByMe = false))
        rule.onNodeWithTag("pet_agree").performScrollTo().performClick()
        assertEquals(listOf("Mochi"), adopted)
    }

    @Test
    fun adoptingWithoutThemAsksFirst() {
        show(PetState(loaded = true, proposedName = "Mochi", proposedByMe = true))
        rule.onNodeWithTag("pet_adopt_anyway").performScrollTo().performClick()
        rule.waitForIdle()
        shot("pet_adopt_confirm", dialog())
        rule.onNodeWithTag("dialog_confirm").performClick()
        assertEquals(listOf("Mochi"), adopted)
    }

    @Test
    fun theRoom() {
        show(cat { copy(gifts = listOf(PetGift("quartz", now, true), PetGift("feather", now, false)), owned = setOf("rug", "plants")) })
        rule.onNodeWithTag("pet_name").assertIsDisplayed()
        rule.onNodeWithTag("pet_apartment").assertIsDisplayed()
        shot("pet_room")
    }

    @Test
    fun feeding() {
        show(cat(30f))
        open("feed")
        shot("pet_panel_feed", dialog())
        rule.onNodeWithTag("feed_kibble").performClick()
        assertTrue(level(PetNeed.Hunger) > 60f)
        rule.onNodeWithTag("pet_panel_result").assertIsDisplayed()
    }

    @Test
    fun pouringWater() {
        show(cat(10f))
        open("water")
        // Tap by tap (holding pours continuously on a phone).
        repeat(17) { rule.onNodeWithTag("water_pour").performClick() }
        assertEquals(100f, level(PetNeed.Thirst))
    }

    @Test
    fun scoopingTheLitter() {
        show(cat(20f))
        open("litter")
        shot("pet_panel_litter", dialog())
        val clumps = rule.onAllNodesWithTag("litter_clump").fetchSemanticsNodes().size
        assertTrue(clumps > 1)
        repeat(clumps) { rule.onAllNodesWithTag("litter_clump")[0].performClick() }
        assertEquals(100f, level(PetNeed.Litter))
    }

    @Test
    fun pettingAndTheDoublePurr() {
        show(cat(40f) { copy(partnerPettingAt = now.minusSeconds(5)) })
        open("pet")
        shot("pet_panel_pet", dialog())
        rule.onNodeWithTag("pet_zone_0").performClick()
        assertEquals(1, pet.count("double_purrs"))
        assertTrue(level(PetNeed.Affection) > 60f)
    }

    @Test
    fun brushing() {
        show(cat(40f))
        open("brush")
        repeat(4) { rule.onNodeWithTag("brush_quick").performClick() }
        assertEquals(1, pet.count("brushes"))
    }

    @Test
    fun playing() {
        show(cat(40f) { copy(owned = setOf("laser")) })
        open("play")
        rule.onNodeWithTag("play_laser").performClick()
        repeat(3) { rule.onNodeWithTag("play_area").performClick() }
        shot("pet_panel_play", dialog())
        rule.onNodeWithTag("play_stop").performClick()
        assertEquals(1, pet.count("plays"))
        assertEquals(40f + 15f + 18f, level(PetNeed.Fun), 0.01f)
    }

    @Test
    fun trimmingNails() {
        show(cat())
        open("care")
        rule.onNodeWithTag("care_nails").performClick()
        repeat(4) { rule.onNodeWithTag("claw_$it").performClick() }
        assertEquals(now, pet.nailsAt)
    }

    @Test
    fun theVetTakesTwo() {
        show(cat { copy(sickSince = now.minus(Duration.ofHours(2)), vetDone = false) })
        open("care")
        shot("pet_panel_care", dialog())
        rule.onNodeWithTag("care_vet").performClick()
        assertEquals(TeamCare.Vet, pet.pending?.kind)
        rule.onNodeWithTag("pet_panel_done").performClick()
        rule.onNodeWithTag("pet_help_banner").assertIsDisplayed()

        // When they started it, you finish it.
        pet = pet.copy(pending = PendingCare(TeamCare.Vet, byMe = false, at = now))
        rule.onNodeWithTag("pet_help_join").performScrollTo().performClick()
        assertNull(pet.pending)
        assertEquals(true, pet.vetDone)
    }

    @Test
    fun shoppingAndDressingUp() {
        show(cat())
        rule.onNodeWithTag("pet_page_shop").performScrollTo().performClick()
        rule.onNodeWithTag("shop_tab_Outfits").performClick()
        shot("pet_shop_outfits")
        rule.onNodeWithTag("buy_crown").performScrollTo().performClick()
        assertTrue(pet.has("crown"))
        assertEquals(0, pet.myCoins)
        rule.onNodeWithTag("pet_back").performScrollTo().performClick()
        rule.onNodeWithTag("pet_action_wardrobe").performScrollTo().performClick()
        rule.onNodeWithTag("wear_crown").performClick()
        assertEquals("crown", pet.outfit)
    }

    @Test
    fun theWardrobeDressesOneThingPerSlot() {
        show(cat { copy(owned = setOf("crown", "beanie", "scarf", "striped_socks")) })
        open("wardrobe")
        rule.onNodeWithTag("wear_crown").performScrollTo().performClick()
        rule.onNodeWithTag("wear_scarf").performScrollTo().performClick()
        rule.onNodeWithTag("wear_striped_socks").performScrollTo().performClick()
        assertEquals(listOf("crown", "scarf", "striped_socks"), pet.wearing())
        // A beanie swaps out the crown; the rest stays on.
        rule.onNodeWithTag("wear_beanie").performScrollTo().performClick()
        assertEquals(setOf("scarf", "striped_socks", "beanie"), pet.wearing().toSet())
        rule.onNodeWithTag("wear_none_Neck").performScrollTo().performClick()
        assertEquals(setOf("striped_socks", "beanie"), pet.wearing().toSet())
        shot("pet_wardrobe_layers")
        rule.onNodeWithTag("wear_nothing").performScrollTo().performClick()
        assertEquals(emptyList<String>(), pet.wearing())
    }

    @Test
    fun everyToyHasItsOwnGame() {
        show(cat(60f) { copy(owned = setOf("kicker", "bubble_wand", "box", "mouse", "yarn")) })
        open("play")
        listOf("feather", "kicker", "bubble_wand", "box", "mouse", "yarn").forEach { rule.onNodeWithTag("play_$it").assertExists() }
        rule.onNodeWithTag("play_bubble_wand").performClick()
        repeat(2) { rule.onNodeWithTag("play_area").performClick() }
        rule.onNodeWithTag("play_stop").performClick()
        rule.onNodeWithText("popped 2 bubbles", substring = true).assertExists()
    }

    @Test
    fun giftsDiaryAndTrophies() {
        val diary = listOf(
            PetDiaryEntry("1", "Brought them a quartz crystal. Left it on the pillow.", now.minusSeconds(3600)),
            PetDiaryEntry("2", "They brought me home and called me Mochi. I have decided to allow it.", now.minus(Duration.ofDays(40))),
        )
        show(cat { copy(gifts = listOf(PetGift("quartz", now, true), PetGift("ammonite", now, false), PetGift("quartz", now, false))) }, diary)
        rule.onNodeWithTag("pet_page_gifts").performScrollTo().performClick()
        rule.onNodeWithTag("gift_ammonite").assertIsDisplayed()
        shot("pet_gifts")
        rule.onNodeWithTag("pet_back").performScrollTo().performClick()
        rule.onNodeWithTag("pet_page_diary").performScrollTo().performClick()
        // A little diary for each month; open one to read it.
        val thisMonth = java.time.YearMonth.from(now.atZone(java.time.ZoneId.systemDefault()))
        val earlier = java.time.YearMonth.from(now.minus(Duration.ofDays(40)).atZone(java.time.ZoneId.systemDefault()))
        rule.onNodeWithTag("diary_month_$earlier").assertExists()
        shot("pet_diary")
        rule.onNodeWithTag("diary_month_$thisMonth").performClick()
        assertEquals(1, rule.onAllNodesWithTag("diary_entry").fetchSemanticsNodes().size)
        shot("pet_diary_month")
        rule.onNodeWithTag("diary_back").performClick()
        rule.onNodeWithTag("pet_back").performScrollTo().performClick()
        rule.onNodeWithTag("pet_page_trophies").performScrollTo().performClick()
        assertNotNull(rule.onNodeWithTag("trophy_rockhound").fetchSemanticsNode())
    }
}
