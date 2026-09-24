package com.mid.varagh.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mid.varagh.core.data.SystemTimeProvider
import com.mid.varagh.core.data.repository.DataStoreUserPreferencesRepository
import com.mid.varagh.core.data.repository.local.LocalAuthRepository
import com.mid.varagh.core.data.repository.local.LocalBookRepository
import com.mid.varagh.core.data.repository.local.LocalBookmarkRepository
import com.mid.varagh.core.data.repository.local.LocalReadingSessionRepository
import com.mid.varagh.core.data.repository.local.LocalSocialRepository
import com.mid.varagh.core.data.repository.local.LocalUserProfileRepository
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.repository.SocialRepository
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Chooses the repository implementations.
 *
 * Each binding takes `Provider`s so only the chosen implementation is ever constructed. Phase 8
 * adds a `remote: Provider<Remote…Repository>` parameter to each and switches on
 * `BuildConfig.USE_REMOTE_BACKEND`; nothing outside this module changes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object RepositoryModule {

    @Provides
    @Singleton
    fun provideBookRepository(local: Provider<LocalBookRepository>): BookRepository = local.get()

    @Provides
    @Singleton
    fun provideReadingSessionRepository(local: Provider<LocalReadingSessionRepository>): ReadingSessionRepository =
        local.get()

    @Provides
    @Singleton
    fun provideBookmarkRepository(local: Provider<LocalBookmarkRepository>): BookmarkRepository = local.get()

    @Provides
    @Singleton
    fun provideUserProfileRepository(local: Provider<LocalUserProfileRepository>): UserProfileRepository =
        local.get()

    @Provides
    @Singleton
    fun provideAuthRepository(local: Provider<LocalAuthRepository>): AuthRepository = local.get()

    @Provides
    @Singleton
    fun provideSocialRepository(local: Provider<LocalSocialRepository>): SocialRepository = local.get()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingsModule {

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    /** Device settings are local in both builds (see [UserPreferencesRepository]). */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("user_preferences") })
}
