package com.meditrack.app.di

import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.dao.MedicineDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun doseLogDao(): DoseLogDao
    fun medicineDao(): MedicineDao
    fun alarmScheduler(): AlarmScheduler
}
