package com.mid.varagh.feature.reader.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mid.varagh.feature.reader.ReaderScreen
import kotlinx.serialization.Serializable

@Serializable
data class ReaderRoute(val bookId: Long)

fun NavController.navigateToReader(bookId: Long, navOptions: NavOptions? = null) =
    navigate(ReaderRoute(bookId), navOptions)

fun NavGraphBuilder.readerScreen(onBack: () -> Unit) {
    composable<ReaderRoute> { entry ->
        val route = entry.toRoute<ReaderRoute>()
        ReaderScreen(bookId = route.bookId, onBack = onBack)
    }
}
