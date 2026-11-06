package com.zenvora.browser.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var showTabOverview by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val isBookmarked = remember(bookmarks, activeTabState.url) {
        bookmarks.any { it.url == activeTabState.url }
    }

    // Penanganan tombol kembali Android
    BackHandler(
        enabled = activeTabState.isFindInPageActive ||
            showSecurityDialog ||
            showTabOverview ||
            showHistorySheet ||
            showSettingsSheet ||
            activeTabState.canGoBack ||
            activeTabState.url != "about:blank"
    ) {
        when {
            activeTabState.isFindInPageActive -> viewModel.closeFindInPage()
            showSecurityDialog -> showSecurityDialog = false
            showTabOverview -> showTabOverview = false
            showHistorySheet -> showHistorySheet = false
            showSettingsSheet -> showSettingsSheet = false
            activeTabState.canGoBack -> viewModel.goBack()
            activeTabState.url != "about:blank" -> viewModel.navigateHome()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = OledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(OledBlack)
        ) {
            // 1. Top Toolbar ala Google Chrome Android
            BrowserOmnibox(
                currentUrl = activeTabState.url,
                isLoading = activeTabState.isLoading,
                canGoForward = activeTabState.canGoForward,
                isDesktopMode = activeTabState.isDesktopMode,
                securityInfo = securityInfo,
                blockedCount = activeTabState.blockedRequestsCount,
                tabsCount = tabs.size,
                isBookmarked = isBookmarked,
                isIncognito = activeTabState.isIncognito,
                onNavigate = { viewModel.loadInput(it) },
                onHomeClick = { viewModel.navigateHome() },
                onReload = { viewModel.reload() },
                onForwardClick = { viewModel.goForward() },
                onNewTabClick = { viewModel.createNewTab(isIncognito = false) },
                onNewIncognitoTabClick = { viewModel.createNewTab(isIncognito = true) },
                onFindInPageClick = { viewModel.startFindInPage() },
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onSecurityClick = { showSecurityDialog = true },
                onTabsClick = { showTabOverview = true },
                onBookmarkClick = {
                    viewModel.toggleBookmark(activeTabState.url, activeTabState.title)
                },
                onHistoryClick = { showHistorySheet = true },
                onSettingsClick = { showSettingsSheet = true }
            )

            // 2. Indikator Progres Loading Electric Orange Tepat di Bawah Address Bar (Gaya Chrome)
            AnimatedVisibility(
                visible = activeTabState.isLoading,
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

            // 3. Find in Page Bar (jika aktif)
            if (activeTabState.isFindInPageActive) {
                FindInPageBar(
                    query = activeTabState.findQuery,
                    onQueryChange = { viewModel.findInPage(it) },
                    onNext = { viewModel.findNext() },
                    onPrevious = { viewModel.findPrevious() },
                    onClose = { viewModel.closeFindInPage() }
                )
            }

            // 4. Area Konten Utama: Halaman Beranda Cepat ala Chrome atau GeckoView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(OledBlack)
            ) {
                BrowserGeckoView(
                    activeSession = viewModel.getActiveSession(),
                    modifier = Modifier.fillMaxSize()
                )

                if (activeTabState.url == "about:blank" || activeTabState.url.isEmpty()) {
                    val totalBlockedAllTabs = remember(tabs) {
                        tabs.sumOf { it.blockedRequestsCount }
                    }
                    ChromeNewTabPage(
                        isIncognito = activeTabState.isIncognito,
                        bookmarks = bookmarks,
                        totalBlockedCount = totalBlockedAllTabs,
                        onNavigate = { viewModel.loadInput(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal Sheet & Dialog
    if (showSecurityDialog) {
        SecurityDetailDialog(
            securityInfo = securityInfo,
            onDismissRequest = { showSecurityDialog = false }
        )
    }

    if (showTabOverview) {
        TabOverviewSheet(
            tabs = tabs,
            activeTabId = activeTabId,
            onTabSelected = { tabId ->
                viewModel.selectTab(tabId)
                showTabOverview = false
            },
            onTabClosed = { tabId ->
                viewModel.closeTab(tabId)
            },
            onNewTab = { isIncognito ->
                viewModel.createNewTab(isIncognito = isIncognito)
                showTabOverview = false
            },
            onDismissRequest = { showTabOverview = false }
        )
    }

    if (showHistorySheet) {
        HistoryBookmarkSheet(
            historyList = historyEntries,
            bookmarkList = bookmarks,
            onItemClick = { url ->
                viewModel.loadInput(url)
                showHistorySheet = false
            },
            onClearHistory = { viewModel.clearHistory() },
            onDismissRequest = { showHistorySheet = false }
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            isAdblockEnabled = isAdblockEnabled,
            isStrictPrivacy = isStrictPrivacy,
            isDesktopMode = activeTabState.isDesktopMode,
            searchEngineUrl = searchEngineUrl,
            totalBlockedCount = activeTabState.blockedRequestsCount,
            onToggleAdblock = { viewModel.toggleAdblock() },
            onToggleStrictPrivacy = { viewModel.toggleStrictPrivacy() },
            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
            onFindInPageClick = {
                showSettingsSheet = false
                viewModel.startFindInPage()
            },
            onSelectSearchEngine = { viewModel.setSearchEngine(it) },
            onClearAllData = {
                viewModel.clearAllBrowsingData()
                showSettingsSheet = false
            },
            onCheckUpdate = {
                showSettingsSheet = false
                viewModel.checkForUpdates()
            },
            onDismissRequest = { showSettingsSheet = false }
        )
    }

    // Dialog Update APK Mandiri
    UpdateDialog(
        state = updateState,
        onDownload = { info -> viewModel.downloadUpdate(info) },
        onInstall = { apkFile -> viewModel.triggerInstall(apkFile) },
        onDismissRequest = { viewModel.dismissUpdate() }
    )
}
