package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvora.browser.core.extensions.ExtensionManager
import com.zenvora.browser.core.extensions.InstalledExtensionInfo
import com.zenvora.browser.core.extensions.RecommendedAddon
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun ExtensionsSheet(
    installedExtensions: List<InstalledExtensionInfo>,
    installingAddonId: String?,
    statusMessage: String?,
    onInstallRecommended: (RecommendedAddon) -> Unit,
    onInstallCustomXpiUrl: (String) -> Unit,
    onToggleExtension: (InstalledExtensionInfo, Boolean) -> Unit,
    onUninstallExtension: (InstalledExtensionInfo) -> Unit,
    onOpenMozillaStore: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customXpiUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(OledBlack)
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                tint = ElectricOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Ekstensi & Add-on Store",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Segarkan Daftar Ekstensi",
                    tint = ElectricOrange
                )
            }
        }

        HorizontalDivider(color = DarkOutline)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (!statusMessage.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, ElectricOrange, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = ElectricOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Banner Buka Mozilla Add-ons Store Resmi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, ElectricOrange, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = ElectricOrange,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mozilla Add-ons Store Resmi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Jelajahi ribuan plugin Firefox/Gecko & klik Add to Firefox untuk pasang langsung",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onOpenMozillaStore(ExtensionManager.AMO_STORE_HOME_URL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricOrange,
                        contentColor = OledBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Buka Store", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Ekstensi Terpasang (${installedExtensions.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(8.dp))

            installedExtensions.forEach { ext ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(
                            width = 1.dp,
                            color = if (ext.isEnabled) ElectricOrange else DarkOutline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (ext.isBuiltIn) Icons.Filled.Shield else Icons.Filled.Extension,
                            contentDescription = null,
                            tint = if (ext.isEnabled) ElectricOrange else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ext.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "v${ext.version}",
                                    fontSize = 11.sp,
                                    color = ElectricOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (ext.isBuiltIn) "Sistem Inti Zenvora (Built-In)" else ext.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        if (!ext.isBuiltIn) {
                            IconButton(
                                onClick = { onUninstallExtension(ext) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Hapus Ekstensi",
                                    tint = SecurityRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Switch(
                            checked = ext.isEnabled,
                            onCheckedChange = { checked -> onToggleExtension(ext, checked) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = ElectricOrange,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ext.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Katalog Pasang Cepat 1-Klik (Rekomendasi)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExtensionManager.RECOMMENDED_ADDONS.forEach { addon ->
                val isAlreadyInstalled = installedExtensions.any {
                    it.id.equals(addon.id, ignoreCase = true) ||
                        it.name.contains(addon.name, ignoreCase = true)
                }
                val isInstallingThis = installingAddonId == addon.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = addon.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = addon.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        if (isAlreadyInstalled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = SecurityGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Terpasang",
                                    color = SecurityGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isInstallingThis) {
                            SkeletonBox(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onOpenMozillaStore(addon.storePageUrl) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = "Buka Halaman Store",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Button(
                                    onClick = { onInstallRecommended(addon) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricOrange,
                                        contentColor = OledBlack
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pasang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pasang Langsung dari Tautan .XPI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricOrange
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp)
                ) {
                    if (customXpiUrl.isEmpty()) {
                        Text(
                            text = "https://addons.mozilla.org/.../addon.xpi",
                            style = TextStyle(color = TextSecondary, fontSize = 13.sp)
                        )
                    }
                    BasicTextField(
                        value = customXpiUrl,
                        onValueChange = { customXpiUrl = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(ElectricOrange),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (customXpiUrl.isNotBlank()) {
                            onInstallCustomXpiUrl(customXpiUrl)
                            customXpiUrl = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricOrange,
                        contentColor = OledBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Instal XPI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
