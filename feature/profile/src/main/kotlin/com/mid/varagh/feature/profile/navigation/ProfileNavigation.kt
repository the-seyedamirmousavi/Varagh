package com.mid.varagh.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.feature.profile.ProfileScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

fun NavController.navigateToProfile(navOptions: NavOptions? = null) = navigate(ProfileRoute, navOptions)

/** [onSignIn] is only reachable when the remote backend is on (the account card is hidden otherwise). */
fun NavGraphBuilder.profileScreen(
    onOpenBook: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onSignIn: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreenRoute(onOpenBook = onOpenBook, onOpenLibrary = onOpenLibrary, onSignIn = onSignIn)
    }
}
