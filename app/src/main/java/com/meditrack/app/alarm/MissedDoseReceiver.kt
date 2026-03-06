package com.meditrack.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditrack.app.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MissedDoseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val medicineId = intent.getIntExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1)
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, 0L)

        if (medicineId == -1 || scheduledTime == 0L) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                val doseLogDao = entryPoint.doseLogDao()

                val doseLog = doseLogDao.getDoseLogByMedicineAndTime(medicineId, scheduledTime)

                if (doseLog != null && doseLog.status == "PENDING") {
                    doseLogDao.updateDoseStatus(
                        id = doseLog.id,
                        status = "MISSED",
                        loggedTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
