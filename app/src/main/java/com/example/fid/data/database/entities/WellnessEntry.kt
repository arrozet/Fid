package com.example.fid.data.database.entities

/**
 * WellnessEntry entity - used for Firestore database
 */
data class WellnessEntry(
    val id: Long = 0,
    val userId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val waterIntakeMl: Float = 0f,
    val sleepHours: Float = 0f
)

