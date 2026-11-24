package com.choomhub.app.feature.activities

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ComingSoonScreen
import com.choomhub.app.core.designsystem.component.CornerArt
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.TarotSpread
import kotlinx.serialization.Serializable

@Serializable
data object ActivitiesRoute

fun NavGraphBuilder.activitiesScreen(contentPadding: PaddingValues) {
    composable<ActivitiesRoute> { ActivitiesScreen(contentPadding) }
}

@Composable
fun ActivitiesScreen(contentPadding: PaddingValues) {
    ComingSoonScreen(
        title = "Activities",
        whisper = "a few minutes a day",
        description = "Daily check-ins, a question a day, logging what you got done, " +
            "and a few small games.",
        planned = listOf("Daily check-ins", "Daily questions", "Achievements", "Point shop", "Mini-games"),
        contentPadding = contentPadding,
        testTag = "screen_activities",
        cornerArt = CornerArt(GothicMotif.SkeletonKey, start = false, size = 64.dp, inset = 12.dp),
    ) { TarotSpread(Modifier.fillMaxSize()) }
}
