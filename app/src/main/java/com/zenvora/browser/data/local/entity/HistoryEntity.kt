package com.zenvora.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val visitTimestamp: Long = System.currentTimeMillis()
)
