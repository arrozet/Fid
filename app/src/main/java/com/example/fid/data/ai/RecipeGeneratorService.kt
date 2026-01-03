package com.example.fid.data.ai

import android.util.Log
import com.example.fid.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * Servicio para generar recetas usando la API de Grok (xAI) con streaming
 * Recibe una lista de ingredientes y genera una receta saludable paso a paso
 */
class RecipeGeneratorService {
    
    companion object {
        private const val TAG = "RecipeGeneratorService"
        private const val API_URL = "https://api.x.ai/v1/chat/completions"
        
        @Volatile
        private var INSTANCE: RecipeGeneratorService? = null
        
        fun getInstance(): RecipeGeneratorService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecipeGeneratorService().also { INSTANCE = it }
            }
        }
    }
    
    private val gson: Gson = GsonBuilder().create()
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()
    
    /**
     * Genera una receta basada en los ingredientes proporcionados usando streaming
     * @param ingredients Lista de ingredientes con sus cantidades
     * @return Flow que emite el texto de la receta progresivamente
     */
    fun generateRecipeStream(ingredients: List<RecipeIngredient>): Flow<RecipeStreamState> = flow {
        try {
            val apiKey = BuildConfig.GROK_API_KEY
            val model = BuildConfig.GROK_MODEL
            
            if (apiKey.isBlank() || apiKey == "tu_api_key_aqui") {
                Log.e(TAG, "API key not configured")
                emit(RecipeStreamState.Error("API key no configurada. Por favor, configura GROK_API_KEY"))
                return@flow
            }
            
            if (ingredients.isEmpty()) {
                emit(RecipeStreamState.Error("Debes añadir al menos un ingrediente"))
                return@flow
            }
            
            emit(RecipeStreamState.Loading)
            
            val requestBody = buildRecipeRequest(model, ingredients)
            
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            Log.d(TAG, "Sending streaming request to Grok API...")
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "API error: ${response.code} - $errorBody")
                emit(RecipeStreamState.Error("Error de API: ${response.code}"))
                return@flow
            }
            
            val responseBody = response.body ?: run {
                emit(RecipeStreamState.Error("Respuesta vacía del servidor"))
                return@flow
            }
            
            val reader = BufferedReader(responseBody.source().inputStream().reader())
            val fullContent = StringBuilder()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        
                        if (data == "[DONE]") {
                            return@forEach
                        }
                        
                        try {
                            val streamResponse = gson.fromJson(data, StreamResponse::class.java)
                            val delta = streamResponse.choices.firstOrNull()?.delta?.content
                            
                            if (!delta.isNullOrEmpty()) {
                                fullContent.append(delta)
                                emit(RecipeStreamState.Streaming(fullContent.toString()))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing stream chunk: $data", e)
                        }
                    }
                }
            }
            
            if (fullContent.isNotEmpty()) {
                emit(RecipeStreamState.Complete(fullContent.toString()))
            } else {
                emit(RecipeStreamState.Error("No se pudo generar la receta"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recipe", e)
            emit(RecipeStreamState.Error("Error al generar la receta: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    private fun buildRecipeRequest(model: String, ingredients: List<RecipeIngredient>): String {
        val ingredientsList = ingredients.joinToString("\n") { ingredient ->
            "- ${ingredient.name}: ${ingredient.quantity}"
        }
        
        val systemPrompt = """
Eres un chef profesional y nutricionista experto. Tu tarea es crear recetas saludables, rápidas y deliciosas usando los ingredientes que el usuario tiene disponibles.

INSTRUCCIONES:
1. Analiza los ingredientes disponibles
2. Crea UNA receta que sea:
   - Rápida de preparar (menos de 30 minutos)
   - Saludable y nutritiva
   - Sencilla de seguir
   - Que aproveche al máximo los ingredientes disponibles

FORMATO DE RESPUESTA:
Usa el siguiente formato con emojis para hacerlo más visual:

🍽️ **[NOMBRE DE LA RECETA]**

⏱️ **Tiempo de preparación:** X minutos
👥 **Porciones:** X personas
🔥 **Dificultad:** Fácil/Media

📝 **INGREDIENTES NECESARIOS:**
(Lista los ingredientes con las cantidades exactas)

👨‍🍳 **PASOS A SEGUIR:**
1. [Paso detallado]
2. [Paso detallado]
...

💡 **CONSEJOS:**
- [Consejo útil para mejorar el plato]

🥗 **INFORMACIÓN NUTRICIONAL APROXIMADA (por porción):**
- Calorías: X kcal
- Proteínas: X g
- Carbohidratos: X g
- Grasas: X g

Sé creativo pero práctico. El usuario quiere cocinar algo rico y saludable con lo que tiene.
""".trimIndent()
        
        val userMessage = """
Tengo estos ingredientes disponibles:

$ingredientsList

Por favor, sugiere una receta rápida, sencilla y saludable que pueda preparar con estos ingredientes. Si falta algún ingrediente básico común (sal, aceite, especias), puedes asumirlo.
""".trimIndent()
        
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to userMessage)
        )
        
        val requestMap = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to 0.7,
            "max_tokens" to 2048,
            "stream" to true
        )
        
        return gson.toJson(requestMap)
    }
    
    /**
     * Verifica si la API key está configurada correctamente
     */
    fun isConfigured(): Boolean {
        val apiKey = BuildConfig.GROK_API_KEY
        return apiKey.isNotBlank() && apiKey != "tu_api_key_aqui"
    }
}

/**
 * Estados del streaming de recetas
 */
sealed class RecipeStreamState {
    object Loading : RecipeStreamState()
    data class Streaming(val content: String) : RecipeStreamState()
    data class Complete(val content: String) : RecipeStreamState()
    data class Error(val message: String) : RecipeStreamState()
}

/**
 * Representa un ingrediente para la generación de recetas
 */
data class RecipeIngredient(
    val name: String,
    val quantity: String
)

/**
 * Modelos para parsear respuestas de streaming SSE
 */
data class StreamResponse(
    val id: String = "",
    val choices: List<StreamChoice> = emptyList()
)

data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta = StreamDelta(),
    val finish_reason: String? = null
)

data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)
