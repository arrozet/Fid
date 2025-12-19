package com.example.fid.data.database.dao

import androidx.room.*
import com.example.fid.data.database.entities.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY isFrequent DESC, lastUsed DESC")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    
    @Query("SELECT * FROM food_items WHERE isFrequent = 1 ORDER BY lastUsed DESC LIMIT 10")
    fun getFrequentFoodItems(): Flow<List<FoodItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem): Long
    
    @Update
    suspend fun updateFoodItem(foodItem: FoodItem)
    
    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItem)
    
    @Query("UPDATE food_items SET lastUsed = :timestamp, isFrequent = 1 WHERE id = :foodId")
    suspend fun markAsUsed(foodId: Long, timestamp: Long = System.currentTimeMillis())
}

