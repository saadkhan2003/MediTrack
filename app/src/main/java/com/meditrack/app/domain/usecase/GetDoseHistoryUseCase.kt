package com.meditrack.app.domain.usecase

import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDoseHistoryUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository
) {
    operator fun invoke(
        startTime: Long,
        endTime: Long,
        statusFilter: DoseStatus? = null
    ): Flow<List<DoseLog>> {
        return if (statusFilter != null) {
            doseLogRepository.getDoseLogsByStatus(startTime, endTime, statusFilter)
        } else {
            doseLogRepository.getDoseLogsBetween(startTime, endTime)
        }
    }

    fun all(): Flow<List<DoseLog>> {
        return doseLogRepository.getAllDoseLogs()
    }
}
