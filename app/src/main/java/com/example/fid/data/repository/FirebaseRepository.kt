package com.example.fid.data.repository

import com.example.fid.data.database.entities.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que usa Firebase/Firestore como única fuente de datos
 * Reemplaza FidRepository que usaba Room
 */
class FirebaseRepository {
    
    private val firestore = Firebase.firestore
    
    // User operations
    fun getUserById(userId: Long): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<User>())
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getUserByEmail(email: String): User? {
        return try {
            android.util.Log.d("FirebaseRepository", "=== GET USER BY EMAIL ===")
            android.util.Log.d("FirebaseRepository", "Buscando email: '$email'")
            
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            android.util.Log.d("FirebaseRepository", "Documentos encontrados: ${snapshot.documents.size}")
            
            val user = snapshot.documents.firstOrNull()?.toObject<User>()
            
            if (user != null) {
                android.util.Log.d("FirebaseRepository", "✅ Usuario encontrado:")
                android.util.Log.d("FirebaseRepository", "  - ID: ${user.id}")
                android.util.Log.d("FirebaseRepository", "  - Email: ${user.email}")
                android.util.Log.d("FirebaseRepository", "  - Name: ${user.name}")
                android.util.Log.d("FirebaseRepository", "  - Age: ${user.age}")
                android.util.Log.d("FirebaseRepository", "  - Height: ${user.heightCm}, Weight: ${user.currentWeightKg}")
            } else {
                android.util.Log.d("FirebaseRepository", "❌ No se encontró usuario con ese email")
            }
            
            user
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Error buscando usuario: ${e.message}")
            null
        }
    }
    
    suspend fun insertUser(user: User): Long {
        return try {
            // Verificar si ya existe un usuario con ese email para evitar duplicados
            val existing = getUserByEmail(user.email)
            if (existing != null) {
                android.util.Log.d("FirebaseRepository", "Usuario ya existe con email ${user.email}, actualizando en lugar de insertar")
                updateUser(existing.copy(
                    name = user.name,
                    age = user.age,
                    gender = user.gender,
                    heightCm = user.heightCm,
                    currentWeightKg = user.currentWeightKg,
                    targetWeightKg = user.targetWeightKg,
                    activityLevel = user.activityLevel,
                    goal = user.goal,
                    tdee = user.tdee,
                    proteinGoalG = user.proteinGoalG,
                    fatGoalG = user.fatGoalG,
                    carbGoalG = user.carbGoalG,
                    numberlessMode = user.numberlessMode
                ))
                return existing.id
            }
            
            // Generamos un ID único basado en timestamp
            val newId = System.currentTimeMillis()
            val userWithId = user.copy(id = newId)
            
            android.util.Log.d("FirebaseRepository", "Insertando nuevo usuario: ${user.name} (${user.email})")
            
            firestore.collection("users")
                .document(newId.toString())
                .set(userWithId)
                .await()
            
            newId
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error insertando usuario: ${e.message}")
            throw e
        }
    }
    
    suspend fun updateUser(user: User) {
        try {
            android.util.Log.d("FirebaseRepository", "=== UPDATE USER ===")
            android.util.Log.d("FirebaseRepository", "ID: ${user.id}")
            android.util.Log.d("FirebaseRepository", "Email: ${user.email}")
            android.util.Log.d("FirebaseRepository", "Name: ${user.name}")
            android.util.Log.d("FirebaseRepository", "Age: ${user.age}")
            android.util.Log.d("FirebaseRepository", "Height: ${user.heightCm}, Weight: ${user.currentWeightKg}")
            android.util.Log.d("FirebaseRepository", "Goal: ${user.goal}, Activity: ${user.activityLevel}")
            
            firestore.collection("users")
                .document(user.id.toString())
                .set(user)
                .await()
            
            android.util.Log.d("FirebaseRepository", "✅ Usuario actualizado exitosamente en Firestore")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Error actualizando usuario: ${e.message}")
            throw e
        }
    }
    
    suspend fun getCurrentUser(): User? {
        return try {
            android.util.Log.d("FirebaseRepository", "=== GET CURRENT USER ===")
            // Obtener el email del usuario autenticado en Firebase Auth
            val currentUserEmail = Firebase.auth.currentUser?.email
            android.util.Log.d("FirebaseRepository", "Firebase Auth email: '$currentUserEmail'")

            if (currentUserEmail != null) {
                // Buscar el usuario por email en Firestore
                val user = getUserByEmail(currentUserEmail)
                android.util.Log.d("FirebaseRepository", "getCurrentUser devuelve: ${user?.name} (age=${user?.age})")
                user
            } else {
                // NO hay usuario autenticado - devolver null para forzar login
                android.util.Log.w("FirebaseRepository", "⚠️ No hay usuario autenticado en Firebase Auth - requiere login")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Error en getCurrentUser: ${e.message}")
            null
        }
    }
    
    // Food Entry operations
    fun getFoodEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<FoodEntry>> = callbackFlow {
        val listener = firestore.collection("food_entries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { it.toObject<FoodEntry>() } ?: emptyList()
                trySend(entries)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getRecentFoodEntries(userId: Long, limit: Int = 10): Flow<List<FoodEntry>> = callbackFlow {
        val listener = firestore.collection("food_entries")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { it.toObject<FoodEntry>() } ?: emptyList()
                trySend(entries)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun insertFoodEntry(foodEntry: FoodEntry): Long {
        return try {
            val newId = System.currentTimeMillis()
            val entryWithId = foodEntry.copy(id = newId)
            
            firestore.collection("food_entries")
                .document(newId.toString())
                .set(entryWithId)
                .await()
            
            // Recalcular y actualizar el resumen diario automáticamente
            try {
                calculateAndSaveDailySummary(entryWithId.userId, entryWithId.timestamp)
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRepository", "Error actualizando resumen diario: ${e.message}")
                // No lanzamos el error para no interrumpir el guardado de la comida
            }
            
            newId
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun deleteFoodEntry(foodEntry: FoodEntry) {
        try {
            firestore.collection("food_entries")
                .document(foodEntry.id.toString())
                .delete()
                .await()
            
            // Recalcular y actualizar el resumen diario automáticamente
            try {
                calculateAndSaveDailySummary(foodEntry.userId, foodEntry.timestamp)
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRepository", "Error actualizando resumen diario: ${e.message}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    // Food Item operations
    fun searchFoodItems(query: String): Flow<List<FoodItem>> = callbackFlow {
        val listener = firestore.collection("food_items")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val foodItem = doc.toObject<FoodItem>()
                    val id = doc.id.toLongOrNull() ?: 0L
                    android.util.Log.d("FirebaseRepository", "Documento ID: ${doc.id}, FoodItem: ${foodItem?.name}, ID asignado: $id")
                    foodItem?.copy(id = id)
                } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Total items encontrados: ${items.size}")
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getFrequentFoodItems(): Flow<List<FoodItem>> = callbackFlow {
        val listener = firestore.collection("food_items")
            .whereEqualTo("isFrequent", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val foodItem = doc.toObject<FoodItem>()
                    foodItem?.copy(id = doc.id.toLongOrNull() ?: 0L)
                } ?: emptyList()
                // Ordenar en memoria por lastUsed descendente y limitar a 20
                val sortedItems = items
                    .sortedByDescending { it.lastUsed ?: 0L }
                    .take(20)
                trySend(sortedItems)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun insertFoodItem(foodItem: FoodItem): Long {
        return try {
            val newId = System.currentTimeMillis()
            val itemWithId = foodItem.copy(id = newId)
            
            firestore.collection("food_items")
                .document(newId.toString())
                .set(itemWithId)
                .await()
            
            newId
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun getFoodItemById(foodId: Long): FoodItem? {
        return try {
            val snapshot = firestore.collection("food_items")
                .document(foodId.toString())
                .get()
                .await()
            
            snapshot.toObject<FoodItem>()?.copy(id = foodId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo alimento: ${e.message}")
            null
        }
    }
    
    suspend fun getFoodItemByName(name: String): FoodItem? {
        return try {
            val snapshot = firestore.collection("food_items")
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .await()
            
            val doc = snapshot.documents.firstOrNull()
            if (doc != null) {
                val foodItem = doc.toObject<FoodItem>()
                foodItem?.copy(id = doc.id.toLongOrNull() ?: 0L)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo alimento por nombre '$name': ${e.message}")
            null
        }
    }
    
    // Obtener sugerencias de alimentos (alimentos aleatorios de la base de datos)
    suspend fun getSuggestedFoods(limit: Int = 3): List<FoodItem> {
        return try {
            android.util.Log.d("FirebaseRepository", "Obteniendo sugerencias de alimentos...")
            
            // Obtener todos los alimentos
            val snapshot = firestore.collection("food_items")
                .limit(20) // Limitar para no traer demasiados
                .get()
                .await()
            
            val allFoods = snapshot.documents.mapNotNull { doc ->
                val foodItem = doc.toObject<FoodItem>()
                foodItem?.copy(id = doc.id.toLongOrNull() ?: 0L)
            }
            
            // Seleccionar alimentos al azar
            val suggestions = allFoods.shuffled().take(limit)
            
            android.util.Log.d("FirebaseRepository", "Sugerencias obtenidas: ${suggestions.size}")
            suggestions.forEach { food ->
                android.util.Log.d("FirebaseRepository", "  - ${food.name} (${food.caloriesPer100g.toInt()} kcal)")
            }
            
            suggestions
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo sugerencias: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun markFoodAsUsed(foodId: Long) {
        try {
            firestore.collection("food_items")
                .document(foodId.toString())
                .update(
                    mapOf(
                        "isFrequent" to true,
                        "lastUsed" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    // Función para limpiar alimentos duplicados
    suspend fun cleanDuplicateFoodItems() {
        try {
            android.util.Log.d("FirebaseRepository", "Iniciando limpieza de alimentos duplicados...")
            
            val snapshot = firestore.collection("food_items")
                .get()
                .await()
            
            val foodsByName = mutableMapOf<String, MutableList<Pair<String, FoodItem>>>()
            
            // Agrupar por nombre
            snapshot.documents.forEach { doc ->
                val foodItem = doc.toObject<FoodItem>()
                if (foodItem != null) {
                    val name = foodItem.name
                    if (!foodsByName.containsKey(name)) {
                        foodsByName[name] = mutableListOf()
                    }
                    foodsByName[name]!!.add(Pair(doc.id, foodItem))
                }
            }
            
            // Eliminar duplicados (mantener solo el primero)
            var deletedCount = 0
            foodsByName.forEach { (name, items) ->
                if (items.size > 1) {
                    android.util.Log.d("FirebaseRepository", "Encontrados ${items.size} duplicados de '$name'")
                    // Mantener solo el primero, eliminar el resto
                    items.drop(1).forEach { (docId, _) ->
                        firestore.collection("food_items")
                            .document(docId)
                            .delete()
                            .await()
                        deletedCount++
                        android.util.Log.d("FirebaseRepository", "Eliminado duplicado: $name (ID: $docId)")
                    }
                }
            }
            
            android.util.Log.d("FirebaseRepository", "Limpieza completada: $deletedCount duplicados eliminados")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error limpiando duplicados: ${e.message}")
            throw e
        }
    }
    
    // Wellness operations
    fun getWellnessEntryByDate(userId: Long, date: Long): Flow<WellnessEntry?> = callbackFlow {
        val listener = firestore.collection("wellness_entries")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entry = snapshot?.documents?.firstOrNull()?.toObject<WellnessEntry>()
                trySend(entry)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getWellnessEntriesByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WellnessEntry>> = callbackFlow {
        val listener = firestore.collection("wellness_entries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { it.toObject<WellnessEntry>() } ?: emptyList()
                trySend(entries)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun insertWellnessEntry(wellnessEntry: WellnessEntry): Long {
        return try {
            val newId = System.currentTimeMillis()
            val entryWithId = wellnessEntry.copy(id = newId)
            
            firestore.collection("wellness_entries")
                .document(newId.toString())
                .set(entryWithId)
                .await()
            
            newId
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun updateWellnessEntry(wellnessEntry: WellnessEntry) {
        try {
            firestore.collection("wellness_entries")
                .document(wellnessEntry.id.toString())
                .set(wellnessEntry)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    // Helper function to calculate TDEE using Mifflin-St Jeor equation
    fun calculateTDEE(
        gender: String,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activityLevel: String
    ): Float {
        // Calculate BMR (Basal Metabolic Rate)
        val bmr = if (gender == "male") {
            10 * weightKg + 6.25f * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25f * heightCm - 5 * age - 161
        }
        
        // Apply activity multiplier
        val activityMultiplier = when (activityLevel) {
            "sedentary" -> 1.2f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "very_active" -> 1.725f
            "athlete" -> 1.9f
            else -> 1.2f
        }
        
        return bmr * activityMultiplier
    }
    
    // Calculate macros based on goal
    fun calculateMacros(tdee: Float, goal: String): Triple<Float, Float, Float> {
        val adjustedCalories = when (goal) {
            "lose_weight" -> tdee * 0.85f // 15% deficit
            "gain_muscle" -> tdee * 1.1f // 10% surplus
            else -> tdee // maintain
        }
        
        // Standard macro split: 30% protein, 30% fat, 40% carbs
        val proteinG = (adjustedCalories * 0.30f) / 4f // 4 cal per gram
        val fatG = (adjustedCalories * 0.30f) / 9f // 9 cal per gram
        val carbG = (adjustedCalories * 0.40f) / 4f // 4 cal per gram
        
        return Triple(proteinG, fatG, carbG)
    }
    
    // ============== DAILY SUMMARY OPERATIONS ==============
    
    /**
     * Obtiene el resumen de un día específico
     */
    suspend fun getDailySummary(userId: Long, date: Long): DailySummary? {
        return try {
            val snapshot = firestore.collection("daily_summaries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject<DailySummary>()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo resumen diario: ${e.message}")
            null
        }
    }
    
    /**
     * Obtiene resúmenes de un rango de fechas
     */
    suspend fun getDailySummariesByDateRange(userId: Long, startDate: Long, endDate: Long): List<DailySummary> {
        return try {
            val snapshot = firestore.collection("daily_summaries")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject<DailySummary>() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo resúmenes por rango: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Crea o actualiza el resumen diario
     */
    suspend fun saveDailySummary(summary: DailySummary) {
        try {
            // Buscar si ya existe un resumen para este día
            val existing = getDailySummary(summary.userId, summary.date)
            
            val summaryToSave = if (existing != null) {
                summary.copy(id = existing.id)
            } else {
                summary.copy(id = System.currentTimeMillis())
            }
            
            firestore.collection("daily_summaries")
                .document(summaryToSave.id.toString())
                .set(summaryToSave)
                .await()
            
            android.util.Log.d("FirebaseRepository", "Resumen diario guardado: ${summaryToSave.date}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error guardando resumen diario: ${e.message}")
            throw e
        }
    }
    
    /**
     * Calcula y guarda el resumen diario basado en las entradas de comida
     */
    suspend fun calculateAndSaveDailySummary(userId: Long, date: Long) {
        try {
            val user = getCurrentUser() ?: return
            
            // Calcular inicio y fin del día
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000
            
            // Obtener todas las entradas de comida del día
            val snapshot = firestore.collection("food_entries")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .get()
                .await()
            
            val foodEntries = snapshot.documents.mapNotNull { it.toObject<FoodEntry>() }
            
            // Calcular totales
            val totalCalories = foodEntries.sumOf { it.calories.toDouble() }.toFloat()
            val totalProtein = foodEntries.sumOf { it.proteinG.toDouble() }.toFloat()
            val totalFat = foodEntries.sumOf { it.fatG.toDouble() }.toFloat()
            val totalCarbs = foodEntries.sumOf { it.carbG.toDouble() }.toFloat()
            
            // Obtener datos de wellness si existen
            val wellnessSnapshot = firestore.collection("wellness_entries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", startOfDay)
                .limit(1)
                .get()
                .await()
            
            val wellness = wellnessSnapshot.documents.firstOrNull()?.toObject<WellnessEntry>()
            
            // Crear resumen
            val summary = DailySummary(
                userId = userId,
                date = startOfDay,
                totalCalories = totalCalories,
                totalProteinG = totalProtein,
                totalFatG = totalFat,
                totalCarbG = totalCarbs,
                calorieGoal = user.tdee,
                proteinGoal = user.proteinGoalG,
                fatGoal = user.fatGoalG,
                carbGoal = user.carbGoalG,
                mealsCount = foodEntries.size,
                waterIntakeMl = wellness?.waterIntakeMl ?: 0f,
                sleepHours = wellness?.sleepHours ?: 0f
            )
            
            saveDailySummary(summary)
            
            android.util.Log.d("FirebaseRepository", "Resumen calculado y guardado para: $startOfDay")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error calculando resumen diario: ${e.message}")
        }
    }
    
    /**
     * Obtiene estadísticas para un período
     */
    suspend fun getPeriodStats(userId: Long, startDate: Long, endDate: Long): PeriodStats {
        return try {
            val summaries = getDailySummariesByDateRange(userId, startDate, endDate)
            
            if (summaries.isEmpty()) {
                return PeriodStats()
            }
            
            val avgCalories = summaries.map { it.totalCalories }.average().toFloat()
            val avgProtein = summaries.map { it.totalProteinG }.average().toFloat()
            val avgFat = summaries.map { it.totalFatG }.average().toFloat()
            val avgCarbs = summaries.map { it.totalCarbG }.average().toFloat()
            
            // Contar días que alcanzaron el objetivo (dentro del 10% del objetivo)
            val daysOnTarget = summaries.count { summary ->
                val targetCalories = summary.calorieGoal
                val actual = summary.totalCalories
                actual >= targetCalories * 0.9f && actual <= targetCalories * 1.1f
            }
            
            val totalDays = summaries.size
            
            // Calcular distribución de macros promedio
            val totalMacroCalories = avgProtein * 4 + avgFat * 9 + avgCarbs * 4
            val proteinPercentage = if (totalMacroCalories > 0) (avgProtein * 4 / totalMacroCalories * 100).toInt() else 0
            val fatPercentage = if (totalMacroCalories > 0) (avgFat * 9 / totalMacroCalories * 100).toInt() else 0
            val carbPercentage = if (totalMacroCalories > 0) (avgCarbs * 4 / totalMacroCalories * 100).toInt() else 0
            
            PeriodStats(
                avgCalories = avgCalories,
                avgProteinG = avgProtein,
                avgFatG = avgFat,
                avgCarbG = avgCarbs,
                daysOnTarget = daysOnTarget,
                totalDays = totalDays,
                proteinPercentage = proteinPercentage,
                fatPercentage = fatPercentage,
                carbPercentage = carbPercentage
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error obteniendo estadísticas del período: ${e.message}")
            PeriodStats()
        }
    }
}

/**
 * Clase de datos para estadísticas de período
 */
data class PeriodStats(
    val avgCalories: Float = 0f,
    val avgProteinG: Float = 0f,
    val avgFatG: Float = 0f,
    val avgCarbG: Float = 0f,
    val daysOnTarget: Int = 0,
    val totalDays: Int = 0,
    val proteinPercentage: Int = 0,
    val fatPercentage: Int = 0,
    val carbPercentage: Int = 0
)
