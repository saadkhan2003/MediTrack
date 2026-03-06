package com.meditrack.app.di

import android.content.Context
import androidx.room.Room
import com.meditrack.app.data.local.MediTrackDatabase
import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.dao.MedicineDao
import com.meditrack.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MediTrackDatabase {
        return Room.databaseBuilder(
            ctx,
            MediTrackDatabase::class.java,
            "meditrack_database"
        )
            .addMigrations(MediTrackDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideMedicineDao(db: MediTrackDatabase): MedicineDao = db.medicineDao()

    @Provides
    fun provideDoseLogDao(db: MediTrackDatabase): DoseLogDao = db.doseLogDao()

    @Provides
    fun provideUserDao(db: MediTrackDatabase): UserDao = db.userDao()
}
