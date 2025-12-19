package com.example.fid.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val fatPer100g: Float,
    val carbPer100g: Float,
    val verificationLevel: String,
    val isFrequent: Boolean = false,
    val lastUsed: Long? = null
)

