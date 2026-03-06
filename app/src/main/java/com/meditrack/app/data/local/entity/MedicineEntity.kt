package com.meditrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicines",
    indices = [
        Index(value = ["isActive"])
    ]
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: String, // ONCE_DAILY, TWICE_DAILY, THREE_TIMES_DAILY, CUSTOM
    val scheduledTimes: String, // JSON array of time strings e.g., ["08:00","20:00"]
    val startDate: Long, // epoch millis
    val endDate: Long? = null, // nullable — null means ongoing
    val totalStock: Int,
    val remainingStock: Int,
    val refillThreshold: Int = 5,
    val notes: String? = null,
    val color: Int, // ARGB color for UI card
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
