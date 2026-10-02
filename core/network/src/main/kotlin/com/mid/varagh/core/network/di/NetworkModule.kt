package com.mid.varagh.core.network.di

import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.core.network.VaraghApi
import com.mid.varagh.core.network.auth.AuthInterceptor
import com.mid.varagh.core.network.auth.TokenRefreshAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Retrofit/OkHttp setup. Nothing here is created unless a Remote… repository is used, i.e. only
 * when USE_REMOTE_BACKEND is true.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Named(TokenRefreshAuthenticator.BASE_URL)
    fun provideBaseUrl(flags: FeatureFlags): String =
        flags.apiBaseUrl.let { if (it.endsWith("/")) it else "$it/" }

    @Provides
    @Singleton
    @Named(TokenRefreshAuthenticator.REFRESH_CLIENT)
    fun provideRefreshClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        flags: FeatureFlags,
        authInterceptor: AuthInterceptor,
        authenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .apply {
            if (flags.isDebugBuild) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named(TokenRefreshAuthenticator.BASE_URL) baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideVaraghApi(retrofit: Retrofit): VaraghApi = retrofit.create(VaraghApi::class.java)

    private const val TIMEOUT_SECONDS = 20L
}
