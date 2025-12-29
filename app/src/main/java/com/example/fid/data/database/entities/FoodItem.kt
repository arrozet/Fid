package com.example.fid.data.database.entities

/**
 * FoodItem entity - used for Firestore database
 */
data class FoodItem(
    val id: Long = 0,
    val name: String = "",
    val caloriesPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val carbPer100g: Float = 0f,
    val verificationLevel: String = "user",
    val isFrequent: Boolean = false,
    val lastUsed: Long? = null
)

