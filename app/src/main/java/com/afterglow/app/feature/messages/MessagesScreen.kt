package com.afterglow.app.feature.messages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afterglow.app.core.designsystem.component.ComingSoonScreen
import com.afterglow.app.core.designsystem.illustration.LoveLetter
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
        whisper = "nothing here yet, but it's saving a spot",
        description = "Messages, voice notes and handwritten-style letters will live here, " +
            "kept on your phones first so they open even without signal.",
        planned = listOf("Text messages", "Voice notes", "Love notes", "Open-when letters", "Time capsules", "Reactions"),
        contentPadding = contentPadding,
        testTag = "screen_messages",
    ) { LoveLetter(Modifier.fillMaxSize()) }
}
