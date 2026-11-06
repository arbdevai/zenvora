package com.zenvora.browser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.ElectricOrangeLight
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.SecurityYellow
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun BrowserOmnibox(
    currentUrl: String,
    isLoading: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    securityInfo: SecurityInfo,
    blockedCount: Int,
    tabsCount: Int,
    isBookmarked: Boolean,
    networkSpeedText: String,
    isIncognito: Boolean = false,
    onNavigate: (String) -> Unit,
    onHomeClick: () -> Unit,
    onReload: () -> Unit,
    onForwardClick: () -> Unit,
    onNewTabClick: () -> Unit,
    onNewIncognitoTabClick: () -> Unit,
    onFindInPageClick: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onSecurityClick: () -> Unit,
    onTabsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(if (currentUrl == "about:blank") "" else currentUrl) }
    var isFocused by remember { mutableStateOf(false) }
    var showInlineActionPanel by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (!isFocused) {
            textFieldValue = if (currentUrl == "about:blank") "" else currentUrl
        }
    }

    val activeAccent = if (isIncognito) ElectricOrangeLight else ElectricOrange

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledBlack)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Tombol Home ala Chrome Android
            IconButton(
                onClick = {
                    showInlineActionPanel = false
                    onHomeClick()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Beranda",
                    tint = if (currentUrl == "about:blank") ElectricOrange else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Pill Address Bar ala Chrome (Murni DarkSurfaceContainer tanpa tint oranye)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(DarkSurfaceContainer)
                    .border(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = if (isFocused) activeAccent else DarkOutline,
                        shape = RoundedCornerShape(21.dp)
                    )
                    .padding(horizontal = 12.dp)
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

                // Ikon Gembok / Search ala Chrome
                val (statusIcon, statusTint) = when {
                    currentUrl == "about:blank" -> Icons.Filled.Search to ElectricOrange
                    securityInfo.status == SecurityStatus.SECURE -> Icons.Filled.Lock to SecurityGreen
                    securityInfo.status == SecurityStatus.INSECURE -> Icons.Filled.Warning to SecurityRed
                    securityInfo.status == SecurityStatus.INTERNAL -> Icons.Filled.Security to SecurityYellow
                    else -> Icons.Filled.Lock to TextSecondary
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Info Situs",
                    tint = statusTint,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            showInlineActionPanel = false
                            onSecurityClick()
                        }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Input URL / Kata Kunci
                Box(modifier = Modifier.weight(1f)) {
                    if (textFieldValue.isEmpty() && !isFocused) {
                        Text(
                            text = "Telusuri atau ketik URL",
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
                        cursorBrush = SolidColor(ElectricOrange),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                showInlineActionPanel = false
                                onNavigate(textFieldValue)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    showInlineActionPanel = false
                                    if (textFieldValue == "about:blank") {
                                        textFieldValue = ""
                                    }
                                }
                            }
                    )
                }

                // Indikator Mode Desktop aktif di dalam kanan pill
                if (!isFocused && isDesktopMode) {
                    Icon(
                        imageVector = Icons.Filled.DesktopWindows,
                        contentDescription = "Mode Desktop Aktif",
                        tint = ElectricOrange,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onToggleDesktopMode() }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Badge jumlah iklan diblokir (Murni DarkSurfaceVariant + border ElectricOrange, tanpa background oranye)
                if (!isFocused && blockedCount > 0 && currentUrl != "about:blank") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, ElectricOrange, RoundedCornerShape(10.dp))
                            .clickable {
                                showInlineActionPanel = false
                                onSecurityClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Adblock",
                            tint = ElectricOrange,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (blockedCount > 99) "99+" else blockedCount.toString(),
                            style = TextStyle(
                                color = ElectricOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Tombol Hapus saat mengetik
                if (isFocused && textFieldValue.isNotEmpty()) {
                    IconButton(
                        onClick = { textFieldValue = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Hapus",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tombol Tab Counter Kotak ala Chrome
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        showInlineActionPanel = false
                        onTabsClick()
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OledBlack)
                        .border(1.8.dp, if (isIncognito) ElectricOrangeLight else ElectricOrange, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = if (tabsCount > 99) "99" else tabsCount.toString(),
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            // Tombol Panel Kontrol Inline (Bukan Popup/Floating)
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    showInlineActionPanel = !showInlineActionPanel
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (showInlineActionPanel) Icons.Filled.Close else Icons.Filled.MoreVert,
                    contentDescription = "Menu Utama",
                    tint = if (showInlineActionPanel) ElectricOrange else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Panel Kontrol Inline Non-Floating (Tertanam langsung di bawah Top Bar saat dibuka)
        AnimatedVisibility(
            visible = showInlineActionPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Baris Status Kecepatan Internet Real-Time & Navigasi Cepat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceContainer)
                        .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = ElectricOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kecepatan: $networkSpeedText",
                            style = TextStyle(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                showInlineActionPanel = false
                                onForwardClick()
                            },
                            enabled = canGoForward,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Maju",
                                tint = if (canGoForward) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showInlineActionPanel = false
                                onBookmarkClick()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) ElectricOrange else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showInlineActionPanel = false
                                onReload()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid 2 Baris Tombol Aksi (Tanpa Popup Mengambang)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InlineMenuActionChip(
                        icon = Icons.Filled.Add,
                        label = "Tab Baru",
                        isActive = false,
                        onClick = {
                            showInlineActionPanel = false
                            onNewTabClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InlineMenuActionChip(
                        icon = Icons.Filled.VisibilityOff,
                        label = "Tab Samaran",
                        isActive = isIncognito,
                        onClick = {
                            showInlineActionPanel = false
                            onNewIncognitoTabClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InlineMenuActionChip(
                        icon = Icons.Filled.DesktopWindows,
                        label = if (isDesktopMode) "Desktop: ON" else "Mode Desktop",
                        isActive = isDesktopMode,
                        onClick = {
                            showInlineActionPanel = false
                            onToggleDesktopMode()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InlineMenuActionChip(
                        icon = Icons.Filled.History,
                        label = "Histori & Markah",
                        isActive = false,
                        onClick = {
                            showInlineActionPanel = false
                            onHistoryClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InlineMenuActionChip(
                        icon = Icons.Filled.FindInPage,
                        label = "Cari Kata",
                        isActive = false,
                        onClick = {
                            showInlineActionPanel = false
                            onFindInPageClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InlineMenuActionChip(
                        icon = Icons.Filled.Settings,
                        label = "Setelan",
                        isActive = false,
                        onClick = {
                            showInlineActionPanel = false
                            onSettingsClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Garis pemisah tipis antara Top Bar dan konten web
        HorizontalDivider(
            thickness = 1.dp,
            color = DarkOutline
        )
    }
}

@Composable
private fun InlineMenuActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceContainer)
            .border(
                width = 1.dp,
                color = if (isActive) ElectricOrange else DarkOutline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) ElectricOrange else TextPrimary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = TextStyle(
                color = if (isActive) ElectricOrange else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
