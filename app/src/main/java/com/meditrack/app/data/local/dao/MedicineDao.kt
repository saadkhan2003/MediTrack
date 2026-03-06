package com.meditrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meditrack.app.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Int): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun getMedicineByIdFlow(id: Int): Flow<MedicineEntity?>

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getAllActiveMedicinesList(): List<MedicineEntity>

    @Query("UPDATE medicines SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteMedicine(id: Int)

    @Query("UPDATE medicines SET remainingStock = :stock WHERE id = :id")
    suspend fun updateRemainingStock(id: Int, stock: Int)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun hardDeleteMedicineById(id: Int)

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    suspend fun getAllMedicinesOnce(): List<MedicineEntity>

    @Query("DELETE FROM medicines")
    suspend fun deleteAllMedicines()
}
