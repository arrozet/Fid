package com.example.fid.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fid.data.database.dao.*
import com.example.fid.data.database.entities.*

@Database(
    entities = [
        User::class,
        FoodEntry::class,
        FoodItem::class,
        WellnessEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FidDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun wellnessEntryDao(): WellnessEntryDao
    
    companion object {
        @Volatile
        private var INSTANCE: FidDatabase? = null
        
        fun getDatabase(context: Context): FidDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FidDatabase::class.java,
                    "fid_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

