package com.choomhub.app.feature.bounty

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.domain.model.Bounty
import com.choomhub.app.domain.model.BountyDue
import com.choomhub.app.domain.model.BountyRules
import com.choomhub.app.domain.model.BountyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi")
class BountyBoardTest {
    @get:Rule val rule = createComposeRule()

    private val now = Instant.parse("2026-11-27T10:00:00Z")
    private var board by mutableStateOf(listOf<Bounty>())
    private val posted = mutableListOf<Triple<String, Int, BountyDue>>()
    private val paid = mutableListOf<String>()

    private fun show() {
        rule.setContent {
            ChoomHubTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides false) {
                    BountyBoardContent(
                        ui = BountyBoardUi(true, board, "Shri", "Jas"),
                        contentPadding = PaddingValues(),
                        lapsed = { BountyRules.lapsed(it, now, ZoneOffset.UTC) },
                        onBack = {},
                        onPost = { t, r, d -> posted += Triple(t, r, d); true },
                        onClaim = { b -> board = board.map { if (it.id == b.id) it.copy(status = BountyStatus.Claimed) else it } },
                        onNotYet = {},
                        onPay = { b -> paid += b.id },
                        onTakeDown = {},
                    )
                }
            }
        }
    }

    @Test
    fun theirJobsForYouAndYoursToPay() {
        board = listOf(
            Bounty("a", "Drink 2 L of water", 3, BountyDue.Tonight, byMe = false, status = BountyStatus.Open, postedAt = now),
            Bounty("b", "Finish the lab report", 8, BountyDue.Week, byMe = true, status = BountyStatus.Claimed, postedAt = now),
        )
        show()
        rule.onNodeWithTag("bounty_done_a").performClick()
        assertEquals(BountyStatus.Claimed, board.first { it.id == "a" }.status)
        rule.onNodeWithTag("bounty_tab_ForThem").performClick()
        rule.onNodeWithTag("bounty_pay_b").performClick()
        rule.onNodeWithText("Pay 8 points").performClick()
        assertEquals(listOf("b"), paid)
    }

    @Test
    fun postingOne() {
        show()
        rule.onNodeWithTag("bounty_new").performScrollTo().performClick()
        rule.onNodeWithTag("bounty_task").performTextInput("Call your mum")
        rule.onNodeWithText("This week").performClick()
        rule.onNodeWithText("Pin it up").performClick()
        assertEquals(listOf(Triple("Call your mum", 3, BountyDue.Week)), posted)
    }

    @Test
    fun tonightLapsesAtMidnightAndPaidOnesComeDownAfterAFewDays() {
        val b = Bounty("a", "x", 1, BountyDue.Tonight, byMe = true, status = BountyStatus.Open, postedAt = Instant.parse("2026-11-27T20:00:00Z"))
        assertFalse(BountyRules.lapsed(b, Instant.parse("2026-11-27T23:59:00Z"), ZoneOffset.UTC))
        assertTrue(BountyRules.lapsed(b, Instant.parse("2026-11-28T00:01:00Z"), ZoneOffset.UTC))
        assertFalse(BountyRules.lapsed(b.copy(due = BountyDue.Whenever), Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC))
        val old = b.copy(status = BountyStatus.Paid, paidAt = now.minusSeconds(4 * 86400))
        assertEquals(emptyList<Bounty>(), BountyRules.shown(listOf(old), now))
    }
}
