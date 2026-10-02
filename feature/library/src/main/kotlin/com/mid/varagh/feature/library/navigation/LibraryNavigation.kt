package com.mid.varagh.feature.library.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.feature.library.LibraryScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavController.navigateToLibrary(navOptions: NavOptions? = null) = navigate(LibraryRoute, navOptions)

fun NavGraphBuilder.libraryScreen(
    onOpenBook: (bookId: Long) -> Unit,
    onOpenBookDetails: (bookId: Long) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreenRoute(onOpenBook = onOpenBook, onOpenDetails = onOpenBookDetails)
    }
}
