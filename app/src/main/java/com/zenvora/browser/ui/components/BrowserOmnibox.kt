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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    var showOverflowMenu by remember { mutableStateOf(false) }
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
                onClick = onHomeClick,
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
                        .clickable { onSecurityClick() }
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

                // Badge jumlah iklan diblokir (Murni DarkSurfaceVariant + border ElectricOrange)
                if (!isFocused && blockedCount > 0 && currentUrl != "about:blank") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, ElectricOrange, RoundedCornerShape(10.dp))
                            .clickable { onSecurityClick() }
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
                    .clickable(onClick = onTabsClick)
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

            // Menu Titik Tiga (⋮) Standar Google Chrome Android
            Box {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        showOverflowMenu = true
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu Utama",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .width(260.dp)
                        .background(DarkSurface)
                        .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                ) {
                    // Baris Action Cepat Atas ala Chrome (Maju, Bookmark, Reload, Info Keamanan)
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onForwardClick()
                            },
                            enabled = canGoForward
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Maju",
                                tint = if (canGoForward) TextPrimary else TextSecondary.copy(alpha = 0.35f)
                            )
                        }

                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onBookmarkClick()
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Simpan Bookmark",
                                tint = if (isBookmarked) ElectricOrange else TextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onReload()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onSecurityClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Info Keamanan",
                                tint = ElectricOrange
                            )
                        }
                    }

                    HorizontalDivider(color = DarkOutline)

                    DropdownMenuItem(
                        text = { Text("Tab baru", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = ElectricOrange)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onNewTabClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Tab Samaran baru", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = ElectricOrangeLight)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onNewIncognitoTabClick()
                        }
                    )

                    HorizontalDivider(color = DarkOutline)

                    DropdownMenuItem(
                        text = { Text("Histori & Bookmark", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.History, contentDescription = null, tint = TextSecondary)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onHistoryClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Cari di halaman", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.FindInPage, contentDescription = null, tint = TextSecondary)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onFindInPageClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Situs desktop", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.DesktopWindows,
                                contentDescription = null,
                                tint = if (isDesktopMode) ElectricOrange else TextSecondary
                            )
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = isDesktopMode,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ElectricOrange,
                                    checkmarkColor = OledBlack,
                                    uncheckedColor = TextSecondary
                                )
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleDesktopMode()
                        }
                    )

                    HorizontalDivider(color = DarkOutline)

                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Setelan & Jaringan", color = TextPrimary)
                                Text(
                                    text = "Kecepatan: $networkSpeedText",
                                    style = TextStyle(color = ElectricOrange, fontSize = 11.sp)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = ElectricOrange)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Speed,
                                contentDescription = null,
                                tint = ElectricOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onSettingsClick()
                        }
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
