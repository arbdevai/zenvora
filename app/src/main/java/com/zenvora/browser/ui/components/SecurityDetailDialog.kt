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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import com.zenvora.browser.ui.theme.DarkOutline
import com.zenvora.browser.ui.theme.DarkSurfaceContainer
import com.zenvora.browser.ui.theme.OledBlack
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.SecurityYellow
import com.zenvora.browser.ui.theme.TextPrimary
import com.zenvora.browser.ui.theme.TextSecondary

@Composable
fun SecurityDetailDialog(
    securityInfo: SecurityInfo,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Halaman Penuh Informasi Keamanan & Sertifikat Situs (Zero Floating Dialog)
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
            Text(
                text = "Keamanan & Enkripsi Situs",
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
                .padding(16.dp)
        ) {
            val (icon, tint, statusText) = when (securityInfo.status) {
                SecurityStatus.SECURE -> Triple(
                    Icons.Filled.Lock,
                    SecurityGreen,
                    "Koneksi Terenkripsi (HTTPS)"
                )
                SecurityStatus.INSECURE -> Triple(
                    Icons.Filled.Warning,
                    SecurityRed,
                    "Koneksi Tidak Aman (HTTP)"
                )
                SecurityStatus.INTERNAL -> Triple(
                    Icons.Filled.Info,
                    SecurityYellow,
                    "Halaman Internal Zenvora"
                )
                SecurityStatus.UNKNOWN -> Triple(
                    Icons.Filled.Info,
                    TextSecondary,
                    "Status Koneksi Tidak Diketahui"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceContainer)
                    .border(1.dp, tint, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailItem(label = "Host Domain", value = securityInfo.host.ifEmpty { "Halaman Beranda Internal" })
            DetailItem(label = "Protokol Transmisi", value = securityInfo.scheme.uppercase().ifEmpty { "INTERNAL" })

            securityInfo.certificateSubject?.let { subject ->
                DetailItem(label = "Subjek Sertifikat", value = subject)
            }
            securityInfo.certificateIssuer?.let { issuer ->
                DetailItem(label = "Penerbit Sertifikat", value = issuer)
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceContainer)
            .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}
