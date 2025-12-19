package com.example.fid.data.cloud

import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry

/**
 * Interface for cloud database operations
 * 
 * This interface defines the contract for cloud database operations.
 * Implementation can be done using Firebase, AWS, Azure, or any other cloud provider.
 * 
 * TODO: Implement this interface with your chosen cloud database provider
 */
interface CloudDatabaseInterface {
    
    /**
     * Sync local user data to cloud
     */
    suspend fun syncUser(user: User): Result<Unit>
    
    /**
     * Fetch user data from cloud
     */
    suspend fun fetchUser(userId: Long): Result<User>
    
    /**
     * Sync food entries to cloud
     */
    suspend fun syncFoodEntries(entries: List<FoodEntry>): Result<Unit>
    
    /**
     * Fetch food entries from cloud for a date range
     */
    suspend fun fetchFoodEntries(userId: Long, startDate: Long, endDate: Long): Result<List<FoodEntry>>
    
    /**
     * Sync wellness entries to cloud
     */
    suspend fun syncWellnessEntries(entries: List<WellnessEntry>): Result<Unit>
    
    /**
     * Fetch wellness entries from cloud
     */
    suspend fun fetchWellnessEntries(userId: Long, startDate: Long, endDate: Long): Result<List<WellnessEntry>>
    
    /**
     * Perform full sync (both upload and download)
     */
    suspend fun performFullSync(userId: Long): Result<Unit>
}

