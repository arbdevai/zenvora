package com.zenvora.browser.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zenvora.browser.core.engine.BrowserSession
import kotlinx.coroutines.delay
import org.mozilla.geckoview.GeckoView

private fun captureWebThumbnail(
    geckoView: GeckoView,
    tabId: String,
    onThumbnailCaptured: ((String, ImageBitmap) -> Unit)?
) {
    if (onThumbnailCaptured == null || tabId.isEmpty()) return
    try {
        geckoView.capturePixels().accept(
            { bitmap ->
                if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                    try {
                        val targetWidth = 420
                        val targetHeight = ((bitmap.height.toFloat() / bitmap.width.toFloat()) * targetWidth)
                            .toInt()
                            .coerceIn(240, 760)
                        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                        onThumbnailCaptured(tabId, scaled.asImageBitmap())
                    } catch (_: Throwable) {}
                }
            },
            { _ -> }
        )
    } catch (_: Throwable) {}
}

@Composable
fun BrowserGeckoView(
    activeSession: BrowserSession?,
    isFullScreen: Boolean = false,
    isLoading: Boolean = false,
    currentUrl: String = "",
    captureTrigger: Int = 0,
    onThumbnailCaptured: ((String, ImageBitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geckoView = remember {
        GeckoView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
        }
    }

    DisposableEffect(activeSession) {
        if (activeSession != null) {
            geckoView.setSession(activeSession.geckoSession)
        }
        onDispose {
            if (activeSession != null && currentUrl.startsWith("http")) {
                captureWebThumbnail(geckoView, activeSession.tabId, onThumbnailCaptured)
            }
            geckoView.releaseSession()
        }
    }

    // Tangkap otomatis preview halaman web untuk Grid Tab Switcher ala Chrome setelah halaman selesai dimuat
    LaunchedEffect(activeSession?.tabId, isLoading, currentUrl, captureTrigger) {
        val tabId = activeSession?.tabId ?: return@LaunchedEffect
        if (currentUrl.startsWith("http") && !isLoading) {
            delay(250L)
            captureWebThumbnail(geckoView, tabId, onThumbnailCaptured)
        }
    }

    LaunchedEffect(isFullScreen) {
        ViewCompat.setOnApplyWindowInsetsListener(geckoView) { _, insets ->
            if (isFullScreen) {
                WindowInsetsCompat.CONSUMED
            } else {
                insets
            }
        }
        geckoView.post {
            ViewCompat.requestApplyInsets(geckoView)
            geckoView.requestLayout()
            geckoView.invalidate()
        }
        geckoView.postDelayed({
            ViewCompat.requestApplyInsets(geckoView)
            geckoView.requestLayout()
            geckoView.invalidate()
        }, 180L)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { geckoView },
            update = { view ->
                view.setPadding(0, 0, 0, 0)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
