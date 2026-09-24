package com.mid.varagh.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.feature.history.navigation.historyScreen
import com.mid.varagh.feature.library.navigation.LibraryRoute
import com.mid.varagh.feature.library.navigation.libraryScreen
import com.mid.varagh.feature.profile.navigation.profileScreen
import com.mid.varagh.feature.reader.navigation.navigateToReader
import com.mid.varagh.feature.reader.navigation.readerScreen
import com.mid.varagh.feature.settings.navigation.settingsScreens
import com.mid.varagh.feature.social.navigation.socialScreens
import com.mid.varagh.navigation.TopLevelDestination

@Composable
fun VaraghApp(
    featureFlags: FeatureFlags,
    navController: NavHostController = rememberNavController(),
) {
    val destinations = remember(featureFlags) { TopLevelDestination.visible(featureFlags) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The reader is immersive: no bottom bar.
    val showBottomBar = currentDestination == null ||
        destinations.any { currentDestination.isOnTopLevel(it) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                VaraghBottomBar(
                    destinations = destinations,
                    currentDestination = currentDestination,
                    onNavigate = { navController.navigateToTopLevel(it) },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            NavHost(navController = navController, startDestination = LibraryRoute) {
                libraryScreen(onOpenBook = navController::navigateToReader)
                readerScreen(onBack = navController::popBackStack)
                historyScreen()
                profileScreen()
                socialScreens(featureFlags)
                settingsScreens(navController)
            }
        }
    }
}

@Composable
internal fun VaraghBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.testTag("bottom_bar")) {
        destinations.forEach { destination ->
            val selected = currentDestination.isOnTopLevel(destination)
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${destination.name.lowercase()}"),
            )
        }
    }
}

private fun NavDestination?.isOnTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(
        destination.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}
