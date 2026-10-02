package com.mid.varagh.feature.reader

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.feature.reader.pdf.PdfDocument
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.pdf.PdfDocument as PdfWriter

/**
 * Performance/memory check on a generated 500-page PDF (run on a device:
 * `./gradlew :feature:reader:connectedDebugAndroidTest`). Timings are logged under the
 * "VaraghPerf" tag; thresholds are generous so mid-range phones pass while real regressions fail.
 */
@RunWith(AndroidJUnit4::class)
class LargePdfPerformanceTest {

    @Test
    fun opensAndMeasures500Pages_quickly() = runBlocking {
        val start = SystemClock.elapsedRealtime()
        PdfDocument.Opener(context).open(uri).use { doc ->
            assertEquals(PAGES, doc.pageCount)
            val sizes = doc.allPageSizes()
            val elapsed = SystemClock.elapsedRealtime() - start
            Log.i(TAG, "open + measure $PAGES pages: $elapsed ms")
            assertEquals(PAGES, sizes.size)
            assertTrue("opening took $elapsed ms", elapsed < 5_000)
        }
    }

    @Test
    fun scrollingThroughTheBook_staysFastAndWithinTheCacheBudget() = runBlocking {
        PdfDocument.Opener(context).open(uri).use { doc ->
            val width = 1080
            val times = mutableListOf<Long>()
            // Simulate scrolling: render every page in order with prefetch, like the reader does.
            for (page in 0 until PAGES step 5) {
                val t = SystemClock.elapsedRealtime()
                assertNotNull(doc.render(page, width))
                times += SystemClock.elapsedRealtime() - t
                doc.prefetch(page, width, radius = 2)
                assertTrue("cache ${doc.cacheBytes} exceeds ${doc.cacheLimitBytes}", doc.cacheBytes <= doc.cacheLimitBytes)
            }
            val sorted = times.sorted()
            val median = sorted[sorted.size / 2]
            val p95 = sorted[(sorted.size * 95) / 100]
            Log.i(TAG, "render ${times.size} pages @${width}px: median=$median ms p95=$p95 ms, cache=${doc.cacheBytes / 1024} KB")
            assertTrue("median render $median ms", median < 120)
            assertTrue("p95 render $p95 ms", p95 < 400)
        }
    }

    @Test
    fun reopeningAtTheLastPage_isImmediate() = runBlocking {
        PdfDocument.Opener(context).open(uri).use { doc ->
            val t = SystemClock.elapsedRealtime()
            assertNotNull(doc.render(PAGES - 1, 1080))
            val elapsed = SystemClock.elapsedRealtime() - t
            Log.i(TAG, "first render of the last page: $elapsed ms")
            assertTrue(elapsed < 500)
        }
    }

    companion object {
        private const val TAG = "VaraghPerf"
        private const val PAGES = 500
        private val context: Context get() = ApplicationProvider.getApplicationContext()
        private lateinit var file: File
        private val uri: String get() = Uri.fromFile(file).toString()

        @BeforeClass
        @JvmStatic
        fun createBook() {
            file = File(context.cacheDir, "perf-500.pdf")
            val writer = PdfWriter()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 11f
            }
            for (i in 0 until PAGES) {
                val page = writer.startPage(PdfWriter.PageInfo.Builder(420, 595, i + 1).create()) // A5 in points
                val canvas = page.canvas
                canvas.drawText("Page ${i + 1}", 40f, 40f, paint.apply { textSize = 18f })
                paint.textSize = 11f
                for (line in 0 until 45) {
                    canvas.drawText("Line $line — the quick brown fox jumps over the lazy dog. ۱۲۳", 40f, 70f + line * 11.5f, paint)
                }
                writer.finishPage(page)
            }
            file.outputStream().use { writer.writeTo(it) }
            writer.close()
        }

        @AfterClass
        @JvmStatic
        fun deleteBook() {
            file.delete()
        }
    }
}
