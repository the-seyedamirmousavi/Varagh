package com.mid.varagh.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mid.varagh.core.data.BuildConfig
import com.mid.varagh.core.data.SystemTimeProvider
import com.mid.varagh.core.data.backup.LocalBackupRepository
import com.mid.varagh.core.data.file.AndroidAvatarStore
import com.mid.varagh.core.data.file.AndroidBookFileRepository
import com.mid.varagh.core.data.remote.EncryptedTokenStore
import com.mid.varagh.core.data.repository.DataStoreUserPreferencesRepository
import com.mid.varagh.core.data.repository.local.LocalAuthRepository
import com.mid.varagh.core.data.repository.local.LocalBookRepository
import com.mid.varagh.core.data.repository.local.LocalBookmarkRepository
import com.mid.varagh.core.data.repository.local.LocalReadingSessionRepository
import com.mid.varagh.core.data.repository.local.LocalSocialRepository
import com.mid.varagh.core.data.repository.local.LocalUserProfileRepository
import com.mid.varagh.core.data.repository.remote.RemoteAuthRepository
import com.mid.varagh.core.data.repository.remote.RemoteBookRepository
import com.mid.varagh.core.data.repository.remote.RemoteBookmarkRepository
import com.mid.varagh.core.data.repository.remote.RemoteReadingSessionRepository
import com.mid.varagh.core.data.repository.remote.RemoteSocialRepository
import com.mid.varagh.core.data.repository.remote.RemoteUserProfileRepository
import com.mid.varagh.core.data.sync.SyncScheduler
import com.mid.varagh.core.data.sync.WorkManagerSyncScheduler
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.AvatarStore
import com.mid.varagh.core.domain.repository.BackupRepository
import com.mid.varagh.core.domain.repository.BookFileRepository
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.repository.SocialRepository
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.domain.repository.UserProfileRepository
import com.mid.varagh.core.network.auth.TokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * THE BACKEND SWITCH. Every repository is chosen here, once, from BuildConfig.USE_REMOTE_BACKEND.
 * Providers make sure only the chosen implementation (and, for remote, Retrofit/OkHttp/Tink) is
 * ever constructed. Flipping the flag needs no code change, only a rebuild.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object RepositoryModule {

    private val useRemote: Boolean get() = BuildConfig.USE_REMOTE_BACKEND

    @Provides
    @Singleton
    fun provideBookRepository(
        local: Provider<LocalBookRepository>,
        remote: Provider<RemoteBookRepository>,
    ): BookRepository = if (useRemote) remote.get() else local.get()

    @Provides
    @Singleton
    fun provideReadingSessionRepository(
        local: Provider<LocalReadingSessionRepository>,
        remote: Provider<RemoteReadingSessionRepository>,
    ): ReadingSessionRepository = if (useRemote) remote.get() else local.get()

    @Provides
    @Singleton
    fun provideBookmarkRepository(
        local: Provider<LocalBookmarkRepository>,
        remote: Provider<RemoteBookmarkRepository>,
    ): BookmarkRepository = if (useRemote) remote.get() else local.get()

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        local: Provider<LocalUserProfileRepository>,
        remote: Provider<RemoteUserProfileRepository>,
    ): UserProfileRepository = if (useRemote) remote.get() else local.get()

    @Provides
    @Singleton
    fun provideAuthRepository(
        local: Provider<LocalAuthRepository>,
        remote: Provider<RemoteAuthRepository>,
    ): AuthRepository = if (useRemote) remote.get() else local.get()

    @Provides
    @Singleton
    fun provideSocialRepository(
        local: Provider<LocalSocialRepository>,
        remote: Provider<RemoteSocialRepository>,
    ): SocialRepository = if (useRemote) remote.get() else local.get()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingsModule {

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    /** PDFs, covers and avatars stay on the device in both builds. */
    @Binds
    abstract fun bindBookFileRepository(impl: AndroidBookFileRepository): BookFileRepository

    @Binds
    abstract fun bindAvatarStore(impl: AndroidAvatarStore): AvatarStore

    @Binds
    abstract fun bindBackupRepository(impl: LocalBackupRepository): BackupRepository

    /** Device settings are local in both builds (see [UserPreferencesRepository]). */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("user_preferences") })
}
