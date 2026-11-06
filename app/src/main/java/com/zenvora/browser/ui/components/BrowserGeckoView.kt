package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.zenvora.browser.core.engine.BrowserSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserGeckoView(
    activeSession: BrowserSession?,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { geckoView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
