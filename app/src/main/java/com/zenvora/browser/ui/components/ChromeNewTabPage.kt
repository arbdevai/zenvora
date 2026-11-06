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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvora.browser.core.network.NetworkStats
import com.zenvora.browser.data.local.entity.BookmarkEntity
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.ElectricOrangeLight
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

private data class QuickDialSite(
    val title: String,
    val url: String,
    val icon: ImageVector
)

@Composable
fun ChromeNewTabPage(
    isIncognito: Boolean,
    bookmarks: List<BookmarkEntity>,
    totalBlockedCount: Int,
    networkStats: NetworkStats,
    onNavigate: (String) -> Unit,
    onOpenNetworkSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isIncognito) ElectricOrangeLight else ElectricOrange

    val defaultQuickSites = remember {
        listOf(
            QuickDialSite("Google", "https://www.google.com", Icons.Filled.Search),
            QuickDialSite("YouTube", "https://www.youtube.com", Icons.Filled.PlayArrow),
            QuickDialSite("GitHub", "https://github.com", Icons.Filled.Code),
            QuickDialSite("DuckDuckGo", "https://duckduckgo.com", Icons.Filled.Shield),
            QuickDialSite("Wikipedia", "https://id.wikipedia.org", Icons.Filled.Language),
            QuickDialSite("Reddit", "https://www.reddit.com", Icons.Filled.Public),
            QuickDialSite("Brave Search", "https://search.brave.com", Icons.Filled.Search),
            QuickDialSite("Cloudflare", "https://1.1.1.1", Icons.Filled.Shield)
        )
    }

    // Halaman Beranda Bersih Tanpa Duplikasi Search Bar (Fokus 1 Search Bar Utama di Top Omnibox)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Identitas ZENVORA Murni
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(OledBlack)
                    .border(1.5.dp, accentColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isIncognito) "ZENVORA SAMARAN" else "ZENVORA",
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = if (isIncognito) "Mode Privasi Tanpa Riwayat" else "Gecko Engine x Brave-Grade Shield",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid 4x2 Speed Dial ala Chrome
        Text(
            text = "Pintasan Cepat",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        val rows = defaultQuickSites.chunked(4)
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                rowItems.forEach { site ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(site.url) }
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkOutline, CircleShape)
                        ) {
                            Icon(
                                imageVector = site.icon,
                                contentDescription = site.title,
                                tint = ElectricOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = site.title,
                            style = TextStyle(
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Baris 2 Panel Status: Perisai Adblock & Manajemen Kecepatan Internet
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = ElectricOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stealth Shield",
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = totalBlockedCount.toString(),
                    style = TextStyle(
                        color = ElectricOrange,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "Iklan & Tracker Diblokir",
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    .clickable { onOpenNetworkSettings() }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = ElectricOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (networkStats.isTurboEnabled) "DoH Turbo Aktif" else "Jaringan",
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = networkStats.formattedDownloadSpeed,
                    style = TextStyle(
                        color = ElectricOrange,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1
                )
                val pingLabel = if (networkStats.latencyMs > 0) {
                    "Ping ${networkStats.latencyMs} ms | ${networkStats.dnsProvider.primaryIp}"
                } else {
                    "DNS ${networkStats.dnsProvider.primaryIp}"
                }
                Text(
                    text = pingLabel,
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bookmark Tersimpan (Jika ada)
        if (bookmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Bookmark Tersimpan",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
            bookmarks.take(4).forEach { bm ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                        .clickable { onNavigate(bm.url) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = ElectricOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bm.title,
                            style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = bm.url,
                            style = TextStyle(color = TextSecondary, fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
