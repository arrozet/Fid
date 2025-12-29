package com.example.fid.data.database.entities

/**
 * User entity - used for Firestore database
 * 
 * IMPORTANTE: Todos los parámetros tienen valores por defecto para que Firestore
 * pueda deserializar los objetos (requiere un constructor sin argumentos).
 */
data class User(
    val id: Long = 0,
    val email: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "male", // "male", "female", "other"
    val heightCm: Float = 0f,
    val currentWeightKg: Float = 0f,
    val targetWeightKg: Float? = null,
    val activityLevel: String = "moderate", // "sedentary", "light", "moderate", "very_active", "athlete"
    val goal: String = "maintain_weight", // "lose_weight", "maintain_weight", "gain_muscle"
    val tdee: Float = 0f, // Total Daily Energy Expenditure
    val proteinGoalG: Float = 0f,
    val fatGoalG: Float = 0f,
    val carbGoalG: Float = 0f,
    val waterGoalMl: Float = 2500f, // Objetivo de hidratación diaria en ml (por defecto 2.5L)
    val numberlessMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

