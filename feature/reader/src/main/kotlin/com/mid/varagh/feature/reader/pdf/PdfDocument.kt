package com.mid.varagh.feature.reader.pdf

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.unit.IntSize
import com.mid.varagh.core.domain.VaraghException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * A PDF opened with the platform [PdfRenderer].
 *
 * - PdfRenderer allows only one open page at a time and is not thread-safe, so every call runs on a
 *   single-threaded dispatcher.
 * - Rendered pages live in an LRU cache bounded by bytes (a fraction of the heap); evicted bitmaps
 *   are simply dropped (never recycled) because Compose may still be drawing them, and the GC
 *   reclaims them once unreferenced.
 * - Queued renders are cancelled as soon as their page scrolls out of view.
 */
class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
) : Closeable {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
    // Prefetch failures (e.g. a damaged page) must never crash the reader.
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher + CoroutineExceptionHandler { _, _ -> })
    private val renderer = PdfRenderer(descriptor)
    private val sizes = arrayOfNulls<IntSize>(renderer.pageCount)

    @Volatile
    private var closed = false

    val pageCount: Int = renderer.pageCount

    private val cache = object : LruCache<CacheKey, Bitmap>(cacheBudgetBytes()) {
        override fun sizeOf(key: CacheKey, value: Bitmap): Int = value.allocationByteCount
    }

    /** Page size in PDF points (1/72 inch). */
    suspend fun pageSize(index: Int): IntSize = withContext(renderDispatcher) { sizeLocked(index) }

    /** Sizes of all pages (measured once, in the background). */
    suspend fun allPageSizes(): List<IntSize> = withContext(renderDispatcher) {
        List(pageCount) { index ->
            ensureActive()
            sizeLocked(index)
        }
    }

    fun cached(index: Int, widthPx: Int): Bitmap? = cache.get(CacheKey(index, bucket(widthPx)))

    /**
     * Renders page [index] [widthPx] wide (height follows the page's aspect ratio), or returns null
     * if the page cannot be rendered (document closed or page damaged).
     */
    suspend fun render(index: Int, widthPx: Int): Bitmap? {
        val key = CacheKey(index, bucket(widthPx))
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            cache.get(key) ?: try {
                renderLocked(key).also { cache.put(key, it) }
            } catch (e: IllegalStateException) {
                null
            } catch (e: IOException) {
                null
            }
        }
    }

    /** Warms the cache for the pages around [index]. */
    fun prefetch(index: Int, widthPx: Int, radius: Int = 1) {
        if (closed) return
        for (i in (index - radius)..(index + radius)) {
            if (i !in 0 until pageCount || i == index) continue
            val key = CacheKey(i, bucket(widthPx))
            if (cache.get(key) != null) continue
            scope.launch { if (!closed && cache.get(key) == null) cache.put(key, renderLocked(key)) }
        }
    }

    /** Called from onTrimMemory: frees cached pages under memory pressure. */
    fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) cache.evictAll() else cache.trimToSize(cache.maxSize() / 2)
    }

    override fun close() {
        if (closed) return
        closed = true
        cache.evictAll()
        // Close on the render thread, after any in-flight render has finished.
        scope.launch(NonCancellable) {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        }.invokeOnCompletion { scope.cancel() }
    }

    private fun sizeLocked(index: Int): IntSize {
        sizes[index]?.let { return it }
        check(!closed) { "Document is closed" }
        return renderer.openPage(index).use { IntSize(it.width, it.height) }.also { sizes[index] = it }
    }

    private fun renderLocked(key: CacheKey): Bitmap {
        check(!closed) { "Document is closed" }
        renderer.openPage(key.index).use { page ->
            sizes[key.index] = IntSize(page.width, page.height)
            val width = key.width
            val height = (width * page.height.toFloat() / page.width).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // PdfRenderer draws onto a transparent bitmap; paper must be white for the themes.
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    private data class CacheKey(val index: Int, val width: Int)

    class Opener @Inject constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** Opens [fileUri]. Throws [VaraghException.FileUnavailable] or [VaraghException.InvalidFile]. */
        suspend fun open(fileUri: String): PdfDocument = withContext(Dispatchers.IO) {
            val descriptor = try {
                context.contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")
            } catch (e: FileNotFoundException) {
                throw VaraghException.FileUnavailable(e)
            } catch (e: SecurityException) {
                throw VaraghException.FileUnavailable(e)
            } ?: throw VaraghException.FileUnavailable()
            try {
                PdfDocument(descriptor)
            } catch (e: IOException) {
                descriptor.close()
                throw VaraghException.InvalidFile(e)
            } catch (e: SecurityException) {
                descriptor.close()
                throw VaraghException.InvalidFile(e)
            }
        }
    }

    companion object {
        /** Largest bitmap width we render (2x zoom on a ~1440px screen). */
        const val MAX_RENDER_WIDTH = 2880
        private const val WIDTH_BUCKET = 64

        /** Snaps widths so tiny layout changes reuse cached bitmaps. */
        fun bucket(widthPx: Int): Int =
            (((widthPx.coerceIn(WIDTH_BUCKET, MAX_RENDER_WIDTH) + WIDTH_BUCKET - 1) / WIDTH_BUCKET) * WIDTH_BUCKET)

        /** 1/6 of the app heap, between 24 MB and 96 MB. */
        fun cacheBudgetBytes(maxMemory: Long = Runtime.getRuntime().maxMemory()): Int =
            (maxMemory / 6).coerceIn(24L shl 20, 96L shl 20).toInt()
    }
}
