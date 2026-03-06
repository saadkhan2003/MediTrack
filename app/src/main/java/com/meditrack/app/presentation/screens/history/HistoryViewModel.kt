package com.meditrack.app.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import com.meditrack.app.domain.usecase.GetDoseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class DateRange(val displayName: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time")
}

data class HistoryUiState(
    val doseLogs: List<DoseLog> = emptyList(),
    val dateRange: DateRange = DateRange.LAST_7_DAYS,
    val statusFilter: DoseStatus? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getDoseHistoryUseCase: GetDoseHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    init {
        loadHistory()
    }

    fun onDateRangeChanged(dateRange: DateRange) {
        _uiState.value = _uiState.value.copy(dateRange = dateRange, isLoading = true)
        loadHistory()
    }

    fun onStatusFilterChanged(status: DoseStatus?) {
        _uiState.value = _uiState.value.copy(statusFilter = status, isLoading = true)
        loadHistory()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadHistory()
    }

    private fun loadHistory() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val state = _uiState.value
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()

            val (startTime, endTime) = when (state.dateRange) {
                DateRange.LAST_7_DAYS -> {
                    val start = now.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = now.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                    start to end
                }
                DateRange.LAST_30_DAYS -> {
                    val start = now.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = now.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                    start to end
                }
                DateRange.ALL_TIME -> {
                    0L to now.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                }
            }

            getDoseHistoryUseCase(startTime, endTime, state.statusFilter).collect { logs ->
                _uiState.value = _uiState.value.copy(
                    doseLogs = logs,
                    isLoading = false
                )
            }
        }
    }
}
