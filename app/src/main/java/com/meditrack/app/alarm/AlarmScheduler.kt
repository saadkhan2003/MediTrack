package com.meditrack.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.meditrack.app.domain.model.Medicine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDose(
        medicineId: Int,
        medicineName: String,
        dosage: String,
        triggerAtMillis: Long,
        slotIndex: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms — permission not granted")
                return
            }
        }

        Log.d(TAG, "Scheduling dose alarm: medicine=$medicineName, id=$medicineId, slot=$slotIndex, trigger=${java.util.Date(triggerAtMillis)}")

        val requestCode = generateRequestCode(medicineId, triggerAtMillis, slotIndex)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULED_TIME, triggerAtMillis)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        // Schedule missed dose check 60 minutes later
        scheduleMissedDoseCheck(medicineId, medicineName, triggerAtMillis, slotIndex)
    }

    private fun scheduleMissedDoseCheck(
        medicineId: Int,
        medicineName: String,
        scheduledTimeMillis: Long,
        slotIndex: Int
    ) {
        val missedCheckTime = scheduledTimeMillis + MISSED_CHECK_DELAY_MILLIS
        val requestCode = generateMissedCheckRequestCode(medicineId, scheduledTimeMillis, slotIndex)

        val intent = Intent(context, MissedDoseReceiver::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            missedCheckTime,
            pendingIntent
        )
    }

    fun cancelAllForMedicine(medicineId: Int) {
        val zone = ZoneId.systemDefault()
        val startDate = LocalDate.now().minusDays(1)
        for (dayOffset in 0..SCHEDULE_HORIZON_DAYS) {
            val date = startDate.plusDays(dayOffset.toLong())
            for (slotIndex in 0 until MAX_SLOTS) {
                val dayTriggerMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val requestCode = generateRequestCode(medicineId, dayTriggerMillis, slotIndex)
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()

                val missedRequestCode = generateMissedCheckRequestCode(medicineId, dayTriggerMillis, slotIndex)
                val missedIntent = Intent(context, MissedDoseReceiver::class.java)
                val missedPendingIntent = PendingIntent.getBroadcast(
                    context,
                    missedRequestCode,
                    missedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(missedPendingIntent)
                missedPendingIntent.cancel()
            }
        }
    }

    fun scheduleMedicineAlarms(medicine: Medicine) {
        Log.d(TAG, "scheduleMedicineAlarms: ${medicine.name} (id=${medicine.id}, active=${medicine.isActive}, stock=${medicine.remainingStock}, times=${medicine.scheduledTimes})")
        if (!medicine.isActive || medicine.remainingStock <= 0) {
            Log.w(TAG, "Skipping: inactive or zero stock")
            return
        }

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        for (dayOffset in 0 until SCHEDULE_HORIZON_DAYS) {
            val date = today.plusDays(dayOffset.toLong())
            if (!isWithinMedicineDateRange(medicine, date, zone)) continue
            medicine.scheduledTimes.forEachIndexed { slotIndex, timeStr ->
                try {
                    val parts = timeStr.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    val triggerTime = date.atTime(LocalTime.of(hour, minute))
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()

                    if (triggerTime > System.currentTimeMillis()) {
                        scheduleDose(
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            triggerAtMillis = triggerTime,
                            slotIndex = slotIndex
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping invalid time format '$timeStr'", e)
                }
            }
        }
    }

    fun rescheduleAll(medicines: List<Medicine>) {
        medicines.forEach { medicine ->
            if (medicine.isActive && medicine.remainingStock > 0) {
                cancelAllForMedicine(medicine.id)
                scheduleMedicineAlarms(medicine)
            }
        }
    }

    private fun generateRequestCode(medicineId: Int, triggerAtMillis: Long, slotIndex: Int): Int {
        val dayOfYear = java.time.Instant.ofEpochMilli(triggerAtMillis)
            .atZone(ZoneId.systemDefault())
            .dayOfYear
        return medicineId * 10000 + dayOfYear * 10 + slotIndex
    }

    private fun generateMissedCheckRequestCode(medicineId: Int, triggerAtMillis: Long, slotIndex: Int): Int {
        return generateRequestCode(medicineId, triggerAtMillis, slotIndex) + 5
    }

    private fun isWithinMedicineDateRange(medicine: Medicine, date: LocalDate, zone: ZoneId): Boolean {
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate).atZone(zone).toLocalDate()
        if (date.isBefore(startDate)) return false
        val end = medicine.endDate
        if (end != null) {
            val endDate = java.time.Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
            if (date.isAfter(endDate)) return false
        }
        return true
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val MAX_SLOTS = 10
        const val MISSED_CHECK_DELAY_MILLIS = 60 * 60 * 1000L // 60 minutes
        const val SCHEDULE_HORIZON_DAYS = 14
    }
}
