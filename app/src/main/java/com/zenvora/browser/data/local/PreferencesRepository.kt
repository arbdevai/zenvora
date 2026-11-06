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
        val NETWORK_TURBO = booleanPreferencesKey("network_turbo")
        val DNS_PROVIDER_ID = stringPreferencesKey("dns_provider_id")
        val VIDEO_CODEC_MODE_ID = stringPreferencesKey("video_codec_mode_id")
        val BACKGROUND_AUDIO_ENABLED = booleanPreferencesKey("background_audio_enabled")
        val SAVED_TABS_JSON = stringPreferencesKey("saved_tabs_json")
        val ACTIVE_TAB_ID = stringPreferencesKey("active_tab_id")
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

    val isNetworkTurboEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NETWORK_TURBO] ?: true
    }

    val dnsProviderId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DNS_PROVIDER_ID] ?: "cloudflare"
    }

    val videoCodecModeId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIDEO_CODEC_MODE_ID] ?: "auto_hardware"
    }

    val isBackgroundAudioEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] ?: true
    }

    val savedTabsJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVED_TABS_JSON] ?: ""
    }

    val savedActiveTabId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_TAB_ID] ?: ""
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

    suspend fun setNetworkTurboEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NETWORK_TURBO] = enabled
        }
    }

    suspend fun setDnsProviderId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DNS_PROVIDER_ID] = id
        }
    }

    suspend fun setVideoCodecModeId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_CODEC_MODE_ID] = id
        }
    }

    suspend fun setBackgroundAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] = enabled
        }
    }

    suspend fun saveTabsSession(tabsJson: String, activeTabId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVED_TABS_JSON] = tabsJson
            preferences[PreferencesKeys.ACTIVE_TAB_ID] = activeTabId
        }
    }
}
