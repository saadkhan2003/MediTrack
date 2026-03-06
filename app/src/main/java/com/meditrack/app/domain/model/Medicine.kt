package com.meditrack.app.domain.model

data class Medicine(
    val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: Frequency,
    val scheduledTimes: List<String>, // e.g., ["08:00", "20:00"]
    val startDate: Long,
    val endDate: Long? = null,
    val totalStock: Int,
    val remainingStock: Int,
    val refillThreshold: Int = 5,
    val notes: String? = null,
    val color: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Frequency(val displayName: String, val timesPerDay: Int) {
    ONCE_DAILY("Once Daily", 1),
    TWICE_DAILY("Twice Daily", 2),
    THREE_TIMES_DAILY("Three Times Daily", 3),
    CUSTOM("Custom", 0)
}
