package com.choomhub.app.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    contentPadding: PaddingValues,
    showMessage: suspend (String) -> Unit,
    onSetMood: () -> Unit,
    onComposeSnap: (android.net.Uri) -> Unit,
    onOpenSnap: (String) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            contentPadding = contentPadding,
            showMessage = showMessage,
            onSetMood = onSetMood,
            onComposeSnap = onComposeSnap,
            onOpenSnap = onOpenSnap,
        )
    }
}
