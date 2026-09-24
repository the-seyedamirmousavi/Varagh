package com.mid.varagh.core.model

/** Where a book sits on the reader's shelf. */
enum class ReadingStatus {
    WANT_TO_READ,
    READING,
    FINISHED,
    ABANDONED,
}

/**
 * A PDF in the reader's library. Timestamps are epoch milliseconds (UTC).
 *
 * @property fileUri persisted Storage Access Framework URI. The PDF itself never leaves the device.
 * @property fileHash SHA-256 of the file, used to detect duplicates.
 * @property coverPath app-private path of the thumbnail rendered from page 1.
 * @property finishedAt set when [status] becomes [ReadingStatus.FINISHED]; drives "finished this year".
 * @property rating 0..5, null when unrated.
 * @property remoteId id in the shared catalog once synced (remote backend only).
 */
data class Book(
    val id: Long,
    val title: String,
    val author: String?,
    val fileUri: String,
    val fileHash: String,
    val pageCount: Int,
    val coverPath: String?,
    val addedAt: Long,
    val lastOpenedAt: Long?,
    val finishedAt: Long?,
    val status: ReadingStatus,
    val rating: Int?,
    val remoteId: String?,
) {
    init {
        require(rating == null || rating in 0..MAX_RATING) { "rating must be 0..$MAX_RATING, was $rating" }
    }

    companion object {
        const val MAX_RATING = 5
    }
}

/** Data needed to add a book; ids and timestamps are assigned by the repository. */
data class NewBook(
    val title: String,
    val author: String?,
    val fileUri: String,
    val fileHash: String,
    val pageCount: Int,
    val coverPath: String?,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
)

/** Where the reader is in a book. [percent] is 0f..1f. */
data class ReadingProgress(
    val bookId: Long,
    val currentPage: Int,
    val percent: Float,
    val updatedAt: Long,
) {
    companion object {
        /** Progress for a zero-based [page] of [pageCount] pages; the last page counts as 100%. */
        fun percentOf(page: Int, pageCount: Int): Float =
            if (pageCount <= 1) 1f else (page.coerceIn(0, pageCount - 1).toFloat() / (pageCount - 1)).coerceIn(0f, 1f)
    }
}

/** A book together with its (optional) reading progress, as shown in lists. */
data class BookWithProgress(
    val book: Book,
    val progress: ReadingProgress?,
)

/** Library list sort orders. */
enum class LibrarySort {
    LAST_OPENED,
    TITLE,
    DATE_ADDED,
}

/** Library search/filter/sort. Blank [search] matches everything; null [status] means all. */
data class LibraryQuery(
    val search: String = "",
    val status: ReadingStatus? = null,
    val sort: LibrarySort = LibrarySort.LAST_OPENED,
)
