package com.mid.varagh.core.data.backup

import android.content.Context
import android.net.Uri
import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.dao.BookDao
import com.mid.varagh.core.database.dao.BookmarkDao
import com.mid.varagh.core.database.dao.ReadingProgressDao
import com.mid.varagh.core.database.dao.ReadingSessionDao
import com.mid.varagh.core.database.dao.UserProfileDao
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.BookmarkEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.ReadingSessionEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.UserProfileEntity
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BackupRepository
import com.mid.varagh.core.domain.repository.BackupSummary
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.CustomReadingColors
import com.mid.varagh.core.model.DarkThemeConfig
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.ReadingMode
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.ReadingTheme
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.core.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/** JSON backup/restore of everything except the PDFs. Same in both builds (device-side). */
class LocalBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val sessionDao: ReadingSessionDao,
    private val bookmarkDao: BookmarkDao,
    private val profileDao: UserProfileDao,
    private val preferences: UserPreferencesRepository,
    private val transaction: TransactionRunner,
    private val time: TimeProvider,
) : BackupRepository {

    override suspend fun exportTo(documentUri: String): BackupSummary {
        val backup = buildBackup()
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(Uri.parse(documentUri), "wt")?.use {
                    it.write(json.encodeToString(BackupFile.serializer(), backup).toByteArray())
                } ?: throw VaraghException.FileUnavailable()
            } catch (e: IOException) {
                throw VaraghException.FileUnavailable(e)
            } catch (e: SecurityException) {
                throw VaraghException.FileUnavailable(e)
            }
        }
        return BackupSummary(backup.books.size, backup.sessions.size, backup.bookmarks.size)
    }

    override suspend fun importFrom(documentUri: String): BackupSummary {
        val text = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(documentUri))?.use { it.readBytes().decodeToString() }
                    ?: throw VaraghException.FileUnavailable()
            } catch (e: IOException) {
                throw VaraghException.FileUnavailable(e)
            } catch (e: SecurityException) {
                throw VaraghException.FileUnavailable(e)
            }
        }
        return importJson(text)
    }

    internal suspend fun buildBackup(): BackupFile {
        val books = bookDao.getAll()
        val progress = progressDao.getAll().associateBy { it.bookId }
        val hashById = books.associate { it.id to it.fileHash }
        val profile = profileDao.get(UserProfile.LOCAL_USER_ID)
        return BackupFile(
            exportedAt = time.nowMillis(),
            books = books.map { b ->
                val p = progress[b.id]
                BookBackup(
                    fileHash = b.fileHash, title = b.title, author = b.author, fileUri = b.fileUri,
                    pageCount = b.pageCount, addedAt = b.addedAt, lastOpenedAt = b.lastOpenedAt,
                    finishedAt = b.finishedAt, status = b.status.name, rating = b.rating, remoteId = b.remoteId,
                    updatedAt = b.updatedAt, progressPage = p?.currentPage, progressPercent = p?.percent,
                    progressUpdatedAt = p?.updatedAt,
                )
            },
            sessions = sessionDao.getAll().filter { it.syncState != SyncState.DELETED }.mapNotNull { s ->
                hashById[s.bookId]?.let { SessionBackup(it, s.startedAt, s.endedAt, s.pagesRead) }
            },
            bookmarks = bookmarkDao.getAll().filter { it.syncState != SyncState.DELETED }.mapNotNull { m ->
                hashById[m.bookId]?.let { BookmarkBackup(it, m.page, m.note, m.createdAt) }
            },
            profile = profile?.let { ProfileBackup(it.displayName, it.username, it.bio, it.isPublic) },
            preferences = preferences.preferences.first().toBackup(),
        )
    }

    internal suspend fun importJson(text: String): BackupSummary {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw VaraghException.InvalidBackup(e)
        } catch (e: IllegalArgumentException) {
            throw VaraghException.InvalidBackup(e)
        }
        if (backup.format != BackupFile.FORMAT || backup.version > BackupFile.VERSION) throw VaraghException.InvalidBackup()

        var books = 0
        var sessions = 0
        var bookmarks = 0
        transaction {
            val now = time.nowMillis()
            val idByHash = mutableMapOf<String, Long>()
            for (b in backup.books) {
                val status = runCatching { ReadingStatus.valueOf(b.status) }.getOrDefault(ReadingStatus.WANT_TO_READ)
                val existing = bookDao.findByHashIncludingDeleted(b.fileHash)
                val id = if (existing == null || existing.syncState == SyncState.DELETED) {
                    val entity = BookEntity(
                        id = existing?.id ?: 0, title = b.title, author = b.author, fileUri = b.fileUri,
                        fileHash = b.fileHash, pageCount = b.pageCount, coverPath = null, addedAt = b.addedAt,
                        lastOpenedAt = b.lastOpenedAt, finishedAt = b.finishedAt, status = status,
                        rating = b.rating?.coerceIn(0, 5), remoteId = b.remoteId, updatedAt = now,
                    )
                    books++
                    if (existing != null) {
                        bookDao.update(entity)
                        existing.id
                    } else {
                        bookDao.insert(entity)
                    }
                } else {
                    existing.id
                }
                idByHash[b.fileHash] = id
                if (b.progressPage != null && b.progressUpdatedAt != null) {
                    val current = progressDao.get(id)
                    if (current == null || current.updatedAt < b.progressUpdatedAt) {
                        progressDao.upsert(ReadingProgressEntity(id, b.progressPage, b.progressPercent ?: 0f, b.progressUpdatedAt))
                    }
                }
            }
            val knownSessions = sessionDao.getAll().map { it.bookId to it.startedAt }.toHashSet()
            for (s in backup.sessions) {
                val id = idByHash[s.bookHash] ?: continue
                if (knownSessions.add(id to s.startedAt)) {
                    sessionDao.insert(ReadingSessionEntity(bookId = id, startedAt = s.startedAt, endedAt = s.endedAt, pagesRead = s.pagesRead))
                    sessions++
                }
            }
            val knownMarks = bookmarkDao.getAll().map { Triple(it.bookId, it.page, it.createdAt) }.toHashSet()
            for (m in backup.bookmarks) {
                val id = idByHash[m.bookHash] ?: continue
                if (knownMarks.add(Triple(id, m.page, m.createdAt))) {
                    bookmarkDao.insert(BookmarkEntity(bookId = id, page = m.page, note = m.note, createdAt = m.createdAt, updatedAt = now))
                    bookmarks++
                }
            }
            val profile = profileDao.get(UserProfile.LOCAL_USER_ID)
            if (backup.profile != null && (profile == null || profile.displayName.isBlank())) {
                profileDao.upsert(
                    UserProfileEntity(
                        id = UserProfile.LOCAL_USER_ID,
                        displayName = backup.profile.displayName,
                        username = backup.profile.username,
                        bio = backup.profile.bio,
                        avatarPath = profile?.avatarPath,
                        isPublic = backup.profile.isPublic,
                        currentlyReadingBookId = profile?.currentlyReadingBookId,
                        remoteId = profile?.remoteId,
                        updatedAt = now,
                    ),
                )
            }
        }
        backup.preferences?.let { saved -> preferences.update { saved.applyTo(it) } }
        return BackupSummary(books, sessions, bookmarks)
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }
}

private fun UserPreferences.toBackup() = PreferencesBackup(
    readingTheme = readingTheme.name,
    customBackgroundArgb = customReadingColors.backgroundArgb,
    customTextArgb = customReadingColors.textArgb,
    readingMode = readingMode.name,
    rightToLeftPaging = rightToLeftPaging,
    keepScreenOn = keepScreenOn,
    darkThemeConfig = darkThemeConfig.name,
    useDynamicColor = useDynamicColor,
    language = language.tag,
    warmFilter = warmFilter,
    libraryGrid = libraryGrid,
    librarySort = librarySort.name,
)

private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
    name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback

private fun PreferencesBackup.applyTo(p: UserPreferences) = p.copy(
    readingTheme = enumOr<ReadingTheme>(readingTheme, p.readingTheme),
    customReadingColors = CustomReadingColors(
        backgroundArgb = customBackgroundArgb ?: p.customReadingColors.backgroundArgb,
        textArgb = customTextArgb ?: p.customReadingColors.textArgb,
    ),
    readingMode = enumOr<ReadingMode>(readingMode, p.readingMode),
    rightToLeftPaging = rightToLeftPaging ?: p.rightToLeftPaging,
    keepScreenOn = keepScreenOn ?: p.keepScreenOn,
    darkThemeConfig = enumOr<DarkThemeConfig>(darkThemeConfig, p.darkThemeConfig),
    useDynamicColor = useDynamicColor ?: p.useDynamicColor,
    language = language?.let(AppLanguage::fromTag) ?: p.language,
    warmFilter = (warmFilter ?: p.warmFilter).coerceIn(0f, 1f),
    libraryGrid = libraryGrid ?: p.libraryGrid,
    librarySort = enumOr<LibrarySort>(librarySort, p.librarySort),
)
