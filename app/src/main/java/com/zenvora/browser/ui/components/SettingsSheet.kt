package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zenvora.browser.BuildConfig
import com.zenvora.browser.ui.theme.AccentTeal
import com.zenvora.browser.ui.theme.DarkSurfaceContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isAdblockEnabled: Boolean,
    isStrictPrivacy: Boolean,
    searchEngineUrl: String,
    totalBlockedCount: Int,
    onToggleAdblock: () -> Unit,
    onToggleStrictPrivacy: () -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onClearAllData: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSearchEngineMenu by remember { mutableStateOf(false) }

    val searchEngines = listOf(
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Startpage" to "https://www.startpage.com/sp/search?query=",
        "Brave Search" to "https://search.brave.com/search?q=",
        "Google" to "https://www.google.com/search?q="
    )

    val currentEngineName = searchEngines.firstOrNull { it.second == searchEngineUrl }?.first ?: "Kustom"

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Pengaturan Browser",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bagian Engine & Privasi
            Text(
                text = "Privasi & Filter Engine",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )
            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.Block,
                title = "Native Rust Ad-Blocker",
                subtitle = "Total $totalBlockedCount tracker & iklan diblokir",
                checked = isAdblockEnabled,
                onCheckedChange = { onToggleAdblock() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.Security,
                title = "Strict Tracking Protection",
                subtitle = "Isolasi cookie pihak ketiga dan pencegahan fingerprinting",
                checked = isStrictPrivacy,
                onCheckedChange = { onToggleStrictPrivacy() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bagian Mesin Pencari
            Text(
                text = "Pencarian Web",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .clickable { showSearchEngineMenu = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mesin Pencari Default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentEngineName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentTeal
                    )
                }

                DropdownMenu(
                    expanded = showSearchEngineMenu,
                    onDismissRequest = { showSearchEngineMenu = false }
                ) {
                    searchEngines.forEach { (name, url) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onSelectSearchEngine(url)
                                showSearchEngineMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bagian Pembersihan Data
            Text(
                text = "Penyimpanan & Riwayat",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .clickable { onClearAllData() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hapus Histori & Data Penelusuran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Menghapus entri Room database lokal seketika",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bagian Info & Update
            Text(
                text = "Informasi Sistem",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zenvora Browser v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Engine: Mozilla GeckoView (Multi-process)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCheckUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Cek Update")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = AccentTeal
            )
        )
    }
}
