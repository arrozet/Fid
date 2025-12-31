package com.example.fid.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

/**
 * Manages scheduling of recurring notifications
 */
class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("fid_notifications", Context.MODE_PRIVATE)
    
    companion object {
        private const val REQUEST_CODE_BREAKFAST = 1001
        private const val REQUEST_CODE_LUNCH = 1002
        private const val REQUEST_CODE_DINNER = 1003
        private const val REQUEST_CODE_HYDRATION_1 = 1004
        private const val REQUEST_CODE_HYDRATION_2 = 1005
        private const val REQUEST_CODE_HYDRATION_3 = 1006
        private const val REQUEST_CODE_DAILY_SUMMARY = 1007
    }
    
    fun scheduleAllNotifications() {
        if (prefs.getBoolean("enabled", true)) {
            if (prefs.getBoolean("meal_reminders", true)) {
                scheduleMealReminders()
            }
            if (prefs.getBoolean("hydration_reminders", true)) {
                scheduleHydrationReminders()
            }
            if (prefs.getBoolean("daily_summary", true)) {
                scheduleDailySummary()
            }
        }
    }
    
    fun cancelAllNotifications() {
        cancelMealReminders()
        cancelHydrationReminders()
        cancelDailySummary()
    }
    
    fun scheduleMealReminders() {
        val breakfastTime = prefs.getString("breakfast_time", "08:00") ?: "08:00"
        val lunchTime = prefs.getString("lunch_time", "13:00") ?: "13:00"
        val dinnerTime = prefs.getString("dinner_time", "20:00") ?: "20:00"
        
        android.util.Log.d("NotificationScheduler", "Programando recordatorios de comidas:")
        android.util.Log.d("NotificationScheduler", "Desayuno: $breakfastTime")
        android.util.Log.d("NotificationScheduler", "Almuerzo: $lunchTime")
        android.util.Log.d("NotificationScheduler", "Cena: $dinnerTime")
        
        scheduleNotification(
            requestCode = REQUEST_CODE_BREAKFAST,
            time = breakfastTime,
            notificationType = "meal_breakfast"
        )
        
        scheduleNotification(
            requestCode = REQUEST_CODE_LUNCH,
            time = lunchTime,
            notificationType = "meal_lunch"
        )
        
        scheduleNotification(
            requestCode = REQUEST_CODE_DINNER,
            time = dinnerTime,
            notificationType = "meal_dinner"
        )
        
        android.util.Log.d("NotificationScheduler", "Recordatorios de comidas programados correctamente")
    }
    
    fun cancelMealReminders() {
        cancelNotification(REQUEST_CODE_BREAKFAST)
        cancelNotification(REQUEST_CODE_LUNCH)
        cancelNotification(REQUEST_CODE_DINNER)
    }
    
    fun scheduleHydrationReminders() {
        // Schedule 3 hydration reminders throughout the day
        scheduleNotification(
            requestCode = REQUEST_CODE_HYDRATION_1,
            time = "10:00",
            notificationType = "hydration"
        )
        
        scheduleNotification(
            requestCode = REQUEST_CODE_HYDRATION_2,
            time = "15:00",
            notificationType = "hydration"
        )
        
        scheduleNotification(
            requestCode = REQUEST_CODE_HYDRATION_3,
            time = "18:00",
            notificationType = "hydration"
        )
    }
    
    fun cancelHydrationReminders() {
        cancelNotification(REQUEST_CODE_HYDRATION_1)
        cancelNotification(REQUEST_CODE_HYDRATION_2)
        cancelNotification(REQUEST_CODE_HYDRATION_3)
    }
    
    fun scheduleDailySummary() {
        val summaryTime = prefs.getString("daily_summary_time", "21:00") ?: "21:00"
        
        scheduleNotification(
            requestCode = REQUEST_CODE_DAILY_SUMMARY,
            time = summaryTime,
            notificationType = "daily_summary"
        )
    }
    
    fun cancelDailySummary() {
        cancelNotification(REQUEST_CODE_DAILY_SUMMARY)
    }
    
    private fun scheduleNotification(
        requestCode: Int,
        time: String,
        notificationType: String
    ) {
        val timeParts = time.split(":")
        val hour = timeParts[0].toIntOrNull() ?: 12
        val minute = timeParts[1].toIntOrNull() ?: 0
        
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If the time has already passed today, schedule for tomorrow
        // Only postpone if the time is in the past (already passed)
        val currentTime = now.timeInMillis
        val scheduledTime = calendar.timeInMillis
        val timeDifference = scheduledTime - currentTime
        val THIRTY_SECONDS_IN_MILLIS = 30 * 1000L // Safety margin: 30 seconds minimum
        
        android.util.Log.d("NotificationScheduler", "Programando notificación tipo: $notificationType a las $time")
        android.util.Log.d("NotificationScheduler", "Hora actual: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(now.time)}")
        android.util.Log.d("NotificationScheduler", "Hora programada inicial: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)}")
        android.util.Log.d("NotificationScheduler", "Diferencia de tiempo: ${timeDifference / 1000} segundos (${timeDifference / 60000} minutos)")
        
        if (scheduledTime <= currentTime) {
            // Time already passed today, schedule for tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            android.util.Log.d("NotificationScheduler", "La hora ya pasó hoy, programando para mañana: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)}")
        } else if (timeDifference < THIRTY_SECONDS_IN_MILLIS) {
            // Time is too close (less than 30 seconds), add a small buffer
            calendar.add(Calendar.SECOND, 30)
            android.util.Log.d("NotificationScheduler", "La hora está muy cerca (menos de 30 segundos), añadiendo buffer de 30 segundos: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)}")
        } else {
            android.util.Log.d("NotificationScheduler", "Programando para hoy: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)}")
        }
        
        android.util.Log.d("NotificationScheduler", "Timestamp final: ${calendar.timeInMillis} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)})")
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_type", notificationType)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Cancel any existing alarm first
        alarmManager.cancel(pendingIntent)
        
        // Schedule EXACT alarm (not repeating - receiver will reschedule)
        try {
            val triggerTime = calendar.timeInMillis
            val timeUntilTrigger = triggerTime - System.currentTimeMillis()
            
            android.util.Log.d("NotificationScheduler", "Tiempo hasta la alarma: ${timeUntilTrigger / 1000} segundos (${timeUntilTrigger / 60000} minutos)")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, check if we can schedule exact alarms
                val canScheduleExact = alarmManager.canScheduleExactAlarms()
                android.util.Log.d("NotificationScheduler", "¿Puede programar alarmas exactas? $canScheduleExact")
                
                if (canScheduleExact) {
                    // Use setExactAndAllowWhileIdle for reliable delivery
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.d("NotificationScheduler", "✅ Alarma exacta (una vez) programada para $notificationType")
                } else {
                    // Fallback to setAndAllowWhileIdle
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.d("NotificationScheduler", "⚠️ Alarma (una vez, sin garantía exacta) programada para $notificationType")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0+, use setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                android.util.Log.d("NotificationScheduler", "✅ Alarma exacta (una vez) programada para $notificationType")
            } else {
                // For older versions
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                android.util.Log.d("NotificationScheduler", "✅ Alarma exacta (una vez) programada para $notificationType")
            }
            
            // Verify the alarm was set
            android.util.Log.d("NotificationScheduler", "Alarma programada - Trigger: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "❌ Error al programar notificación: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    private fun cancelNotification(requestCode: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

