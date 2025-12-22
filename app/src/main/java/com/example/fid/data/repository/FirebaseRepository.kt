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
                android.util.Log.w("FirebaseRepository", "⚠️ No hay usuario autenticado en Firebase Auth")
                // Si no hay usuario autenticado, buscar el primer usuario (fallback)
                val snapshot = firestore.collection("users")
                    .limit(1)
                    .get()
                    .await()

                val user = snapshot.documents.firstOrNull()?.toObject<User>()
                android.util.Log.d("FirebaseRepository", "Fallback - primer usuario: ${user?.name} (age=${user?.age})")
                user
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
                val items = snapshot?.documents?.mapNotNull { it.toObject<FoodItem>() } ?: emptyList()
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getFrequentFoodItems(): Flow<List<FoodItem>> = callbackFlow {
        val listener = firestore.collection("food_items")
            .whereEqualTo("isFrequent", true)
            .orderBy("lastUsed", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toObject<FoodItem>() } ?: emptyList()
                trySend(items)
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
}
