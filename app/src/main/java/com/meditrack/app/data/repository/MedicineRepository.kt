package com.meditrack.app.data.repository

import android.util.Log
import com.meditrack.app.data.local.dao.MedicineDao
import com.meditrack.app.data.local.entity.MedicineEntity
import com.meditrack.app.data.sync.FirestoreSyncService
import com.meditrack.app.domain.model.Frequency
import com.meditrack.app.domain.model.Medicine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepository @Inject constructor(
    private val medicineDao: MedicineDao,
    private val syncService: FirestoreSyncService
) {

    fun getAllActiveMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllActiveMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getMedicineById(id: Int): Medicine? {
        return withContext(Dispatchers.IO) {
            medicineDao.getMedicineById(id)?.toDomain()
        }
    }

    fun getMedicineByIdFlow(id: Int): Flow<Medicine?> {
        return medicineDao.getMedicineByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun getAllActiveMedicinesList(): List<Medicine> {
        return withContext(Dispatchers.IO) {
            medicineDao.getAllActiveMedicinesList().map { it.toDomain() }
        }
    }

    suspend fun insertMedicine(medicine: Medicine): Long {
        return withContext(Dispatchers.IO) {
            val entity = medicine.toEntity()
            val id = medicineDao.insertMedicine(entity)
            Log.d("MedicineRepository", "Inserted medicine locally: id=$id, name=${entity.name}")
            val entityWithId = entity.copy(id = id.toInt())
            Log.d("MedicineRepository", "Attempting cloud sync for medicine: id=$id")
            syncService.syncMedicineToCloud(entityWithId)
            Log.d("MedicineRepository", "Cloud sync completed for medicine: id=$id")
            id
        }
    }

    suspend fun updateMedicine(medicine: Medicine) {
        withContext(Dispatchers.IO) {
            val entity = medicine.toEntity()
            medicineDao.updateMedicine(entity)
            Log.d("MedicineRepository", "Updated medicine locally: id=${entity.id}, name=${entity.name}")
            syncService.syncMedicineToCloud(entity)
        }
    }

    suspend fun softDeleteMedicine(id: Int) {
        withContext(Dispatchers.IO) {
            medicineDao.softDeleteMedicine(id)
            syncService.deleteMedicineFromCloud(id)
        }
    }

    suspend fun updateRemainingStock(id: Int, stock: Int) {
        withContext(Dispatchers.IO) {
            medicineDao.updateRemainingStock(id, stock)
        }
    }

    suspend fun deleteAllMedicines() {
        withContext(Dispatchers.IO) {
            medicineDao.deleteAllMedicines()
        }
    }

    companion object {
        fun MedicineEntity.toDomain(): Medicine {
            val timesList = try {
                val jsonArray = JSONArray(scheduledTimes)
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }

            return Medicine(
                id = id,
                name = name,
                dosage = dosage,
                frequency = try { Frequency.valueOf(frequency) } catch (e: Exception) { Frequency.CUSTOM },
                scheduledTimes = timesList,
                startDate = startDate,
                endDate = endDate,
                totalStock = totalStock,
                remainingStock = remainingStock,
                refillThreshold = refillThreshold,
                notes = notes,
                color = color,
                isActive = isActive,
                createdAt = createdAt
            )
        }

        fun Medicine.toEntity(): MedicineEntity {
            val timesJson = JSONArray(scheduledTimes).toString()

            return MedicineEntity(
                id = id,
                name = name,
                dosage = dosage,
                frequency = frequency.name,
                scheduledTimes = timesJson,
                startDate = startDate,
                endDate = endDate,
                totalStock = totalStock,
                remainingStock = remainingStock,
                refillThreshold = refillThreshold,
                notes = notes,
                color = color,
                isActive = isActive,
                createdAt = createdAt
            )
        }
    }
}
