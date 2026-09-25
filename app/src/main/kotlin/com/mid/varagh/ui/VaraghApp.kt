package com.mid.varagh.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.mid.varagh.core.designsystem.motion.VaraghMotion
import com.mid.varagh.core.designsystem.theme.VaraghDimens
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
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
            NavHost(
                navController = navController,
                startDestination = LibraryRoute,
                enterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.97f) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 1.03f) },
                popExitTransition = { fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.97f) },
            ) {
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
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSurface,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Floating white rounded bar over the grey canvas, as in FitSho.
    Surface(
        modifier = modifier
            .testTag("bottom_bar")
            .navigationBarsPadding()
            .padding(horizontal = VaraghSpacing.ScreenGutter, vertical = VaraghSpacing.Small),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = VaraghDimens.BarElevation,
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0),
        ) {
            destinations.forEach { destination ->
                val selected = currentDestination.isOnTopLevel(destination)
                val label = stringResource(destination.labelRes)
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = VaraghMotion.Bouncy,
                    label = "navIcon",
                )
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        )
                    },
                    label = {
                        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    colors = colors,
                    modifier = Modifier.testTag("nav_${destination.name.lowercase()}"),
                )
            }
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
