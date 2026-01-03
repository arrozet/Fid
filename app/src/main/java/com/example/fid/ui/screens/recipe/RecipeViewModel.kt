package com.example.fid.ui.screens.recipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fid.data.ai.RecipeGeneratorService
import com.example.fid.data.ai.RecipeIngredient
import com.example.fid.data.ai.RecipeStreamState
import com.example.fid.data.database.entities.FoodItem
import com.example.fid.data.repository.FirebaseRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de generación de recetas
 * Gestiona el estado de ingredientes y el streaming de la receta generada
 */
class RecipeViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "RecipeViewModel"
    }
    
    private val recipeService = RecipeGeneratorService.getInstance()
    private val repository = FirebaseRepository()
    
    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()
    
    private val _ingredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())
    val ingredients: StateFlow<List<RecipeIngredient>> = _ingredients.asStateFlow()
    
    private val _recipeContent = MutableStateFlow("")
    val recipeContent: StateFlow<String> = _recipeContent.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    // Información nutricional extraída de la receta
    private val _nutritionInfo = MutableStateFlow<RecipeNutritionInfo?>(null)
    val nutritionInfo: StateFlow<RecipeNutritionInfo?> = _nutritionInfo.asStateFlow()
    
    /**
     * Añade un nuevo ingrediente a la lista
     */
    fun addIngredient(name: String, quantity: String) {
        if (name.isBlank()) return
        
        val ingredient = RecipeIngredient(
            name = name.trim(),
            quantity = quantity.trim().ifBlank { "al gusto" }
        )
        
        _ingredients.value = _ingredients.value + ingredient
        _uiState.value = _uiState.value.copy(
            ingredientName = "",
            ingredientQuantity = ""
        )
    }
    
    /**
     * Elimina un ingrediente de la lista
     */
    fun removeIngredient(index: Int) {
        _ingredients.value = _ingredients.value.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
    }
    
    /**
     * Actualiza el campo de nombre del ingrediente actual
     */
    fun updateIngredientName(name: String) {
        _uiState.value = _uiState.value.copy(ingredientName = name)
    }
    
    /**
     * Actualiza el campo de cantidad del ingrediente actual
     */
    fun updateIngredientQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(ingredientQuantity = quantity)
    }
    
    /**
     * Genera una receta basada en los ingredientes actuales usando streaming
     */
    fun generateRecipe() {
        if (_ingredients.value.isEmpty()) {
            _errorMessage.value = "Añade al menos un ingrediente"
            return
        }
        
        if (_isGenerating.value) return
        
        viewModelScope.launch {
            _recipeContent.value = ""
            _errorMessage.value = null
            _nutritionInfo.value = null
            _saveSuccess.value = false
            
            recipeService.generateRecipeStream(_ingredients.value).collect { state ->
                when (state) {
                    is RecipeStreamState.Loading -> {
                        _isGenerating.value = true
                    }
                    is RecipeStreamState.Streaming -> {
                        _recipeContent.value = state.content
                    }
                    is RecipeStreamState.Complete -> {
                        _recipeContent.value = state.content
                        _isGenerating.value = false
                        // Extraer información nutricional
                        extractNutritionInfo(state.content)
                    }
                    is RecipeStreamState.Error -> {
                        _errorMessage.value = state.message
                        _isGenerating.value = false
                    }
                }
            }
        }
    }
    
    /**
     * Extrae la información nutricional del texto de la receta
     */
    private fun extractNutritionInfo(recipeText: String) {
        try {
            // Extraer nombre de la receta (primera línea con emoji de plato)
            val nameRegex = """🍽️\s*\**([^*\n]+)\**""".toRegex()
            val nameMatch = nameRegex.find(recipeText)
            val recipeName = nameMatch?.groupValues?.get(1)?.trim() ?: "Receta del Chef IA"
            
            // Extraer porciones
            val portionsRegex = """👥\s*\**Porciones:\**\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val portionsMatch = portionsRegex.find(recipeText)
            val portions = portionsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
            
            // Extraer calorías
            val caloriesRegex = """Calorías[:\s]*(\d+)\s*kcal""".toRegex(RegexOption.IGNORE_CASE)
            val caloriesMatch = caloriesRegex.find(recipeText)
            val totalCalories = caloriesMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            
            // Extraer proteínas
            val proteinRegex = """Proteínas[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val proteinMatch = proteinRegex.find(recipeText)
            val totalProtein = proteinMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            
            // Extraer carbohidratos
            val carbsRegex = """Carbohidratos[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val carbsMatch = carbsRegex.find(recipeText)
            val totalCarbs = carbsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            
            // Extraer grasas
            val fatRegex = """Grasas[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val fatMatch = fatRegex.find(recipeText)
            val totalFat = fatMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            
            _nutritionInfo.value = RecipeNutritionInfo(
                name = recipeName,
                portions = portions,
                caloriesPerPortion = totalCalories,
                proteinPerPortion = totalProtein,
                carbsPerPortion = totalCarbs,
                fatPerPortion = totalFat
            )
            
            Log.d(TAG, "Nutrition info extracted: ${_nutritionInfo.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting nutrition info", e)
        }
    }
    
    /**
     * Guarda la receta como comida personalizada
     */
    fun saveAsCustomFood() {
        val nutrition = _nutritionInfo.value
        if (nutrition == null) {
            _errorMessage.value = "No se pudo extraer la información nutricional"
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = "Debes iniciar sesión para guardar"
                    _isSaving.value = false
                    return@launch
                }
                
                val user = repository.getUserByEmail(currentUser.email ?: "")
                if (user == null) {
                    _errorMessage.value = "No se encontró el usuario"
                    _isSaving.value = false
                    return@launch
                }
                
                // Calcular valores por 100g (asumiendo porción de 300g aprox)
                val portionGrams = 300f
                val multiplier = 100f / portionGrams
                
                val foodItem = FoodItem(
                    id = System.currentTimeMillis(),
                    name = nutrition.name,
                    nameEs = nutrition.name,
                    nameEn = nutrition.name,
                    caloriesPer100g = nutrition.caloriesPerPortion * multiplier,
                    proteinPer100g = nutrition.proteinPerPortion * multiplier,
                    fatPer100g = nutrition.fatPerPortion * multiplier,
                    carbPer100g = nutrition.carbsPerPortion * multiplier,
                    verificationLevel = "ai_generated",
                    isFrequent = false,
                    createdByUserId = user.id
                )
                
                repository.insertCustomFoodItem(foodItem, user.id)
                _saveSuccess.value = true
                Log.d(TAG, "Recipe saved as custom food: ${foodItem.nameEs}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving recipe as custom food", e)
                _errorMessage.value = "Error al guardar: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Reinicia el estado para generar una nueva receta
     */
    fun resetRecipe() {
        _recipeContent.value = ""
        _errorMessage.value = null
        _nutritionInfo.value = null
        _saveSuccess.value = false
    }
    
    /**
     * Limpia todos los ingredientes
     */
    fun clearIngredients() {
        _ingredients.value = emptyList()
        _recipeContent.value = ""
        _nutritionInfo.value = null
        _saveSuccess.value = false
    }
    
    /**
     * Verifica si la API está configurada
     */
    fun isApiConfigured(): Boolean = recipeService.isConfigured()
}

/**
 * Estado de la UI de la pantalla de recetas
 */
data class RecipeUiState(
    val ingredientName: String = "",
    val ingredientQuantity: String = ""
)

/**
 * Información nutricional extraída de la receta
 */
data class RecipeNutritionInfo(
    val name: String,
    val portions: Float,
    val caloriesPerPortion: Float,
    val proteinPerPortion: Float,
    val carbsPerPortion: Float,
    val fatPerPortion: Float
)
