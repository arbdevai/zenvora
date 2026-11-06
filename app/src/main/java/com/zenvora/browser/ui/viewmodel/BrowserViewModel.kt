package com.zenvora.browser.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenvora.browser.core.download.BrowserDownloadManager
import com.zenvora.browser.core.download.DownloadItem
import com.zenvora.browser.core.engine.BrowserSession
import com.zenvora.browser.core.engine.GeckoRuntimeManager
import com.zenvora.browser.core.network.DnsProvider
import com.zenvora.browser.core.network.NetworkSpeedManager
import com.zenvora.browser.core.network.NetworkStats
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

    val networkSpeedManager = NetworkSpeedManager()
    val networkStats: StateFlow<NetworkStats> = networkSpeedManager.networkStats

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

    val isNetworkTurboEnabled: StateFlow<Boolean> = preferencesRepository.isNetworkTurboEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dnsProviderId: StateFlow<String> = preferencesRepository.dnsProviderId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "cloudflare")

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
        networkSpeedManager.startMonitoring(viewModelScope)
        viewModelScope.launch {
            preferencesRepository.isAdblockEnabled.collect { enabled ->
                sessionsMap.values.forEach { it.adblockEnabled = enabled }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isNetworkTurboEnabled.collect { enabled ->
                networkSpeedManager.setTurboEnabled(enabled)
                val provider = DnsProvider.fromId(dnsProviderId.value)
                GeckoRuntimeManager.writeGeckoConfigFile(context, provider, enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.dnsProviderId.collect { id ->
                val provider = DnsProvider.fromId(id)
                networkSpeedManager.setDnsProvider(provider)
                GeckoRuntimeManager.writeGeckoConfigFile(context, provider, isNetworkTurboEnabled.value)
            }
        }
        createNewTab("about:blank")
    }

    fun getSession(tabId: String): BrowserSession? {
        return sessionsMap[tabId]
    }

    fun getActiveSession(): BrowserSession? {
        return sessionsMap[_activeTabId.value]
    }

    fun createNewTab(initialUrl: String = "about:blank", isIncognito: Boolean = false) {
        val tabId = UUID.randomUUID().toString()
        val session = BrowserSession(tabId, isIncognito = isIncognito)
        session.adblockEnabled = isAdblockEnabled.value
        session.initSession(context, runtime)
        sessionsMap[tabId] = session

        val newTab = BrowserTab(
            id = tabId,
            url = initialUrl,
            title = if (isIncognito) "Tab Samaran" else "Tab Baru",
            isIncognito = isIncognito
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
                    if (updatedTab.url.startsWith("http") && !updatedTab.isLoading && updatedTab.progress == 100 && !updatedTab.isIncognito) {
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
            createNewTab("about:blank")
        } else if (_activeTabId.value == tabId) {
            selectTab(remainingTabs.last().id)
        }
    }

    fun navigateHome() {
        getActiveSession()?.exitFullScreen()
        getActiveSession()?.loadUri("about:blank")
        _activeTabState.update {
            it.copy(
                url = "about:blank",
                title = if (it.isIncognito) "Tab Samaran" else "Tab Baru",
                isLoading = false,
                progress = 100,
                isFullScreen = false
            )
        }
    }

    fun loadInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = resolveUrlOrQuery(trimmed)
        if (isNetworkTurboEnabled.value && targetUrl.startsWith("http")) {
            viewModelScope.launch {
                networkSpeedManager.warmupAndMeasureLatency(targetUrl)
            }
        }
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

    fun enterFullScreen() {
        getActiveSession()?.enterFullScreen()
    }

    fun exitFullScreen() {
        getActiveSession()?.exitFullScreen()
    }

    fun toggleDesktopMode() {
        val current = _activeTabState.value.isDesktopMode
        getActiveSession()?.setDesktopMode(!current)
    }

    fun startFindInPage() {
        _activeTabState.update { it.copy(isFindInPageActive = true) }
    }

    fun findInPage(query: String) {
        getActiveSession()?.findInPage(query)
    }

    fun findNext() {
        getActiveSession()?.findNext()
    }

    fun findPrevious() {
        getActiveSession()?.findPrevious()
    }

    fun closeFindInPage() {
        getActiveSession()?.clearFindInPage()
        _activeTabState.update { it.copy(isFindInPageActive = false, findQuery = "") }
    }

    fun toggleAdblock() {
        viewModelScope.launch {
            val next = !isAdblockEnabled.value
            preferencesRepository.setAdblockEnabled(next)
            sessionsMap.values.forEach { it.adblockEnabled = next }
        }
    }

    fun toggleStrictPrivacy() {
        viewModelScope.launch {
            val current = isStrictPrivacy.value
            preferencesRepository.setStrictPrivacy(!current)
        }
    }

    fun toggleNetworkTurbo() {
        viewModelScope.launch {
            val next = !isNetworkTurboEnabled.value
            preferencesRepository.setNetworkTurboEnabled(next)
            networkSpeedManager.setTurboEnabled(next)
            val provider = DnsProvider.fromId(dnsProviderId.value)
            GeckoRuntimeManager.writeGeckoConfigFile(context, provider, next)
            if (next) {
                networkSpeedManager.warmupAndMeasureLatency(_activeTabState.value.url)
            }
        }
    }

    fun selectDnsProvider(provider: DnsProvider) {
        viewModelScope.launch {
            preferencesRepository.setDnsProviderId(provider.id)
            networkSpeedManager.setDnsProvider(provider)
            GeckoRuntimeManager.writeGeckoConfigFile(context, provider, isNetworkTurboEnabled.value)
            networkSpeedManager.warmupAndMeasureLatency(_activeTabState.value.url)
        }
    }

    fun refreshNetworkLatency() {
        viewModelScope.launch {
            networkSpeedManager.warmupAndMeasureLatency(_activeTabState.value.url)
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
            networkSpeedManager.clearDnsCache()
            getActiveSession()?.loadUri("about:blank")
        }
    }

    fun checkForUpdates(releasesApiUrl: String = "https://api.github.com/repos/arbdevai/zenvora/releases/latest") {
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
