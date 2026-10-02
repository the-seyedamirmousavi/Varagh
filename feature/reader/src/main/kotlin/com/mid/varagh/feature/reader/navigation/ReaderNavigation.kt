package com.mid.varagh.feature.reader.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.feature.reader.ReaderScreenRoute
import kotlinx.serialization.Serializable

/** [startPage] (zero-based) opens a specific page, e.g. from a bookmark; -1 resumes where the reader left off. */
@Serializable
data class ReaderRoute(val bookId: Long, val startPage: Int = -1)

fun NavController.navigateToReader(bookId: Long, startPage: Int = -1, navOptions: NavOptions? = null) =
    navigate(ReaderRoute(bookId, startPage), navOptions)

fun NavGraphBuilder.readerScreen(onBack: () -> Unit) {
    composable<ReaderRoute> {
        ReaderScreenRoute(onBack = onBack)
    }
}
