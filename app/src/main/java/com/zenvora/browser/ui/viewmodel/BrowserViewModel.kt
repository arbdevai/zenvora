package com.zenvora.browser.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenvora.browser.core.download.BrowserDownloadManager
import com.zenvora.browser.core.download.DownloadItem
import com.zenvora.browser.core.engine.BrowserSession
import com.zenvora.browser.core.engine.GeckoRuntimeManager
import com.zenvora.browser.core.updater.AppUpdateInfo
import com.zenvora.browser.core.updater.UpdateManager
import com.zenvora.browser.core.updater.UpdateState
import com.zenvora.browser.data.local.PreferencesRepository
import com.zenvora.browser.data.local.ZenvoraDatabase
import com.zenvora.browser.data.local.entity.BookmarkEntity
import com.zenvora.browser.data.local.entity.HistoryEntity
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.data.model.SecurityInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val runtime = GeckoRuntimeManager.getRuntime(context)
    private val database = ZenvoraDatabase.getInstance(context)
    private val preferencesRepository = PreferencesRepository(context)

    val updateManager = UpdateManager(context)
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    val downloadManager = BrowserDownloadManager(context)
    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads

    val isAdblockEnabled: StateFlow<Boolean> = preferencesRepository.isAdblockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isStrictPrivacy: StateFlow<Boolean> = preferencesRepository.isStrictPrivacy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val searchEngineUrl: StateFlow<String> = preferencesRepository.searchEngineUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://duckduckgo.com/?q=")

    val historyEntries: StateFlow<List<HistoryEntity>> = database.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sessionsMap = mutableMapOf<String, BrowserSession>()

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeTabState = MutableStateFlow(BrowserTab(id = "", url = "about:blank"))
    val activeTabState: StateFlow<BrowserTab> = _activeTabState.asStateFlow()

    private val _securityInfo = MutableStateFlow(SecurityInfo())
    val securityInfo: StateFlow<SecurityInfo> = _securityInfo.asStateFlow()

    init {
        // Inisialisasi tab pertama
        createNewTab("https://duckduckgo.com")
    }

    fun getSession(tabId: String): BrowserSession? {
        return sessionsMap[tabId]
    }

    fun getActiveSession(): BrowserSession? {
        return sessionsMap[_activeTabId.value]
    }

    fun createNewTab(initialUrl: String = "https://duckduckgo.com") {
        val tabId = UUID.randomUUID().toString()
        val session = BrowserSession(tabId)
        session.initSession(context, runtime)
        sessionsMap[tabId] = session

        val newTab = BrowserTab(
            id = tabId,
            url = initialUrl,
            title = "Tab Baru"
        )

        _tabs.update { it + newTab }
        selectTab(tabId)

        if (initialUrl != "about:blank") {
            session.loadUri(initialUrl)
        }

        observeSession(session)
    }

    private fun observeSession(session: BrowserSession) {
        viewModelScope.launch {
            session.tabState.collect { updatedTab ->
                _tabs.update { tabList ->
                    tabList.map { if (it.id == updatedTab.id) updatedTab else it }
                }
                if (session.tabId == _activeTabId.value) {
                    _activeTabState.value = updatedTab
                    if (updatedTab.url.startsWith("http") && !updatedTab.isLoading && updatedTab.progress == 100) {
                        saveHistoryEntry(updatedTab.url, updatedTab.title.ifEmpty { updatedTab.url })
                    }
                }
            }
        }

        viewModelScope.launch {
            session.securityInfo.collect { info ->
                if (session.tabId == _activeTabId.value) {
                    _securityInfo.value = info
                }
            }
        }
    }

    fun selectTab(tabId: String) {
        if (sessionsMap.containsKey(tabId)) {
            _activeTabId.value = tabId
            val session = sessionsMap[tabId]
            if (session != null) {
                _activeTabState.value = session.tabState.value
                _securityInfo.value = session.securityInfo.value
            }
        }
    }

    fun closeTab(tabId: String) {
        val session = sessionsMap.remove(tabId)
        session?.close()

        val remainingTabs = _tabs.value.filter { it.id != tabId }
        _tabs.value = remainingTabs

        if (remainingTabs.isEmpty()) {
            createNewTab("https://duckduckgo.com")
        } else if (_activeTabId.value == tabId) {
            selectTab(remainingTabs.last().id)
        }
    }

    fun loadInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = resolveUrlOrQuery(trimmed)
        getActiveSession()?.loadUri(targetUrl)
    }

    private fun resolveUrlOrQuery(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("about:") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> {
                val encodedQuery = URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
                "${searchEngineUrl.value}$encodedQuery"
            }
        }
    }

    fun goBack(): Boolean {
        return getActiveSession()?.goBack() ?: false
    }

    fun goForward(): Boolean {
        return getActiveSession()?.goForward() ?: false
    }

    fun reload() {
        getActiveSession()?.reload()
    }

    fun toggleAdblock() {
        viewModelScope.launch {
            val current = isAdblockEnabled.value
            preferencesRepository.setAdblockEnabled(!current)
        }
    }

    fun toggleStrictPrivacy() {
        viewModelScope.launch {
            val current = isStrictPrivacy.value
            preferencesRepository.setStrictPrivacy(!current)
        }
    }

    fun setSearchEngine(url: String) {
        viewModelScope.launch {
            preferencesRepository.setSearchEngineUrl(url)
        }
    }

    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            val isBookmarked = database.bookmarkDao().isBookmarked(url)
            if (isBookmarked) {
                database.bookmarkDao().deleteBookmarkByUrl(url)
            } else {
                database.bookmarkDao().insertBookmark(
                    BookmarkEntity(
                        url = url,
                        title = title.ifEmpty { url }
                    )
                )
            }
        }
    }

    private fun saveHistoryEntry(url: String, title: String) {
        viewModelScope.launch {
            database.historyDao().insertHistory(
                HistoryEntity(
                    url = url,
                    title = title
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.historyDao().clearAllHistory()
        }
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch {
            database.historyDao().clearAllHistory()
            getActiveSession()?.loadUri("about:blank")
        }
    }

    fun checkForUpdates(releasesApiUrl: String = "https://api.github.com/repos/zenvora/zenvora-browser/releases/latest") {
        viewModelScope.launch {
            updateManager.checkForUpdates(releasesApiUrl)
        }
    }

    fun downloadUpdate(info: AppUpdateInfo) {
        viewModelScope.launch {
            updateManager.downloadUpdate(info)
        }
    }

    fun triggerInstall(apkFile: File) {
        updateManager.triggerInstall(apkFile)
    }

    fun dismissUpdate() {
        updateManager.dismiss()
    }

    override fun onCleared() {
        super.onCleared()
        sessionsMap.values.forEach { it.close() }
        sessionsMap.clear()
    }
}
