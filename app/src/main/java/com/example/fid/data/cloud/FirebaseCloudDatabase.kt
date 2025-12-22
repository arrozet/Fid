package com.example.fid.data.cloud

import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of CloudDatabaseInterface
 * Todas las operaciones usan Firestore como base de datos en la nube
 */
class FirebaseCloudDatabase : CloudDatabaseInterface {
    
    private val firestore = Firebase.firestore
    
    override suspend fun syncUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.id.toString())
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fetchUser(userId: Long): Result<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId.toString())
                .get()
                .await()
            val user = snapshot.toObject<User>()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncFoodEntries(entries: List<FoodEntry>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            entries.forEach { entry ->
                val docRef = firestore.collection("food_entries")
                    .document(entry.id.toString())
                batch.set(docRef, entry)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fetchFoodEntries(userId: Long, startDate: Long, endDate: Long): Result<List<FoodEntry>> {
        return try {
            val snapshot = firestore.collection("food_entries")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .get()
                .await()
            
            val entries = snapshot.documents.mapNotNull { it.toObject<FoodEntry>() }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncWellnessEntries(entries: List<WellnessEntry>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            entries.forEach { entry ->
                val docRef = firestore.collection("wellness_entries")
                    .document(entry.id.toString())
                batch.set(docRef, entry)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fetchWellnessEntries(userId: Long, startDate: Long, endDate: Long): Result<List<WellnessEntry>> {
        return try {
            val snapshot = firestore.collection("wellness_entries")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            val entries = snapshot.documents.mapNotNull { it.toObject<WellnessEntry>() }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun performFullSync(userId: Long): Result<Unit> {
        return try {
            // Por ahora simplemente verificamos la conexión con la BD
            firestore.collection("users")
                .document(userId.toString())
                .get()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

