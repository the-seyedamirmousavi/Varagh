package com.mid.varagh.feature.reader

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.feature.reader.pdf.PdfDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fa")
class ReaderTest {

    @get:Rule
    val rule = createComposeRule()

    private var located = 0
    private var retried = 0

    private fun showError(error: Throwable) {
        rule.setContent {
            VaraghTheme {
                ReaderScreen(
                    state = ReaderUiState(load = ReaderLoadState.Failed(error), title = "کتاب"),
                    preferences = UserPreferences(),
                    document = null,
                    onBack = {},
                    onRetry = { retried++ },
                    onLocateFile = { located++ },
                    onPageChanged = {},
                    onToggleBookmark = {},
                    onSaveNote = {},
                    onEditBookmark = { _, _ -> },
                    onDeleteBookmark = {},
                    onPreferencesChange = {},
                )
            }
        }
    }

    @Test
    fun missingFile_offersToLocateIt() {
        showError(VaraghException.FileUnavailable())
        rule.onNodeWithTag("reader_error").assertExists()
        rule.onNodeWithText("پیدا کردن فایل").performClick()
        assertEquals(1, located)
    }

    @Test
    fun brokenFile_offersRetry() {
        showError(VaraghException.InvalidFile())
        rule.onNodeWithText("تلاش دوباره").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun persianDigitsAreAccepted_forGoToPage() {
        assertEquals(125, "۱۲۵".toPersianSafeInt())
        assertEquals(42, " 42 ".toPersianSafeInt())
        assertEquals(7, "٧".toPersianSafeInt())
        assertNull("abc".toPersianSafeInt())
    }

    @Test
    fun renderWidthsAreBucketedAndBounded() {
        assertEquals(1088, PdfDocument.bucket(1080))
        assertEquals(1088, PdfDocument.bucket(1088))
        assertEquals(PdfDocument.MAX_RENDER_WIDTH, PdfDocument.bucket(10_000))
        assertEquals(64, PdfDocument.bucket(1))
    }

    @Test
    fun cacheBudgetIsAFractionOfTheHeap_withinBounds() {
        assertEquals(24 shl 20, PdfDocument.cacheBudgetBytes(64L shl 20))
        assertEquals(256L * 1024 * 1024 / 6, PdfDocument.cacheBudgetBytes(256L shl 20).toLong())
        assertEquals(96 shl 20, PdfDocument.cacheBudgetBytes(2048L shl 20))
    }

    @Test
    fun zoomStateClampsScaleAndPan() {
        val zoom = ZoomState(allowVerticalPan = false).apply { size = androidx.compose.ui.unit.IntSize(1000, 2000) }
        zoom.transform(10f, androidx.compose.ui.geometry.Offset(5000f, 5000f))
        assertEquals(ZoomState.MAX_SCALE, zoom.scale)
        assertEquals((ZoomState.MAX_SCALE - 1) * 500f, zoom.offset.x)
        assertEquals("vertical movement is left to scrolling", 0f, zoom.offset.y)
        zoom.transform(0.01f, androidx.compose.ui.geometry.Offset.Zero)
        assertTrue(!zoom.isZoomed)
        assertEquals(androidx.compose.ui.geometry.Offset.Zero, zoom.offset)
    }
}
