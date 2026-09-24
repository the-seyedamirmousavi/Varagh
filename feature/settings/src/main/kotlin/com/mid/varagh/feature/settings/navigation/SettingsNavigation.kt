package com.mid.varagh.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.feature.settings.DeveloperInfoScreenRoute
import com.mid.varagh.feature.settings.SettingsScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object DeveloperInfoRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) = navigate(SettingsRoute, navOptions)

fun NavGraphBuilder.settingsScreens(navController: NavController) {
    composable<SettingsRoute> {
        SettingsScreenRoute(onOpenDeveloperInfo = { navController.navigate(DeveloperInfoRoute) })
    }
    composable<DeveloperInfoRoute> {
        DeveloperInfoScreenRoute(onBack = navController::popBackStack)
    }
}
