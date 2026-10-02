package com.mid.varagh.feature.social.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.feature.social.DiscoverScreenRoute
import com.mid.varagh.feature.social.FeedScreenRoute
import com.mid.varagh.feature.social.LoginScreenRoute
import com.mid.varagh.feature.social.PublicProfileScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object SocialRoute

@Serializable
data object DiscoverRoute

@Serializable
data class PublicProfileRoute(val username: String)

@Serializable
data object LoginRoute

fun NavController.navigateToSocial(navOptions: NavOptions? = null) = navigate(SocialRoute, navOptions)

fun NavController.navigateToLogin() = navigate(LoginRoute)

fun NavController.navigateToPublicProfile(username: String) = navigate(PublicProfileRoute(username))

/**
 * Registers the social and account destinations only when the remote backend is on, so they can't
 * be reached (not even by a stray deep link) in the offline build.
 */
fun NavGraphBuilder.socialScreens(featureFlags: FeatureFlags, navController: NavController) {
    if (!featureFlags.isSocialEnabled) return
    composable<SocialRoute> {
        FeedScreenRoute(
            onOpenReader = navController::navigateToPublicProfile,
            onDiscover = { navController.navigate(DiscoverRoute) },
            onSignIn = navController::navigateToLogin,
        )
    }
    composable<DiscoverRoute> {
        DiscoverScreenRoute(onBack = navController::popBackStack, onOpenReader = navController::navigateToPublicProfile)
    }
    composable<PublicProfileRoute> {
        PublicProfileScreenRoute(onBack = navController::popBackStack)
    }
    composable<LoginRoute> {
        LoginScreenRoute(onDone = { navController.popBackStack() })
    }
}
