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
import com.zenvora.browser.ui.components.FindInPageBar
import com.zenvora.browser.ui.components.HistoryBookmarkSheet
import com.zenvora.browser.ui.components.SecurityDetailDialog
import com.zenvora.browser.ui.components.SettingsSheet
import com.zenvora.browser.ui.components.TabOverviewSheet
import com.zenvora.browser.ui.components.UpdateDialog
import com.zenvora.browser.ui.theme.AccentTeal
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
    BackHandler {
        when {
            activeTabState.isFindInPageActive -> viewModel.closeFindInPage()
            showSecurityDialog -> showSecurityDialog = false
            showTabOverview -> showTabOverview = false
            showHistorySheet -> showHistorySheet = false
            showSettingsSheet -> showSettingsSheet = false
            activeTabState.canGoBack -> viewModel.goBack()
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
            // Find in Page Bar
            if (activeTabState.isFindInPageActive) {
                FindInPageBar(
                    query = activeTabState.findQuery,
                    onQueryChange = { viewModel.findInPage(it) },
                    onNext = { viewModel.findNext() },
                    onPrevious = { viewModel.findPrevious() },
                    onClose = { viewModel.closeFindInPage() }
                )
            }
            // Indikator Progres Loading Tipis
            AnimatedVisibility(
                visible = activeTabState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { activeTabState.progress / 100f },
                    color = AccentTeal,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                )
            }

            // Area Tampilan Web GeckoView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                BrowserGeckoView(
                    activeSession = viewModel.getActiveSession(),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Omnibox Bar di Bagian Bawah
            BrowserOmnibox(
                currentUrl = activeTabState.url,
                isLoading = activeTabState.isLoading,
                securityInfo = securityInfo,
                blockedCount = activeTabState.blockedRequestsCount,
                tabsCount = tabs.size,
                isBookmarked = isBookmarked,
                isIncognito = activeTabState.isIncognito,
                onNavigate = { viewModel.loadInput(it) },
                onReload = { viewModel.reload() },
                onSecurityClick = { showSecurityDialog = true },
                onTabsClick = { showTabOverview = true },
                onBookmarkClick = {
                    viewModel.toggleBookmark(activeTabState.url, activeTabState.title)
                },
                onHistoryClick = { showHistorySheet = true },
                onSettingsClick = { showSettingsSheet = true }
            )
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
