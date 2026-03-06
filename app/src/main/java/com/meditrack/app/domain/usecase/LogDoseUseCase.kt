package com.meditrack.app.domain.usecase

import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.data.repository.MedicineRepository
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import com.meditrack.app.notification.NotificationHelper
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LogDoseUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicineRepository: MedicineRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        logId: Int,
        status: DoseStatus,
        loggedTimeMillis: Long
    ): Result<Unit> {
        return try {
            doseLogRepository.updateDoseStatus(logId, status, loggedTimeMillis)

            if (status == DoseStatus.TAKEN) {
                val doseLog = doseLogRepository.getDoseLogById(logId)
                if (doseLog != null) {
                    val medicine = medicineRepository.getMedicineById(doseLog.medicineId)
                    if (medicine != null) {
                        val newStock = (medicine.remainingStock - 1).coerceAtLeast(0)
                        medicineRepository.updateRemainingStock(medicine.id, newStock)

                        if (newStock <= medicine.refillThreshold) {
                            NotificationHelper.showRefillAlert(
                                context,
                                medicine.name,
                                newStock
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
