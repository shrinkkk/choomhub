package com.choomhub.app.feature.shop

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.domain.model.RewardChange
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.model.ShopRules
import com.choomhub.app.ui.screenshotDir
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1400dp-xxhdpi")
class PointsShopScreenTest {
    @get:Rule val rule = createComposeRule()

    private val t = Instant.parse("2026-11-25T12:00:00Z")
    private val log = mutableListOf<String>()
    private var ui by mutableStateOf(ShopUi())

    private fun show(state: ShopUi) {
        ui = state
        rule.setContent {
            ChoomHubTheme {
                Box(Modifier.background(ChoomHubTheme.colors.night)) {
                    PointsShopContent(
                        ui = ui,
                        message = null,
                        contentPadding = PaddingValues(),
                        actions = ShopActions(
                            buy = { log += "buy ${it.id}"; true },
                            use = { log += "use ${it.id}" },
                            giveBack = { log += "give back ${it.id}" },
                            markDone = { log += "done ${it.id}" },
                            suggestReward = { n, p -> log += "suggest $n $p"; null },
                            suggestPrice = { r, p -> log += "price ${r.id} $p"; null },
                            suggestRemove = { log += "remove ${it.id}" },
                            accept = { log += "accept ${it.id}" },
                            dismiss = { log += "dismiss ${it.id}" },
                        ),
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun shop(points: Int = 42) = ShopUi(
        loaded = true,
        myRole = PointsRole.GoodBoy,
        partnerName = "Jas",
        myPoints = points,
        partnerPoints = 18,
        catalogue = ShopRules.catalogue(listOf(RewardChange("p1", "Back scratch", 18, custom = true, removed = false))),
        changes = listOf(RewardChange("p1", "Back scratch", 18, custom = true, removed = false)),
        proposals = listOf(
            ShopProposal("p2", ProposalKind.Add, null, "Breakfast in bed", 40, byMe = false, at = t),
            ShopProposal("p3", ProposalKind.Price, "playlist", null, 20, byMe = true, at = t),
        ),
        purchases = listOf(
            Purchase("b1", "head_massage", "5-minute head massage at the next meetup", 15, byMe = true, at = t, state = PurchaseState.Owned),
            Purchase("b2", "selfie", "Selfie on demand, within 10 minutes", 5, byMe = true, at = t, state = PurchaseState.Done),
            Purchase("b3", "letter", "Handwritten letter, posted or handed over at the next meetup", 40, byMe = false, at = t, state = PurchaseState.Used),
        ),
    )

    private fun shot(name: String, node: SemanticsNodeInteraction = rule.onRoot()) {
        rule.waitForIdle()
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(screenshotDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun buyingAsksFirst() {
        show(shop())
        shot("shop_main")
        rule.onNodeWithTag("buy_head_massage").performScrollTo().performClick()
        shot("shop_buy_dialog", rule.onNode(isDialog()))
        rule.onNodeWithTag("dialog_confirm").performClick()
        assertEquals(listOf("buy head_massage"), log)
        // Too expensive.
        rule.onNodeWithTag("buy_your_day").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun suggestionsShowWhoAskedAndCanBeAgreedTo() {
        show(shop())
        rule.onNodeWithTag("proposal_p2").assertIsDisplayed()
        rule.onNodeWithTag("proposal_accept_p2").performClick()
        // My own price suggestion can be taken back, and the reward can't be edited meanwhile.
        rule.onNodeWithTag("pending_playlist").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("proposal_withdraw_p3").performScrollTo().performClick()
        rule.onNodeWithTag("reward_edit_playlist").assertIsNotEnabled()
        assertEquals(listOf("accept p2", "dismiss p3"), log)
    }

    @Test
    fun yoursAndTheirs() {
        show(shop())
        rule.onNodeWithTag("shop_tab_yours").performClick()
        shot("shop_yours")
        rule.onNodeWithTag("use_b1").performClick()
        rule.onNodeWithTag("dialog_confirm").performClick()
        rule.onNodeWithTag("shop_tab_theirs").performClick()
        shot("shop_theirs")
        rule.onNodeWithTag("done_b3").performClick()
        assertEquals(listOf("use b1", "done b3"), log)
    }
}

/** Dialogs with text fields only settle in Robolectric without size qualifiers. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "xxhdpi")
class PointsShopSuggestTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun suggestingAReward() {
        val log = mutableListOf<String>()
        rule.setContent {
            ChoomHubTheme {
                PointsShopContent(
                    ui = ShopUi(loaded = true, partnerName = "Jas", catalogue = ShopRules.catalogue(emptyList())),
                    message = null,
                    contentPadding = PaddingValues(),
                    actions = ShopActions(suggestReward = { n, p -> log += "$n|$p"; null }),
                )
            }
        }
        rule.onNodeWithTag("shop_suggest").performScrollTo().performClick()
        rule.onNodeWithTag("suggest_name").performTextInput("Back scratch")
        rule.onNodeWithTag("suggest_price").performTextInput("18")
        rule.onNodeWithTag("dialog_confirm").performClick()
        assertEquals(listOf("Back scratch|18"), log)
    }

    @Test
    fun suggestingANewPrice() {
        val log = mutableListOf<String>()
        rule.setContent {
            ChoomHubTheme {
                PointsShopContent(
                    ui = ShopUi(loaded = true, partnerName = "Jas", catalogue = ShopRules.catalogue(emptyList())),
                    message = null,
                    contentPadding = PaddingValues(),
                    actions = ShopActions(suggestPrice = { r, p -> log += "${r.id}|$p"; null }),
                )
            }
        }
        rule.onNodeWithTag("reward_edit_selfie").performScrollTo().performClick()
        rule.onNodeWithTag("reward_change_price").performClick()
        rule.onNodeWithTag("price_field").performTextReplacement("8")
        rule.onNodeWithTag("dialog_confirm").performClick()
        assertEquals(listOf("selfie|8"), log)
    }
}
