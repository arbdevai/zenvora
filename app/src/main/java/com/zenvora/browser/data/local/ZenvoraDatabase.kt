package com.zenvora.browser.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenvora.browser.data.local.dao.BookmarkDao
import com.zenvora.browser.data.local.dao.HistoryDao
import com.zenvora.browser.data.local.dao.SavedLoginDao
import com.zenvora.browser.data.local.entity.BookmarkEntity
import com.zenvora.browser.data.local.entity.HistoryEntity
import com.zenvora.browser.data.local.entity.SavedLoginEntity

@Database(
    entities = [HistoryEntity::class, BookmarkEntity::class, SavedLoginEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ZenvoraDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun savedLoginDao(): SavedLoginDao

    companion object {
        @Volatile
        private var instance: ZenvoraDatabase? = null

        fun getInstance(context: Context): ZenvoraDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZenvoraDatabase::class.java,
                    "zenvora_browser.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
