package com.choomhub.app.feature.world

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.ComingSoonScreen
import com.choomhub.app.core.designsystem.illustration.PolaroidWall
import kotlinx.serialization.Serializable

@Serializable
data object OurWorldRoute

fun NavGraphBuilder.ourWorldScreen(contentPadding: PaddingValues) {
    composable<OurWorldRoute> { OurWorldScreen(contentPadding) }
}

@Composable
fun OurWorldScreen(contentPadding: PaddingValues) {
    ComingSoonScreen(
        title = "Our World",
        whisper = "a shared room, slowly filling up",
        description = "A room you both decorate, photos on the wall, the cat once it moves in, " +
            "a tomato garden, and the things worth keeping.",
        planned = listOf("Shared room", "Photo wall", "The cat", "Memories", "Milestones", "Tomato garden", "Soundtrack"),
        contentPadding = contentPadding,
        testTag = "screen_our_world",
    ) { PolaroidWall(Modifier.fillMaxSize()) }
}
