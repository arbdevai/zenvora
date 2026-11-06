package com.zenvora.browser.core.engine

import android.content.Context
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import java.net.URI

class BrowserSession(
    val tabId: String,
    val geckoSession: GeckoSession = GeckoSession()
) {
    private val _tabState = MutableStateFlow(
        BrowserTab(
            id = tabId,
            url = "about:blank",
            title = "Tab Baru"
        )
    )
    val tabState: StateFlow<BrowserTab> = _tabState.asStateFlow()

    private val _securityInfo = MutableStateFlow(SecurityInfo())
    val securityInfo: StateFlow<SecurityInfo> = _securityInfo.asStateFlow()

    private var isOpen = false

    fun initSession(context: Context, runtime: GeckoRuntime) {
        if (!isOpen) {
            setupDelegates()
            geckoSession.open(runtime)
            isOpen = true
        }
    }

    private fun setupDelegates() {
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                _tabState.update {
                    it.copy(
                        url = url,
                        isLoading = true,
                        progress = 10
                    )
                }
                updateSecurityInfo(url, isSecure = false)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                _tabState.update {
                    it.copy(
                        isLoading = false,
                        progress = 100
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
                        certificateSubject = securityInfo.subjectName,
                        certificateIssuer = securityInfo.issuerName
                    )
                }
            }
        }

        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                val safeUrl = url ?: "about:blank"
                _tabState.update { it.copy(url = safeUrl) }
                updateSecurityInfo(safeUrl, _tabState.value.isSecure)
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                _tabState.update { it.copy(canGoBack = canGoBack) }
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                _tabState.update { it.copy(canGoForward = canGoForward) }
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
        geckoSession.loadUri(uri)
    }

    fun goBack(): Boolean {
        if (_tabState.value.canGoBack) {
            geckoSession.goBack()
            return true
        }
        return false
    }

    fun goForward(): Boolean {
        if (_tabState.value.canGoForward) {
            geckoSession.goForward()
            return true
        }
        return false
    }

    fun reload() {
        geckoSession.reload()
    }

    fun close() {
        if (isOpen) {
            geckoSession.close()
            isOpen = false
        }
    }
}
