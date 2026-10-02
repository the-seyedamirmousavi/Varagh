package com.mid.varagh.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mid.varagh.core.designsystem.R
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.ReadingStatus
import java.io.File

/** Width / height of a book cover. */
const val COVER_ASPECT_RATIO = 0.7f

/**
 * A book cover thumbnail (rendered from page 1) with a typographic placeholder while there is
 * none. Decorative: callers describe the book in text next to it.
 */
@Composable
fun BookCover(
    coverPath: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(COVER_ASPECT_RATIO)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.trim().take(TITLE_PLACEHOLDER_CHARS),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(VaraghSpacing.Small),
        )
        if (coverPath != null) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Thin rounded progress bar (0f..1f) used under covers and in lists. */
@Composable
fun VaraghProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(MaterialTheme.shapes.extraSmall),
        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

@StringRes
fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.WANT_TO_READ -> R.string.status_want_to_read
    ReadingStatus.READING -> R.string.status_reading
    ReadingStatus.FINISHED -> R.string.status_finished
    ReadingStatus.ABANDONED -> R.string.status_abandoned
}

private const val TITLE_PLACEHOLDER_CHARS = 40
