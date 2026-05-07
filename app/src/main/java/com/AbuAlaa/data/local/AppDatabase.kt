package com.AbuAlaa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for the app
 */
@Database(
    entities = [PrayerDayEntity::class],
    version = 2, // Keep version incremented or revert if no migration needed yet
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun prayerDao(): PrayerDao
    // abstract fun noorDao(): NoorDao // Commented out
    
    companion object {
        private const val DATABASE_NAME = "almuadhin_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
