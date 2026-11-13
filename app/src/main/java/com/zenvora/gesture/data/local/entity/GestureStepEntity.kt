package com.zenvora.gesture.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zenvora.gesture.data.model.StepActionType

@Entity(
    tableName = "gesture_steps",
    foreignKeys = [
        ForeignKey(
            entity = GestureProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class GestureStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val profileId: Long,
    val orderIndex: Int = 0,
    val actionType: StepActionType = StepActionType.TAP,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val endX: Float = 0.5f,
    val endY: Float = 0.35f,
    val delayAfterMs: Long = 1_500L,
    val durationMs: Long = 80L
)
