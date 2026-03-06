package com.meditrack.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val action = intent.action
        val medicineId = intent.getIntExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: ""
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, 0L)

        if (medicineId == -1 || scheduledTime == 0L || action == null) {
            pendingResult.finish()
            return
        }

        val status = when (action) {
            ACTION_TAKEN -> "TAKEN"
            ACTION_MISSED -> "MISSED"
            else -> {
                pendingResult.finish()
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                val doseLogDao = entryPoint.doseLogDao()
                val medicineDao = entryPoint.medicineDao()

                val now = System.currentTimeMillis()

                // Fetch existing log — may be null if AlarmReceiver coroutine hasn't finished yet
                var doseLog = doseLogDao.getDoseLogByMedicineAndTime(medicineId, scheduledTime)

                if (doseLog == null) {
                    // Race condition: notification action tapped before AlarmReceiver finished
                    // creating the PENDING row. Create it now with the correct final status.
                    val newLog = com.meditrack.app.data.local.entity.DoseLogEntity(
                        medicineId = medicineId,
                        medicineName = medicineName,
                        scheduledTime = scheduledTime,
                        loggedTime = now,
                        status = status
                    )
                    val insertedId = doseLogDao.insertDoseLog(newLog)
                    if (insertedId == -1L) {
                        // Row was inserted by AlarmReceiver between our read and now — try again
                        doseLog = doseLogDao.getDoseLogByMedicineAndTime(medicineId, scheduledTime)
                        if (doseLog != null) {
                            doseLogDao.updateDoseStatus(id = doseLog.id, status = status, loggedTime = now)
                        }
                    }
                } else {
                    doseLogDao.updateDoseStatus(id = doseLog.id, status = status, loggedTime = now)
                }

                // Decrement stock and show refill alert on TAKEN
                if (status == "TAKEN") {
                    val medicine = medicineDao.getMedicineById(medicineId)
                    if (medicine != null) {
                        val newStock = (medicine.remainingStock - 1).coerceAtLeast(0)
                        medicineDao.updateRemainingStock(medicineId, newStock)
                        if (newStock <= medicine.refillThreshold) {
                            NotificationHelper.showRefillAlert(context, medicine.name, newStock)
                        }
                    }
                }

                // Always dismiss the notification
                NotificationHelper.dismissNotification(context, medicineId, scheduledTime)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "ACTION_TAKEN"
        const val ACTION_MISSED = "ACTION_MISSED"
    }
}
