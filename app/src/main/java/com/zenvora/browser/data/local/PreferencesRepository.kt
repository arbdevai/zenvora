package com.zenvora.browser.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zenvora_settings")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ADBLOCK_ENABLED = booleanPreferencesKey("adblock_enabled")
        val SEARCH_ENGINE_URL = stringPreferencesKey("search_engine_url")
        val STRICT_PRIVACY = booleanPreferencesKey("strict_privacy")
    }

    val isAdblockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADBLOCK_ENABLED] ?: true
    }

    val searchEngineUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SEARCH_ENGINE_URL] ?: "https://duckduckgo.com/?q="
    }

    val isStrictPrivacy: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STRICT_PRIVACY] ?: true
    }

    suspend fun setAdblockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADBLOCK_ENABLED] = enabled
        }
    }

    suspend fun setSearchEngineUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_ENGINE_URL] = url
        }
    }

    suspend fun setStrictPrivacy(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_PRIVACY] = enabled
        }
    }
}
