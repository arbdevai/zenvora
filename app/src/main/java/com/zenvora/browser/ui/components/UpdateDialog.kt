package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenvora.browser.core.updater.AppUpdateInfo
import com.zenvora.browser.core.updater.UpdateState
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.DarkSurfaceVariant
import com.zenvora.browser.ui.theme.ElectricOrange
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: (AppUpdateInfo) -> Unit,
    onInstall: (File) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is UpdateState.Idle) return

    // Halaman Penuh Pembaruan Sistem Zenvora (Zero Floating Dialog)
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
                .padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary
                )
            }
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = ElectricOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Pembaruan Sistem Zenvora",
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
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                when (state) {
                    is UpdateState.Checking -> {
                        Text(
                            text = "Memeriksa rilis terbaru di repositori GitHub...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(20.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        )
                    }

                    is UpdateState.NoUpdateAvailable -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = ElectricOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Zenvora Anda sudah berada pada versi terbaru.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricOrange,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Kembali ke Zenvora", fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateState.UpdateAvailable -> {
                        Text(
                            text = "Versi Baru Tersedia: v${state.info.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val sizeMb = String.format(Locale.US, "%.1f MB", state.info.fileSizeBytes / (1024f * 1024f))
                        Text(
                            text = "Target ABI Perangkat: ${state.info.matchedAbi} | Ukuran Paket: $sizeMb",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkOutline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onDownload(state.info) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricOrange,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unduh & Pasang Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateState.Downloading -> {
                        Text(
                            text = "Mengunduh Paket Pembaruan (${state.progressPercent}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            color = ElectricOrange,
                            trackColor = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val downloadedMb = state.bytesDownloaded / (1024f * 1024f)
                        val totalMb = state.totalBytes / (1024f * 1024f)
                        Text(
                            text = String.format(Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    is UpdateState.ReadyToInstall -> {
                        Text(
                            text = "Unduhan selesai dan integritas paket APK terverifikasi.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { onInstall(state.apkFile) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricOrange,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pasang Pembaruan Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateState.Error -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = SecurityRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecurityRed
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecurityRed,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Kembali", fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateState.Idle -> Unit
                }
            }
        }
    }
}
