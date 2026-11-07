package com.zenvora.browser.core.engine

import android.content.Context
import android.net.Uri
import com.zenvora.browser.core.adblock.AdblockEngine
import com.zenvora.browser.data.local.entity.SavedLoginEntity
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import com.zenvora.browser.ui.components.DetectedVideoStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
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

    private val _detectedVideoStream = MutableStateFlow<DetectedVideoStream?>(null)
    val detectedVideoStream: StateFlow<DetectedVideoStream?> = _detectedVideoStream.asStateFlow()

    private var isOpen = false
    private var desktopModeEnabled = false
    var adblockEnabled = true
    var externalAdblockActive = false
    var onXpiInstallRequested: ((String) -> Unit)? = null
    var onLoginCaptured: ((SavedLoginEntity) -> Unit)? = null

    fun initSession(
        context: Context,
        runtime: GeckoRuntime,
        mediaSessionDelegate: org.mozilla.geckoview.MediaSession.Delegate? = null
    ) {
        if (!isOpen) {
            setupDelegates(mediaSessionDelegate)
            geckoSession.open(runtime)
            geckoSession.setActive(true)
            isOpen = true
        }
    }

    fun keepBackgroundMediaAlive(enabled: Boolean) {
        if (isOpen && enabled) {
            geckoSession.setActive(true)
        }
    }

    private fun applyDesktopSettings(enabled: Boolean, targetUrl: String = _tabState.value.url) {
        val isAmoStore = targetUrl.contains("addons.mozilla.org", ignoreCase = true)
        if (enabled) {
            geckoSession.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            geckoSession.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            geckoSession.settings.userAgentOverride = DESKTOP_USER_AGENT
        } else if (isAmoStore) {
            // Gunakan User-Agent Desktop Firefox 136 khusus di addons.mozilla.org agar semua ekstensi (termasuk Claude Sidebar Free) aktif dan tidak terkunci di HP
            geckoSession.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            geckoSession.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_MOBILE
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

    private fun setupDelegates(mediaSessionDelegate: org.mozilla.geckoview.MediaSession.Delegate? = null) {
        if (mediaSessionDelegate != null) {
            geckoSession.mediaSessionDelegate = mediaSessionDelegate
        }
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                if (url.startsWith("https://zenvora.stream.detect/") ||
                    url.startsWith("https://zenvora.auth.capture/") ||
                    url.startsWith("https://zenvora.cmd.local/")
                ) {
                    return
                }
                _detectedVideoStream.value = null
                applyDesktopSettings(desktopModeEnabled, url)
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
                val targetUri = request.uri

                // 0a. Tangkap sinyal deteksi aliran video (.m3u8 / .mp4 / .mpd) lintas-iframe untuk Zenvora Native Player
                if (targetUri.startsWith("https://zenvora.stream.detect/")) {
                    try {
                        val parsed = Uri.parse(targetUri)
                        val streamSrc = parsed.getQueryParameter("src").orEmpty()
                        val referer = parsed.getQueryParameter("ref").orEmpty().ifEmpty { _tabState.value.url }
                        if (streamSrc.startsWith("http")) {
                            _detectedVideoStream.value = DetectedVideoStream(
                                streamUrl = streamSrc,
                                refererUrl = referer,
                                title = _tabState.value.title.ifEmpty { "Video Streaming" }
                            )
                        }
                    } catch (_: Exception) {}
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 0b. Tangkap kredensial form login otomatis untuk Zenvora Password Manager
                if (targetUri.startsWith("https://zenvora.auth.capture/")) {
                    if (!isIncognito) {
                        try {
                            val parsed = Uri.parse(targetUri)
                            val host = parsed.getQueryParameter("host").orEmpty().trim()
                            val siteUrl = parsed.getQueryParameter("url").orEmpty().trim().ifEmpty { _tabState.value.url }
                            val username = parsed.getQueryParameter("user").orEmpty().trim()
                            val passwordVal = parsed.getQueryParameter("pass").orEmpty()
                            if (host.isNotEmpty() && username.isNotEmpty() && passwordVal.isNotEmpty()) {
                                onLoginCaptured?.invoke(
                                    SavedLoginEntity(
                                        host = host,
                                        siteUrl = siteUrl,
                                        username = username,
                                        passwordValue = passwordVal
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                applyDesktopSettings(desktopModeEnabled, targetUri)

                // 0. Tangkap instalasi langsung file ekstensi .xpi dari Zenvora / Mozilla Add-ons Store
                if (targetUri.startsWith("https://") &&
                    (targetUri.endsWith(".xpi") ||
                        targetUri.contains(".xpi?") ||
                        targetUri.contains("addons.mozilla.org/firefox/downloads/") ||
                        targetUri.contains("addons.mozilla.org/android/downloads/"))
                ) {
                    onXpiInstallRequested?.invoke(targetUri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 1. Blokir permintaan / redirect ke domain iklan & pop-under via Native Rust AdblockEngine
                // Jika uBlock Origin sedang aktif (externalAdblockActive), lewati pemeriksaan ganda JNI agar loading web 2x lebih cepat
                if (adblockEnabled && !externalAdblockActive && targetUri.startsWith("http") && AdblockEngine.isUrlBlocked(targetUri)) {
                    _tabState.update {
                        it.copy(blockedRequestsCount = it.blockedRequestsCount + 1)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 2. Blokir pembukaan tab pop-under paksa (TARGET_WINDOW_NEW) dari script situs streaming
                if (adblockEnabled && request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW) {
                    if (!request.hasUserGesture || (!externalAdblockActive && AdblockEngine.isUrlBlocked(targetUri))) {
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
        applyDesktopSettings(desktopModeEnabled, uri)
        val normalized = rewriteMobileUrlForDesktop(uri)
        geckoSession.loadUri(normalized)
    }

    fun applyVideoObjectFit(fitMode: String) {
        try {
            val safeFit = JSONObject.quote(fitMode)
            val js = """
                javascript:(function(){
                  try {
                    var mode = $safeFit;
                    var vids = document.querySelectorAll('video');
                    for (var i = 0; i < vids.length; i++) {
                      var v = vids[i];
                      v.style.setProperty('transform-origin', 'center center', 'important');
                      if (mode === 'stretch_20_9') {
                        v.style.setProperty('object-fit', 'fill', 'important');
                        v.style.setProperty('transform', 'scale(1.25, 1.03)', 'important');
                      } else if (mode === 'zoom_20_9') {
                        v.style.setProperty('object-fit', 'cover', 'important');
                        v.style.setProperty('transform', 'scale(1.26, 1.26)', 'important');
                      } else {
                        v.style.setProperty('object-fit', 'contain', 'important');
                        v.style.setProperty('transform', 'scale(1, 1)', 'important');
                      }
                    }
                  } catch (e) {}
                })();
            """.trimIndent().replace("\n", " ")
            geckoSession.loadUri(js)
        } catch (_: Exception) {}
    }

    fun pauseWebVideos() {
        try {
            val js = """
                javascript:(function(){
                  try {
                    var vids = document.querySelectorAll('video');
                    for (var i = 0; i < vids.length; i++) {
                      vids[i].pause();
                    }
                  } catch (e) {}
                })();
            """.trimIndent().replace("\n", " ")
            geckoSession.loadUri(js)
        } catch (_: Exception) {}
    }

    fun autofillCredentials(username: String, passwordValue: String) {
        try {
            val escapedUser = JSONObject.quote(username)
            val escapedPass = JSONObject.quote(passwordValue)
            val js = """
                javascript:(function(){
                  try {
                    var setVal = function(el, val) {
                      if (!el) return;
                      var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                      if (setter && setter.set) {
                        setter.set.call(el, val);
                      } else {
                        el.value = val;
                      }
                      el.dispatchEvent(new Event('input', { bubbles: true }));
                      el.dispatchEvent(new Event('change', { bubbles: true }));
                    };
                    var passInputs = document.querySelectorAll('input[type="password"]');
                    for (var i = 0; i < passInputs.length; i++) {
                      setVal(passInputs[i], $escapedPass);
                    }
                    var userInputs = document.querySelectorAll('input[type="email"], input[type="text"], input[name*="user"], input[name*="login"], input[name*="email"], input[id*="user"], input[id*="login"]');
                    for (var j = 0; j < userInputs.length; j++) {
                      var inp = userInputs[j];
                      if (inp.type !== 'hidden' && inp.offsetParent !== null) {
                        setVal(inp, $escapedUser);
                        break;
                      }
                    }
                  } catch (e) {}
                })();
            """.trimIndent().replace("\n", " ")
            geckoSession.loadUri(js)
        } catch (_: Exception) {
            // Abaikan jika gagal mengeksekusi script autofill
        }
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
        geckoSession.setActive(true)
        _tabState.update { it.copy(isFullScreen = false) }
    }

    fun configureInitialDesktopMode(enabled: Boolean) {
        desktopModeEnabled = enabled
        applyDesktopSettings(enabled)
        _tabState.update { it.copy(isDesktopMode = enabled) }
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
