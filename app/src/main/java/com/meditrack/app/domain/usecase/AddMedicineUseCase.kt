package com.meditrack.app.domain.usecase

import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.data.repository.MedicineRepository
import com.meditrack.app.domain.model.Medicine
import javax.inject.Inject

class AddMedicineUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(medicine: Medicine): Result<Long> {
        return try {
            val id = medicineRepository.insertMedicine(medicine)
            val savedMedicine = medicine.copy(id = id.toInt())
            if (savedMedicine.isActive && savedMedicine.remainingStock > 0) {
                alarmScheduler.scheduleMedicineAlarms(savedMedicine)
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
