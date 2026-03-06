package com.meditrack.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditrack.app.data.local.entity.DoseLogEntity
import com.meditrack.app.di.ReceiverEntryPoint
import com.meditrack.app.notification.NotificationHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val medicineId = intent.getIntExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: return
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, 0L)

        if (medicineId == -1 || scheduledTime == 0L) {
            pendingResult.finish()
            return
        }

        // Show notification
        NotificationHelper.showDoseReminder(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTimeMillis = scheduledTime
        )

        // Insert a PENDING dose log entry
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                val doseLogDao = entryPoint.doseLogDao()
                val alarmScheduler = entryPoint.alarmScheduler()

                val existingLog = doseLogDao.getDoseLogByMedicineAndTime(medicineId, scheduledTime)

                if (existingLog == null) {
                    val doseLog = DoseLogEntity(
                        medicineId = medicineId,
                        medicineName = medicineName,
                        scheduledTime = scheduledTime,
                        status = "PENDING"
                    )
                    doseLogDao.insertDoseLog(doseLog)
                }

                // Schedule same slot for next day to keep rolling coverage.
                val slotIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_INDEX, 0)
                val nextDayTime = scheduledTime + 24 * 60 * 60 * 1000L
                alarmScheduler.scheduleDose(
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dosage = dosage,
                    triggerAtMillis = nextDayTime,
                    slotIndex = slotIndex
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
