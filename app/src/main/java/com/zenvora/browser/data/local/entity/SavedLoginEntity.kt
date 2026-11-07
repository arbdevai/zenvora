package com.zenvora.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_logins")
data class SavedLoginEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val host: String,
    val siteUrl: String,
    val username: String,
    val passwordValue: String,
    val updatedAt: Long = System.currentTimeMillis()
)
