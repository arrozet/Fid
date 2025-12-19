package com.example.fid.data.repository

import com.example.fid.data.database.FidDatabase
import com.example.fid.data.database.entities.*
import kotlinx.coroutines.flow.Flow

class FidRepository(private val database: FidDatabase) {
    
    // User operations
    fun getUserById(userId: Long): Flow<User?> = database.userDao().getUserById(userId)
    
    suspend fun getUserByEmail(email: String): User? = database.userDao().getUserByEmail(email)
    
    suspend fun insertUser(user: User): Long = database.userDao().insertUser(user)
    
    suspend fun updateUser(user: User) = database.userDao().updateUser(user)
    
    suspend fun getCurrentUser(): User? = database.userDao().getCurrentUser()
    
    // Food Entry operations
    fun getFoodEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<FoodEntry>> =
        database.foodEntryDao().getFoodEntriesByDateRange(userId, startDate, endDate)
    
    fun getRecentFoodEntries(userId: Long, limit: Int = 10): Flow<List<FoodEntry>> =
        database.foodEntryDao().getRecentFoodEntries(userId, limit)
    
    suspend fun insertFoodEntry(foodEntry: FoodEntry): Long =
        database.foodEntryDao().insertFoodEntry(foodEntry)
    
    suspend fun deleteFoodEntry(foodEntry: FoodEntry) =
        database.foodEntryDao().deleteFoodEntry(foodEntry)
    
    // Food Item operations
    fun searchFoodItems(query: String): Flow<List<FoodItem>> =
        database.foodItemDao().searchFoodItems(query)
    
    fun getFrequentFoodItems(): Flow<List<FoodItem>> =
        database.foodItemDao().getFrequentFoodItems()
    
    suspend fun insertFoodItem(foodItem: FoodItem): Long =
        database.foodItemDao().insertFoodItem(foodItem)
    
    suspend fun markFoodAsUsed(foodId: Long) =
        database.foodItemDao().markAsUsed(foodId)
    
    // Wellness operations
    fun getWellnessEntryByDate(userId: Long, date: Long): Flow<WellnessEntry?> =
        database.wellnessEntryDao().getWellnessEntryByDate(userId, date)
    
    fun getWellnessEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WellnessEntry>> =
        database.wellnessEntryDao().getWellnessEntriesByDateRange(userId, startDate, endDate)
    
    suspend fun insertWellnessEntry(wellnessEntry: WellnessEntry): Long =
        database.wellnessEntryDao().insertWellnessEntry(wellnessEntry)
    
    suspend fun updateWellnessEntry(wellnessEntry: WellnessEntry) =
        database.wellnessEntryDao().updateWellnessEntry(wellnessEntry)
    
    // Helper function to calculate TDEE using Mifflin-St Jeor equation
    fun calculateTDEE(
        gender: String,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activityLevel: String
    ): Float {
        // Calculate BMR (Basal Metabolic Rate)
        val bmr = if (gender == "male") {
            10 * weightKg + 6.25f * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25f * heightCm - 5 * age - 161
        }
        
        // Apply activity multiplier
        val activityMultiplier = when (activityLevel) {
            "sedentary" -> 1.2f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "very_active" -> 1.725f
            "athlete" -> 1.9f
            else -> 1.2f
        }
        
        return bmr * activityMultiplier
    }
    
    // Calculate macros based on goal
    fun calculateMacros(tdee: Float, goal: String): Triple<Float, Float, Float> {
        val adjustedCalories = when (goal) {
            "lose_weight" -> tdee * 0.85f // 15% deficit
            "gain_muscle" -> tdee * 1.1f // 10% surplus
            else -> tdee // maintain
        }
        
        // Standard macro split: 30% protein, 30% fat, 40% carbs
        val proteinG = (adjustedCalories * 0.30f) / 4f // 4 cal per gram
        val fatG = (adjustedCalories * 0.30f) / 9f // 9 cal per gram
        val carbG = (adjustedCalories * 0.40f) / 4f // 4 cal per gram
        
        return Triple(proteinG, fatG, carbG)
    }
}

