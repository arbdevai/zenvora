package com.zenvora.browser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenvora.browser.data.local.entity.SavedLoginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLoginDao {
    @Query("SELECT * FROM saved_logins ORDER BY updatedAt DESC")
    fun getAllSavedLogins(): Flow<List<SavedLoginEntity>>

    @Query("SELECT * FROM saved_logins WHERE host = :host ORDER BY updatedAt DESC")
    suspend fun getLoginsForHost(host: String): List<SavedLoginEntity>

    @Query("SELECT * FROM saved_logins WHERE host = :host AND username = :username LIMIT 1")
    suspend fun findLoginByHostAndUser(host: String, username: String): SavedLoginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLogin(login: SavedLoginEntity): Long

    @Query("DELETE FROM saved_logins WHERE id = :id")
    suspend fun deleteSavedLoginById(id: Long): Int

    @Query("DELETE FROM saved_logins")
    suspend fun clearAllSavedLogins(): Int
}
