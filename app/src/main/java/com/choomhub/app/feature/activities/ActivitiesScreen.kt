package com.choomhub.app.feature.activities

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ComingSoonScreen
import com.choomhub.app.core.designsystem.illustration.SparkleCluster
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
        whisper = "little rituals, a few minutes a day",
        description = "Daily check-ins, questions for two, productivity wins worth celebrating " +
            "and small games to play together.",
        planned = listOf("Daily check-ins", "Daily questions", "Achievements", "Point shop", "Mini-games"),
        contentPadding = contentPadding,
        testTag = "screen_activities",
    ) { SparkleCluster(Modifier.fillMaxSize()) }
}
