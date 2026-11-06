package com.zenvora.browser.data.model

data class BrowserTab(
    val id: String,
    val url: String = "about:blank",
    val title: String = "Tab Baru",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val blockedRequestsCount: Int = 0,
    val isDesktopMode: Boolean = false,
    val isFindInPageActive: Boolean = false,
    val findQuery: String = "",
    val isIncognito: Boolean = false,
    val isFullScreen: Boolean = false
)
