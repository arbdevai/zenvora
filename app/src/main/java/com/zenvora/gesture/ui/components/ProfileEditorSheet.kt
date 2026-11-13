package com.zenvora.gesture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity
import com.zenvora.gesture.data.model.IntervalUnit
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.data.model.ScrollDirection
import com.zenvora.gesture.data.model.StepActionType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorSheet(
    initial: ProfileWithSteps,
    onDismiss: () -> Unit,
    onSave: (GestureProfileEntity, List<GestureStepEntity>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(initial) { mutableStateOf(initial.profile.name) }
    var mode by remember(initial) { mutableStateOf(initial.profile.mode) }

    val initialUnit = remember(initial) {
        when {
            initial.profile.intervalMs >= 60_000L && initial.profile.intervalMs % 60_000L == 0L -> IntervalUnit.MINUTES
            initial.profile.intervalMs >= 1_000L -> IntervalUnit.SECONDS
            else -> IntervalUnit.MILLISECONDS
        }
    }
    var intervalUnit by remember(initial) { mutableStateOf(initialUnit) }
    var intervalInput by remember(initial) {
        mutableStateOf((initial.profile.intervalMs / initialUnit.multiplierMs).coerceAtLeast(1L).toString())
    }
    var varianceSec by remember(initial) {
        mutableFloatStateOf((initial.profile.intervalVarianceMs / 1_000f).coerceIn(0f, 15f))
    }
    var jitterPx by remember(initial) {
        mutableFloatStateOf(initial.profile.humanizedJitterPx.toFloat().coerceIn(0f, 40f))
    }
    var useBezier by remember(initial) { mutableStateOf(initial.profile.useBezierCurve) }
    var scrollDirection by remember(initial) { mutableStateOf(initial.profile.scrollDirection) }
    var scrollDistance by remember(initial) {
        mutableFloatStateOf(initial.profile.scrollDistancePercent.toFloat().coerceIn(15f, 85f))
    }
    var repeatInput by remember(initial) { mutableStateOf(initial.profile.repeatCount.toString()) }
    var steps by remember(initial) { mutableStateOf(initial.orderedSteps) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial.profile.id == 0L) "Buat Profil Gestur" else "Ubah Profil Gestur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Profil") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Mode Eksekusi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { mode = item },
                        label = { Text(item.label) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Interval Siklus",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickPresets = listOf(
                    "5 dtk" to (5L to IntervalUnit.SECONDS),
                    "15 dtk" to (15L to IntervalUnit.SECONDS),
                    "30 dtk" to (30L to IntervalUnit.SECONDS),
                    "60 dtk" to (60L to IntervalUnit.SECONDS),
                    "2 mnt" to (2L to IntervalUnit.MINUTES)
                )
                quickPresets.forEach { (label, pair) ->
                    val (valNum, unit) = pair
                    val isSelected = intervalUnit == unit && intervalInput == valNum.toString()
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            intervalUnit = unit
                            intervalInput = valNum.toString()
                        },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { raw ->
                        intervalInput = raw.filter { it.isDigit() }
                    },
                    label = { Text("Nilai Interval") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IntervalUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = intervalUnit == unit,
                                onClick = { intervalUnit = unit },
                                label = { Text(unit.label) }
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Variasi Acak Interval: +/- ${String.format("%.1f", varianceSec)} detik",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = varianceSec,
                    onValueChange = { varianceSec = it },
                    valueRange = 0f..15f,
                    steps = 29
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Humanisasi Gerakan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column {
                Text(
                    text = "Radius Jitter Koordinat: ${jitterPx.roundToInt()} px",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = jitterPx,
                    onValueChange = { jitterPx = it },
                    valueRange = 0f..40f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jalur Kurva Bezier", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Membuat lintasan swipe melengkung natural seperti ibu jari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = useBezier, onCheckedChange = { useBezier = it })
            }

            if (mode == ProfileMode.AUTO_SCROLL) {
                Text(
                    text = "Arah & Jarak Gulir",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScrollDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = scrollDirection == dir,
                            onClick = { scrollDirection = dir },
                            label = { Text(dir.label) }
                        )
                    }
                }
                Column {
                    Text(
                        text = "Jarak Gulir: ${scrollDistance.roundToInt()}% layar",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = scrollDistance,
                        onValueChange = { scrollDistance = it },
                        valueRange = 15f..85f
                    )
                }
            }

            if (mode == ProfileMode.MACRO_SEQUENCE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Langkah Macro (${steps.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            steps = steps + GestureStepEntity(
                                profileId = initial.profile.id,
                                orderIndex = steps.size,
                                actionType = StepActionType.TAP,
                                x = 0.5f,
                                y = (0.4f + (steps.size * 0.08f)).coerceAtMost(0.85f),
                                delayAfterMs = 1_500L
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tambah Titik")
                    }
                }

                steps.forEachIndexed { idx, step ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Langkah #${idx + 1} - ${step.actionType.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Posisi (${(step.x * 100).roundToInt()}%, ${(step.y * 100).roundToInt()}%) | Jeda ${step.delayAfterMs} ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = step.actionType == StepActionType.SWIPE,
                                    onClick = {
                                        val nextType = when (step.actionType) {
                                            StepActionType.TAP -> StepActionType.SWIPE
                                            StepActionType.SWIPE -> StepActionType.TAP
                                            StepActionType.WAIT -> StepActionType.TAP
                                        }
                                        steps = steps.toMutableList().also {
                                            it[idx] = step.copy(actionType = nextType)
                                        }
                                    },
                                    label = { Text(step.actionType.label) }
                                )
                                if (steps.size > 1) {
                                    IconButton(
                                        onClick = {
                                            steps = steps.filterIndexed { i, _ -> i != idx }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus langkah")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = repeatInput,
                onValueChange = { raw -> repeatInput = raw.filter { it.isDigit() } },
                label = { Text("Batas Ulang Siklus (0 = Tanpa Batas)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val numericInterval = intervalInput.toLongOrNull()?.coerceAtLeast(1L) ?: 30L
                        val finalIntervalMs = (numericInterval * intervalUnit.multiplierMs).coerceAtLeast(150L)
                        val finalRepeat = repeatInput.toIntOrNull() ?: 0
                        val updatedProfile = initial.profile.copy(
                            name = name.trim().ifEmpty { "Profil Gestur" },
                            mode = mode,
                            intervalMs = finalIntervalMs,
                            intervalVarianceMs = (varianceSec * 1_000f).roundToInt().toLong(),
                            humanizedJitterPx = jitterPx.roundToInt(),
                            useBezierCurve = useBezier,
                            scrollDirection = scrollDirection,
                            scrollDistancePercent = scrollDistance.roundToInt(),
                            repeatCount = finalRepeat
                        )
                        onSave(updatedProfile, steps)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Profil")
                }
            }
        }
    }
}
