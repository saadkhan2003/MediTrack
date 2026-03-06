package com.meditrack.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.meditrack.app.R
import com.meditrack.app.alarm.AlarmScheduler
import com.meditrack.app.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID_REMINDERS = "meditrack_reminders"
    const val CHANNEL_ID_REFILLS = "meditrack_refills"

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val remindersChannel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        val refillsChannel = NotificationChannel(
            CHANNEL_ID_REFILLS,
            context.getString(R.string.channel_refills_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_refills_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(remindersChannel)
        notificationManager.createNotificationChannel(refillsChannel)
    }

    fun showDoseReminder(
        context: Context,
        medicineId: Int,
        medicineName: String,
        dosage: String,
        scheduledTimeMillis: Long
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent — open app to home screen
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("highlightMedicineId", medicineId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            medicineId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Taken action
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId * 2,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Missed action
        val missedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MISSED
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }
        val missedPendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId * 2 + 1,
            missedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(scheduledTimeMillis))

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_dose_title, medicineName))
            .setContentText(context.getString(R.string.notification_dose_text, dosage, timeStr))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.action_taken), takenPendingIntent)
            .addAction(0, context.getString(R.string.action_missed), missedPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationId = "$medicineId-$scheduledTimeMillis".hashCode()
        notificationManager.notify(notificationId, notification)
    }

    fun showRefillAlert(context: Context, medicineName: String, remaining: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            medicineName.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REFILLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_refill_title, medicineName))
            .setContentText(context.getString(R.string.notification_refill_text, remaining))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(medicineName.hashCode(), notification)
    }

    fun dismissNotification(context: Context, medicineId: Int, scheduledTimeMillis: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = "$medicineId-$scheduledTimeMillis".hashCode()
        notificationManager.cancel(notificationId)
    }
}
