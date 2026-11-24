package com.choomhub.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choomhub.app.core.designsystem.component.NightSkyBackground
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.feature.home.component.AffectionSection
import com.choomhub.app.feature.home.component.HomeHeader
import com.choomhub.app.feature.home.component.LatestSnapSection
import com.choomhub.app.feature.home.component.PartnerMoodCard
import com.choomhub.app.feature.home.component.PointsSection
import com.choomhub.app.feature.home.component.SampleDataNote
import com.choomhub.app.feature.home.component.SharedCatSection
import java.time.ZonedDateTime

/** Stateful entry point: wires the ViewModel to [HomeContent]. */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    showMessage: suspend (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.pendingNotice.collectAsStateWithLifecycle()

    LaunchedEffect(notice) {
        val kind = notice ?: return@LaunchedEffect
        showMessage(affectionNoticeText(kind))
        viewModel.onNoticeShown()
    }

    HomeContent(
        state = state,
        contentPadding = contentPadding,
        onAffection = viewModel::onAffectionTapped,
    )
}

internal fun affectionNoticeText(kind: AffectionKind): String {
    val what = when (kind) {
        AffectionKind.Hug -> "Hugs"
        AffectionKind.Kiss -> "Kisses"
        AffectionKind.HeadPats -> "Head pats"
        AffectionKind.TomatoDelivery -> "Tomato deliveries"
    }
    return "$what can't travel yet. Sending arrives once your shared space is connected."
}

/** Stateless home screen, previewable and testable without Hilt. */
@Composable
fun HomeContent(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onAffection: (AffectionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is HomeUiState.Ready -> {
            val spacing = ChoomHubTheme.spacing
            Box(modifier.fillMaxSize().testTag("screen_home"), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    // Cap width so the layout stays comfortable on tablets/foldables.
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("home_list"),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding() + spacing.lg,
                        bottom = contentPadding.calculateBottomPadding() + spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
                ) {
                    val gutter = Modifier.padding(horizontal = spacing.screenGutter)
                    item(key = "header") {
                        HomeHeader(
                            dateLabel = state.dateLabel,
                            greeting = state.greeting,
                            viewerName = state.viewerName,
                            isSampleData = state.isSampleData,
                            modifier = gutter,
                        )
                    }
                    item(key = "partner") { PartnerMoodCard(state.partner, gutter) }
                    item(key = "snap") { LatestSnapSection(state.snap, gutter) }
                    item(key = "affection") { AffectionSection(onAffection, gutter) }
                    item(key = "points") { PointsSection(state.points, gutter) }
                    item(key = "cat") { SharedCatSection(state.cat, gutter) }
                    if (state.isSampleData) {
                        item(key = "sample") { SampleDataNote(gutter) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14121B, heightDp = 1600)
@Composable
private fun HomeContentPreview() {
    ChoomHubTheme {
        NightSkyBackground {
            HomeContent(
                state = HomeUiState.Ready(
                    greeting = "Good evening",
                    dateLabel = "Tuesday, 24 November",
                    viewerName = "Choom",
                    partner = PartnerMoodUi("Moonie", Mood.Sleepy, "rewatching ghibli under three blankets", "12 min ago"),
                    snap = SnapUi("Moonie", "the sky looked like you today", ZonedDateTime.now(), "47 min ago", isNew = true),
                    points = PointBalances(goodBoy = 128, goodGirl = 142),
                    cat = SharedCatStatus(isAdopted = false, name = null),
                    isSampleData = true,
                ),
                contentPadding = PaddingValues(),
                onAffection = {},
            )
        }
    }
}
