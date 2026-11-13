package com.zenvora.gesture.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenvora.gesture.core.engine.EngineStatus
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.data.model.formatIntervalMs
import com.zenvora.gesture.service.OverlayForegroundService
import com.zenvora.gesture.service.ZenvoraAccessibilityService
import com.zenvora.gesture.ui.components.ProfileEditorSheet
import com.zenvora.gesture.ui.components.SkeletonCard
import com.zenvora.gesture.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val editingProfile by viewModel.editingProfile.collectAsStateWithLifecycle()

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Zenvora", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Humanized Gesture Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewProfileEditor() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Buat Profil Baru")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionReadinessCard(
                    isAccessibilityReady = isAccessibilityConnected,
                    isOverlayReady = canDrawOverlays,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            item {
                EngineControlCard(
                    activeProfile = uiState.activeProfile,
                    status = runtimeState.status,
                    cycleCount = runtimeState.cycleCount,
                    nextActionInMs = runtimeState.nextActionInMs,
                    canStart = isAccessibilityConnected && uiState.activeProfile != null,
                    canOverlay = canDrawOverlays,
                    onStartOrPause = {
                        val service = ZenvoraAccessibilityService.instance ?: return@EngineControlCard
                        when (runtimeState.status) {
                            EngineStatus.RUNNING -> service.pauseExecution()
                            EngineStatus.PAUSED -> service.resumeExecution()
                            EngineStatus.IDLE, EngineStatus.ERROR -> {
                                uiState.activeProfile?.let { service.startProfile(it) }
                            }
                        }
                    },
                    onStop = {
                        ZenvoraAccessibilityService.instance?.stopExecution()
                    },
                    onShowOverlay = {
                        if (canDrawOverlays) {
                            OverlayForegroundService.start(context)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                Text(
                    text = "Daftar Profil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.isLoading) {
                items(3) {
                    SkeletonCard()
                }
            } else {
                items(uiState.profiles, key = { it.profile.id }) { item ->
                    ProfileCard(
                        item = item,
                        onSelectActive = { viewModel.selectActiveProfile(item.profile.id) },
                        onEdit = { viewModel.openEditor(item) },
                        onDuplicate = { viewModel.duplicateProfile(item) },
                        onDelete = { viewModel.deleteProfile(item.profile.id) }
                    )
                }
            }
        }
    }

    editingProfile?.let { target ->
        ProfileEditorSheet(
            initial = target,
            onDismiss = { viewModel.closeEditor() },
            onSave = { profile, steps ->
                viewModel.saveProfile(profile, steps)
            }
        )
    }
}

@Composable
private fun PermissionReadinessCard(
    isAccessibilityReady: Boolean,
    isOverlayReady: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status Izin Sistem",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PermissionRow(
                icon = Icons.Rounded.AccessibilityNew,
                title = "Layanan Aksesibilitas",
                isGranted = isAccessibilityReady,
                actionLabel = if (isAccessibilityReady) "Aktif" else "Aktifkan",
                onClick = onOpenAccessibility
            )

            PermissionRow(
                icon = Icons.Rounded.Layers,
                title = "Tampil di Atas Aplikasi Lain (Overlay)",
                isGranted = isOverlayReady,
                actionLabel = if (isOverlayReady) "Aktif" else "Izinkan",
                onClick = onOpenOverlay
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (isGranted) "Siap digunakan" else "Diperlukan agar gestur dapat berjalan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isGranted) {
            OutlinedButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EngineControlCard(
    activeProfile: ProfileWithSteps?,
    status: EngineStatus,
    cycleCount: Int,
    nextActionInMs: Long,
    canStart: Boolean,
    canOverlay: Boolean,
    onStartOrPause: () -> Unit,
    onStop: () -> Unit,
    onShowOverlay: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeProfile?.profile?.name ?: "Belum Ada Profil Aktif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val intervalLabel = activeProfile?.let {
                        "${it.profile.mode.label} • Interval ${formatIntervalMs(it.profile.intervalMs)}"
                    } ?: "Buat atau pilih profil di bawah"
                    Text(
                        text = intervalLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when {
                            status == EngineStatus.RUNNING && nextActionInMs > 0L ->
                                "${(nextActionInMs + 999L) / 1000L}d • #$cycleCount"
                            status == EngineStatus.RUNNING -> "Aktif • #$cycleCount"
                            else -> status.label
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartOrPause,
                    enabled = canStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (status == EngineStatus.RUNNING) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (status) {
                            EngineStatus.RUNNING -> "Jeda"
                            EngineStatus.PAUSED -> "Lanjut"
                            else -> "Mulai"
                        }
                    )
                }

                if (status == EngineStatus.RUNNING || status == EngineStatus.PAUSED) {
                    OutlinedButton(onClick = onStop) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }

                OutlinedButton(
                    onClick = onShowOverlay,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (canOverlay) "Buka Overlay" else "Izin Overlay")
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    item: ProfileWithSteps,
    onSelectActive: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val profile = item.profile
    Surface(
        onClick = onSelectActive,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (profile.isActive) 1.5.dp else 1.dp,
            color = if (profile.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${profile.mode.label} • ${formatIntervalMs(profile.intervalMs)} (±${profile.intervalVarianceMs / 1000.0}d)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (profile.isActive) {
                    Text(
                        text = "Aktif",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jitter ${profile.humanizedJitterPx}px • ${if (profile.useBezierCurve) "Kurva Bezier" else "Garis Lurus"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplikat")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus")
                    }
                }
            }
        }
    }
}
