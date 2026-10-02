package com.mid.varagh.core.data.file

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BookFileRepository
import com.mid.varagh.core.domain.repository.PdfFileInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.roundToInt

/** [BookFileRepository] on the Storage Access Framework and the platform PdfRenderer. */
class AndroidBookFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : BookFileRepository {

    private val resolver: ContentResolver get() = context.contentResolver
    private val coverDir: File get() = File(context.filesDir, COVER_DIR).apply { mkdirs() }

    override suspend fun inspect(fileUri: String): PdfFileInfo = withContext(Dispatchers.IO) {
        val uri = Uri.parse(fileUri)
        persistAccess(uri)
        val (hash, size) = hash(uri)
        val pages = try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd -> PdfRenderer(pfd).use { it.pageCount } }
                ?: throw VaraghException.FileUnavailable()
        } catch (e: SecurityException) {
            // Password-protected PDFs throw SecurityException from PdfRenderer.
            throw VaraghException.InvalidFile(e)
        } catch (e: IOException) {
            throw VaraghException.InvalidFile(e)
        }
        if (pages <= 0) throw VaraghException.InvalidFile()
        PdfFileInfo(displayName = displayName(uri), fileHash = hash, pageCount = pages, sizeBytes = size)
    }

    override suspend fun renderCover(fileUri: String, fileHash: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openFileDescriptor(Uri.parse(fileUri), "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val height = (COVER_WIDTH * page.height.toFloat() / page.width).roundToInt().coerceIn(1, COVER_WIDTH * 2)
                        val bitmap = Bitmap.createBitmap(COVER_WIDTH, height, Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val out = File(coverDir, "$fileHash.jpg")
                            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, it) }
                            out.absolutePath
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }.getOrNull()
    }

    override suspend fun deleteCover(coverPath: String?) {
        if (coverPath == null) return
        withContext(Dispatchers.IO) {
            val file = File(coverPath)
            // Only ever delete inside our own cover directory.
            if (file.parentFile?.canonicalPath == coverDir.canonicalPath) file.delete()
        }
    }

    override suspend fun releaseAccess(fileUri: String) = withContext(Dispatchers.IO) {
        runCatching {
            resolver.releasePersistableUriPermission(Uri.parse(fileUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Unit
    }

    override suspend fun isReadable(fileUri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { resolver.openFileDescriptor(Uri.parse(fileUri), "r")?.use { true } ?: false }.getOrDefault(false)
    }

    private fun persistAccess(uri: Uri) {
        // Not every provider grants persistable permissions; reading still works for this session.
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun hash(uri: Uri): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        try {
            resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    size += read
                }
            } ?: throw VaraghException.FileUnavailable()
        } catch (e: FileNotFoundException) {
            throw VaraghException.FileUnavailable(e)
        } catch (e: SecurityException) {
            throw VaraghException.FileUnavailable(e)
        }
        return digest.digest().joinToString("") { "%02x".format(it) } to size
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private companion object {
        const val COVER_DIR = "covers"
        const val COVER_WIDTH = 360
        const val COVER_QUALITY = 85
        const val BUFFER_SIZE = 64 * 1024
    }
}
