package com.meditrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.dao.MedicineDao
import com.meditrack.app.data.local.dao.UserDao
import com.meditrack.app.data.local.entity.DoseLogEntity
import com.meditrack.app.data.local.entity.MedicineEntity
import com.meditrack.app.data.local.entity.UserEntity

@Database(
    entities = [
        MedicineEntity::class,
        DoseLogEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class MediTrackDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_isActive ON medicines(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dose_logs_scheduledTime ON dose_logs(scheduledTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dose_logs_status ON dose_logs(status)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_dose_logs_medicineId_scheduledTime ON dose_logs(medicineId, scheduledTime)")
            }
        }
    }
}
