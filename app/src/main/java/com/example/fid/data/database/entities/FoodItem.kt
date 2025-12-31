package com.example.fid.data.database.entities

import android.content.Context
import com.example.fid.utils.LocaleHelper

/**
 * FoodItem entity - used for Firestore database
 * Supports multiple languages through nameEs and nameEn fields
 */
data class FoodItem(
    val id: Long = 0,
    val name: String = "", // Deprecated: mantener por compatibilidad
    val nameEs: String = "", // Nombre en español
    val nameEn: String = "", // Nombre en inglés
    val caloriesPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val carbPer100g: Float = 0f,
    val verificationLevel: String = "user",
    val isFrequent: Boolean = false,
    val lastUsed: Long? = null,
    val createdByUserId: Long? = null // null = comida global, userId = comida personalizada
) {
    /**
     * Obtiene el nombre del alimento según el idioma actual del dispositivo
     */
    fun getLocalizedName(context: Context): String {
        val currentLanguage = LocaleHelper.getCurrentLanguage(context)
        return when (currentLanguage) {
            "en" -> {
                // Prioridad: nameEn -> nameEs -> name (para compatibilidad)
                if (nameEn.isNotBlank()) nameEn
                else if (nameEs.isNotBlank()) nameEs
                else name
            }
            else -> {
                // Prioridad: nameEs -> nameEn -> name (para compatibilidad)
                if (nameEs.isNotBlank()) nameEs
                else if (nameEn.isNotBlank()) nameEn
                else name
            }
        }
    }
    
    /**
     * Obtiene el nombre del alimento según un idioma específico
     */
    fun getNameForLanguage(language: String): String {
        return when (language) {
            "en" -> {
                if (nameEn.isNotBlank()) nameEn
                else if (nameEs.isNotBlank()) nameEs
                else name
            }
            else -> {
                if (nameEs.isNotBlank()) nameEs
                else if (nameEn.isNotBlank()) nameEn
                else name
            }
        }
    }
}
