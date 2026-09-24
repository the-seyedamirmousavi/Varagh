package com.mid.varagh.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.mid.varagh.R
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.feature.history.navigation.HistoryRoute
import com.mid.varagh.feature.library.navigation.LibraryRoute
import com.mid.varagh.feature.profile.navigation.ProfileRoute
import com.mid.varagh.feature.settings.navigation.SettingsRoute
import com.mid.varagh.feature.social.navigation.SocialRoute
import kotlin.reflect.KClass

/** Bottom-bar destinations. [requiresRemote] ones are dropped when the backend switch is off. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val labelRes: Int,
    val requiresRemote: Boolean = false,
) {
    LIBRARY(LibraryRoute, LibraryRoute::class, VaraghIcons.Library, VaraghIcons.LibraryOutlined, R.string.nav_library),
    HISTORY(HistoryRoute, HistoryRoute::class, VaraghIcons.History, VaraghIcons.HistoryOutlined, R.string.nav_history),
    SOCIAL(
        SocialRoute, SocialRoute::class, VaraghIcons.Social, VaraghIcons.SocialOutlined, R.string.nav_social,
        requiresRemote = true,
    ),
    PROFILE(ProfileRoute, ProfileRoute::class, VaraghIcons.Profile, VaraghIcons.ProfileOutlined, R.string.nav_profile),
    SETTINGS(SettingsRoute, SettingsRoute::class, VaraghIcons.Settings, VaraghIcons.SettingsOutlined, R.string.nav_settings),
    ;

    companion object {
        fun visible(flags: FeatureFlags): List<TopLevelDestination> =
            entries.filter { !it.requiresRemote || flags.isSocialEnabled }
    }
}
