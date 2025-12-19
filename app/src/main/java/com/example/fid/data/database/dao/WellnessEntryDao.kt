package com.example.fid.data.database.dao

import androidx.room.*
import com.example.fid.data.database.entities.WellnessEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessEntryDao {
    @Query("SELECT * FROM wellness_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getWellnessEntryByDate(userId: Long, date: Long): Flow<WellnessEntry?>
    
    @Query("SELECT * FROM wellness_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    fun getWellnessEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WellnessEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWellnessEntry(wellnessEntry: WellnessEntry): Long
    
    @Update
    suspend fun updateWellnessEntry(wellnessEntry: WellnessEntry)
    
    @Delete
    suspend fun deleteWellnessEntry(wellnessEntry: WellnessEntry)
}

