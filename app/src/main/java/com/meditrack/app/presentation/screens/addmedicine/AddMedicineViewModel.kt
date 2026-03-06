package com.meditrack.app.presentation.screens.addmedicine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.domain.model.Frequency
import com.meditrack.app.domain.model.Medicine
import com.meditrack.app.domain.usecase.AddMedicineUseCase
import com.meditrack.app.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

fun currentTimeString(): String {
    val now = LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}

data class AddMedicineFormState(
    val name: String = "",
    val dosage: String = "",
    val frequency: Frequency = Frequency.ONCE_DAILY,
    val scheduledTimes: List<String> = listOf(currentTimeString()),
    val startDate: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    val hasEndDate: Boolean = false,
    val endDate: Long? = null,
    val totalStock: Int = 10,
    val refillThreshold: Int = 0,
    val color: Int = 0xFF1954A3.toInt(),
    val notes: String = "",
    val isEditing: Boolean = false,
    val editingMedicineId: Int? = null,
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null
)

sealed class SaveResult {
    data object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

@HiltViewModel
class AddMedicineViewModel @Inject constructor(
    private val addMedicineUseCase: AddMedicineUseCase,
    private val medicineRepository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(AddMedicineFormState())
    val formState: StateFlow<AddMedicineFormState> = _formState.asStateFlow()

    private val _saveResult = MutableSharedFlow<SaveResult>()
    val saveResult: SharedFlow<SaveResult> = _saveResult.asSharedFlow()

    init {
        val medicineId = savedStateHandle.get<Int>("medicineId")
        if (medicineId != null && medicineId != -1) {
            loadMedicine(medicineId)
        }
    }

    private fun loadMedicine(medicineId: Int) {
        viewModelScope.launch {
            val medicine = medicineRepository.getMedicineById(medicineId)
            if (medicine != null) {
                _formState.value = AddMedicineFormState(
                    name = medicine.name,
                    dosage = medicine.dosage,
                    frequency = medicine.frequency,
                    scheduledTimes = medicine.scheduledTimes,
                    startDate = medicine.startDate,
                    hasEndDate = medicine.endDate != null,
                    endDate = medicine.endDate,
                    totalStock = medicine.totalStock,
                    refillThreshold = medicine.refillThreshold,
                    color = medicine.color,
                    notes = medicine.notes ?: "",
                    isEditing = true,
                    editingMedicineId = medicineId
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }

    fun onDosageChanged(value: String) {
        _formState.value = _formState.value.copy(dosage = value, dosageError = null)
    }

    fun onFrequencyChanged(frequency: Frequency) {
        val now = currentTimeString()
        val defaultTimes = when (frequency) {
            Frequency.ONCE_DAILY -> listOf(now)
            Frequency.TWICE_DAILY -> listOf(now, "20:00")
            Frequency.THREE_TIMES_DAILY -> listOf(now, "14:00", "20:00")
            Frequency.CUSTOM -> _formState.value.scheduledTimes
        }
        _formState.value = _formState.value.copy(
            frequency = frequency,
            scheduledTimes = defaultTimes,
            timesError = null
        )
    }

    fun onTimeAdded(timeMillis: Long) {
        val timeString = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }

        val currentTimes = _formState.value.scheduledTimes.toMutableList()
        if (currentTimes.size < 6) {
            currentTimes.add(timeString)
            _formState.value = _formState.value.copy(scheduledTimes = currentTimes, timesError = null)
        }
    }

    fun onTimeUpdated(index: Int, timeString: String) {
        val currentTimes = _formState.value.scheduledTimes.toMutableList()
        if (index in currentTimes.indices) {
            currentTimes[index] = timeString
            _formState.value = _formState.value.copy(scheduledTimes = currentTimes)
        }
    }

    fun onTimeRemoved(index: Int) {
        val currentTimes = _formState.value.scheduledTimes.toMutableList()
        if (index in currentTimes.indices && currentTimes.size > 1) {
            currentTimes.removeAt(index)
            _formState.value = _formState.value.copy(scheduledTimes = currentTimes)
        }
    }

    fun onStartDateChanged(millis: Long) {
        _formState.value = _formState.value.copy(startDate = millis)
    }

    fun onHasEndDateChanged(hasEndDate: Boolean) {
        _formState.value = _formState.value.copy(
            hasEndDate = hasEndDate,
            endDate = if (hasEndDate) _formState.value.startDate + 30L * 24 * 60 * 60 * 1000 else null
        )
    }

    fun onEndDateChanged(millis: Long) {
        _formState.value = _formState.value.copy(endDate = millis)
    }

    fun onStockChanged(value: Int) {
        _formState.value = _formState.value.copy(totalStock = value.coerceAtLeast(0))
    }

    fun onRefillThresholdChanged(value: Int) {
        _formState.value = _formState.value.copy(refillThreshold = value.coerceAtLeast(0))
    }

    fun onColorSelected(color: Int) {
        _formState.value = _formState.value.copy(color = color)
    }

    fun onNotesChanged(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun saveMedicine() {
        val state = _formState.value
        var hasError = false

        if (state.name.isBlank()) {
            _formState.value = state.copy(nameError = "Medicine name is required")
            hasError = true
        }
        if (state.dosage.isBlank()) {
            _formState.value = _formState.value.copy(dosageError = "Dosage is required")
            hasError = true
        }
        if (state.scheduledTimes.isEmpty()) {
            _formState.value = _formState.value.copy(timesError = "Add at least one scheduled time")
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            val medicine = Medicine(
                id = state.editingMedicineId ?: 0,
                name = state.name.trim(),
                dosage = state.dosage.trim(),
                frequency = state.frequency,
                scheduledTimes = state.scheduledTimes,
                startDate = state.startDate,
                endDate = if (state.hasEndDate) state.endDate else null,
                totalStock = state.totalStock,
                remainingStock = if (state.isEditing) state.totalStock else state.totalStock,
                refillThreshold = state.refillThreshold,
                notes = state.notes.ifBlank { null },
                color = state.color,
                isActive = true
            )

            try {
                if (state.isEditing && state.editingMedicineId != null) {
                    medicineRepository.updateMedicine(medicine)
                    alarmScheduler.cancelAllForMedicine(state.editingMedicineId)
                    if (medicine.remainingStock > 0 && medicine.isActive) {
                        alarmScheduler.scheduleMedicineAlarms(medicine)
                    }
                    _saveResult.emit(SaveResult.Success)
                } else {
                    val result = addMedicineUseCase(medicine)
                    result.fold(
                        onSuccess = { _saveResult.emit(SaveResult.Success) },
                        onFailure = { _saveResult.emit(SaveResult.Error(it.message ?: "Failed to save")) }
                    )
                }
            } catch (e: Exception) {
                _saveResult.emit(SaveResult.Error(e.message ?: "Failed to save"))
            }
        }
    }
}
