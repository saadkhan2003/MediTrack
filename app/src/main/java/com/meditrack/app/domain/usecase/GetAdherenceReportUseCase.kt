package com.meditrack.app.domain.usecase

import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class AdherenceReport(
    val weeklyAdherencePercent: Float,
    val dailyBreakdown: List<DailyAdherence>,
    val perMedicineBreakdown: List<MedicineAdherence>
)

data class DailyAdherence(
    val date: LocalDate,
    val taken: Int,
    val missed: Int
)

data class MedicineAdherence(
    val name: String,
    val taken: Int,
    val missed: Int,
    val total: Int
) {
    val adherencePercent: Float
        get() = if (total > 0) (taken.toFloat() / total) * 100f else 0f
}

class GetAdherenceReportUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<AdherenceReport> {
        return doseLogRepository.getDoseLogsBetween(startDate, endDate).map { logs ->
            val completedLogs = logs.filter { it.status != DoseStatus.PENDING }
            val takenCount = completedLogs.count { it.status == DoseStatus.TAKEN }
            val missedCount = completedLogs.count { it.status == DoseStatus.MISSED }
            val totalCompleted = takenCount + missedCount

            val weeklyAdherencePercent = if (totalCompleted > 0) {
                (takenCount.toFloat() / totalCompleted) * 100f
            } else {
                0f
            }

            val zone = ZoneId.systemDefault()
            val dailyBreakdown = completedLogs
                .groupBy { log ->
                    Instant.ofEpochMilli(log.scheduledTime)
                        .atZone(zone)
                        .toLocalDate()
                }
                .map { (date, dayLogs) ->
                    DailyAdherence(
                        date = date,
                        taken = dayLogs.count { it.status == DoseStatus.TAKEN },
                        missed = dayLogs.count { it.status == DoseStatus.MISSED }
                    )
                }
                .sortedBy { it.date }

            val perMedicineBreakdown = completedLogs
                .groupBy { it.medicineName }
                .map { (name, medLogs) ->
                    val medTaken = medLogs.count { it.status == DoseStatus.TAKEN }
                    val medMissed = medLogs.count { it.status == DoseStatus.MISSED }
                    MedicineAdherence(
                        name = name,
                        taken = medTaken,
                        missed = medMissed,
                        total = medTaken + medMissed
                    )
                }

            AdherenceReport(
                weeklyAdherencePercent = weeklyAdherencePercent,
                dailyBreakdown = dailyBreakdown,
                perMedicineBreakdown = perMedicineBreakdown
            )
        }
    }

    fun forMonth(yearMonth: YearMonth): Flow<AdherenceReport> {
        val zone = ZoneId.systemDefault()
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return invoke(startOfMonth, endOfMonth)
    }
}
