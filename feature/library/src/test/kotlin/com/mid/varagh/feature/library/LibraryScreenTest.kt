package com.mid.varagh.feature.library

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.ReadingProgress
import com.mid.varagh.core.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fa")
class LibraryScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun book(id: Long, title: String) = BookWithProgress(
        Book(id, title, "نویسنده", "content://$id", "h$id", 100, null, 0, null, null, ReadingStatus.READING, null, null),
        ReadingProgress(id, 10, 0.1f, 0),
    )

    private var added = 0
    private var filter: ReadingStatus? = ReadingStatus.FINISHED
    private var toggled = 0
    private var opened = -1L
    private var details = -1L

    private fun show(state: LibraryUiState) {
        rule.setContent {
            VaraghTheme {
                LibraryScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onAddBook = { added++ },
                    onOpenBook = { opened = it },
                    onOpenDetails = { details = it },
                    onSearchChange = {},
                    onSearchActiveChange = {},
                    onStatusFilter = { filter = it },
                    onSortChange = { _: LibrarySort -> },
                    onToggleLayout = { toggled++ },
                )
            }
        }
    }

    @Test
    fun emptyLibrary_showsHeroWithCallToAction_andNoFab() {
        show(LibraryUiState(loading = false, libraryEmpty = true))
        rule.onNodeWithTag("library_hero").assertExists()
        rule.onNodeWithTag("add_book_fab").assertDoesNotExist()
        rule.onNodeWithText("افزودن اولین کتاب").performClick()
        assertEquals(1, added)
    }

    @Test
    fun grid_showsBooks_filtersAndOpens() {
        show(LibraryUiState(loading = false, books = listOf(book(1, "شاهنامه"), book(2, "بوستان"))))
        rule.onNodeWithTag("book_grid").assertExists()
        rule.onNodeWithText("شاهنامه").assertExists()
        rule.onNodeWithTag("book_1").performClick()
        assertEquals(1L, opened)
        rule.onNodeWithTag("filter_READING").performClick()
        assertEquals(ReadingStatus.READING, filter)
        rule.onNodeWithTag("filter_ALL").performClick()
        assertEquals(null, filter)
        rule.onNodeWithTag("layout_toggle").performClick()
        assertEquals(1, toggled)
        rule.onNodeWithTag("add_book_fab").performClick()
        assertEquals(1, added)
    }

    @Test
    fun listLayout_showsDetailsButton() {
        show(LibraryUiState(loading = false, grid = false, books = listOf(book(3, "گلستان"))))
        rule.onNodeWithTag("book_list").assertExists()
        rule.onNodeWithTag("book_3").assertExists()
        rule.onNodeWithContentDescription("جزئیات کتاب").performClick()
        assertEquals(3L, details)
    }

    @Test
    fun filteredToNothing_showsNoResults() {
        show(LibraryUiState(loading = false, books = emptyList(), statusFilter = ReadingStatus.ABANDONED))
        rule.onNodeWithText("کتابی پیدا نشد").assertExists()
        rule.onNodeWithTag("library_hero").assertDoesNotExist()
    }
}
