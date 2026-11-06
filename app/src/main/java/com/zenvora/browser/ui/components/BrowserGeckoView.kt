package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.zenvora.browser.core.engine.BrowserSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserGeckoView(
    activeSession: BrowserSession?,
    onScrollDown: () -> Unit = {},
    onScrollUp: () -> Unit = {},
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var startY = down.position.y
                    var accumulatedDy = 0f

                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            val dy = change.position.y - startY
                            accumulatedDy += dy
                            startY = change.position.y

                            // Jari geser ke atas (scroll halaman ke bawah) -> sembunyikan navbar atas
                            if (accumulatedDy < -42f) {
                                onScrollDown()
                                accumulatedDy = 0f
                            }
                            // Jari geser ke bawah (scroll halaman ke atas) -> tampilkan kembali navbar atas
                            else if (accumulatedDy > 42f) {
                                onScrollUp()
                                accumulatedDy = 0f
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AndroidView(
            factory = { geckoView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
