package com.example.fid.data.database.dao

import androidx.room.*
import com.example.fid.data.database.entities.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE userId = :userId AND timestamp >= :startDate AND timestamp < :endDate ORDER BY timestamp DESC")
    fun getFoodEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<FoodEntry>>
    
    @Query("SELECT * FROM food_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFoodEntries(userId: Long, limit: Int = 10): Flow<List<FoodEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(foodEntry: FoodEntry): Long
    
    @Update
    suspend fun updateFoodEntry(foodEntry: FoodEntry)
    
    @Delete
    suspend fun deleteFoodEntry(foodEntry: FoodEntry)
    
    @Query("DELETE FROM food_entries WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)
}

