package com.meditrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meditrack.app.data.local.entity.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseLog(doseLog: DoseLogEntity): Long

    @Update
    suspend fun updateDoseLog(doseLog: DoseLogEntity)

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getDoseLogById(id: Int): DoseLogEntity?

    @Query("SELECT * FROM dose_logs ORDER BY scheduledTime DESC")
    fun getAllDoseLogs(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime DESC")
    fun getDoseLogsBetween(startTime: Long, endTime: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime DESC")
    suspend fun getDoseLogsBetweenList(startTime: Long, endTime: Long): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getDoseLogsForMedicine(medicineId: Int): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId AND scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime ASC")
    fun getDoseLogsForMedicineBetween(medicineId: Int, startTime: Long, endTime: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledTime BETWEEN :startTime AND :endTime AND status = :status ORDER BY scheduledTime DESC")
    fun getDoseLogsByStatus(startTime: Long, endTime: Long, status: String): Flow<List<DoseLogEntity>>

    @Query("UPDATE dose_logs SET status = :status, loggedTime = :loggedTime WHERE id = :id")
    suspend fun updateDoseStatus(id: Int, status: String, loggedTime: Long)

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId AND scheduledTime = :scheduledTime LIMIT 1")
    suspend fun getDoseLogByMedicineAndTime(medicineId: Int, scheduledTime: Long): DoseLogEntity?

    @Query("UPDATE dose_logs SET status = 'MISSED', loggedTime = :now WHERE status = 'PENDING' AND scheduledTime < :cutoffTime")
    suspend fun markOverdueDosesAsMissed(cutoffTime: Long, now: Long)

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAllDoseLogs()

    @Query("SELECT COUNT(*) FROM dose_logs WHERE scheduledTime BETWEEN :startTime AND :endTime AND status = :status")
    suspend fun countByStatus(startTime: Long, endTime: Long, status: String): Int

    @Query("SELECT * FROM dose_logs ORDER BY scheduledTime DESC")
    suspend fun getAllDoseLogsOnce(): List<DoseLogEntity>

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteDoseLogById(id: Int)
}
