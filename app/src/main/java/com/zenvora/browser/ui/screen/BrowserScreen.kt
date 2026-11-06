package com.zenvora.browser.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class ActiveFullScreenPage {
    BROWSER,
    TABS,
    HISTORY_BOOKMARKS,
    SETTINGS,
    SECURITY
}

private enum class RedmiVideoFitMode(
    val label: String,
    val scaleX: Float,
    val scaleY: Float
) {
    PAS_LAYAR_20_9("Pas Layar 20:9", 1.24f, 1.02f),
    ZOOM_PENUH_20_9("Zoom Penuh", 1.25f, 1.25f),
    STANDAR_16_9("Rasio 16:9", 1.0f, 1.0f)
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
    var lastToolbarToggleMs by remember { mutableLongStateOf(0L) }

    var videoFitMode by remember { mutableStateOf(RedmiVideoFitMode.PAS_LAYAR_20_9) }
    var showVideoOverlayControls by remember { mutableStateOf(true) }
    var lastVideoTouchMs by remember { mutableLongStateOf(0L) }

    val isBookmarked = remember(bookmarks, activeTabState.url) {
        bookmarks.any { it.url == activeTabState.url }
    }

    val isOnNewTabPage = activeTabState.url == "about:blank" || activeTabState.url.isEmpty()

    LaunchedEffect(activeTabState.url, activeTabId) {
        isToolbarVisible = true
    }

    // Sembunyikan otomatis kontrol rasio video setelah 3.5 detik saat menonton fullscreen
    LaunchedEffect(activeTabState.isFullScreen, lastVideoTouchMs) {
        if (activeTabState.isFullScreen) {
            showVideoOverlayControls = true
            delay(3500L)
            showVideoOverlayControls = false
        }
    }

    // Mode Immersive Penuh untuk Video Streaming Fullscreen pada Layar Redmi 9C (20:9)
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

    // Seluruh navigasi kembali dikendalikan oleh tombol Back fisik/gesture HP
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

    val density = LocalDensity.current
    val toolbarHeightPx = with(density) { 57.dp.toPx() }
    val showTopBar = !activeTabState.isFullScreen && (isToolbarVisible || isOnNewTabPage)
    val animatedTopOffsetPx by animateFloatAsState(
        targetValue = if (showTopBar) toolbarHeightPx else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "toolbarOffsetPx"
    )

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
                    onInstall = { apkFile -> viewModel.triggerInstall(apkFile) }
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
                                }
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
                                onClearHistory = { viewModel.clearHistory() }
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
                                }
                            )
                        }

                        ActiveFullScreenPage.SECURITY -> {
                            SecurityDetailDialog(
                                securityInfo = securityInfo
                            )
                        }

                        ActiveFullScreenPage.BROWSER -> {
                            // Container Statis dengan Deteksi Scroll Anti-Glitch (Tanpa Resize SurfaceView)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .background(OledBlack)
                                    .pointerInput(isOnNewTabPage, activeTabState.isFullScreen) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Initial
                                            )
                                            if (activeTabState.isFullScreen) {
                                                // Tampilkan kontrol rasio layar saat menyentuh bagian atas video fullscreen
                                                if (down.position.y < 220f) {
                                                    lastVideoTouchMs = SystemClock.elapsedRealtime()
                                                }
                                            }
                                            var prevY = down.position.y
                                            var totalDy = 0f

                                            do {
                                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()
                                                if (change != null && change.pressed && !isOnNewTabPage && !activeTabState.isFullScreen) {
                                                    val stepDy = change.position.y - prevY
                                                    prevY = change.position.y

                                                    // Reset akumulator jika arah jari berbalik
                                                    if ((totalDy > 0f && stepDy < 0f) || (totalDy < 0f && stepDy > 0f)) {
                                                        totalDy = stepDy
                                                    } else {
                                                        totalDy += stepDy
                                                    }

                                                    val now = SystemClock.elapsedRealtime()
                                                    if (now - lastToolbarToggleMs > 420L) {
                                                        if (totalDy < -95f && isToolbarVisible) {
                                                            isToolbarVisible = false
                                                            lastToolbarToggleMs = now
                                                            totalDy = 0f
                                                        } else if (totalDy > 95f && !isToolbarVisible) {
                                                            isToolbarVisible = true
                                                            lastToolbarToggleMs = now
                                                            totalDy = 0f
                                                        }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                    }
                            ) {
                                // 1. Area Konten Web / Beranda dengan Tinggi Konstan 100% (Nol Glitch Hitam Bawah)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset {
                                            IntOffset(0, animatedTopOffsetPx.roundToInt())
                                        }
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
                                            videoScaleX = if (activeTabState.isFullScreen) videoFitMode.scaleX else 1f,
                                            videoScaleY = if (activeTabState.isFullScreen) videoFitMode.scaleY else 1f,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // 2. Top Omnibox yang Meluncur Halus via Offset Tanpa Mengubah Ukuran SurfaceView
                                if (!activeTabState.isFullScreen) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset {
                                                IntOffset(0, (animatedTopOffsetPx - toolbarHeightPx).roundToInt())
                                            }
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
                                            onEnterVideoFullScreen = { viewModel.enterFullScreen() },
                                            onSecurityClick = { currentPage = ActiveFullScreenPage.SECURITY },
                                            onTabsClick = { currentPage = ActiveFullScreenPage.TABS },
                                            onBookmarkClick = {
                                                viewModel.toggleBookmark(activeTabState.url, activeTabState.title)
                                            },
                                            onHistoryClick = { currentPage = ActiveFullScreenPage.HISTORY_BOOKMARKS },
                                            onSettingsClick = { currentPage = ActiveFullScreenPage.SETTINGS }
                                        )

                                        if (activeTabState.isLoading) {
                                            LinearProgressIndicator(
                                                progress = { activeTabState.progress / 100f },
                                                color = ElectricOrange,
                                                trackColor = Color.Transparent,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.5.dp)
                                            )
                                        }

                                        if (activeTabState.isFindInPageActive) {
                                            FindInPageBar(
                                                query = activeTabState.findQuery,
                                                onQueryChange = { viewModel.findInPage(it) },
                                                onNext = { viewModel.findNext() },
                                                onPrevious = { viewModel.findPrevious() },
                                                onClose = { viewModel.closeFindInPage() }
                                            )
                                        }
                                    }
                                }

                                // 3. Kontrol Mode Rasio Video Layar Penuh Redmi 9C (20:9)
                                AnimatedVisibility(
                                    visible = activeTabState.isFullScreen && showVideoOverlayControls,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 14.dp, end = 18.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(DarkSurface)
                                            .border(1.dp, DarkOutline, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        RedmiVideoFitMode.entries.forEach { mode ->
                                            val isSelected = videoFitMode == mode
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) ElectricOrange else DarkSurfaceContainer)
                                                    .clickable {
                                                        videoFitMode = mode
                                                        lastVideoTouchMs = SystemClock.elapsedRealtime()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = mode.label,
                                                    color = if (isSelected) OledBlack else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
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
    }
}
