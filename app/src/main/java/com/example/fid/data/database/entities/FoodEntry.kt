package com.example.fid.data.database.entities

/**
 * FoodEntry entity - used for Firestore database
 */
data class FoodEntry(
    val id: Long = 0,
    val userId: Long = 0,
    val foodName: String = "",
    val amountGrams: Float = 0f,
    val calories: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f,
    val carbG: Float = 0f,
    val mealType: String = "snack", // "breakfast", "lunch", "dinner", "snack"
    val registrationMethod: String = "manual", // "photo", "voice", "manual"
    val verificationLevel: String = "user", // "manufacturer", "government", "community", "user"
    val timestamp: Long = System.currentTimeMillis()
)

