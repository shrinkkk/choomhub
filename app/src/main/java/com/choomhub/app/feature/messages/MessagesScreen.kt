package com.choomhub.app.feature.messages

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
import com.choomhub.app.core.designsystem.illustration.SealedLetter
import kotlinx.serialization.Serializable

@Serializable
data object MessagesRoute

fun NavGraphBuilder.messagesScreen(contentPadding: PaddingValues) {
    composable<MessagesRoute> { MessagesScreen(contentPadding) }
}

@Composable
fun MessagesScreen(contentPadding: PaddingValues) {
    ComingSoonScreen(
        title = "Mailbox",
        whisper = "empty, for now",
        description = "Messages and letters will live here. They're stored on " +
            "the phone first, so they open without signal.",
        planned = listOf("Messages", "Letters", "Open-when letters", "Time capsules", "Reactions"),
        contentPadding = contentPadding,
        testTag = "screen_messages",
        cornerArt = CornerArt(GothicMotif.Moth, start = false, size = 60.dp, inset = 14.dp),
    ) { SealedLetter(Modifier.fillMaxSize()) }
}
