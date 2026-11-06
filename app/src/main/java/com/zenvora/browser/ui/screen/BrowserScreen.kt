package com.zenvora.browser.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenvora.browser.core.updater.UpdateState
import com.zenvora.browser.ui.components.BrowserGeckoView
import com.zenvora.browser.ui.components.BrowserOmnibox
import com.zenvora.browser.ui.components.ChromeNewTabPage
import com.zenvora.browser.ui.components.FindInPageBar
import com.zenvora.browser.ui.components.HistoryBookmarkSheet
import com.zenvora.browser.ui.components.SecurityDetailDialog
import com.zenvora.browser.ui.components.SettingsSheet
import com.zenvora.browser.ui.components.TabOverviewSheet
import com.zenvora.browser.ui.components.UpdateDialog
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.viewmodel.BrowserViewModel

private enum class ActiveFullScreenPage {
    BROWSER,
    TABS,
    HISTORY_BOOKMARKS,
    SETTINGS,
    SECURITY
}

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val activeTabState by viewModel.activeTabState.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val securityInfo by viewModel.securityInfo.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val historyEntries by viewModel.historyEntries.collectAsStateWithLifecycle()
    val isAdblockEnabled by viewModel.isAdblockEnabled.collectAsStateWithLifecycle()
    val isStrictPrivacy by viewModel.isStrictPrivacy.collectAsStateWithLifecycle()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsStateWithLifecycle()
    val networkStats by viewModel.networkStats.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var currentPage by remember { mutableStateOf(ActiveFullScreenPage.BROWSER) }
    var isToolbarVisible by remember { mutableStateOf(true) }

    val isBookmarked = remember(bookmarks, activeTabState.url) {
        bookmarks.any { it.url == activeTabState.url }
    }

    val isOnNewTabPage = activeTabState.url == "about:blank" || activeTabState.url.isEmpty()

    // Tampilkan kembali toolbar saat berpindah URL atau kembali ke beranda
    LaunchedEffect(activeTabState.url, activeTabId) {
        isToolbarVisible = true
    }

    // Mode Immersive Penuh untuk Video Streaming Fullscreen pada Layar HP Besar
    val view = LocalView.current
    LaunchedEffect(activeTabState.isFullScreen) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, view)

        if (activeTabState.isFullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Penanganan tombol kembali Android (termasuk keluar dari Video Fullscreen)
    BackHandler(
        enabled = activeTabState.isFullScreen ||
            updateState !is UpdateState.Idle ||
            currentPage != ActiveFullScreenPage.BROWSER ||
            activeTabState.isFindInPageActive ||
            activeTabState.canGoBack ||
            !isOnNewTabPage
    ) {
        when {
            activeTabState.isFullScreen -> viewModel.exitFullScreen()
            updateState !is UpdateState.Idle -> viewModel.dismissUpdate()
            currentPage != ActiveFullScreenPage.BROWSER -> currentPage = ActiveFullScreenPage.BROWSER
            activeTabState.isFindInPageActive -> viewModel.closeFindInPage()
            activeTabState.canGoBack -> viewModel.goBack()
            !isOnNewTabPage -> viewModel.navigateHome()
        }
    }

    Scaffold(
        contentWindowInsets = if (activeTabState.isFullScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
        containerColor = OledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { base ->
                    if (activeTabState.isFullScreen) base else base.padding(innerPadding)
                }
                .background(OledBlack)
        ) {
            if (updateState !is UpdateState.Idle) {
                UpdateDialog(
                    state = updateState,
                    onDownload = { info -> viewModel.downloadUpdate(info) },
                    onInstall = { apkFile -> viewModel.triggerInstall(apkFile) },
                    onDismissRequest = { viewModel.dismissUpdate() }
                )
            } else {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "screenTransition"
                ) { page ->
                    when (page) {
                        ActiveFullScreenPage.TABS -> {
                            TabOverviewSheet(
                                tabs = tabs,
                                activeTabId = activeTabId,
                                onTabSelected = { tabId ->
                                    viewModel.selectTab(tabId)
                                    currentPage = ActiveFullScreenPage.BROWSER
                                },
                                onTabClosed = { tabId ->
                                    viewModel.closeTab(tabId)
                                },
                                onNewTab = { isIncognito ->
                                    viewModel.createNewTab(isIncognito = isIncognito)
                                    currentPage = ActiveFullScreenPage.BROWSER
                                },
                                onDismissRequest = { currentPage = ActiveFullScreenPage.BROWSER }
                            )
                        }

                        ActiveFullScreenPage.HISTORY_BOOKMARKS -> {
                            HistoryBookmarkSheet(
                                historyList = historyEntries,
                                bookmarkList = bookmarks,
                                onItemClick = { url ->
                                    viewModel.loadInput(url)
                                    currentPage = ActiveFullScreenPage.BROWSER
                                },
                                onClearHistory = { viewModel.clearHistory() },
                                onDismissRequest = { currentPage = ActiveFullScreenPage.BROWSER }
                            )
                        }

                        ActiveFullScreenPage.SETTINGS -> {
                            val totalBlockedAllTabs = remember(tabs) {
                                tabs.sumOf { it.blockedRequestsCount }
                            }
                            SettingsSheet(
                                isAdblockEnabled = isAdblockEnabled,
                                isStrictPrivacy = isStrictPrivacy,
                                isDesktopMode = activeTabState.isDesktopMode,
                                searchEngineUrl = searchEngineUrl,
                                totalBlockedCount = totalBlockedAllTabs,
                                networkStats = networkStats,
                                onToggleAdblock = { viewModel.toggleAdblock() },
                                onToggleStrictPrivacy = { viewModel.toggleStrictPrivacy() },
                                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                                onToggleNetworkTurbo = { viewModel.toggleNetworkTurbo() },
                                onSelectDnsProvider = { viewModel.selectDnsProvider(it) },
                                onRefreshNetworkPing = { viewModel.refreshNetworkLatency() },
                                onFindInPageClick = {
                                    currentPage = ActiveFullScreenPage.BROWSER
                                    viewModel.startFindInPage()
                                },
                                onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                                onClearAllData = {
                                    viewModel.clearAllBrowsingData()
                                    currentPage = ActiveFullScreenPage.BROWSER
                                },
                                onCheckUpdate = {
                                    viewModel.checkForUpdates()
                                },
                                onDismissRequest = { currentPage = ActiveFullScreenPage.BROWSER }
                            )
                        }

                        ActiveFullScreenPage.SECURITY -> {
                            SecurityDetailDialog(
                                securityInfo = securityInfo,
                                onDismissRequest = { currentPage = ActiveFullScreenPage.BROWSER }
                            )
                        }

                        ActiveFullScreenPage.BROWSER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(OledBlack)
                            ) {
                                // 1. Top Toolbar ala Google Chrome Android (Otomatis sembunyi saat scroll ke bawah atau saat Video Fullscreen)
                                AnimatedVisibility(
                                    visible = !activeTabState.isFullScreen && (isToolbarVisible || isOnNewTabPage),
                                    enter = slideInVertically { height -> -height } + fadeIn(),
                                    exit = slideOutVertically { height -> -height } + fadeOut()
                                ) {
                                    BrowserOmnibox(
                                        currentUrl = activeTabState.url,
                                        isLoading = activeTabState.isLoading,
                                        canGoForward = activeTabState.canGoForward,
                                        isDesktopMode = activeTabState.isDesktopMode,
                                        securityInfo = securityInfo,
                                        blockedCount = activeTabState.blockedRequestsCount,
                                        tabsCount = tabs.size,
                                        isBookmarked = isBookmarked,
                                        networkSpeedText = networkStats.formattedDownloadSpeed,
                                        isIncognito = activeTabState.isIncognito,
                                        onNavigate = { viewModel.loadInput(it) },
                                        onHomeClick = { viewModel.navigateHome() },
                                        onReload = { viewModel.reload() },
                                        onForwardClick = { viewModel.goForward() },
                                        onNewTabClick = { viewModel.createNewTab(isIncognito = false) },
                                        onNewIncognitoTabClick = { viewModel.createNewTab(isIncognito = true) },
                                        onFindInPageClick = { viewModel.startFindInPage() },
                                        onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                                        onSecurityClick = { currentPage = ActiveFullScreenPage.SECURITY },
                                        onTabsClick = { currentPage = ActiveFullScreenPage.TABS },
                                        onBookmarkClick = {
                                            viewModel.toggleBookmark(activeTabState.url, activeTabState.title)
                                        },
                                        onHistoryClick = { currentPage = ActiveFullScreenPage.HISTORY_BOOKMARKS },
                                        onSettingsClick = { currentPage = ActiveFullScreenPage.SETTINGS }
                                    )
                                }

                                // 2. Indikator Progres Loading Electric Orange
                                AnimatedVisibility(
                                    visible = activeTabState.isLoading && !activeTabState.isFullScreen,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { activeTabState.progress / 100f },
                                        color = ElectricOrange,
                                        trackColor = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                    )
                                }

                                // 3. Find in Page Bar (jika aktif dan tidak sedang video fullscreen)
                                if (activeTabState.isFindInPageActive && !activeTabState.isFullScreen) {
                                    FindInPageBar(
                                        query = activeTabState.findQuery,
                                        onQueryChange = { viewModel.findInPage(it) },
                                        onNext = { viewModel.findNext() },
                                        onPrevious = { viewModel.findPrevious() },
                                        onClose = { viewModel.closeFindInPage() }
                                    )
                                }

                                // 4. Area Konten Utama: Isolasi penuh antara ChromeNewTabPage dan BrowserGeckoView
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(OledBlack)
                                ) {
                                    if (isOnNewTabPage) {
                                        val totalBlockedAllTabs = remember(tabs) {
                                            tabs.sumOf { it.blockedRequestsCount }
                                        }
                                        ChromeNewTabPage(
                                            isIncognito = activeTabState.isIncognito,
                                            bookmarks = bookmarks,
                                            totalBlockedCount = totalBlockedAllTabs,
                                            networkStats = networkStats,
                                            onNavigate = { viewModel.loadInput(it) },
                                            onOpenNetworkSettings = { currentPage = ActiveFullScreenPage.SETTINGS },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        BrowserGeckoView(
                                            activeSession = viewModel.getActiveSession(),
                                            onScrollDown = {
                                                if (isToolbarVisible) {
                                                    isToolbarVisible = false
                                                }
                                            },
                                            onScrollUp = {
                                                if (!isToolbarVisible) {
                                                    isToolbarVisible = true
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
