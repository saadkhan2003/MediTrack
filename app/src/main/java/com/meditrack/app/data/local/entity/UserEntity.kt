package com.meditrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1, // always one row
    val displayName: String = "User",
    val email: String? = null,
    val dateOfBirth: Long? = null,
    val notificationsEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0, // alert at exact time
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val fontSize: String = "NORMAL" // NORMAL, LARGE, EXTRA_LARGE
)
