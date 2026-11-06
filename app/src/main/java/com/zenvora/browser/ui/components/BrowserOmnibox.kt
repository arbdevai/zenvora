package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import com.zenvora.browser.ui.theme.AccentTeal
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.SecurityYellow
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun BrowserOmnibox(
    currentUrl: String,
    isLoading: Boolean,
    securityInfo: SecurityInfo,
    blockedCount: Int,
    tabsCount: Int,
    isBookmarked: Boolean,
    isIncognito: Boolean = false,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onSecurityClick: () -> Unit,
    onTabsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(currentUrl) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (!isFocused) {
            textFieldValue = if (currentUrl == "about:blank") "" else currentUrl
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Input Bar Container
            val activeAccent = if (isIncognito) androidx.compose.ui.graphics.Color(0xFFBA68C8) else AccentTeal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, if (isFocused) activeAccent else DarkOutline, RoundedCornerShape(22.dp))
                    .padding(horizontal = 10.dp)
            ) {
                if (isIncognito) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = "Mode Samaran",
                        tint = activeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Security Icon
                val securityIcon = when (securityInfo.status) {
                    SecurityStatus.SECURE -> Icons.Filled.Lock to SecurityGreen
                    SecurityStatus.INSECURE -> Icons.Filled.Warning to SecurityRed
                    SecurityStatus.INTERNAL -> Icons.Filled.Security to SecurityYellow
                    SecurityStatus.UNKNOWN -> Icons.Filled.Lock to TextSecondary
                }

                Icon(
                    imageVector = securityIcon.first,
                    contentDescription = "Status Keamanan",
                    tint = securityIcon.second,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onSecurityClick)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // BasicTextField for URL / Search
                Box(modifier = Modifier.weight(1f)) {
                    if (textFieldValue.isEmpty() && !isFocused) {
                        Text(
                            text = "Ketik URL atau cari web...",
                            style = TextStyle(color = TextSecondary, fontSize = 14.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(AccentTeal),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onNavigate(textFieldValue)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused && textFieldValue == "about:blank") {
                                    textFieldValue = ""
                                }
                            }
                    )
                }

                // Clear or Reload Button
                if (isFocused && textFieldValue.isNotEmpty()) {
                    IconButton(
                        onClick = { textFieldValue = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Hapus Teks",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onReload,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Shield / Adblock Counter Badge
            BadgedBox(
                badge = {
                    if (blockedCount > 0) {
                        Badge(
                            containerColor = AccentTeal,
                            contentColor = DarkSurface
                        ) {
                            Text(
                                text = if (blockedCount > 99) "99+" else blockedCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onSecurityClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Adblock Tracker Shield",
                        tint = if (blockedCount > 0) AccentTeal else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bookmark Toggle
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) AccentTeal else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // History Button
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Histori & Bookmark",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tabs Counter Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, TextSecondary, RoundedCornerShape(6.dp))
                    .clickable(onClick = onTabsClick)
            ) {
                Text(
                    text = tabsCount.toString(),
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Pengaturan",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
