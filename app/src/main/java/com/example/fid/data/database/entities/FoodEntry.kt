package com.example.fid.data.database.entities

/**
 * FoodEntry entity - used for Firestore database
 */
data class FoodEntry(
    val id: Long = 0,
    val userId: Long,
    val foodName: String,
    val amountGrams: Float,
    val calories: Float,
    val proteinG: Float,
    val fatG: Float,
    val carbG: Float,
    val mealType: String, // "breakfast", "lunch", "dinner", "snack"
    val registrationMethod: String, // "photo", "voice", "manual"
    val verificationLevel: String, // "manufacturer", "government", "community", "user"
    val timestamp: Long = System.currentTimeMillis()
)

