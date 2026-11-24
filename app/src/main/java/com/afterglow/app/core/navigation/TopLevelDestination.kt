package com.afterglow.app.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.afterglow.app.core.designsystem.icon.AfterglowIcons
import com.afterglow.app.feature.activities.ActivitiesRoute
import com.afterglow.app.feature.home.HomeRoute
import com.afterglow.app.feature.messages.MessagesRoute
import com.afterglow.app.feature.settings.SettingsRoute
import com.afterglow.app.feature.world.OurWorldRoute

/** Destinations reachable from the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val label: String,
    val route: Any,
    val icon: () -> ImageVector,
) {
    Home("Home", HomeRoute, { AfterglowIcons.Home }),
    Messages("Messages", MessagesRoute, { AfterglowIcons.Mailbox }),
    Activities("Activities", ActivitiesRoute, { AfterglowIcons.Sparkles }),
    OurWorld("Our World", OurWorldRoute, { AfterglowIcons.Moon }),
    Settings("Settings", SettingsRoute, { AfterglowIcons.Sliders }),
    ;

    val testTag: String get() = "nav_$name"
}
