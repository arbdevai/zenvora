package com.zenvora.gesture.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity
import com.zenvora.gesture.data.model.ProfileWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Transaction
    @Query("SELECT * FROM gesture_profiles ORDER BY updatedAt DESC")
    fun observeProfilesWithSteps(): Flow<List<ProfileWithSteps>>

    @Transaction
    @Query("SELECT * FROM gesture_profiles WHERE isActive = 1 LIMIT 1")
    fun observeActiveProfile(): Flow<ProfileWithSteps?>

    @Transaction
    @Query("SELECT * FROM gesture_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileWithSteps(profileId: Long): ProfileWithSteps?

    @Transaction
    @Query("SELECT * FROM gesture_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): ProfileWithSteps?

    @Query("SELECT COUNT(*) FROM gesture_profiles")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GestureProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: GestureProfileEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<GestureStepEntity>): List<Long>

    @Query("DELETE FROM gesture_steps WHERE profileId = :profileId")
    suspend fun deleteStepsByProfile(profileId: Long): Int

    @Query("DELETE FROM gesture_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long): Int

    @Query("UPDATE gesture_profiles SET isActive = 0")
    suspend fun clearAllActive(): Int

    @Query("UPDATE gesture_profiles SET isActive = 1, updatedAt = :timestamp WHERE id = :profileId")
    suspend fun markProfileActive(profileId: Long, timestamp: Long): Int

    @Query("UPDATE gesture_steps SET x = :x, y = :y WHERE id = :stepId")
    suspend fun updateStepCoordinates(stepId: Long, x: Float, y: Float): Int
}
