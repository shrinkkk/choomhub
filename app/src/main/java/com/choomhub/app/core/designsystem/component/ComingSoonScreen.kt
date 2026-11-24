package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/**
 * Shared layout for destinations that exist in navigation but whose feature
 * has not been built yet. Says so plainly and lists what is planned, so the
 * app never pretends something works.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComingSoonScreen(
    title: String,
    whisper: String,
    description: String,
    planned: List<String>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    testTag: String = "screen_$title",
    /** A small fixed piece of art in a corner of the screen (it doesn't scroll). */
    cornerArt: CornerArt? = null,
    illustration: @Composable () -> Unit,
) {
    val spacing = ChoomHubTheme.spacing
    Box(modifier.fillMaxSize().testTag(testTag)) {
        if (cornerArt != null) {
            GothicMotifArt(
                cornerArt.motif,
                Modifier
                    .align(if (cornerArt.start) Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = contentPadding.calculateTopPadding() + cornerArt.inset, start = cornerArt.inset, end = cornerArt.inset)
                    .size(cornerArt.size),
            )
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = spacing.screenGutter, vertical = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.xl))
        Box(Modifier.size(width = 200.dp, height = 150.dp), contentAlignment = Alignment.Center) { illustration() }
        Spacer(Modifier.height(spacing.xl))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = ChoomHubTheme.colors.textMain,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = whisper,
            style = ChoomHubTextStyles.Handwritten,
            color = ChoomHubTheme.colors.pink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.lg))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = ChoomHubTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(spacing.xl))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            tone = CardTone.Calm,
        ) {
            Text(
                text = "PLANNED HERE",
                style = ChoomHubTextStyles.Eyebrow,
                color = ChoomHubTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                planned.forEach { PillBadge(text = it, color = ChoomHubTheme.colors.lavender) }
            }
        }
        Spacer(Modifier.height(spacing.xxl))
    }
    }
}

/** Which motif sits in which top corner, how big, and how far from the edges. */
data class CornerArt(val motif: GothicMotif, val start: Boolean, val size: Dp, val inset: Dp)
