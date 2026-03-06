package com.meditrack.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import com.meditrack.app.domain.model.Medicine
import com.meditrack.app.domain.usecase.GetAllMedicinesUseCase
import com.meditrack.app.domain.usecase.LogDoseUseCase
import com.meditrack.app.domain.usecase.DeleteMedicineUseCase
import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val todaySchedule: List<TodayDoseItem> = emptyList(),
    val takenCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

data class TodayDoseItem(
    val medicine: Medicine,
    val slots: List<DoseSlot>
)

data class DoseSlot(
    val scheduledTimeMillis: Long,
    val logId: Int? = null,
    val status: DoseStatus = DoseStatus.PENDING,
    val timeString: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllMedicinesUseCase: GetAllMedicinesUseCase,
    private val logDoseUseCase: LogDoseUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
    private val doseLogRepository: DoseLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodaySchedule()
    }

    private fun loadTodaySchedule() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            combine(
                getAllMedicinesUseCase(),
                doseLogRepository.getDoseLogsBetween(startOfDay, endOfDay)
            ) { medicines, todayLogs ->
                buildTodaySchedule(medicines, todayLogs)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildTodaySchedule(
        medicines: List<Medicine>,
        todayLogs: List<DoseLog>
    ): HomeUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        val todayItems = medicines.map { medicine ->
            val slots = medicine.scheduledTimes.map { timeStr ->
                try {
                    val parts = timeStr.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    val scheduledMillis = today.atTime(hour, minute)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()

                    val matchingLog = todayLogs.find {
                        it.medicineId == medicine.id && it.scheduledTime == scheduledMillis
                    }

                    DoseSlot(
                        scheduledTimeMillis = scheduledMillis,
                        logId = matchingLog?.id,
                        status = matchingLog?.status ?: DoseStatus.PENDING,
                        timeString = timeStr
                    )
                } catch (e: Exception) {
                    DoseSlot(
                        scheduledTimeMillis = 0L,
                        timeString = timeStr
                    )
                }
            }

            TodayDoseItem(medicine = medicine, slots = slots)
        }

        val allSlots = todayItems.flatMap { it.slots }
        val takenCount = allSlots.count { it.status == DoseStatus.TAKEN }
        val totalCount = allSlots.size

        return HomeUiState(
            todaySchedule = todayItems,
            takenCount = takenCount,
            totalCount = totalCount,
            isLoading = false
        )
    }

    fun markDose(logId: Int?, medicineId: Int, scheduledTimeMillis: Long, status: DoseStatus) {
        viewModelScope.launch {
            if (logId != null) {
                logDoseUseCase(logId, status, System.currentTimeMillis())
            } else {
                // Create a new dose log first
                val medicine = _uiState.value.todaySchedule.find { it.medicine.id == medicineId }?.medicine
                if (medicine != null) {
                    val doseLog = DoseLog(
                        medicineId = medicineId,
                        medicineName = medicine.name,
                        scheduledTime = scheduledTimeMillis,
                        loggedTime = System.currentTimeMillis(),
                        status = status
                    )
                    val newId = doseLogRepository.insertDoseLog(doseLog)
                    if (status == DoseStatus.TAKEN) {
                        logDoseUseCase(newId.toInt(), status, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            deleteMedicineUseCase(medicine.id)
        }
    }
}
