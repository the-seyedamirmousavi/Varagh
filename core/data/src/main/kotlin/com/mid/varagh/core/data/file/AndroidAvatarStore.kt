package com.mid.varagh.core.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.AvatarStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.max

/** Stores a downscaled copy of the picked profile picture in app-private storage. */
class AndroidAvatarStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AvatarStore {

    override suspend fun saveAvatar(sourceUri: String): String = withContext(Dispatchers.IO) {
        val bitmap = decode(Uri.parse(sourceUri)) ?: throw VaraghException.InvalidFile()
        try {
            val out = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            out.absolutePath
        } finally {
            bitmap.recycle()
        }
    }

    override suspend fun deleteAvatar(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (file.parentFile?.canonicalPath == context.filesDir.canonicalPath && file.name.startsWith("avatar_")) {
                file.delete()
            }
        }
    }

    private fun decode(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation for us.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_SIZE) {
                    val scale = MAX_SIZE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }.getOrNull()

    private companion object {
        const val MAX_SIZE = 512
        const val QUALITY = 88
    }
}
