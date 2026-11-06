package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvora.browser.BuildConfig
import com.zenvora.browser.core.engine.HardwareVideoCapabilities
import com.zenvora.browser.core.engine.VideoCodecMode
import com.zenvora.browser.core.network.DnsProvider
import com.zenvora.browser.core.network.NetworkStats
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun SettingsSheet(
    isAdblockEnabled: Boolean,
    isStrictPrivacy: Boolean,
    isDesktopMode: Boolean,
    isBackgroundAudioEnabled: Boolean,
    videoCodecMode: VideoCodecMode,
    hardwareVideoCapabilities: HardwareVideoCapabilities,
    searchEngineUrl: String,
    totalBlockedCount: Int,
    networkStats: NetworkStats,
    onToggleAdblock: () -> Unit,
    onToggleStrictPrivacy: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onSelectVideoCodecMode: (VideoCodecMode) -> Unit,
    onToggleNetworkTurbo: () -> Unit,
    onSelectDnsProvider: (DnsProvider) -> Unit,
    onRefreshNetworkPing: () -> Unit,
    onFindInPageClick: () -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onClearAllData: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchEngines = listOf(
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Brave Search" to "https://search.brave.com/search?q=",
        "Google" to "https://www.google.com/search?q=",
        "Startpage" to "https://www.startpage.com/sp/search?query="
    )

    // Halaman Penuh Setelan Zenvora (Tanpa Tombol Back di Layar - Gunakan Tombol Back Sistem HP)
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
                text = "Setelan Zenvora",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = DarkOutline)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Manajemen Kecepatan Internet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ElectricOrange
                )
                IconButton(
                    onClick = onRefreshNetworkPing,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Uji Ping Jaringan",
                        tint = ElectricOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NetworkMetricBox(
                        label = "Unduh Live",
                        value = networkStats.formattedDownloadSpeed,
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkMetricBox(
                        label = "Unggah Live",
                        value = networkStats.formattedUploadSpeed,
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkMetricBox(
                        label = "Puncak",
                        value = networkStats.formattedPeakSpeed,
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkOutline)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NetworkMetricBox(
                        label = "Latensi Ping",
                        value = if (networkStats.latencyMs > 0) "${networkStats.latencyMs} ms" else "Siap",
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkMetricBox(
                        label = "Cache DNS",
                        value = "${networkStats.cachedDomainsCount} domain",
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkMetricBox(
                        label = "Data Sesi",
                        value = networkStats.formattedSessionTotal,
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.Speed,
                title = "Akselerasi Jaringan Turbo & Preconnect",
                subtitle = "Koneksi paralel 16-jalur, DNS in-memory cache, dan buffer unduhan 256 KB",
                checked = networkStats.isTurboEnabled,
                onCheckedChange = { onToggleNetworkTurbo() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Resolver DNS-over-HTTPS (Strict DoH)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            DnsProvider.entries.forEach { provider ->
                val isSelected = networkStats.dnsProvider == provider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricOrange else DarkOutline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectDnsProvider(provider) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                        tint = if (isSelected) ElectricOrange else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = provider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectDnsProvider(provider) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ElectricOrange,
                            unselectedColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Privasi & Perisai Iklan (Brave-Grade Shield)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.Block,
                title = "Stealth Ad-Blocker & Anti-Adblock Defuser",
                subtitle = "Total $totalBlockedCount iklan, pop-under & tracker diblokir",
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

            Text(
                text = "Akselerasi Video Hardware & Pemutaran Latar Belakang",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.Headphones,
                title = "Putar Video & Musik di Latar Belakang (YouTube Non-Stop)",
                subtitle = "Tetap putar audio/video saat berpindah aplikasi atau layar HP dimatikan",
                checked = isBackgroundAudioEnabled,
                onCheckedChange = { onToggleBackgroundAudio() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Dekoder Hardware Chipset Terdeteksi: ${hardwareVideoCapabilities.decoderNamesSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (!hardwareVideoCapabilities.hasHardwareVp9 && !hardwareVideoCapabilities.hasHardwareAv1) {
                        "Chipset ini mengandalkan H.264 Hardware. Mode Otomatis menonaktifkan VP9/AV1 software agar video 720p/1080p tidak patah-patah."
                    } else {
                        "Chipset ini mendukung dekoder hardware modern (${hardwareVideoCapabilities.decoderNamesSummary}) untuk pemutaran resolusi tinggi."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            VideoCodecMode.entries.forEach { mode ->
                val isSelected = videoCodecMode == mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricOrange else DarkOutline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectVideoCodecMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = if (isSelected) ElectricOrange else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectVideoCodecMode(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ElectricOrange,
                            unselectedColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Halaman & Penjelajahan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                icon = Icons.Filled.DesktopWindows,
                title = "Mode Situs Desktop Permanen",
                subtitle = "Kunci User-Agent desktop, viewport lebar, dan alihkan m. ke www.",
                checked = isDesktopMode,
                onCheckedChange = { onToggleDesktopMode() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                    .clickable { onFindInPageClick() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FindInPage,
                    contentDescription = null,
                    tint = ElectricOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cari di Halaman",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Temukan kata atau frasa dalam halaman web aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mesin Pencari Default",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            searchEngines.forEach { (name, url) ->
                val isSelected = searchEngineUrl == url
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricOrange else DarkOutline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectSearchEngine(url) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = if (isSelected) ElectricOrange else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectSearchEngine(url) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ElectricOrange,
                            unselectedColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Penyimpanan & Riwayat",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                    .clickable { onClearAllData() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = SecurityRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hapus Histori, Cache DNS & Data Sesi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SecurityRed
                    )
                    Text(
                        text = "Menghapus entri Room database lokal & mereset cache domain",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Informasi Sistem",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = ElectricOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zenvora v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "GeckoView Engine x Brave-Grade Shield",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onCheckUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricOrange,
                        contentColor = OledBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Cek Update",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun NetworkMetricBox(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) ElectricOrange else TextPrimary
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
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
            .border(
                width = 1.dp,
                color = if (checked) ElectricOrange else DarkOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) ElectricOrange else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OledBlack,
                checkedTrackColor = ElectricOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
