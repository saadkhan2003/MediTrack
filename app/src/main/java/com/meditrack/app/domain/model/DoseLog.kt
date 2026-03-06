package com.meditrack.app.domain.model

data class DoseLog(
    val id: Int = 0,
    val medicineId: Int,
    val medicineName: String,
    val scheduledTime: Long,
    val loggedTime: Long? = null,
    val status: DoseStatus = DoseStatus.PENDING,
    val notes: String? = null
)
