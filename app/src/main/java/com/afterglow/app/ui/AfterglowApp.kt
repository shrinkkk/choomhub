package com.afterglow.app.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afterglow.app.core.designsystem.component.NightSkyBackground
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import com.afterglow.app.core.navigation.AfterglowBottomBar
import com.afterglow.app.core.navigation.TopLevelDestination
import com.afterglow.app.feature.activities.activitiesScreen
import com.afterglow.app.feature.home.HomeRoute
import com.afterglow.app.feature.home.homeScreen
import com.afterglow.app.feature.messages.messagesScreen
import com.afterglow.app.feature.settings.settingsScreen
import com.afterglow.app.feature.world.ourWorldScreen

/**
 * Root of the UI: night-sky backdrop, bottom navigation, snackbar host and
 * the navigation graph. Each feature contributes its own destination.
 */
@Composable
fun AfterglowApp(navController: NavHostController = rememberNavController()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val colors = AfterglowTheme.colors

    NightSkyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.textMain,
            bottomBar = {
                AfterglowBottomBar(
                    currentDestination = backStackEntry?.destination,
                    onNavigate = navController::navigateToTopLevel,
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        containerColor = colors.surfaceHighest,
                        contentColor = colors.textMain,
                    ) { Text(data.visuals.message) }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                homeScreen(contentPadding = padding, showMessage = { snackbarHostState.showSnackbar(it) })
                messagesScreen(contentPadding = padding)
                activitiesScreen(contentPadding = padding)
                ourWorldScreen(contentPadding = padding)
                settingsScreen(contentPadding = padding)
            }
        }
    }
}

/**
 * Standard bottom-nav behaviour: one copy of each tab on the back stack,
 * tab state saved and restored, back from any tab returns to Home.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
