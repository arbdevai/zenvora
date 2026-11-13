package com.zenvora.gesture.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.zenvora.gesture.data.local.dao.ProfileDao
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ScrollDirection
import com.zenvora.gesture.data.model.StepActionType

class ZenvoraConverters {
    @TypeConverter
    fun fromProfileMode(value: ProfileMode): String = value.name

    @TypeConverter
    fun toProfileMode(value: String): ProfileMode =
        runCatching { ProfileMode.valueOf(value) }.getOrDefault(ProfileMode.AUTO_SCROLL)

    @TypeConverter
    fun fromScrollDirection(value: ScrollDirection): String = value.name

    @TypeConverter
    fun toScrollDirection(value: String): ScrollDirection =
        runCatching { ScrollDirection.valueOf(value) }.getOrDefault(ScrollDirection.DOWN)

    @TypeConverter
    fun fromStepActionType(value: StepActionType): String = value.name

    @TypeConverter
    fun toStepActionType(value: String): StepActionType =
        runCatching { StepActionType.valueOf(value) }.getOrDefault(StepActionType.TAP)
}

@Database(
    entities = [
        GestureProfileEntity::class,
        GestureStepEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ZenvoraConverters::class)
abstract class ZenvoraDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ZenvoraDatabase? = null

        fun getInstance(context: Context): ZenvoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZenvoraDatabase::class.java,
                    "zenvora_gesture.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
