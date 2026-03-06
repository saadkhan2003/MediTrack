package com.meditrack.app.domain.usecase

import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.data.repository.MedicineRepository
import javax.inject.Inject

class DeleteMedicineUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(medicineId: Int): Result<Unit> {
        return try {
            alarmScheduler.cancelAllForMedicine(medicineId)
            medicineRepository.softDeleteMedicine(medicineId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
