package com.meditrack.app.domain.usecase

import com.meditrack.app.data.repository.MedicineRepository
import com.meditrack.app.domain.model.Medicine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(includeInactive: Boolean = false): Flow<List<Medicine>> {
        return if (includeInactive) {
            medicineRepository.getAllMedicines()
        } else {
            medicineRepository.getAllActiveMedicines()
        }
    }
}
