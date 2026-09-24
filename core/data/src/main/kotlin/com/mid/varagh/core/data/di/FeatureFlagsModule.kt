package com.mid.varagh.core.data.di

import com.mid.varagh.core.data.BuildConfig
import com.mid.varagh.core.model.FeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Exposes the build-time backend switch to the rest of the app. This is the only place that
 * reads `BuildConfig.USE_REMOTE_BACKEND`; repository bindings (DataModule) and UI decisions all
 * derive from it.
 */
@Module
@InstallIn(SingletonComponent::class)
object FeatureFlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(): FeatureFlags = FeatureFlags(
        useRemoteBackend = BuildConfig.USE_REMOTE_BACKEND,
        apiBaseUrl = BuildConfig.API_BASE_URL,
        isDebugBuild = BuildConfig.DEBUG,
    )
}
