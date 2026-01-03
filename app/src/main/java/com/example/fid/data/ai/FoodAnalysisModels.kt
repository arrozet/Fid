package com.example.fid.data.ai

import com.google.gson.annotations.SerializedName

/**
 * Representa un ingrediente individual analizado por la IA
 */
data class IngredientAnalysis(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("name_es")
    val nameEs: String = "",
    
    @SerializedName("name_en")
    val nameEn: String = "",
    
    @SerializedName("estimated_grams")
    val estimatedGrams: Float = 0f,
    
    @SerializedName("calories")
    val calories: Float = 0f,
    
    @SerializedName("protein_g")
    val proteinG: Float = 0f,
    
    @SerializedName("fat_g")
    val fatG: Float = 0f,
    
    @SerializedName("carbs_g")
    val carbsG: Float = 0f
)

/**
 * Resultado del análisis de comida por IA
 * Contiene la información nutricional total y el desglose por ingredientes
 */
data class FoodAnalysisResult(
    @SerializedName("success")
    val success: Boolean = false,
    
    @SerializedName("food_name")
    val foodName: String = "",
    
    @SerializedName("food_name_es")
    val foodNameEs: String = "",
    
    @SerializedName("food_name_en")
    val foodNameEn: String = "",
    
    @SerializedName("total_estimated_grams")
    val totalEstimatedGrams: Float = 0f,
    
    @SerializedName("total_calories")
    val totalCalories: Float = 0f,
    
    @SerializedName("total_protein_g")
    val totalProteinG: Float = 0f,
    
    @SerializedName("total_fat_g")
    val totalFatG: Float = 0f,
    
    @SerializedName("total_carbs_g")
    val totalCarbsG: Float = 0f,
    
    @SerializedName("ingredients")
    val ingredients: List<IngredientAnalysis> = emptyList(),
    
    @SerializedName("confidence")
    val confidence: String = "low", // "high", "medium", "low"
    
    @SerializedName("suggested_meal_type")
    val suggestedMealType: String = "snack", // "breakfast", "lunch", "dinner", "snack"
    
    @SerializedName("error_message")
    val errorMessage: String? = null
)

/**
 * Wrapper para la respuesta de la API de Grok
 */
data class GrokApiResponse(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("object")
    val objectType: String = "",
    
    @SerializedName("created")
    val created: Long = 0,
    
    @SerializedName("model")
    val model: String = "",
    
    @SerializedName("choices")
    val choices: List<GrokChoice> = emptyList()
)

data class GrokChoice(
    @SerializedName("index")
    val index: Int = 0,
    
    @SerializedName("message")
    val message: GrokMessage = GrokMessage()
)

data class GrokMessage(
    @SerializedName("role")
    val role: String = "",
    
    @SerializedName("content")
    val content: String = ""
)

/**
 * Request body para la API de Grok
 */
data class GrokApiRequest(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("messages")
    val messages: List<GrokRequestMessage>,
    
    @SerializedName("temperature")
    val temperature: Float = 0.3f,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 2048
)

data class GrokRequestMessage(
    @SerializedName("role")
    val role: String,
    
    @SerializedName("content")
    val content: Any // Can be String or List<ContentPart>
)

data class ContentPart(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url")
    val url: String // Can be base64 data URL
)
