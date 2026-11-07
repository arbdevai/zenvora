package com.zenvora.browser.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenvora.browser.core.download.BrowserDownloadManager
import com.zenvora.browser.core.download.DownloadItem
import com.zenvora.browser.core.engine.BrowserSession
import com.zenvora.browser.core.engine.GeckoRuntimeManager
import com.zenvora.browser.core.engine.HardwareCodecDetector
import com.zenvora.browser.core.engine.HardwareVideoCapabilities
import com.zenvora.browser.core.engine.VideoCodecMode
import com.zenvora.browser.core.extensions.ExtensionManager
import com.zenvora.browser.core.extensions.InstalledExtensionInfo
import com.zenvora.browser.core.extensions.RecommendedAddon
import com.zenvora.browser.core.media.BrowserMediaNotificationManager
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
import com.zenvora.browser.data.local.entity.SavedLoginEntity
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.ui.components.DetectedVideoStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
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
    val availableUpdateBadge: StateFlow<AppUpdateInfo?> = updateManager.availableUpdateBadge

    val downloadManager = BrowserDownloadManager(context)
    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads

    val extensionManager = ExtensionManager(context, runtime, viewModelScope)
    val installedExtensions: StateFlow<List<InstalledExtensionInfo>> = extensionManager.installedExtensions
    val installingAddonId: StateFlow<String?> = extensionManager.installingAddonId
    val installProgressPercent: StateFlow<Int?> = extensionManager.installProgressPercent
    val extensionStatusMessage: StateFlow<String?> = extensionManager.statusMessage

    private val mediaNotificationManager = BrowserMediaNotificationManager(context)

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

    val videoCodecModeId: StateFlow<String> = preferencesRepository.videoCodecModeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto_hardware")

    val isBackgroundAudioEnabled: StateFlow<Boolean> = preferencesRepository.isBackgroundAudioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hardwareVideoCapabilities: HardwareVideoCapabilities = HardwareCodecDetector.detectCapabilities()

    val historyEntries: StateFlow<List<HistoryEntity>> = database.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLogins: StateFlow<List<SavedLoginEntity>> = database.savedLoginDao().getAllSavedLogins()
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

    private val _detectedVideoStream = MutableStateFlow<DetectedVideoStream?>(null)
    val detectedVideoStream: StateFlow<DetectedVideoStream?> = _detectedVideoStream.asStateFlow()

    private val _activeNativeVideoStream = MutableStateFlow<DetectedVideoStream?>(null)
    val activeNativeVideoStream: StateFlow<DetectedVideoStream?> = _activeNativeVideoStream.asStateFlow()

    private var isRestoringTabs = true

    init {
        networkSpeedManager.startMonitoring(viewModelScope)
        viewModelScope.launch {
            preferencesRepository.isAdblockEnabled.collect { enabled ->
                sessionsMap.values.forEach { it.adblockEnabled = enabled }
            }
        }
        viewModelScope.launch {
            extensionManager.installedExtensions.collect { extList ->
                val hasActiveExternalAdblock = extList.any { ext ->
                    !ext.isBuiltIn && ext.isEnabled && (
                        ext.id.contains("uBlock", ignoreCase = true) ||
                            ext.name.contains("uBlock", ignoreCase = true) ||
                            ext.name.contains("AdGuard", ignoreCase = true)
                        )
                }
                sessionsMap.values.forEach { it.externalAdblockActive = hasActiveExternalAdblock }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isNetworkTurboEnabled.collect { enabled ->
                networkSpeedManager.setTurboEnabled(enabled)
                val provider = DnsProvider.fromId(dnsProviderId.value)
                val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
                GeckoRuntimeManager.writeGeckoConfigFile(
                    context = context,
                    dnsProvider = provider,
                    isTurboEnabled = enabled,
                    videoCodecMode = codecMode,
                    isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
                )
            }
        }
        viewModelScope.launch {
            preferencesRepository.dnsProviderId.collect { id ->
                val provider = DnsProvider.fromId(id)
                val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
                networkSpeedManager.setDnsProvider(provider)
                GeckoRuntimeManager.writeGeckoConfigFile(
                    context = context,
                    dnsProvider = provider,
                    isTurboEnabled = isNetworkTurboEnabled.value,
                    videoCodecMode = codecMode,
                    isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
                )
            }
        }
        viewModelScope.launch {
            preferencesRepository.videoCodecModeId.collect { id ->
                val codecMode = VideoCodecMode.fromId(id)
                val provider = DnsProvider.fromId(dnsProviderId.value)
                GeckoRuntimeManager.writeGeckoConfigFile(
                    context = context,
                    dnsProvider = provider,
                    isTurboEnabled = isNetworkTurboEnabled.value,
                    videoCodecMode = codecMode,
                    isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
                )
            }
        }
        viewModelScope.launch {
            preferencesRepository.isBackgroundAudioEnabled.collect { enabled ->
                val provider = DnsProvider.fromId(dnsProviderId.value)
                val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
                GeckoRuntimeManager.writeGeckoConfigFile(
                    context = context,
                    dnsProvider = provider,
                    isTurboEnabled = isNetworkTurboEnabled.value,
                    videoCodecMode = codecMode,
                    isBackgroundAudioEnabled = enabled
                )
            }
        }
        // Pulihkan seluruh tab yang terbuka (termasuk saat APK di-update) & jalankan deteksi update otomatis
        viewModelScope.launch {
            restoreSavedTabsOrDefault()
            updateManager.checkForUpdatesSilently("https://api.github.com/repos/arbdevai/zenvora/releases/latest")
        }
    }

    private suspend fun restoreSavedTabsOrDefault() {
        isRestoringTabs = true
        try {
            val savedJson = preferencesRepository.savedTabsJson.first()
            val savedActiveId = preferencesRepository.savedActiveTabId.first()
            if (savedJson.isNotEmpty()) {
                val jsonArray = JSONArray(savedJson)
                var restoredCount = 0
                var targetActiveId = ""
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val url = obj.optString("url", "about:blank").ifEmpty { "about:blank" }
                    val title = obj.optString("title", "Tab Baru").ifEmpty { "Tab Baru" }
                    val isDesktop = obj.optBoolean("isDesktopMode", false)

                    val session = BrowserSession(id, isIncognito = false)
                    session.adblockEnabled = isAdblockEnabled.value
                    session.onXpiInstallRequested = { xpiUri ->
                        extensionManager.installFromUrl(xpiUri)
                    }
                    session.initSession(
                        context = context,
                        runtime = runtime,
                        mediaSessionDelegate = mediaNotificationManager.createGeckoMediaDelegate {
                            session.tabState.value.title
                        }
                    )
                    if (isDesktop) {
                        session.configureInitialDesktopMode(true)
                    }
                    sessionsMap[id] = session

                    val restoredTab = BrowserTab(
                        id = id,
                        url = url,
                        title = title,
                        isIncognito = false,
                        isDesktopMode = isDesktop
                    )
                    _tabs.update { it + restoredTab }
                    if (id == savedActiveId || targetActiveId.isEmpty()) {
                        targetActiveId = id
                    }
                    if (url != "about:blank") {
                        session.loadUri(url)
                    }
                    observeSession(session)
                    restoredCount++
                }
                if (restoredCount > 0) {
                    selectTab(targetActiveId)
                    isRestoringTabs = false
                    return
                }
            }
        } catch (_: Exception) {
            // Jika format lama/rusak, buat tab standar baru
        }
        isRestoringTabs = false
        createNewTab("about:blank")
    }

    private fun persistTabsToStorage() {
        if (isRestoringTabs) return
        val currentTabs = _tabs.value.filter { !it.isIncognito }
        val activeId = _activeTabId.value
        viewModelScope.launch {
            try {
                val arr = JSONArray()
                for (tab in currentTabs) {
                    val obj = JSONObject().apply {
                        put("id", tab.id)
                        put("url", tab.url)
                        put("title", tab.title)
                        put("isDesktopMode", tab.isDesktopMode)
                    }
                    arr.put(obj)
                }
                preferencesRepository.saveTabsSession(arr.toString(), activeId)
            } catch (_: Exception) {
                // Abaikan kegagalan serialisasi minor
            }
        }
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
        session.onXpiInstallRequested = { xpiUri ->
            extensionManager.installFromUrl(xpiUri)
        }
        session.initSession(
            context = context,
            runtime = runtime,
            mediaSessionDelegate = mediaNotificationManager.createGeckoMediaDelegate {
                session.tabState.value.title
            }
        )
        sessionsMap[tabId] = session

        val newTab = BrowserTab(
            id = tabId,
            url = initialUrl,
            title = if (isIncognito) "Tab Samaran" else "Tab Baru",
            isIncognito = isIncognito
        )

        _tabs.update { it + newTab }
        selectTab(tabId)
        persistTabsToStorage()

        if (initialUrl != "about:blank") {
            session.loadUri(initialUrl)
        }

        observeSession(session)
    }

    private fun observeSession(session: BrowserSession) {
        var lastPersistedUrl = ""
        var lastPersistedTitle = ""
        var lastPersistedDesktop = false
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
                if (!updatedTab.isIncognito &&
                    (updatedTab.url != lastPersistedUrl ||
                        updatedTab.title != lastPersistedTitle ||
                        updatedTab.isDesktopMode != lastPersistedDesktop)
                ) {
                    lastPersistedUrl = updatedTab.url
                    lastPersistedTitle = updatedTab.title
                    lastPersistedDesktop = updatedTab.isDesktopMode
                    persistTabsToStorage()
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

        viewModelScope.launch {
            session.detectedVideoStream.collect { stream ->
                if (session.tabId == _activeTabId.value) {
                    _detectedVideoStream.value = stream
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
                _detectedVideoStream.value = session.detectedVideoStream.value
            }
            persistTabsToStorage()
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
        persistTabsToStorage()
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
        persistTabsToStorage()
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
            lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("about:") ||
                lower.startsWith("moz-extension://") ||
                lower.startsWith("file://") -> input
            isLocalOrIpAddress(input) -> "http://$input"
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> {
                val encodedQuery = URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
                "${searchEngineUrl.value}$encodedQuery"
            }
        }
    }

    private fun isLocalOrIpAddress(rawInput: String): Boolean {
        if (rawInput.contains(" ")) return false
        val hostPort = rawInput.substringBefore('/').substringBefore('?').trim()
        val host = hostPort.substringBefore(':').lowercase()
        val portPart = if (hostPort.contains(':')) hostPort.substringAfter(':') else ""

        // Deteksi alamat IPv4 (contoh: 192.168.1.1, 10.0.0.1, 172.16.0.1, 127.0.0.1)
        val ipv4Parts = host.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        ) {
            return true
        }

        // Deteksi host jaringan lokal / router / custom port
        if (host == "localhost" ||
            host.endsWith(".local") ||
            host.endsWith(".lan") ||
            host.endsWith(".home") ||
            host.endsWith(".internal") ||
            host.endsWith(".router")
        ) {
            return true
        }

        if (portPart.isNotEmpty() && portPart.all { it.isDigit() } && portPart != "443") {
            return true
        }

        return false
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

    fun exitFullScreen() {
        getActiveSession()?.exitFullScreen()
    }

    fun applyVideoObjectFit(objectFitValue: String) {
        getActiveSession()?.applyVideoObjectFit(objectFitValue)
    }

    fun openNativeVideoPlayer(stream: DetectedVideoStream) {
        getActiveSession()?.pauseWebVideos()
        if (_activeTabState.value.isFullScreen) {
            getActiveSession()?.exitFullScreen()
        }
        _activeNativeVideoStream.value = stream
    }

    fun closeNativeVideoPlayer() {
        _activeNativeVideoStream.value = null
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
            val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
            GeckoRuntimeManager.writeGeckoConfigFile(
                context = context,
                dnsProvider = provider,
                isTurboEnabled = next,
                videoCodecMode = codecMode,
                isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
            )
            if (next) {
                networkSpeedManager.warmupAndMeasureLatency(_activeTabState.value.url)
            }
        }
    }

    fun selectDnsProvider(provider: DnsProvider) {
        viewModelScope.launch {
            preferencesRepository.setDnsProviderId(provider.id)
            networkSpeedManager.setDnsProvider(provider)
            val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
            GeckoRuntimeManager.writeGeckoConfigFile(
                context = context,
                dnsProvider = provider,
                isTurboEnabled = isNetworkTurboEnabled.value,
                videoCodecMode = codecMode,
                isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
            )
            networkSpeedManager.warmupAndMeasureLatency(_activeTabState.value.url)
        }
    }

    fun selectVideoCodecMode(mode: VideoCodecMode) {
        viewModelScope.launch {
            preferencesRepository.setVideoCodecModeId(mode.id)
            val provider = DnsProvider.fromId(dnsProviderId.value)
            GeckoRuntimeManager.writeGeckoConfigFile(
                context = context,
                dnsProvider = provider,
                isTurboEnabled = isNetworkTurboEnabled.value,
                videoCodecMode = mode,
                isBackgroundAudioEnabled = isBackgroundAudioEnabled.value
            )
        }
    }

    fun toggleBackgroundAudio() {
        viewModelScope.launch {
            val next = !isBackgroundAudioEnabled.value
            preferencesRepository.setBackgroundAudioEnabled(next)
            val provider = DnsProvider.fromId(dnsProviderId.value)
            val codecMode = VideoCodecMode.fromId(videoCodecModeId.value)
            GeckoRuntimeManager.writeGeckoConfigFile(
                context = context,
                dnsProvider = provider,
                isTurboEnabled = isNetworkTurboEnabled.value,
                videoCodecMode = codecMode,
                isBackgroundAudioEnabled = next
            )
        }
    }

    fun onAppBackgrounded() {
        if (isBackgroundAudioEnabled.value) {
            getActiveSession()?.keepBackgroundMediaAlive(true)
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

    fun installRecommendedAddon(addon: RecommendedAddon) {
        extensionManager.installFromUrl(addon.xpiDownloadUrl, addon.id)
    }

    fun installCustomXpi(xpiUrl: String) {
        extensionManager.installFromUrl(xpiUrl)
    }

    fun toggleExtension(info: InstalledExtensionInfo, enable: Boolean) {
        extensionManager.toggleExtension(info, enable)
    }

    fun uninstallExtension(info: InstalledExtensionInfo) {
        extensionManager.uninstallExtension(info)
    }

    fun refreshExtensions() {
        extensionManager.refreshInstalledExtensions()
    }

    fun clearExtensionStatusMessage() {
        extensionManager.clearStatusMessage()
    }

    fun addSavedLogin(siteInput: String, username: String, passwordValue: String) {
        viewModelScope.launch {
            val normalizedUrl = resolveUrlOrQuery(siteInput.trim())
            val extractedHost = try {
                URI(normalizedUrl).host?.removePrefix("www.") ?: siteInput.trim()
            } catch (_: Exception) {
                siteInput.trim()
            }
            val existing = database.savedLoginDao().findLoginByHostAndUser(extractedHost, username.trim())
            if (existing != null) {
                database.savedLoginDao().insertSavedLogin(
                    existing.copy(
                        siteUrl = normalizedUrl,
                        passwordValue = passwordValue,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                database.savedLoginDao().insertSavedLogin(
                    SavedLoginEntity(
                        host = extractedHost,
                        siteUrl = normalizedUrl,
                        username = username.trim(),
                        passwordValue = passwordValue
                    )
                )
            }
        }
    }

    fun deleteSavedLogin(id: Long) {
        viewModelScope.launch {
            database.savedLoginDao().deleteSavedLoginById(id)
        }
    }

    fun openAndAutofillLogin(login: SavedLoginEntity) {
        loadInput(login.siteUrl)
        viewModelScope.launch {
            delay(1500L)
            getActiveSession()?.autofillCredentials(login.username, login.passwordValue)
        }
    }

    fun autofillLoginOnCurrentPage(login: SavedLoginEntity) {
        getActiveSession()?.autofillCredentials(login.username, login.passwordValue)
    }

    override fun onCleared() {
        super.onCleared()
        mediaNotificationManager.release()
        sessionsMap.values.forEach { it.close() }
        sessionsMap.clear()
    }
}
