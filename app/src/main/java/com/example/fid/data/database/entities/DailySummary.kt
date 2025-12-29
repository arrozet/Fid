package com.example.fid.data.database.entities

/**
 * DailySummary entity - used for Firestore database
 * Almacena un resumen diario de la nutrición del usuario
 */
data class DailySummary(
    val id: Long = 0,
    val userId: Long = 0,
    val date: Long = System.currentTimeMillis(), // Timestamp del inicio del día (00:00:00)
    val totalCalories: Float = 0f,
    val totalProteinG: Float = 0f,
    val totalFatG: Float = 0f,
    val totalCarbG: Float = 0f,
    val calorieGoal: Float = 0f,
    val proteinGoal: Float = 0f,
    val fatGoal: Float = 0f,
    val carbGoal: Float = 0f,
    val mealsCount: Int = 0,
    val waterIntakeMl: Float = 0f,
    val sleepHours: Float = 0f
)

