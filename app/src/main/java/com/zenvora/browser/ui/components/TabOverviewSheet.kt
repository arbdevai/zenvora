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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenvora.browser.data.model.BrowserTab
import com.zenvora.browser.ui.theme.AccentTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabOverviewSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daftar Tab Aktif (${tabs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onNewTab,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tab Baru",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tab Baru")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    TabItemCard(
                        tab = tab,
                        isActive = isActive,
                        onSelect = { onTabSelected(tab.id) },
                        onClose = { onTabClosed(tab.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    val borderColor = if (isActive) AccentTeal else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Tab,
            contentDescription = null,
            tint = if (isActive) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.title.ifEmpty { "Tab Baru" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Tutup Tab",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
