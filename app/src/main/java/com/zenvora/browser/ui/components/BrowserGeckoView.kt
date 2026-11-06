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
import com.zenvora.browser.core.engine.BrowserSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserGeckoView(
    activeSession: BrowserSession?,
    isFullScreen: Boolean = false,
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

    // Paksa sinkronisasi layout & surface buffer saat masuk/keluar mode fullscreen agar layar tidak blank hitam
    LaunchedEffect(isFullScreen) {
        geckoView.post {
            geckoView.requestLayout()
            geckoView.invalidate()
        }
        geckoView.postDelayed({
            geckoView.requestLayout()
            geckoView.invalidate()
        }, 180L)
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
                view.requestLayout()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
