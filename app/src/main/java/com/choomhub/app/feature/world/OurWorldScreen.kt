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
        whisper = "a cozy room we build together",
        description = "A shared room to decorate, photos pinned to the wall, the cat's favourite corner, " +
            "a tomato garden and the moments worth keeping.",
        planned = listOf("Shared room", "Photo wall", "Memories", "Milestones", "Tomato garden", "Soundtrack"),
        contentPadding = contentPadding,
        testTag = "screen_our_world",
    ) { PolaroidWall(Modifier.fillMaxSize()) }
}
