package com.mid.varagh.feature.library

import app.cash.turbine.test
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.domain.usecase.ImportBookUseCase
import com.mid.varagh.core.domain.usecase.ImportResult
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.LibraryQuery
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val books = mockk<BookRepository>()
    private val prefs = mockk<UserPreferencesRepository>()
    private val import = mockk<ImportBookUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { books.observeLibrary(any()) } returns flowOf(emptyList())
        every { prefs.preferences } returns MutableStateFlow(UserPreferences())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emptyLibrary_isReportedAsEmpty() = runTest(dispatcher) {
        val vm = LibraryViewModel(books, prefs, import)
        vm.uiState.test {
            assertTrue(awaitItem().loading)
            val loaded = awaitItem()
            assertTrue(loaded.libraryEmpty)
            assertTrue(loaded.grid)
        }
    }

    @Test
    fun import_reportsAddedDuplicateAndFailure_inOrder() = runTest(dispatcher) {
        val existing = Book(9, "Masnavi", null, "u", "h", 1, null, 0, null, null, ReadingStatus.READING, null, null)
        coEvery { import("a") } returns ImportResult.Added(1, "Divan")
        coEvery { import("b") } returns ImportResult.Duplicate(existing)
        coEvery { import("c") } throws VaraghException.InvalidFile()
        val vm = LibraryViewModel(books, prefs, import)
        vm.events.test {
            vm.onImport(listOf("a", "b", "c"))
            advanceUntilIdle()
            assertEquals(LibraryMessage.Added("Divan"), awaitItem())
            assertEquals(LibraryMessage.Duplicate("Masnavi"), awaitItem())
            assertTrue((awaitItem() as LibraryMessage.Failed).error is VaraghException.InvalidFile)
        }
    }

    @Test
    fun statusFilter_isPassedToTheRepositoryQuery() = runTest(dispatcher) {
        val queries = mutableListOf<LibraryQuery>()
        every { books.observeLibrary(capture(queries)) } returns flowOf(emptyList())
        val vm = LibraryViewModel(books, prefs, import)
        vm.uiState.test {
            vm.onStatusFilter(ReadingStatus.FINISHED)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(queries.any { it.status == ReadingStatus.FINISHED })
    }
}
