package com.afterglow.app.core.designsystem.component

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
import androidx.compose.ui.unit.dp
import com.afterglow.app.core.designsystem.theme.AfterglowTextStyles
import com.afterglow.app.core.designsystem.theme.AfterglowTheme

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
    illustration: @Composable () -> Unit,
) {
    val spacing = AfterglowTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
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
            color = AfterglowTheme.colors.textMain,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = whisper,
            style = AfterglowTextStyles.Handwritten,
            color = AfterglowTheme.colors.pink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.lg))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AfterglowTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(spacing.xl))
        AfterglowCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            tone = CardTone.Calm,
        ) {
            Text(
                text = "PLANNED HERE",
                style = AfterglowTextStyles.Eyebrow,
                color = AfterglowTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                planned.forEach { PillBadge(text = it, color = AfterglowTheme.colors.lavender) }
            }
        }
        Spacer(Modifier.height(spacing.xxl))
    }
}
