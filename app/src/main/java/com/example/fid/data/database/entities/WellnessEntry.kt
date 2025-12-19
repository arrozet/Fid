package com.example.fid.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wellness_entries")
data class WellnessEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long,
    val waterIntakeMl: Float = 0f,
    val sleepHours: Float = 0f
)

