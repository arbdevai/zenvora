package com.zenvora.browser.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.ElectricOrangeLight
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary
import java.net.URI

@Composable
fun TabOverviewSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    tabThumbnails: Map<String, ImageBitmap> = emptyMap(),
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val normalTabs = remember(tabs) { tabs.filter { !it.isIncognito } }
    val incognitoTabs = remember(tabs) { tabs.filter { it.isIncognito } }
    val currentDisplayTabs = if (selectedCategoryIndex == 0) normalTabs else incognitoTabs
    val activeAccent = if (selectedCategoryIndex == 0) ElectricOrange else ElectricOrangeLight

    // Halaman Penuh Pengelola Tab Grid 2 Kolom ala Google Chrome Android
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(OledBlack)
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = { onNewTab(selectedCategoryIndex == 1) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeAccent,
                    contentColor = OledBlack
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Tab Baru",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedCategoryIndex == 0) "Tab Baru" else "Samaran Baru",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${tabs.size} Tab Aktif",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }

        HorizontalDivider(color = DarkOutline)

        TabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = DarkSurface,
            contentColor = activeAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = activeAccent
                )
            }
        ) {
            Tab(
                selected = selectedCategoryIndex == 0,
                onClick = { selectedCategoryIndex = 0 },
                text = {
                    Text(
                        text = "Tab Biasa (${normalTabs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selectedCategoryIndex == 0) ElectricOrange else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedCategoryIndex == 1,
                onClick = { selectedCategoryIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedCategoryIndex == 1) ElectricOrangeLight else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tab Samaran (${incognitoTabs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedCategoryIndex == 1) ElectricOrangeLight else TextSecondary
                        )
                    }
                }
            )
        }

        if (currentDisplayTabs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = if (selectedCategoryIndex == 0) "Tidak ada tab reguler aktif" else "Tidak ada tab samaran aktif",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(currentDisplayTabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    val thumbnail = tabThumbnails[tab.id]
                    ChromeTabGridCard(
                        tab = tab,
                        isActive = isActive,
                        thumbnail = thumbnail,
                        onSelect = { onTabSelected(tab.id) },
                        onClose = { onTabClosed(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeTabGridCard(
    tab: BrowserTab,
    isActive: Boolean,
    thumbnail: ImageBitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val highlightColor = if (tab.isIncognito) ElectricOrangeLight else ElectricOrange
    val borderColor = if (isActive) highlightColor else DarkOutline
    val headerBg = if (isActive) DarkSurfaceVariant else DarkSurfaceContainer
    val isHomeBlank = tab.url == "about:blank" || tab.url.isEmpty()

    val cleanHost = remember(tab.url) {
        if (isHomeBlank) {
            "Beranda Zenvora"
        } else {
            try {
                URI(tab.url).host?.removePrefix("www.") ?: tab.url
            } catch (_: Exception) {
                tab.url
            }
        }
    }

    val hostInitial = remember(cleanHost) {
        cleanHost.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "Z"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(DarkSurface)
            .clickable(onClick = onSelect)
    ) {
        // 1. Header Kartu Tab ala Google Chrome (Favicon/Icon + Judul Halaman + Tombol X)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(headerBg)
                .padding(start = 10.dp, end = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isActive) highlightColor else DarkSurface)
            ) {
                if (tab.isIncognito) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = if (isActive) OledBlack else highlightColor,
                        modifier = Modifier.size(11.dp)
                    )
                } else if (isHomeBlank) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = if (isActive) OledBlack else highlightColor,
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Text(
                        text = hostInitial,
                        color = if (isActive) OledBlack else highlightColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = tab.title.ifEmpty {
                    if (isHomeBlank) "Tab Baru" else cleanHost
                },
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup Tab",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        HorizontalDivider(color = DarkOutline)

        // 2. Area Preview Visual Halaman Web (Live Bitmap Screenshot atau Preview Kartu Situs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(OledBlack)
        ) {
            if (!isHomeBlank && thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = tab.title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Preview Kartu Visual jika Tab berada di Beranda atau belum selesai dirender
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, if (isActive) highlightColor else DarkOutline, CircleShape)
                    ) {
                        if (isHomeBlank) {
                            Text(
                                text = "Z",
                                color = highlightColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = highlightColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHomeBlank) "ZENVORA" else cleanHost,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isHomeBlank) {
                            if (tab.isIncognito) "Mode Samaran Pribadi" else "Halaman Tab Baru"
                        } else {
                            tab.title.ifEmpty { tab.url }
                        },
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Footer Pill Domain & Status Adblock di Bagian Bawah Preview Kartu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(DarkSurface.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = if (isHomeBlank) Icons.Filled.Home else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (tab.isSecure) SecurityGreen else TextSecondary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cleanHost,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (tab.blockedRequestsCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = highlightColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = tab.blockedRequestsCount.toString(),
                        color = highlightColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
