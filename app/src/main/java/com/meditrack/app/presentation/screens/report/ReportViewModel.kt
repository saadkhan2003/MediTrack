package com.meditrack.app.presentation.screens.report

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.domain.usecase.AdherenceReport
import com.meditrack.app.domain.usecase.DailyAdherence
import com.meditrack.app.domain.usecase.GetAdherenceReportUseCase
import com.meditrack.app.domain.usecase.MedicineAdherence
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReportUiState(
    val weeklyAdherencePercent: Float = 0f,
    val dailyBarData: List<DailyAdherence> = emptyList(),
    val perMedicineBreakdown: List<MedicineAdherence> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getAdherenceReportUseCase: GetAdherenceReportUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    init {
        loadReport()
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(selectedMonth = yearMonth, isLoading = true)
        loadReport()
    }

    fun previousMonth() {
        onMonthChanged(_uiState.value.selectedMonth.minusMonths(1))
    }

    fun nextMonth() {
        onMonthChanged(_uiState.value.selectedMonth.plusMonths(1))
    }

    private fun loadReport() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            getAdherenceReportUseCase.forMonth(_uiState.value.selectedMonth).collect { report ->
                _uiState.value = _uiState.value.copy(
                    weeklyAdherencePercent = report.weeklyAdherencePercent,
                    dailyBarData = report.dailyBreakdown,
                    perMedicineBreakdown = report.perMedicineBreakdown,
                    isLoading = false
                )
            }
        }
    }

    fun exportReport() {
        val state = _uiState.value
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val monthStr = state.selectedMonth.format(formatter)

        val sb = StringBuilder()
        sb.appendLine("MediTrack Adherence Report — $monthStr")
        sb.appendLine("=" .repeat(40))
        sb.appendLine()
        sb.appendLine("Overall Adherence: ${"%.1f".format(state.weeklyAdherencePercent)}%")
        sb.appendLine()
        sb.appendLine("Per Medicine Breakdown:")
        sb.appendLine("-".repeat(40))

        state.perMedicineBreakdown.forEach { med ->
            sb.appendLine("${med.name}: ${med.taken}/${med.total} taken (${"%.1f".format(med.adherencePercent)}%)")
        }

        sb.appendLine()
        sb.appendLine("Daily Breakdown:")
        sb.appendLine("-".repeat(40))

        state.dailyBarData.forEach { day ->
            sb.appendLine("${day.date}: Taken=${day.taken}, Missed=${day.missed}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MediTrack Report — $monthStr")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Report").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
