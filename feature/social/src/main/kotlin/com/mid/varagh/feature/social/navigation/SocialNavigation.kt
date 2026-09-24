package com.mid.varagh.feature.social.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.feature.social.SocialFeedScreen
import kotlinx.serialization.Serializable

@Serializable
data object SocialRoute

fun NavController.navigateToSocial(navOptions: NavOptions? = null) = navigate(SocialRoute, navOptions)

/**
 * Registers the social destinations only when the remote backend is on, so they can't be reached
 * (not even by a stray deep link) in the offline build.
 */
fun NavGraphBuilder.socialScreens(featureFlags: FeatureFlags) {
    if (!featureFlags.isSocialEnabled) return
    composable<SocialRoute> {
        SocialFeedScreen()
    }
}
