package com.zenvora.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zenvora.browser.ui.screen.BrowserScreen
import com.zenvora.browser.ui.theme.ZenvoraTheme
import com.zenvora.browser.ui.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenvoraTheme {
                BrowserScreen(viewModel = browserViewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        browserViewModel.onAppBackgrounded()
    }

    override fun onStop() {
        super.onStop()
        browserViewModel.onAppBackgrounded()
    }
}
