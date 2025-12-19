package com.example.fid.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val name: String,
    val age: Int,
    val gender: String, // "male", "female", "other"
    val heightCm: Float,
    val currentWeightKg: Float,
    val targetWeightKg: Float?,
    val activityLevel: String, // "sedentary", "light", "moderate", "very_active", "athlete"
    val goal: String, // "lose_weight", "maintain_weight", "gain_muscle"
    val tdee: Float, // Total Daily Energy Expenditure
    val proteinGoalG: Float,
    val fatGoalG: Float,
    val carbGoalG: Float,
    val numberlessMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

