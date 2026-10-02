package com.mid.varagh.feature.profile.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.mid.varagh.core.designsystem.R as DesignR
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

/** Already-localized text for the card (resolved in the UI so the in-app language is respected). */
data class ShareCardText(
    val eyebrow: String,
    val title: String,
    val author: String?,
    val progressLabel: String,
    val readerName: String?,
    val appName: String,
)

/** Draws the "I'm reading X" image and exposes it through the app's FileProvider. Works offline. */
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun render(text: ShareCardText, coverPath: String?, progress: Float): Uri = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            draw(Canvas(bitmap), text, coverPath, progress.coerceIn(0f, 1f))
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            val file = File(dir, "varagh-reading.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
        } finally {
            bitmap.recycle()
        }
    }

    private fun draw(canvas: Canvas, text: ShareCardText, coverPath: String?, progress: Float) {
        val regular = font(DesignR.font.vazirmatn_regular)
        val bold = font(DesignR.font.vazirmatn_bold)
        canvas.drawColor(BACKGROUND)

        // Eyebrow + app name
        var y = 120f
        y = drawText(canvas, text.eyebrow, bold, 48f, MUTED, y) + 40f

        // Cover with rounded corners and a soft shadow
        val coverWidth = 520f
        val coverHeight = coverWidth / COVER_ASPECT
        val left = (WIDTH - coverWidth) / 2f
        val rect = RectF(left, y, left + coverWidth, y + coverHeight)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 90
            setShadowLayer(40f, 0f, 16f, Color.BLACK)
        }
        canvas.drawRoundRect(rect, CORNER, CORNER, shadow)
        val clip = Path().apply { addRoundRect(rect, CORNER, CORNER, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        val cover = coverPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        if (cover != null) {
            canvas.drawBitmap(cover, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            cover.recycle()
        } else {
            canvas.drawColor(PLACEHOLDER)
            drawTextIn(canvas, text.title, bold, 44f, Color.WHITE, rect)
        }
        canvas.restore()
        y = rect.bottom + 64f

        // Title / author
        y = drawText(canvas, text.title, bold, 64f, Color.WHITE, y, maxLines = 3) + 16f
        text.author?.takeIf { it.isNotBlank() }?.let { y = drawText(canvas, it, regular, 44f, MUTED, y, maxLines = 2) + 16f }

        // Progress bar
        y += 24f
        val barLeft = 180f
        val barRight = WIDTH - 180f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TRACK }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        canvas.drawRoundRect(RectF(barLeft, y, barRight, y + 18f), 9f, 9f, track)
        if (progress > 0f) {
            // RTL-friendly: fill from the right when the label is Persian.
            val rtl = text.progressLabel.any { Character.getDirectionality(it) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC }
            val fillWidth = (barRight - barLeft) * progress
            val fillRect = if (rtl) RectF(barRight - fillWidth, y, barRight, y + 18f) else RectF(barLeft, y, barLeft + fillWidth, y + 18f)
            canvas.drawRoundRect(fillRect, 9f, 9f, fill)
        }
        y += 18f + 24f
        drawText(canvas, text.progressLabel, regular, 40f, MUTED, y)

        // Footer
        val footer = listOfNotNull(text.readerName?.takeIf { it.isNotBlank() }, text.appName).joinToString(" · ")
        drawText(canvas, footer, bold, 40f, MUTED, HEIGHT - 120f)
    }

    /** Centred multi-line text starting at [top]; returns the bottom y. */
    private fun drawText(
        canvas: Canvas,
        value: String,
        typeface: Typeface,
        size: Float,
        color: Int,
        top: Float,
        maxLines: Int = 1,
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
        }
        val width = WIDTH - 2 * SIDE_MARGIN
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(SIDE_MARGIN.toFloat(), top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    private fun drawTextIn(canvas: Canvas, value: String, typeface: Typeface, size: Float, color: Int, rect: RectF) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
        }
        val width = (rect.width() - 64f).roundToInt()
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(5)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(rect.left + 32f, rect.centerY() - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun font(id: Int): Typeface = ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1350
        const val SIDE_MARGIN = 96
        const val COVER_ASPECT = 0.7f
        const val CORNER = 36f
        const val SHARE_DIR = "share"
        const val AUTHORITY_SUFFIX = ".share"
        val BACKGROUND = Color.rgb(0x1C, 0x1C, 0x1C)
        val PLACEHOLDER = Color.rgb(0x3A, 0x3A, 0x3A)
        val MUTED = Color.rgb(0xB8, 0xB8, 0xB8)
        val TRACK = Color.rgb(0x3A, 0x3A, 0x3A)
        val ACCENT = Color.rgb(0x3E, 0xE0, 0x3A)
    }
}
