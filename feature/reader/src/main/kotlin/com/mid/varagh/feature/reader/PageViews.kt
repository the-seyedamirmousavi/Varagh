package com.mid.varagh.feature.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.reading.ReadingPalette
import com.mid.varagh.feature.reader.pdf.PdfDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** A.4 portrait until real page sizes are measured. */
private const val DEFAULT_PAGE_ASPECT = 0.707f

/** Above this zoom, pages are re-rendered at double resolution so text stays sharp. */
private const val HI_RES_SCALE = 1.4f

private fun List<IntSize>.aspectOf(index: Int): Float =
    getOrNull(index)?.takeIf { it.height > 0 }?.let { it.width.toFloat() / it.height } ?: DEFAULT_PAGE_ASPECT

/** One rendered page; shows the paper colour until its bitmap is ready. */
@Composable
internal fun PdfPage(
    document: PdfDocument,
    index: Int,
    renderWidthPx: Int,
    aspectRatio: Float,
    palette: ReadingPalette,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.reader_page_description, index + 1, document.pageCount)
    val bitmap: ImageBitmap? by produceState(
        initialValue = document.cached(index, renderWidthPx)?.asImageBitmap(),
        document,
        index,
        renderWidthPx,
    ) {
        document.render(index, renderWidthPx)?.let { value = it.asImageBitmap() }
    }
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .background(palette.background)
            .semantics { contentDescription = label },
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                colorFilter = palette.colorFilter,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Continuous vertical scroll; the whole column zooms, horizontal pan when zoomed. */
@Composable
internal fun VerticalReader(
    document: PdfDocument,
    state: ReaderUiState,
    startPage: Int,
    palette: ReadingPalette,
    jumpRequest: JumpRequest?,
    onJumpHandled: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startPage)
    val zoom = rememberZoomState(allowVerticalPan = false, key = document)
    val scope = rememberCoroutineScope()

    // The page crossing the middle of the screen is the "current" page.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.firstOrNull { middle in it.offset..(it.offset + it.size) }?.index
                ?: info.visibleItemsInfo.firstOrNull()?.index
        }.filterNotNull().distinctUntilChanged().collect(onPageChanged)
    }
    LaunchedEffect(jumpRequest) {
        jumpRequest?.let {
            listState.scrollToItem(it.page)
            onJumpHandled()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .zoomGestures(zoom, scope) { _, _ -> onTap() },
    ) {
        val baseWidth = constraints.maxWidth
        val renderWidth = if (zoom.scale > HI_RES_SCALE) baseWidth * 2 else baseWidth
        LaunchedEffect(document, state.currentPage, renderWidth) {
            document.prefetch(state.currentPage, renderWidth, radius = 2)
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .zoomContent(zoom)
                .testTag("reader_vertical"),
        ) {
            items(count = document.pageCount, key = { it }) { index ->
                PdfPage(
                    document = document,
                    index = index,
                    renderWidthPx = renderWidth,
                    aspectRatio = state.pageSizes.aspectOf(index),
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Page-by-page. [rightToLeft] makes the next page come from the left, as in Persian books,
 * independently of the app's UI language. Tap the outer quarters to turn pages.
 */
@Composable
internal fun PagedReader(
    document: PdfDocument,
    state: ReaderUiState,
    startPage: Int,
    palette: ReadingPalette,
    rightToLeft: Boolean,
    jumpRequest: JumpRequest?,
    onJumpHandled: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = startPage) { document.pageCount }
    val scope = rememberCoroutineScope()
    val zoom = rememberZoomState(allowVerticalPan = true, key = pagerState.currentPage)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect(onPageChanged)
    }
    LaunchedEffect(jumpRequest) {
        jumpRequest?.let {
            pagerState.scrollToPage(it.page)
            onJumpHandled()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .zoomGestures(zoom, scope) { tap, size ->
                val edge = size.width / 4f
                // "Forward" is on the left for right-to-left books.
                val forwardOnLeft = rightToLeft
                when {
                    tap.x < edge -> scope.launchPageTurn(pagerState, if (forwardOnLeft) 1 else -1)
                    tap.x > size.width - edge -> scope.launchPageTurn(pagerState, if (forwardOnLeft) -1 else 1)
                    else -> onTap()
                }
            },
    ) {
        val renderWidth = if (zoom.scale > HI_RES_SCALE) constraints.maxWidth * 2 else constraints.maxWidth
        LaunchedEffect(document, pagerState.settledPage, renderWidth) {
            document.prefetch(pagerState.settledPage, renderWidth, radius = 1)
        }
        CompositionLocalProvider(LocalLayoutDirection provides if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !zoom.isZoomed,
                key = { it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reader_pager"),
            ) { index ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PdfPage(
                        document = document,
                        index = index,
                        renderWidthPx = renderWidth,
                        aspectRatio = state.pageSizes.aspectOf(index),
                        palette = palette,
                        modifier = if (index == pagerState.currentPage) Modifier.zoomContent(zoom) else Modifier,
                    )
                }
            }
        }
    }
}

/** A request to show [page]; [id] makes repeated jumps to the same page distinct. */
data class JumpRequest(val page: Int, val id: Long = System.nanoTime())

private fun CoroutineScope.launchPageTurn(pagerState: PagerState, delta: Int) {
    val target = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
    launch { pagerState.animateScrollToPage(target) }
}
