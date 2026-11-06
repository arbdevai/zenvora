package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zenvora.browser.core.engine.BrowserSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserGeckoView(
    activeSession: BrowserSession?,
    isFullScreen: Boolean = false,
    dynamicToolbarMaxHeightPx: Int = 0,
    verticalClippingPx: Int = 0,
    videoScaleX: Float = 1f,
    videoScaleY: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geckoView = remember {
        GeckoView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(activeSession) {
        if (activeSession != null) {
            geckoView.setSession(activeSession.geckoSession)
        }
        onDispose {
            geckoView.releaseSession()
        }
    }

    // Konsumsi total Display Cutout & System Bars Insets saat Fullscreen agar tidak ada jarak/margin kiri di layar poni
    // Serta sinkronkan layout saat keluar/masuk fullscreen
    LaunchedEffect(isFullScreen, dynamicToolbarMaxHeightPx) {
        ViewCompat.setOnApplyWindowInsetsListener(geckoView) { _, insets ->
            if (isFullScreen) {
                WindowInsetsCompat.CONSUMED
            } else {
                insets
            }
        }
        try {
            geckoView.setDynamicToolbarMaxHeight(if (isFullScreen) 0 else dynamicToolbarMaxHeightPx)
        } catch (_: Exception) {
            // Abaikan jika nilai negatif
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

    LaunchedEffect(verticalClippingPx, isFullScreen) {
        try {
            geckoView.setVerticalClipping(if (isFullScreen) 0 else verticalClippingPx)
        } catch (_: Exception) {
            // Abaikan
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                transformOrigin = TransformOrigin.Center
                scaleX = videoScaleX
                scaleY = videoScaleY
            }
    ) {
        AndroidView(
            factory = { geckoView },
            update = { view ->
                if (isFullScreen) {
                    view.setPadding(0, 0, 0, 0)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
