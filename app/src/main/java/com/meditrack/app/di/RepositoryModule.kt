package com.meditrack.app.di

import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.dao.MedicineDao
import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.data.repository.MedicineRepository
import com.meditrack.app.data.sync.FirestoreSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMedicineRepository(dao: MedicineDao, syncService: FirestoreSyncService): MedicineRepository {
        return MedicineRepository(dao, syncService)
    }

    @Provides
    @Singleton
    fun provideDoseLogRepository(dao: DoseLogDao, syncService: FirestoreSyncService): DoseLogRepository {
        return DoseLogRepository(dao, syncService)
    }
}
