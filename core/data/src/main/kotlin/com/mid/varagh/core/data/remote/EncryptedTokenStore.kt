package com.mid.varagh.core.data.remote

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.mid.varagh.core.network.auth.AuthTokens
import com.mid.varagh.core.network.auth.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWTs in a dedicated DataStore, encrypted with Tink AES-256-GCM. The Tink keyset itself is
 * wrapped by a key in the Android Keystore, so tokens are unreadable outside this app/device.
 * Only created when USE_REMOTE_BACKEND is true.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(FILE) })
    }

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    @Volatile
    private var cached: AuthTokens? = null

    @Volatile
    private var loaded = false

    override val tokens: Flow<AuthTokens?>
        get() = dataStore.data.map { prefs ->
            decrypt(prefs[ACCESS], prefs[REFRESH]).also {
                cached = it
                loaded = true
            }
        }

    override fun current(): AuthTokens? {
        if (!loaded) runBlocking { tokens.first() }
        return cached
    }

    override suspend fun save(tokens: AuthTokens) {
        dataStore.edit {
            it[ACCESS] = encrypt(tokens.accessToken)
            it[REFRESH] = encrypt(tokens.refreshToken)
        }
        cached = tokens
        loaded = true
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
        cached = null
        loaded = true
    }

    private fun encrypt(value: String): String =
        Base64.encodeToString(aead.encrypt(value.toByteArray(), ASSOCIATED_DATA), Base64.NO_WRAP)

    private fun decrypt(access: String?, refresh: String?): AuthTokens? {
        if (access == null || refresh == null) return null
        return runCatching {
            AuthTokens(
                accessToken = aead.decrypt(Base64.decode(access, Base64.NO_WRAP), ASSOCIATED_DATA).decodeToString(),
                refreshToken = aead.decrypt(Base64.decode(refresh, Base64.NO_WRAP), ASSOCIATED_DATA).decodeToString(),
            )
        }.getOrNull() // Undecryptable (e.g. restored onto another device): treat as signed out.
    }

    private companion object {
        const val FILE = "auth_tokens"
        const val KEYSET_NAME = "varagh_token_keyset"
        const val KEYSET_PREFS = "varagh_token_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://varagh_token_master_key"
        val ASSOCIATED_DATA = "varagh-auth".toByteArray()
        val ACCESS = stringPreferencesKey("access")
        val REFRESH = stringPreferencesKey("refresh")
    }
}
