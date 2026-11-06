package com.zenvora.browser.core.engine

import android.content.Context
import com.zenvora.browser.core.adblock.AdblockEngine
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.net.URI

class BrowserSession(
    val tabId: String,
    val isIncognito: Boolean = false,
    val geckoSession: GeckoSession = createGeckoSession(isIncognito)
) {
    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0"

        private fun createGeckoSession(isIncognito: Boolean): GeckoSession {
            val settings = GeckoSessionSettings.Builder()
                .usePrivateMode(isIncognito)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .fullAccessibilityTree(false)
                .build()
            return GeckoSession(settings)
        }
    }

    private val _tabState = MutableStateFlow(
        BrowserTab(
            id = tabId,
            url = "about:blank",
            title = if (isIncognito) "Tab Samaran" else "Tab Baru",
            isIncognito = isIncognito
        )
    )
    val tabState: StateFlow<BrowserTab> = _tabState.asStateFlow()

    private val _securityInfo = MutableStateFlow(SecurityInfo())
    val securityInfo: StateFlow<SecurityInfo> = _securityInfo.asStateFlow()

    private var isOpen = false
    private var desktopModeEnabled = false
    var adblockEnabled = true

    fun initSession(context: Context, runtime: GeckoRuntime) {
        if (!isOpen) {
            setupDelegates()
            geckoSession.open(runtime)
            isOpen = true
        }
    }

    private fun applyDesktopSettings(enabled: Boolean) {
        if (enabled) {
            geckoSession.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            geckoSession.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            geckoSession.settings.userAgentOverride = DESKTOP_USER_AGENT
        } else {
            geckoSession.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            geckoSession.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            geckoSession.settings.userAgentOverride = null
        }
    }

    private fun rewriteMobileUrlForDesktop(rawUrl: String): String {
        if (!desktopModeEnabled || !rawUrl.startsWith("http")) return rawUrl
        return try {
            val uri = URI(rawUrl)
            val host = uri.host ?: return rawUrl
            val newHost = when {
                host.startsWith("m.") && host.length > 2 -> "www.${host.substring(2)}"
                host.startsWith("mobile.") && host.length > 7 -> "www.${host.substring(7)}"
                host.contains(".m.") -> host.replace(".m.", ".")
                else -> host
            }
            if (newHost != host) {
                URI(
                    uri.scheme,
                    uri.userInfo,
                    newHost,
                    uri.port,
                    uri.path,
                    uri.query,
                    uri.fragment
                ).toString()
            } else {
                rawUrl
            }
        } catch (_: Exception) {
            rawUrl
        }
    }

    private fun setupDelegates() {
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                applyDesktopSettings(desktopModeEnabled)
                _tabState.update {
                    it.copy(
                        url = url,
                        isLoading = true,
                        progress = 10,
                        isDesktopMode = desktopModeEnabled
                    )
                }
                updateSecurityInfo(url, isSecure = false)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                _tabState.update {
                    it.copy(
                        isLoading = false,
                        progress = 100,
                        isDesktopMode = desktopModeEnabled
                    )
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                _tabState.update {
                    it.copy(
                        progress = progress,
                        isLoading = progress < 100
                    )
                }
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                val isSecure = securityInfo.isSecure
                _tabState.update { it.copy(isSecure = isSecure) }
                _securityInfo.update {
                    it.copy(
                        status = if (isSecure) SecurityStatus.SECURE else SecurityStatus.INSECURE,
                        host = securityInfo.origin ?: it.host
                    )
                }
            }
        }

        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny> {
                applyDesktopSettings(desktopModeEnabled)
                val targetUri = request.uri

                // 1. Blokir permintaan / redirect ke domain iklan & pop-under via Native Rust AdblockEngine
                if (adblockEnabled && targetUri.startsWith("http") && AdblockEngine.isUrlBlocked(targetUri)) {
                    _tabState.update {
                        it.copy(blockedRequestsCount = it.blockedRequestsCount + 1)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 2. Blokir pembukaan tab pop-under paksa (TARGET_WINDOW_NEW) dari script situs streaming
                if (adblockEnabled && request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW) {
                    if (!request.hasUserGesture || AdblockEngine.isUrlBlocked(targetUri)) {
                        _tabState.update {
                            it.copy(blockedRequestsCount = it.blockedRequestsCount + 1)
                        }
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }

                // 3. Pertahankan Mode Desktop dengan mengubah subdomain mobile (m.) ke desktop (www.)
                if (desktopModeEnabled && targetUri.startsWith("http")) {
                    val desktopUri = rewriteMobileUrlForDesktop(targetUri)
                    if (desktopUri != targetUri) {
                        session.loadUri(desktopUri)
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession> {
                // Blokir pop-up / pop-under window baru yang sering muncul saat klik video streaming
                if (adblockEnabled) {
                    _tabState.update {
                        it.copy(blockedRequestsCount = it.blockedRequestsCount + 1)
                    }
                }
                return GeckoResult.fromValue(null)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                val safeUrl = url ?: "about:blank"
                _tabState.update {
                    it.copy(
                        url = safeUrl,
                        isDesktopMode = desktopModeEnabled
                    )
                }
                updateSecurityInfo(safeUrl, _tabState.value.isSecure)
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                _tabState.update { it.copy(canGoBack = canGoBack) }
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                _tabState.update { it.copy(canGoForward = canGoForward) }
            }
        }

        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                val cleanTitle = title?.trim().orEmpty()
                if (cleanTitle.isNotEmpty()) {
                    _tabState.update { it.copy(title = cleanTitle) }
                }
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                _tabState.update { it.copy(isFullScreen = fullScreen) }
            }
        }

        geckoSession.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(
                session: GeckoSession,
                event: ContentBlocking.BlockEvent
            ) {
                _tabState.update {
                    it.copy(blockedRequestsCount = it.blockedRequestsCount + 1)
                }
            }
        }
    }

    private fun updateSecurityInfo(url: String, isSecure: Boolean) {
        try {
            val uri = URI(url)
            val scheme = uri.scheme ?: ""
            val host = uri.host ?: ""
            val status = when {
                scheme.equals("https", ignoreCase = true) -> SecurityStatus.SECURE
                scheme.equals("about", ignoreCase = true) -> SecurityStatus.INTERNAL
                scheme.equals("http", ignoreCase = true) -> SecurityStatus.INSECURE
                else -> SecurityStatus.UNKNOWN
            }
            _securityInfo.update {
                it.copy(
                    status = status,
                    host = host,
                    scheme = scheme
                )
            }
        } catch (_: Exception) {
            _securityInfo.update {
                it.copy(
                    status = SecurityStatus.UNKNOWN,
                    host = "",
                    scheme = ""
                )
            }
        }
    }

    fun loadUri(uri: String) {
        applyDesktopSettings(desktopModeEnabled)
        val normalized = rewriteMobileUrlForDesktop(uri)
        geckoSession.loadUri(normalized)
    }

    fun goBack(): Boolean {
        if (_tabState.value.canGoBack) {
            applyDesktopSettings(desktopModeEnabled)
            geckoSession.goBack()
            return true
        }
        return false
    }

    fun goForward(): Boolean {
        if (_tabState.value.canGoForward) {
            applyDesktopSettings(desktopModeEnabled)
            geckoSession.goForward()
            return true
        }
        return false
    }

    fun reload() {
        applyDesktopSettings(desktopModeEnabled)
        geckoSession.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
    }

    fun exitFullScreen() {
        geckoSession.exitFullScreen()
        _tabState.update { it.copy(isFullScreen = false) }
    }

    fun setDesktopMode(enabled: Boolean) {
        desktopModeEnabled = enabled
        applyDesktopSettings(enabled)
        _tabState.update { it.copy(isDesktopMode = enabled) }
        val currentUrl = _tabState.value.url
        if (currentUrl.startsWith("http")) {
            val targetUrl = if (enabled) rewriteMobileUrlForDesktop(currentUrl) else currentUrl
            if (targetUrl != currentUrl) {
                geckoSession.loadUri(targetUrl)
            } else {
                geckoSession.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
            }
        }
    }

    fun findInPage(query: String) {
        _tabState.update { it.copy(isFindInPageActive = true, findQuery = query) }
        if (query.isNotEmpty()) {
            geckoSession.finder.find(query, 0)
        } else {
            geckoSession.finder.clear()
        }
    }

    fun findNext() {
        val query = _tabState.value.findQuery
        if (query.isNotEmpty()) {
            geckoSession.finder.find(query, 0)
        }
    }

    fun findPrevious() {
        val query = _tabState.value.findQuery
        if (query.isNotEmpty()) {
            geckoSession.finder.find(query, GeckoSession.FINDER_FIND_BACKWARDS)
        }
    }

    fun clearFindInPage() {
        _tabState.update { it.copy(isFindInPageActive = false, findQuery = "") }
        geckoSession.finder.clear()
    }

    fun close() {
        if (isOpen) {
            geckoSession.close()
            isOpen = false
        }
    }
}
