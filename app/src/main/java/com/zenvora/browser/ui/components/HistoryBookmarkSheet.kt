package com.zenvora.browser.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenvora.browser.data.local.entity.BookmarkEntity
import com.zenvora.browser.data.local.entity.HistoryEntity
import com.zenvora.browser.ui.theme.AccentTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBookmarkSheet(
    historyList: List<HistoryEntity>,
    bookmarkList: List<BookmarkEntity>,
    onItemClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Histori", "Bookmark")

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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AccentTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentTeal
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTabIndex == index) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTabIndex == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Riwayat Kunjungan (${historyList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Hapus Semua Histori",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada histori penelusuran",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            HistoryRowItem(
                                title = item.title,
                                url = item.url,
                                timestamp = item.visitTimestamp,
                                onClick = { onItemClick(item.url) }
                            )
                        }
                    }
                }
            } else {
                if (bookmarkList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada bookmark yang disimpan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        items(bookmarkList, key = { it.id }) { item ->
                            BookmarkRowItem(
                                title = item.title,
                                url = item.url,
                                onClick = { onItemClick(item.url) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryRowItem(
    title: String,
    url: String,
    timestamp: Long,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()) }
    val formattedDate = remember(timestamp) { dateFormat.format(Date(timestamp)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifEmpty { url },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BookmarkRowItem(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = AccentTeal,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifEmpty { url },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
