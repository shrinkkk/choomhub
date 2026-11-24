package com.choomhub.app.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

@Composable
fun ChoomHubBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    Column(modifier) {
        HorizontalDivider(color = colors.lavender.copy(alpha = 0.08f))
        NavigationBar(
            containerColor = colors.nightDeep,
            tonalElevation = 0.dp,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isOn(destination)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination) },
                    icon = { Icon(destination.icon(), contentDescription = null) },
                    label = { Text(destination.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    modifier = Modifier.testTag(destination.testTag),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.purple,
                        selectedTextColor = colors.lavender,
                        indicatorColor = colors.purple.copy(alpha = 0.16f),
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary,
                    ),
                )
            }
        }
    }
}

internal fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
