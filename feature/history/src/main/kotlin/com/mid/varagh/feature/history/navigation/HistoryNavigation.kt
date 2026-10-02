package com.mid.varagh.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.mid.varagh.feature.history.BookDetailScreenRoute
import com.mid.varagh.feature.history.HistoryScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

fun NavController.navigateToHistory(navOptions: NavOptions? = null) = navigate(HistoryRoute, navOptions)

fun NavController.navigateToBookDetail(bookId: Long, navOptions: NavOptions? = null) =
    navigate(BookDetailRoute(bookId), navOptions)

/**
 * @param onRead opens the reader; `startPage` is zero-based or -1 to resume.
 * @param readersOfBook optional slot under the book details (server-only "readers of this book").
 */
fun NavGraphBuilder.historyScreens(
    onOpenBookDetail: (Long) -> Unit,
    onRead: (bookId: Long, startPage: Int) -> Unit,
    onBack: () -> Unit,
    readersOfBook: (@androidx.compose.runtime.Composable (remoteBookId: String) -> Unit)? = null,
) {
    composable<HistoryRoute> {
        HistoryScreenRoute(onOpenBook = onOpenBookDetail)
    }
    composable<BookDetailRoute> {
        BookDetailScreenRoute(onBack = onBack, onRead = onRead, readersOfBook = readersOfBook)
    }
}
