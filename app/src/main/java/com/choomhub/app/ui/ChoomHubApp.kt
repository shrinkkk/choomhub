package com.choomhub.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choomhub.app.core.designsystem.component.SkyBackground
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.navigation.ChoomHubBottomBar
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.core.navigation.TopLevelDestination
import com.choomhub.app.feature.account.PairingScreen
import com.choomhub.app.feature.account.SignInScreen
import com.choomhub.app.feature.activities.activitiesScreen
import com.choomhub.app.feature.pet.petHouseScreen
import com.choomhub.app.feature.home.HomeRoute
import com.choomhub.app.feature.home.homeScreen
import com.choomhub.app.feature.mood.MoodOptionsRoute
import com.choomhub.app.feature.snaps.SnapComposeRoute
import com.choomhub.app.feature.snaps.SnapViewerRoute
import com.choomhub.app.feature.snaps.snapComposeScreen
import com.choomhub.app.feature.snaps.snapViewerScreen
import com.choomhub.app.feature.mood.MoodRoute
import com.choomhub.app.feature.mood.moodOptionsScreen
import com.choomhub.app.feature.mood.moodScreen
import com.choomhub.app.feature.onboarding.OnboardingScreen
import com.choomhub.app.feature.settings.settingsScreen
import com.choomhub.app.feature.world.MeetupRoute
import com.choomhub.app.feature.world.WorldDoors
import com.choomhub.app.feature.world.atlas.AtlasRoute
import com.choomhub.app.feature.world.atlas.atlasScreen
import com.choomhub.app.feature.world.cabinet.CabinetRoute
import com.choomhub.app.feature.world.cabinet.cabinetScreens
import com.choomhub.app.feature.world.constellation.ConstellationRoute
import com.choomhub.app.feature.world.constellation.constellationScreen
import com.choomhub.app.feature.world.ourWorldScreen
import com.choomhub.app.feature.world.shelf.ShelfRoute
import com.choomhub.app.feature.world.shelf.shelfScreen
import com.choomhub.app.feature.focus.FocusRoomRoute
import com.choomhub.app.feature.focus.focusRoomScreen
import com.choomhub.app.feature.shop.PointsShopRoute
import com.choomhub.app.feature.bounty.BountyBoardRoute
import com.choomhub.app.feature.bounty.bountyBoardScreen
import com.choomhub.app.feature.shop.pointsShopScreen
import com.choomhub.app.core.navigation.PendingOpen
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * Root of the UI: night-sky backdrop, then sign-in, pairing, first-run naming
 * or the main app (bottom navigation, snackbar host and the navigation graph).
 */
@Composable
fun ChoomHubApp(appViewModel: AppViewModel = hiltViewModel()) {
    val gate by appViewModel.gate.collectAsStateWithLifecycle()
    SkyBackground {
        Crossfade(targetState = gate, label = "appGate") { current ->
            when (current) {
                AppGate.Loading -> Unit
                AppGate.SignIn -> SignInScreen()
                AppGate.Pairing -> PairingScreen()
                AppGate.Onboarding -> OnboardingScreen()
                AppGate.Main -> {
                    MainScaffold()
                    NotificationPermissionOnce(appViewModel)
                }
            }
        }
    }
}

/** Asks for the Android 13+ notification permission the first time the main app shows. */
@Composable
private fun NotificationPermissionOnce(appViewModel: AppViewModel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val shouldAsk by appViewModel.shouldAskForNotifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(shouldAsk) {
        if (!shouldAsk) return@LaunchedEffect
        appViewModel.onNotificationsAsked()
        if (!AppNotifications.canPost(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun MainScaffold(navController: NavHostController = rememberNavController()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val colors = ChoomHubTheme.colors
    // Opened from a focus room notification: go straight to the room.
    val openRequest by PendingOpen.request.collectAsStateWithLifecycle()
    LaunchedEffect(openRequest) {
        if (openRequest == PendingOpen.HOME) {
            PendingOpen.request.value = null
            navController.navigateToTopLevel(TopLevelDestination.Home)
        }
        if (openRequest == PendingOpen.FOCUS_ROOM) {
            PendingOpen.request.value = null
            navController.navigate(FocusRoomRoute) { launchSingleTop = true }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = colors.textMain,
        bottomBar = {
            // The focus room, the shop, the board and Our World's big rooms take the whole screen: no tab bar.
            val fullScreen = backStackEntry?.destination?.let {
                it.hasRoute<FocusRoomRoute>() || it.hasRoute<PointsShopRoute>() || it.hasRoute<BountyBoardRoute>() ||
                    it.hasRoute<ConstellationRoute>() || it.hasRoute<ShelfRoute>() || it.hasRoute<AtlasRoute>()
            } == true
            if (!fullScreen) {
                ChoomHubBottomBar(
                    currentDestination = backStackEntry?.destination,
                    onNavigate = navController::navigateToTopLevel,
                )
            }
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
            homeScreen(
                contentPadding = padding,
                showMessage = { snackbarHostState.showSnackbar(it) },
                onSetMood = { navController.navigate(MoodRoute) },
                onComposeSnap = { uri -> navController.navigate(SnapComposeRoute(uri.toString())) },
                onOpenSnap = { id -> navController.navigate(SnapViewerRoute(id)) },
            )
            snapComposeScreen(contentPadding = padding, onClose = { navController.popBackStack() })
            snapViewerScreen(contentPadding = padding)
            moodScreen(contentPadding = padding, onClose = { navController.popBackStack() })
            petHouseScreen(contentPadding = padding)
            activitiesScreen(
                contentPadding = padding,
                onOpenFocusRoom = { navController.navigate(FocusRoomRoute) { launchSingleTop = true } },
                onOpenShop = { navController.navigate(PointsShopRoute) { launchSingleTop = true } },
                onOpenBounties = { navController.navigate(BountyBoardRoute) { launchSingleTop = true } },
            )
            bountyBoardScreen(contentPadding = padding, onBack = { navController.popBackStack() })
            pointsShopScreen(contentPadding = padding, onBack = { navController.popBackStack() })
            focusRoomScreen(contentPadding = padding, onLeft = { navController.popBackStack() })
            val openMeetup: (String) -> Unit = { id -> navController.navigate(MeetupRoute(id)) { launchSingleTop = true } }
            val back: () -> Unit = { navController.popBackStack() }
            ourWorldScreen(
                contentPadding = padding,
                onOpenSnap = { id -> navController.navigate(SnapViewerRoute(id)) },
                doors = WorldDoors(
                    onCabinet = { navController.navigate(CabinetRoute) { launchSingleTop = true } },
                    onConstellation = { navController.navigate(ConstellationRoute) { launchSingleTop = true } },
                    onShelf = { navController.navigate(ShelfRoute) { launchSingleTop = true } },
                    onAtlas = { navController.navigate(AtlasRoute) { launchSingleTop = true } },
                ),
            )
            cabinetScreens(contentPadding = padding, onOpenMeetup = openMeetup, onBack = back)
            constellationScreen(contentPadding = padding, onOpenMeetup = openMeetup, onBack = back)
            shelfScreen(contentPadding = padding, onBack = back)
            atlasScreen(contentPadding = padding, onOpenMeetup = openMeetup, onBack = back)
            settingsScreen(contentPadding = padding, onEditMoods = { navController.navigate(MoodOptionsRoute) })
            moodOptionsScreen(contentPadding = padding)
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
