package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurface
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.ElectricOrangeLight
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun TabOverviewSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
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

    // Halaman Penuh Pengelola Tab (Tanpa Tombol Back di Layar - Gunakan Navigasi Back Sistem HP)
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
            Text(
                text = "Pengelola Tab (${tabs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onNewTab(selectedCategoryIndex == 1) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeAccent,
                    contentColor = OledBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Tab Baru",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedCategoryIndex == 0) "Tab Baru" else "Samaran Baru",
                    fontWeight = FontWeight.Bold
                )
            }
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                items(currentDisplayTabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    TabItemCard(
                        tab = tab,
                        isActive = isActive,
                        onSelect = { onTabSelected(tab.id) },
                        onClose = { onTabClosed(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItemCard(
    tab: BrowserTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val highlightColor = if (tab.isIncognito) ElectricOrangeLight else ElectricOrange
    val borderColor = if (isActive) highlightColor else DarkOutline
    val backgroundColor = if (isActive) DarkSurfaceVariant else DarkSurfaceContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Tab,
            contentDescription = null,
            tint = if (isActive) highlightColor else TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.title.ifEmpty { if (tab.isIncognito) "Tab Samaran" else "Tab Baru" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (tab.url == "about:blank") "Beranda Zenvora" else tab.url,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Tutup Tab",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
