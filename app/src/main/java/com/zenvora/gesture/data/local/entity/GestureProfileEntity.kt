package com.zenvora.gesture.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ScrollDirection

@Entity(tableName = "gesture_profiles")
data class GestureProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val mode: ProfileMode = ProfileMode.AUTO_SCROLL,
    val intervalMs: Long = 30_000L,
    val intervalVarianceMs: Long = 2_000L,
    val humanizedJitterPx: Int = 12,
    val useBezierCurve: Boolean = true,
    val scrollDirection: ScrollDirection = ScrollDirection.DOWN,
    val scrollDistancePercent: Int = 55,
    val scrollDurationMs: Long = 450L,
    val repeatCount: Int = 0,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
