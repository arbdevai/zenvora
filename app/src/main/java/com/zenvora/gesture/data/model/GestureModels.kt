package com.zenvora.gesture.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity

enum class ProfileMode(val label: String) {
    AUTO_SCROLL("Auto-Scroll"),
    SINGLE_TAP("Auto-Tap Tunggal"),
    MACRO_SEQUENCE("Macro Multi-Langkah")
}

enum class ScrollDirection(val label: String) {
    DOWN("Gulir ke Bawah"),
    UP("Gulir ke Atas"),
    LEFT("Geser ke Kiri"),
    RIGHT("Geser ke Kanan")
}

enum class StepActionType(val label: String) {
    TAP("Ketuk"),
    SWIPE("Geser"),
    WAIT("Jeda")
}

enum class IntervalUnit(val label: String, val multiplierMs: Long) {
    MILLISECONDS("Milidetik", 1L),
    SECONDS("Detik", 1_000L),
    MINUTES("Menit", 60_000L)
}

data class ProfileWithSteps(
    @Embedded val profile: GestureProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val steps: List<GestureStepEntity>
) {
    val orderedSteps: List<GestureStepEntity>
        get() = steps.sortedBy { it.orderIndex }
}

fun formatIntervalMs(ms: Long): String {
    return when {
        ms >= 60_000L && ms % 60_000L == 0L -> "${ms / 60_000L} menit"
        ms >= 60_000L -> {
            val mins = ms / 60_000L
            val secs = (ms % 60_000L) / 1_000L
            if (secs > 0) "${mins}m ${secs}d" else "${mins}m"
        }
        ms >= 1_000L && ms % 1_000L == 0L -> "${ms / 1_000L} detik"
        ms >= 1_000L -> String.format("%.1f detik", ms / 1000.0)
        else -> "$ms ms"
    }
}
