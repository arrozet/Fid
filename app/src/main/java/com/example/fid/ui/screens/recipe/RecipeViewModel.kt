package com.example.fid.ui.screens.recipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fid.data.ai.RecipeGeneratorService
import com.example.fid.data.ai.RecipeIngredient
import com.example.fid.data.ai.RecipeStreamState
import com.example.fid.data.database.entities.FoodEntry
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
    
    // Estado para añadir a registro de hoy
    private val _isAddingToLog = MutableStateFlow(false)
    val isAddingToLog: StateFlow<Boolean> = _isAddingToLog.asStateFlow()
    
    private val _addToLogSuccess = MutableStateFlow(false)
    val addToLogSuccess: StateFlow<Boolean> = _addToLogSuccess.asStateFlow()
    
    // Información nutricional extraída de la receta
    private val _nutritionInfo = MutableStateFlow<RecipeNutritionInfo?>(null)
    val nutritionInfo: StateFlow<RecipeNutritionInfo?> = _nutritionInfo.asStateFlow()
    
    /**
     * Añade un nuevo ingrediente a la lista
     * @param defaultQuantity El texto por defecto para cantidad (ej: "al gusto")
     */
    fun addIngredient(name: String, quantity: String, defaultQuantity: String = "al gusto") {
        if (name.isBlank()) return
        
        val ingredient = RecipeIngredient(
            name = name.trim(),
            quantity = quantity.trim().ifBlank { defaultQuantity }
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
     * @param errorNoIngredients Mensaje de error cuando no hay ingredientes
     * @param defaultRecipeName Nombre por defecto para la receta
     * @param systemPrompt Prompt del sistema para la IA
     * @param userMessageTemplate Template del mensaje del usuario
     */
    fun generateRecipe(
        errorNoIngredients: String, 
        defaultRecipeName: String,
        systemPrompt: String,
        userMessageTemplate: String
    ) {
        if (_ingredients.value.isEmpty()) {
            _errorMessage.value = errorNoIngredients
            return
        }
        
        if (_isGenerating.value) return
        
        viewModelScope.launch {
            _recipeContent.value = ""
            _errorMessage.value = null
            _nutritionInfo.value = null
            _saveSuccess.value = false
            
            recipeService.generateRecipeStream(
                _ingredients.value,
                systemPrompt,
                userMessageTemplate
            ).collect { state ->
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
                        extractNutritionInfo(state.content, defaultRecipeName)
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
     * @param defaultRecipeName Nombre por defecto si no se puede extraer
     */
    private fun extractNutritionInfo(recipeText: String, defaultRecipeName: String = "Recipe") {
        try {
            // Extraer nombre de la receta (primera línea con emoji de plato)
            val nameRegex = """🍽️\s*\**([^*\n]+)\**""".toRegex()
            val nameMatch = nameRegex.find(recipeText)
            val recipeName = nameMatch?.groupValues?.get(1)?.trim() ?: defaultRecipeName
            
            // Extraer porciones (español e inglés)
            val portionsRegex = """👥\s*\**(Porciones|Servings):\**\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val portionsMatch = portionsRegex.find(recipeText)
            val portions = portionsMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 1f
            
            // Extraer calorías (español e inglés)
            val caloriesRegex = """(Calorías|Calories)[:\s]*(\d+)\s*kcal""".toRegex(RegexOption.IGNORE_CASE)
            val caloriesMatch = caloriesRegex.find(recipeText)
            val totalCalories = caloriesMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
            
            // Extraer proteínas (español e inglés)
            val proteinRegex = """(Proteínas|Protein)[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val proteinMatch = proteinRegex.find(recipeText)
            val totalProtein = proteinMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
            
            // Extraer carbohidratos (español e inglés)
            val carbsRegex = """(Carbohidratos|Carbohydrates)[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val carbsMatch = carbsRegex.find(recipeText)
            val totalCarbs = carbsMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
            
            // Extraer grasas (español e inglés)
            val fatRegex = """(Grasas|Fat)[:\s]*(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
            val fatMatch = fatRegex.find(recipeText)
            val totalFat = fatMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
            
            Log.d(TAG, "Nutrition extraction - Calories: $totalCalories, Protein: $totalProtein, Carbs: $totalCarbs, Fat: $totalFat")
            
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
     * @param errorNoNutrition Error cuando no hay información nutricional
     * @param errorNotLoggedIn Error cuando no hay sesión
     * @param errorUserNotFound Error cuando no se encuentra el usuario
     * @param errorSaving Patrón de error al guardar (debe contener %s para el mensaje)
     */
    fun saveAsCustomFood(
        errorNoNutrition: String,
        errorNotLoggedIn: String,
        errorUserNotFound: String,
        errorSaving: String
    ) {
        val nutrition = _nutritionInfo.value
        if (nutrition == null) {
            _errorMessage.value = errorNoNutrition
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = errorNotLoggedIn
                    _isSaving.value = false
                    return@launch
                }
                
                val user = repository.getUserByEmail(currentUser.email ?: "")
                if (user == null) {
                    _errorMessage.value = errorUserNotFound
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
                _errorMessage.value = errorSaving.replace("%1\$s", e.message ?: "Unknown error")
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
     * Añade la receta al registro de comidas de hoy
     * @param errorNoNutrition Error cuando no hay información nutricional
     * @param errorNotLoggedIn Error cuando no hay sesión
     * @param errorUserNotFound Error cuando no se encuentra el usuario
     * @param errorSaving Patrón de error al guardar (debe contener %s para el mensaje)
     */
    fun addToTodayLog(
        errorNoNutrition: String,
        errorNotLoggedIn: String,
        errorUserNotFound: String,
        errorSaving: String
    ) {
        val nutrition = _nutritionInfo.value
        if (nutrition == null) {
            _errorMessage.value = errorNoNutrition
            return
        }
        
        viewModelScope.launch {
            _isAddingToLog.value = true
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = errorNotLoggedIn
                    _isAddingToLog.value = false
                    return@launch
                }
                
                val user = repository.getUserByEmail(currentUser.email ?: "")
                if (user == null) {
                    _errorMessage.value = errorUserNotFound
                    _isAddingToLog.value = false
                    return@launch
                }
                
                // Crear FoodEntry con los datos de la receta
                // Asumimos 1 porción = 300g aproximadamente
                val portionGrams = 300f
                
                val foodEntry = FoodEntry(
                    id = 0,
                    userId = user.id,
                    foodName = nutrition.name,
                    foodNameEs = nutrition.name,
                    foodNameEn = nutrition.name,
                    amountGrams = portionGrams,
                    calories = nutrition.caloriesPerPortion,
                    proteinG = nutrition.proteinPerPortion,
                    fatG = nutrition.fatPerPortion,
                    carbG = nutrition.carbsPerPortion,
                    mealType = getCurrentMealType(),
                    registrationMethod = "manual",
                    verificationLevel = "ai_generated",
                    timestamp = System.currentTimeMillis()
                )
                
                repository.insertFoodEntry(foodEntry)
                _addToLogSuccess.value = true
                Log.d(TAG, "Recipe added to today's log: ${nutrition.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding recipe to today's log", e)
                _errorMessage.value = errorSaving.replace("%1\$s", e.message ?: "Unknown error")
            } finally {
                _isAddingToLog.value = false
            }
        }
    }
    
    /**
     * Determina el tipo de comida según la hora del día
     */
    private fun getCurrentMealType(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "breakfast"
            in 11..15 -> "lunch"
            in 16..20 -> "dinner"
            else -> "snack"
        }
    }
    
    /**
     * Reinicia el estado para generar una nueva receta
     */
    fun resetRecipe() {
        _recipeContent.value = ""
        _errorMessage.value = null
        _nutritionInfo.value = null
        _saveSuccess.value = false
        _addToLogSuccess.value = false
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
