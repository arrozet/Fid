package com.example.fid.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fid.MainActivity
import com.example.fid.R

/**
 * BroadcastReceiver that handles scheduled notifications
 */
class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "fid_notifications"
        private const val CHANNEL_NAME = "Fid Notifications"
        
        private const val NOTIFICATION_ID_BREAKFAST = 1001
        private const val NOTIFICATION_ID_LUNCH = 1002
        private const val NOTIFICATION_ID_DINNER = 1003
        private const val NOTIFICATION_ID_HYDRATION = 1004
        private const val NOTIFICATION_ID_SUMMARY = 1005
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("NotificationReceiver", "=== RECEIVER ACTIVADO ===")
        android.util.Log.d("NotificationReceiver", "Intent recibido: ${intent.action}")
        android.util.Log.d("NotificationReceiver", "Extras: ${intent.extras?.keySet()}")
        
        val notificationType = intent.getStringExtra("notification_type")
        android.util.Log.d("NotificationReceiver", "Tipo de notificación: $notificationType")
        
        if (notificationType == null) {
            android.util.Log.e("NotificationReceiver", "ERROR: notification_type es null!")
            return
        }
        
        android.util.Log.d("NotificationReceiver", "Procesando notificación tipo: $notificationType")
        
        createNotificationChannel(context)
        
        when {
            notificationType.startsWith("meal_") -> {
                val mealType = notificationType.removePrefix("meal_")
                android.util.Log.d("NotificationReceiver", "Mostrando notificación de comida: $mealType")
                showMealNotification(context, mealType)
                // Reprogramar para mañana
                rescheduleNotification(context, notificationType)
            }
            notificationType == "hydration" -> {
                android.util.Log.d("NotificationReceiver", "Mostrando notificación de hidratación")
                showHydrationNotification(context)
                // Reprogramar para mañana
                rescheduleNotification(context, notificationType)
            }
            notificationType == "daily_summary" -> {
                android.util.Log.d("NotificationReceiver", "Mostrando notificación de resumen diario")
                showDailySummaryNotification(context)
                // Reprogramar para mañana
                rescheduleNotification(context, notificationType)
            }
        }
    }
    
    /**
     * Reschedules the notification for the next day
     */
    private fun rescheduleNotification(context: Context, notificationType: String) {
        android.util.Log.d("NotificationReceiver", "Reprogramando $notificationType para mañana...")
        
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) {
            android.util.Log.d("NotificationReceiver", "Notificaciones desactivadas, no reprogramar")
            return
        }
        
        // Use NotificationScheduler to reschedule
        val scheduler = NotificationScheduler(context)
        
        when {
            notificationType.startsWith("meal_") -> {
                if (prefs.getBoolean("meal_reminders", true)) {
                    scheduler.scheduleMealReminders()
                    android.util.Log.d("NotificationReceiver", "Comidas reprogramadas")
                }
            }
            notificationType == "hydration" -> {
                if (prefs.getBoolean("hydration_reminders", true)) {
                    scheduler.scheduleHydrationReminders()
                    android.util.Log.d("NotificationReceiver", "Hidratación reprogramada")
                }
            }
            notificationType == "daily_summary" -> {
                if (prefs.getBoolean("daily_summary", true)) {
                    scheduler.scheduleDailySummary()
                    android.util.Log.d("NotificationReceiver", "Resumen diario reprogramado")
                }
            }
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showMealNotification(context: Context, mealType: String) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("meal_reminders", true)) {
            return
        }
        
        val mealName = when (mealType) {
            "breakfast" -> context.getString(R.string.breakfast)
            "lunch" -> context.getString(R.string.lunch)
            "dinner" -> context.getString(R.string.dinner)
            else -> context.getString(R.string.meal)
        }
        
        val notificationId = when (mealType) {
            "breakfast" -> NOTIFICATION_ID_BREAKFAST
            "lunch" -> NOTIFICATION_ID_LUNCH
            "dinner" -> NOTIFICATION_ID_DINNER
            else -> NOTIFICATION_ID_BREAKFAST
        }
        
        val title = context.getString(R.string.notification_meal_reminder_title)
        val text = context.getString(R.string.notification_meal_reminder_text, mealName)
        
        showNotification(context, notificationId, title, text)
    }
    
    private fun showHydrationNotification(context: Context) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("hydration_reminders", true)) {
            return
        }
        
        val title = context.getString(R.string.notification_hydration_title)
        val text = context.getString(R.string.notification_hydration_text)
        
        showNotification(context, NOTIFICATION_ID_HYDRATION, title, text)
    }
    
    private fun showDailySummaryNotification(context: Context) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("daily_summary", true)) {
            return
        }
        
        val title = context.getString(R.string.notification_summary_title)
        val text = context.getString(R.string.notification_summary_text)
        
        showNotification(context, NOTIFICATION_ID_SUMMARY, title, text)
    }
    
    private fun showNotification(context: Context, notificationId: Int, title: String, text: String) {
        android.util.Log.d("NotificationReceiver", "Mostrando notificación - ID: $notificationId, Título: $title, Texto: $text")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("NotificationReceiver", "✅ Notificación mostrada exitosamente - ID: $notificationId")
        } catch (e: Exception) {
            android.util.Log.e("NotificationReceiver", "❌ Error al mostrar notificación: ${e.message}", e)
        }
    }
}

