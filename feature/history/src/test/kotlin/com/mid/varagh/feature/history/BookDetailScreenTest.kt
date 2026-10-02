package com.mid.varagh.feature.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.ReadingProgress
import com.mid.varagh.core.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val book = Book(1, "شاهنامه", "فردوسی", "content://1", "h", 500, null, 0, null, null, ReadingStatus.READING, 3, null)

    private var rating: Int? = -1
    private var status: ReadingStatus? = null
    private var deleted = false
    private var readFrom = -2

    private fun show() {
        rule.setContent {
            VaraghTheme {
                BookDetailScreen(
                    state = BookDetailUiState(loading = false, book = BookWithProgress(book, ReadingProgress(1, 99, 0.2f, 0))),
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onRead = { readFrom = it },
                    onStatus = { status = it },
                    onRating = { rating = it },
                    onSaveDetails = { _, _ -> },
                    onDelete = { deleted = true },
                )
            }
        }
    }

    @Test
    fun ratingStatusAndRead() {
        show()
        rule.onNodeWithTag("detail_title").assertExists()
        rule.onNodeWithTag("star_5").performClick()
        assertEquals(5, rating)
        rule.onNodeWithTag("star_3").performClick()
        assertEquals("tapping the current rating clears it", null, rating)
        rule.onNodeWithTag("status_FINISHED").performScrollTo().performClick()
        assertEquals(ReadingStatus.FINISHED, status)
        rule.onNodeWithTag("read_button").performScrollTo().performClick()
        assertEquals(-1, readFrom)
    }

    @Test
    fun deleteAsksForConfirmation() {
        show()
        rule.onNodeWithTag("delete_book").performClick()
        assertTrue(!deleted)
        rule.onNodeWithTag("confirm_delete").performClick()
        assertTrue(deleted)
    }
}
