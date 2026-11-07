package com.zenvora.browser.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenvora.browser.core.engine.VideoCodecMode
import com.zenvora.browser.core.updater.UpdateState
import com.zenvora.browser.ui.components.BrowserGeckoView
import com.zenvora.browser.ui.components.BrowserOmnibox
import com.zenvora.browser.ui.components.ChromeNewTabPage
import com.zenvora.browser.ui.components.ExtensionsSheet
import com.zenvora.browser.ui.components.FindInPageBar
import com.zenvora.browser.ui.components.HistoryBookmarkSheet
import com.zenvora.browser.ui.components.NativeVideoPlayerOverlay
import com.zenvora.browser.ui.components.PasswordManagerSheet
import com.zenvora.browser.ui.components.SecurityDetailDialog
import com.zenvora.browser.ui.components.SettingsSheet
import com.zenvora.browser.ui.components.TabOverviewSheet
import com.zenvora.browser.ui.components.UpdateDialog
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary
import com.zenvora.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ActiveFullScreenPage {
    BROWSER,
    TABS,
    HISTORY_BOOKMARKS,
    PASSWORDS,
    EXTENSIONS,
    SETTINGS,
    SECURITY
}

private enum class WebVideoFitMode(
    val label: String,
    val fitCommand: String
) {
    STANDAR_16_9("Rasio 16:9", "standard"),
    PAS_LAYAR_20_9("Pas Layar 20:9", "stretch_20_9"),
    ZOOM_PENUH("Zoom Penuh", "zoom_20_9")
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
    val savedLogins by viewModel.savedLogins.collectAsStateWithLifecycle()
    val isAdblockEnabled by viewModel.isAdblockEnabled.collectAsStateWithLifecycle()
    val isStrictPrivacy by viewModel.isStrictPrivacy.collectAsStateWithLifecycle()
    val isBackgroundAudioEnabled by viewModel.isBackgroundAudioEnabled.collectAsStateWithLifecycle()
    val videoCodecModeId by viewModel.videoCodecModeId.collectAsStateWithLifecycle()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsStateWithLifecycle()
    val networkStats by viewModel.networkStats.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val availableUpdateBadge by viewModel.availableUpdateBadge.collectAsStateWithLifecycle()
    val installedExtensions by viewModel.installedExtensions.collectAsStateWithLifecycle()
    val installingAddonId by viewModel.installingAddonId.collectAsStateWithLifecycle()
    val installProgressPercent by viewModel.installProgressPercent.collectAsStateWithLifecycle()
    val extensionStatusMessage by viewModel.extensionStatusMessage.collectAsStateWithLifecycle()
    val detectedVideoStream by viewModel.detectedVideoStream.collectAsStateWithLifecycle()
    val activeNativeVideoStream by viewModel.activeNativeVideoStream.collectAsStateWithLifecycle()
    val pendingCapturedLogin by viewModel.pendingCapturedLogin.collectAsStateWithLifecycle()

    val uiScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var currentPage by remember { mutableStateOf(ActiveFullScreenPage.BROWSER) }
    var dismissedAutofillHost by remember { mutableStateOf("") }
    var dismissedStreamUrl by remember { mutableStateOf("") }

    var videoFitMode by remember { mutableStateOf(WebVideoFitMode.STANDAR_16_9) }
    var showVideoOverlayControls by remember { mutableStateOf(true) }
    var lastVideoTouchMs by remember { mutableLongStateOf(0L) }

    val isBookmarked = remember(bookmarks, activeTabState.url) {
        bookmarks.any { it.url == activeTabState.url }
    }

    val isOnNewTabPage = activeTabState.url == "about:blank" || activeTabState.url.isEmpty()

    // Bersihkan otomatis pesan sukses instalasi ekstensi setelah 4.5 detik
    LaunchedEffect(installProgressPercent, extensionStatusMessage) {
        if (installProgressPercent == null && !extensionStatusMessage.isNullOrBlank()) {
            delay(4500L)
            viewModel.clearExtensionStatusMessage()
        }
    }

    // Sembunyikan otomatis kontrol rasio video setelah 3.5 detik saat menonton fullscreen
    LaunchedEffect(activeTabState.isFullScreen, lastVideoTouchMs) {
        if (activeTabState.isFullScreen) {
            showVideoOverlayControls = true
            delay(3500L)
            showVideoOverlayControls = false
        }
    }

    // Mode Real Fullscreen Edge-to-Edge Tanpa Balok Hitam Kiri
    val view = LocalView.current
    LaunchedEffect(activeTabState.isFullScreen) {
        if (activeNativeVideoStream != null) return@LaunchedEffect
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, view)

        if (activeTabState.isFullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            viewModel.applyVideoObjectFit(videoFitMode.fitCommand)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.applyVideoObjectFit(WebVideoFitMode.STANDAR_16_9.fitCommand)
        }
    }

    // Seluruh navigasi kembali dikendalikan oleh tombol Back fisik/gesture HP
    BackHandler(
        enabled = activeNativeVideoStream != null ||
            activeTabState.isFullScreen ||
            updateState !is UpdateState.Idle ||
            currentPage != ActiveFullScreenPage.BROWSER ||
            activeTabState.isFindInPageActive ||
            activeTabState.canGoBack ||
            !isOnNewTabPage
    ) {
        when {
            activeNativeVideoStream != null -> viewModel.closeNativeVideoPlayer()
            activeTabState.isFullScreen -> viewModel.exitFullScreen()
            updateState !is UpdateState.Idle -> viewModel.dismissUpdate()
            currentPage != ActiveFullScreenPage.BROWSER -> currentPage = ActiveFullScreenPage.BROWSER
            activeTabState.isFindInPageActive -> viewModel.closeFindInPage()
            activeTabState.canGoBack -> viewModel.goBack()
            !isOnNewTabPage -> viewModel.navigateHome()
        }
    }

    // Tinggi statis 57.dp saat mode normal agar SurfaceView GeckoView TIDAK PERNAH berubah ukuran saat scroll (0% glitch hitam bawah!)
    val staticWebTopPaddingDp = if (activeTabState.isFullScreen) 0.dp else 57.dp

    val matchingLoginForCurrentSite = remember(savedLogins, activeTabState.url, dismissedAutofillHost) {
        if (isOnNewTabPage || activeTabState.isFullScreen) {
            null
        } else {
            savedLogins.firstOrNull { login ->
                login.host.isNotBlank() &&
                    activeTabState.url.contains(login.host, ignoreCase = true) &&
                    !login.host.equals(dismissedAutofillHost, ignoreCase = true)
            }
        }
    }

    val isFullScreenOrNativePlayer = activeTabState.isFullScreen || activeNativeVideoStream != null

    Scaffold(
        contentWindowInsets = if (isFullScreenOrNativePlayer) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
        containerColor = OledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { base ->
                    if (isFullScreenOrNativePlayer) base else base.padding(innerPadding)
                }
                .background(OledBlack)
        ) {
            if (activeNativeVideoStream != null) {
                NativeVideoPlayerOverlay(
                    stream = activeNativeVideoStream!!,
                    onClose = { viewModel.closeNativeVideoPlayer() }
                )
            } else if (updateState !is UpdateState.Idle) {
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
                                    currentPage = ActiveFullScreenPage.BROWSER
                                    uiScope.launch {
                                        delay(120L)
                                        viewModel.loadInput(url)
                                    }
                                },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }

                        ActiveFullScreenPage.PASSWORDS -> {
                            PasswordManagerSheet(
                                savedLogins = savedLogins,
                                currentSiteUrl = activeTabState.url,
                                onAddLogin = { site, user, pass ->
                                    viewModel.addSavedLogin(site, user, pass)
                                },
                                onDeleteLogin = { id ->
                                    viewModel.deleteSavedLogin(id)
                                },
                                onOpenAndAutofillLogin = { login ->
                                    currentPage = ActiveFullScreenPage.BROWSER
                                    uiScope.launch {
                                        delay(120L)
                                        viewModel.openAndAutofillLogin(login)
                                    }
                                }
                            )
                        }

                        ActiveFullScreenPage.EXTENSIONS -> {
                            ExtensionsSheet(
                                installedExtensions = installedExtensions,
                                installingAddonId = installingAddonId,
                                installProgressPercent = installProgressPercent,
                                statusMessage = extensionStatusMessage,
                                onInstallRecommended = { addon ->
                                    viewModel.installRecommendedAddon(addon)
                                },
                                onInstallCustomXpiUrl = { url ->
                                    viewModel.installCustomXpi(url)
                                },
                                onToggleExtension = { ext, enabled ->
                                    viewModel.toggleExtension(ext, enabled)
                                },
                                onUninstallExtension = { ext ->
                                    viewModel.uninstallExtension(ext)
                                },
                                onOpenExtensionOptions = { optionsUrl ->
                                    currentPage = ActiveFullScreenPage.BROWSER
                                    uiScope.launch {
                                        delay(140L)
                                        viewModel.loadInput(optionsUrl)
                                    }
                                },
                                onOpenMozillaStore = { storeUrl ->
                                    currentPage = ActiveFullScreenPage.BROWSER
                                    uiScope.launch {
                                        delay(140L)
                                        viewModel.loadInput(storeUrl)
                                    }
                                },
                                onRefresh = { viewModel.refreshExtensions() }
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
                                isBackgroundAudioEnabled = isBackgroundAudioEnabled,
                                videoCodecMode = VideoCodecMode.fromId(videoCodecModeId),
                                hardwareVideoCapabilities = viewModel.hardwareVideoCapabilities,
                                searchEngineUrl = searchEngineUrl,
                                totalBlockedCount = totalBlockedAllTabs,
                                networkStats = networkStats,
                                onToggleAdblock = { viewModel.toggleAdblock() },
                                onToggleStrictPrivacy = { viewModel.toggleStrictPrivacy() },
                                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                                onToggleBackgroundAudio = { viewModel.toggleBackgroundAudio() },
                                onSelectVideoCodecMode = { viewModel.selectVideoCodecMode(it) },
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .background(OledBlack)
                                    .pointerInput(activeTabState.isFullScreen) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Initial
                                            )
                                            if (activeTabState.isFullScreen && down.position.y < 220f) {
                                                lastVideoTouchMs = SystemClock.elapsedRealtime()
                                            }
                                        }
                                    }
                            ) {
                                // 1. Area Konten Web Statis 100% (Tinggi Tetap Tanpa Resize SurfaceView -> 0% Glitch Hitam Saat Scroll)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = staticWebTopPaddingDp)
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
                                            isFullScreen = activeTabState.isFullScreen,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // 2. Top Omnibox Statis + Saran Riwayat & Bookmark ala Chrome
                                if (!activeTabState.isFullScreen) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
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
                                            availableUpdateVersion = availableUpdateBadge?.versionName,
                                            isIncognito = activeTabState.isIncognito,
                                            historyList = historyEntries,
                                            bookmarkList = bookmarks,
                                            onNavigate = { viewModel.loadInput(it) },
                                            onHomeClick = { viewModel.navigateHome() },
                                            onReload = { viewModel.reload() },
                                            onForwardClick = { viewModel.goForward() },
                                            onNewTabClick = { viewModel.createNewTab(isIncognito = false) },
                                            onNewIncognitoTabClick = { viewModel.createNewTab(isIncognito = true) },
                                            onExtensionsClick = {
                                                viewModel.refreshExtensions()
                                                currentPage = ActiveFullScreenPage.EXTENSIONS
                                            },
                                            onPasswordManagerClick = {
                                                currentPage = ActiveFullScreenPage.PASSWORDS
                                            },
                                            onFindInPageClick = { viewModel.startFindInPage() },
                                            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                                            onCheckUpdateClick = { viewModel.checkForUpdates() },
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

                                // 3. Banner Progres Instalasi Ekstensi Live (0% - 100%)
                                AnimatedVisibility(
                                    visible = !activeTabState.isFullScreen &&
                                        (installProgressPercent != null || !extensionStatusMessage.isNullOrBlank()),
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = staticWebTopPaddingDp + 8.dp, start = 14.dp, end = 14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkSurfaceVariant)
                                            .border(1.5.dp, ElectricOrange, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Extension,
                                                contentDescription = null,
                                                tint = ElectricOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = extensionStatusMessage ?: "Memasang ekstensi ke Zenvora...",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (installProgressPercent != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${installProgressPercent}%",
                                                    color = ElectricOrange,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        if (installProgressPercent != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { (installProgressPercent!!.coerceIn(0, 100)) / 100f },
                                                color = ElectricOrange,
                                                trackColor = DarkOutline,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }

                                // 4. Tombol Mengambang "Putar di Player Native (ExoPlayer)" Saat Aliran Video Terdeteksi di Web
                                val activeStream = detectedVideoStream
                                AnimatedVisibility(
                                    visible = !activeTabState.isFullScreen &&
                                        activeStream != null &&
                                        activeStream.streamUrl != dismissedStreamUrl,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = if (matchingLoginForCurrentSite != null) 68.dp else 16.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    activeStream?.let { stream ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(DarkSurface)
                                                .border(1.5.dp, ElectricOrange, RoundedCornerShape(24.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.openNativeVideoPlayer(stream)
                                                }
                                                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircle,
                                                contentDescription = null,
                                                tint = ElectricOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = "Putar di Zenvora Native Player",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Layar penuh mulus 20:9 & kontrol tidak ikut ter-zoom",
                                                    color = ElectricOrange,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    dismissedStreamUrl = stream.streamUrl
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Tutup",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5. Chip Autofill Cepat jika Situs Saat Ini Memiliki Sandi Tersimpan di Zenvora
                                AnimatedVisibility(
                                    visible = matchingLoginForCurrentSite != null && pendingCapturedLogin == null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    matchingLoginForCurrentSite?.let { login ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(DarkSurface)
                                                .border(1.5.dp, ElectricOrange, RoundedCornerShape(24.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.autofillLoginOnCurrentPage(login)
                                                }
                                                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Key,
                                                contentDescription = null,
                                                tint = ElectricOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = "Isi Sandi: ${login.username}",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Ketuk untuk autofill ke ${login.host}",
                                                    color = ElectricOrange,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    dismissedAutofillHost = login.host
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Tutup Saran Sandi",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5b. Prompt Otomatis Simpan Sandi saat Pengguna Login di Situs Web (Auto-Save Password Prompt)
                                AnimatedVisibility(
                                    visible = !activeTabState.isFullScreen && pendingCapturedLogin != null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp, start = 14.dp, end = 14.dp)
                                ) {
                                    pendingCapturedLogin?.let { captured ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(DarkSurfaceVariant)
                                                .border(1.5.dp, ElectricOrange, RoundedCornerShape(16.dp))
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Key,
                                                contentDescription = null,
                                                tint = ElectricOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Simpan sandi untuk ${captured.host}?",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Akun: ${captured.username}",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(DarkSurfaceContainer)
                                                    .clickable {
                                                        viewModel.dismissCapturedLogin()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Jangan",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(ElectricOrange)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.confirmSaveCapturedLogin()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Simpan",
                                                    color = OledBlack,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }

                                // 6. Kontrol Mode Rasio Video Layar Penuh (Transform Hanya pada <video>, Kontrol Player 100% Utuh) + Tombol Player Native
                                AnimatedVisibility(
                                    visible = activeTabState.isFullScreen && showVideoOverlayControls,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
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
                                        WebVideoFitMode.entries.forEach { mode ->
                                            val isSelected = videoFitMode == mode
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) ElectricOrange else DarkSurfaceContainer)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        videoFitMode = mode
                                                        viewModel.applyVideoObjectFit(mode.fitCommand)
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

                                        detectedVideoStream?.let { stream ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(ElectricOrange)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.openNativeVideoPlayer(stream)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayCircle,
                                                    contentDescription = null,
                                                    tint = OledBlack,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Player Native",
                                                    color = OledBlack,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
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
