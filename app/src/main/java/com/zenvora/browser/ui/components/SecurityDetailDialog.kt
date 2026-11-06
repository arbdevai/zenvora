package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenvora.browser.data.model.SecurityInfo
import com.zenvora.browser.data.model.SecurityStatus
import com.zenvora.browser.ui.theme.SecurityGreen
import com.zenvora.browser.ui.theme.SecurityRed
import com.zenvora.browser.ui.theme.SecurityYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDetailDialog(
    securityInfo: SecurityInfo,
    onDismissRequest: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
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
                            "Halaman Internal Browser"
                        )
                        SecurityStatus.UNKNOWN -> Triple(
                            Icons.Filled.Info,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            "Status Koneksi Tidak Diketahui"
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(label = "Host Domain", value = securityInfo.host.ifEmpty { "-" })
                DetailItem(label = "Protokol", value = securityInfo.scheme.uppercase().ifEmpty { "-" })

                securityInfo.certificateSubject?.let { subject ->
                    DetailItem(label = "Subjek Sertifikat", value = subject)
                }
                securityInfo.certificateIssuer?.let { issuer ->
                    DetailItem(label = "Penerbit Sertifikat", value = issuer)
                }
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
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
