package com.example.fid.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.fid.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Servicio para analizar comida usando la API de Grok (xAI)
 * Envía una imagen y recibe un análisis nutricional detallado
 */
class GrokAIService {
    
    companion object {
        private const val TAG = "GrokAIService"
        private const val API_URL = "https://api.x.ai/v1/chat/completions"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: GrokAIService? = null
        
        fun getInstance(): GrokAIService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GrokAIService().also { INSTANCE = it }
            }
        }
    }
    
    private val gson: Gson = GsonBuilder().create()
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()
    
    /**
     * Analiza una imagen de comida y devuelve información nutricional
     * @param bitmap La imagen de la comida a analizar
     * @return FoodAnalysisResult con la información nutricional
     */
    suspend fun analyzeFood(bitmap: Bitmap): FoodAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GROK_API_KEY
            val model = BuildConfig.GROK_MODEL
            
            if (apiKey.isBlank() || apiKey == "tu_api_key_aqui") {
                Log.e(TAG, "API key not configured")
                return@withContext FoodAnalysisResult(
                    success = false,
                    errorMessage = "API key no configurada. Por favor, configura GROK_API_KEY en el archivo .env"
                )
            }
            
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(bitmap)
            
            // Build the request
            val requestBody = buildAnalysisRequest(model, base64Image)
            
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            Log.d(TAG, "Sending request to Grok API...")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code} - $responseBody")
                return@withContext FoodAnalysisResult(
                    success = false,
                    errorMessage = "Error de API: ${response.code}"
                )
            }
            
            Log.d(TAG, "Response received successfully")
            
            // Parse the response
            val grokResponse = gson.fromJson(responseBody, GrokApiResponse::class.java)
            val content = grokResponse.choices.firstOrNull()?.message?.content ?: ""
            
            Log.d(TAG, "AI Response content: $content")
            
            // Extract JSON from the response
            parseAnalysisResponse(content)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food", e)
            FoodAnalysisResult(
                success = false,
                errorMessage = "Error al analizar la imagen: ${e.message}"
            )
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize if too large (max 1024px on longest side for efficiency)
        val resizedBitmap = resizeBitmap(bitmap, 1024)
        
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun buildAnalysisRequest(model: String, base64Image: String): String {
        val systemPrompt = """
You are a professional nutritionist AI assistant. Analyze the food in the image and provide detailed nutritional information.

IMPORTANT: You MUST respond ONLY with a valid JSON object, no additional text before or after.

The JSON must have this exact structure:
{
    "success": true,
    "food_name": "Name of the dish/food",
    "food_name_es": "Nombre del plato en español",
    "food_name_en": "Name of the dish in English",
    "total_estimated_grams": 350,
    "total_calories": 450,
    "total_protein_g": 25,
    "total_fat_g": 15,
    "total_carbs_g": 50,
    "ingredients": [
        {
            "name": "Ingredient name",
            "name_es": "Nombre en español",
            "name_en": "Name in English",
            "estimated_grams": 100,
            "calories": 150,
            "protein_g": 10,
            "fat_g": 5,
            "carbs_g": 15
        }
    ],
    "confidence": "high",
    "suggested_meal_type": "lunch"
}

Guidelines:
- Estimate portions based on visual cues (plate size, utensils, etc.)
- Be accurate with macronutrients based on standard nutritional databases
- confidence should be "high", "medium", or "low" based on image clarity and food identification certainty
- suggested_meal_type should be "breakfast", "lunch", "dinner", or "snack"
- If you cannot identify the food, set success to false and include an error_message field

Respond ONLY with the JSON, nothing else.
""".trimIndent()
        
        val userContent = listOf(
            mapOf("type" to "text", "text" to "Analyze this food image and provide nutritional information in JSON format:"),
            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image"))
        )
        
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to userContent)
        )
        
        val requestMap = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to 0.3,
            "max_tokens" to 2048
        )
        
        return gson.toJson(requestMap)
    }
    
    private fun parseAnalysisResponse(content: String): FoodAnalysisResult {
        return try {
            // Try to extract JSON from the response (in case there's surrounding text)
            val jsonPattern = """\{[\s\S]*\}""".toRegex()
            val jsonMatch = jsonPattern.find(content)
            val jsonString = jsonMatch?.value ?: content
            
            Log.d(TAG, "Parsing JSON: $jsonString")
            
            val result = gson.fromJson(jsonString, FoodAnalysisResult::class.java)
            
            // Ensure success is true if we got valid data
            if (result.foodName.isNotBlank() || result.foodNameEs.isNotBlank()) {
                result.copy(success = true)
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            FoodAnalysisResult(
                success = false,
                errorMessage = "Error al procesar la respuesta de IA: ${e.message}"
            )
        }
    }
    
    /**
     * Verifica si la API key está configurada correctamente
     */
    fun isConfigured(): Boolean {
        val apiKey = BuildConfig.GROK_API_KEY
        return apiKey.isNotBlank() && apiKey != "tu_api_key_aqui"
    }
}
