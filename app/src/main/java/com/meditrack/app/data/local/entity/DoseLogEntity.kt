package com.meditrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["scheduledTime"]),
        Index(value = ["status"]),
        Index(value = ["medicineId", "scheduledTime"], unique = true)
    ]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicineId: Int,
    val medicineName: String, // denormalized for report queries
    val scheduledTime: Long, // epoch millis — exact time the alarm was scheduled
    val loggedTime: Long? = null, // epoch millis — when user tapped Taken/Missed; null if not yet
    val status: String = "PENDING", // enum string: "TAKEN", "MISSED", "PENDING"
    val notes: String? = null
)
