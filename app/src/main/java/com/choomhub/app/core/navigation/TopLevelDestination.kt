package com.choomhub.app.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.choomhub.app.core.designsystem.icon.ChoomHubIcons
import com.choomhub.app.feature.activities.ActivitiesRoute
import com.choomhub.app.feature.home.HomeRoute
import com.choomhub.app.feature.messages.MessagesRoute
import com.choomhub.app.feature.settings.SettingsRoute
import com.choomhub.app.feature.world.OurWorldRoute

/** Destinations reachable from the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val label: String,
    val route: Any,
    val icon: () -> ImageVector,
) {
    Home("Home", HomeRoute, { ChoomHubIcons.Home }),
    Messages("Messages", MessagesRoute, { ChoomHubIcons.Mailbox }),
    Activities("Activities", ActivitiesRoute, { ChoomHubIcons.Sparkles }),
    OurWorld("Our World", OurWorldRoute, { ChoomHubIcons.Moon }),
    Settings("Settings", SettingsRoute, { ChoomHubIcons.Sliders }),
    ;

    val testTag: String get() = "nav_$name"
}
