package com.example.fid.data.database.entities

import android.content.Context
import com.example.fid.utils.LocaleHelper

/**
 * Representa un ingrediente individual detectado por IA
 */
data class AIIngredient(
    val name: String = "",
    val nameEs: String = "",
    val nameEn: String = "",
    val estimatedGrams: Float = 0f,
    val calories: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f,
    val carbsG: Float = 0f
) {
    fun getLocalizedName(context: Context): String {
        val currentLanguage = LocaleHelper.getCurrentLanguage(context)
        return when (currentLanguage) {
            "en" -> nameEn.ifBlank { nameEs.ifBlank { name } }
            else -> nameEs.ifBlank { nameEn.ifBlank { name } }
        }
    }
}

/**
 * FoodEntry entity - used for Firestore database
 * Supports multiple languages through foodNameEs and foodNameEn fields
 */
data class FoodEntry(
    val id: Long = 0,
    val userId: Long = 0,
    val foodName: String = "", // Fallback: se usa español como valor por defecto
    val foodNameEs: String = "", // Nombre en español
    val foodNameEn: String = "", // Nombre en inglés
    val amountGrams: Float = 0f,
    val calories: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f,
    val carbG: Float = 0f,
    val mealType: String = "snack", // "breakfast", "lunch", "dinner", "snack"
    val registrationMethod: String = "manual", // "photo", "voice", "manual", "photo_ai"
    val verificationLevel: String = "user", // "manufacturer", "government", "community", "user", "ai"
    val timestamp: Long = System.currentTimeMillis(),
    // AI-detected ingredients
    val aiIngredients: List<AIIngredient> = emptyList(),
    val aiConfidence: String = "", // "high", "medium", "low"
    val aiAnalyzed: Boolean = false
) {
    /**
     * Obtiene el nombre del alimento según el idioma actual del dispositivo
     */
    fun getLocalizedFoodName(context: Context): String {
        val currentLanguage = LocaleHelper.getCurrentLanguage(context)
        return when (currentLanguage) {
            "en" -> {
                // Prioridad: foodNameEn -> foodNameEs -> foodName (para compatibilidad)
                if (foodNameEn.isNotBlank()) foodNameEn
                else if (foodNameEs.isNotBlank()) foodNameEs
                else foodName
            }
            else -> {
                // Prioridad: foodNameEs -> foodNameEn -> foodName (para compatibilidad)
                if (foodNameEs.isNotBlank()) foodNameEs
                else if (foodNameEn.isNotBlank()) foodNameEn
                else foodName
            }
        }
    }
    
    /**
     * Obtiene el nombre del alimento según un idioma específico
     */
    fun getNameForLanguage(language: String): String {
        return when (language) {
            "en" -> {
                if (foodNameEn.isNotBlank()) foodNameEn
                else if (foodNameEs.isNotBlank()) foodNameEs
                else foodName
            }
            else -> {
                if (foodNameEs.isNotBlank()) foodNameEs
                else if (foodNameEn.isNotBlank()) foodNameEn
                else foodName
            }
        }
    }
}

