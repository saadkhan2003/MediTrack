package com.meditrack.app.data.repository

import android.util.Log
import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.entity.DoseLogEntity
import com.meditrack.app.data.sync.FirestoreSyncService
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoseLogRepository @Inject constructor(
    private val doseLogDao: DoseLogDao,
    private val syncService: FirestoreSyncService
) {

    fun getAllDoseLogs(): Flow<List<DoseLog>> {
        return doseLogDao.getAllDoseLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getDoseLogsBetween(startTime: Long, endTime: Long): Flow<List<DoseLog>> {
        return doseLogDao.getDoseLogsBetween(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDoseLogsBetweenList(startTime: Long, endTime: Long): List<DoseLog> {
        return withContext(Dispatchers.IO) {
            doseLogDao.getDoseLogsBetweenList(startTime, endTime).map { it.toDomain() }
        }
    }

    fun getDoseLogsForMedicine(medicineId: Int): Flow<List<DoseLog>> {
        return doseLogDao.getDoseLogsForMedicine(medicineId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getDoseLogsForMedicineBetween(
        medicineId: Int,
        startTime: Long,
        endTime: Long
    ): Flow<List<DoseLog>> {
        return doseLogDao.getDoseLogsForMedicineBetween(medicineId, startTime, endTime)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun getDoseLogsByStatus(
        startTime: Long,
        endTime: Long,
        status: DoseStatus
    ): Flow<List<DoseLog>> {
        return doseLogDao.getDoseLogsByStatus(startTime, endTime, status.name)
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun insertDoseLog(doseLog: DoseLog): Long {
        return withContext(Dispatchers.IO) {
            val entity = doseLog.toEntity()
            val id = doseLogDao.insertDoseLog(entity)
            Log.d("DoseLogRepository", "Inserted dose log locally: id=$id, medicineId=${entity.medicineId}")
            syncService.syncDoseLogToCloud(entity.copy(id = id.toInt()))
            id
        }
    }

    suspend fun updateDoseStatus(id: Int, status: DoseStatus, loggedTimeMillis: Long) {
        withContext(Dispatchers.IO) {
            doseLogDao.updateDoseStatus(id, status.name, loggedTimeMillis)
            // Sync updated log to cloud
            doseLogDao.getDoseLogById(id)?.let { syncService.syncDoseLogToCloud(it) }
        }
    }

    suspend fun getDoseLogById(id: Int): DoseLog? {
        return withContext(Dispatchers.IO) {
            doseLogDao.getDoseLogById(id)?.toDomain()
        }
    }

    suspend fun getDoseLogByMedicineAndTime(medicineId: Int, scheduledTime: Long): DoseLog? {
        return withContext(Dispatchers.IO) {
            doseLogDao.getDoseLogByMedicineAndTime(medicineId, scheduledTime)?.toDomain()
        }
    }

    suspend fun markOverdueDosesAsMissed(cutoffTime: Long, now: Long) {
        withContext(Dispatchers.IO) {
            doseLogDao.markOverdueDosesAsMissed(cutoffTime, now)
        }
    }

    suspend fun deleteAllDoseLogs() {
        withContext(Dispatchers.IO) {
            doseLogDao.deleteAllDoseLogs()
        }
    }

    suspend fun countByStatus(startTime: Long, endTime: Long, status: DoseStatus): Int {
        return withContext(Dispatchers.IO) {
            doseLogDao.countByStatus(startTime, endTime, status.name)
        }
    }

    companion object {
        fun DoseLogEntity.toDomain(): DoseLog {
            return DoseLog(
                id = id,
                medicineId = medicineId,
                medicineName = medicineName,
                scheduledTime = scheduledTime,
                loggedTime = loggedTime,
                status = DoseStatus.fromString(status),
                notes = notes
            )
        }

        fun DoseLog.toEntity(): DoseLogEntity {
            return DoseLogEntity(
                id = id,
                medicineId = medicineId,
                medicineName = medicineName,
                scheduledTime = scheduledTime,
                loggedTime = loggedTime,
                status = status.name,
                notes = notes
            )
        }
    }
}
