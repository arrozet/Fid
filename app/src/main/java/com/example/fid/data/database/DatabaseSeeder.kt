package com.example.fid.data.database

import com.example.fid.data.database.entities.FoodItem
import com.example.fid.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds the database with initial food items
 */
class DatabaseSeeder(private val repository: FirebaseRepository) {
    
    suspend fun seedFoodItems() = withContext(Dispatchers.IO) {
        val sampleFoods = listOf(
            FoodItem(
                name = "Pollo a la plancha",
                caloriesPer100g = 165f,
                proteinPer100g = 31f,
                fatPer100g = 3.6f,
                carbPer100g = 0f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Arroz blanco cocido",
                caloriesPer100g = 130f,
                proteinPer100g = 2.7f,
                fatPer100g = 0.3f,
                carbPer100g = 28f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Ensalada verde",
                caloriesPer100g = 35f,
                proteinPer100g = 2.5f,
                fatPer100g = 0.5f,
                carbPer100g = 6f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Salmón",
                caloriesPer100g = 208f,
                proteinPer100g = 20f,
                fatPer100g = 13f,
                carbPer100g = 0f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Aguacate",
                caloriesPer100g = 160f,
                proteinPer100g = 2f,
                fatPer100g = 15f,
                carbPer100g = 9f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Plátano",
                caloriesPer100g = 89f,
                proteinPer100g = 1.1f,
                fatPer100g = 0.3f,
                carbPer100g = 23f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Huevo cocido",
                caloriesPer100g = 155f,
                proteinPer100g = 13f,
                fatPer100g = 11f,
                carbPer100g = 1.1f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Pan integral",
                caloriesPer100g = 247f,
                proteinPer100g = 13f,
                fatPer100g = 3.4f,
                carbPer100g = 41f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Yogur griego natural",
                caloriesPer100g = 97f,
                proteinPer100g = 9f,
                fatPer100g = 5f,
                carbPer100g = 3.6f,
                verificationLevel = "manufacturer"
            ),
            FoodItem(
                name = "Pasta cocida",
                caloriesPer100g = 131f,
                proteinPer100g = 5f,
                fatPer100g = 1.1f,
                carbPer100g = 25f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Pechuga de pavo",
                caloriesPer100g = 135f,
                proteinPer100g = 30f,
                fatPer100g = 1f,
                carbPer100g = 0f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Brócoli",
                caloriesPer100g = 34f,
                proteinPer100g = 2.8f,
                fatPer100g = 0.4f,
                carbPer100g = 7f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Almendras",
                caloriesPer100g = 579f,
                proteinPer100g = 21f,
                fatPer100g = 50f,
                carbPer100g = 22f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Lentejas cocidas",
                caloriesPer100g = 116f,
                proteinPer100g = 9f,
                fatPer100g = 0.4f,
                carbPer100g = 20f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Queso fresco",
                caloriesPer100g = 264f,
                proteinPer100g = 18f,
                fatPer100g = 21f,
                carbPer100g = 3.4f,
                verificationLevel = "manufacturer"
            ),
            FoodItem(
                name = "Tomate",
                caloriesPer100g = 18f,
                proteinPer100g = 0.9f,
                fatPer100g = 0.2f,
                carbPer100g = 3.9f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Avena",
                caloriesPer100g = 389f,
                proteinPer100g = 17f,
                fatPer100g = 6.9f,
                carbPer100g = 66f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Atún en lata",
                caloriesPer100g = 116f,
                proteinPer100g = 26f,
                fatPer100g = 1f,
                carbPer100g = 0f,
                verificationLevel = "manufacturer"
            ),
            FoodItem(
                name = "Manzana",
                caloriesPer100g = 52f,
                proteinPer100g = 0.3f,
                fatPer100g = 0.2f,
                carbPer100g = 14f,
                verificationLevel = "government"
            ),
            FoodItem(
                name = "Leche desnatada",
                caloriesPer100g = 34f,
                proteinPer100g = 3.4f,
                fatPer100g = 0.1f,
                carbPer100g = 5f,
                verificationLevel = "manufacturer"
            )
        )
        
        sampleFoods.forEach { food ->
            try {
                repository.insertFoodItem(food)
            } catch (e: Exception) {
                // Food might already exist, ignore
            }
        }
    }
}

