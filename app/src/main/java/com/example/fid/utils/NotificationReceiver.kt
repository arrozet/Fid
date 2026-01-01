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
        val notificationType = intent.getStringExtra("notification_type") ?: return
        val appContext = context.applicationContext
        
        createNotificationChannel(appContext)
        
        when {
            notificationType.startsWith("meal_") -> {
                val mealType = notificationType.removePrefix("meal_")
                showMealNotification(appContext, mealType)
                rescheduleNotification(context, notificationType)
            }
            notificationType == "hydration" -> {
                showHydrationNotification(appContext)
                rescheduleNotification(context, notificationType)
            }
            notificationType == "daily_summary" -> {
                showDailySummaryNotification(appContext)
                rescheduleNotification(context, notificationType)
            }
        }
    }
    
    private fun rescheduleNotification(context: Context, notificationType: String) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) return
        
        val scheduler = NotificationScheduler(context)
        
        when {
            notificationType.startsWith("meal_") && prefs.getBoolean("meal_reminders", true) -> {
                scheduler.scheduleMealReminders()
            }
            notificationType == "hydration" && prefs.getBoolean("hydration_reminders", true) -> {
                scheduler.scheduleHydrationReminders()
            }
            notificationType == "daily_summary" && prefs.getBoolean("daily_summary", true) -> {
                scheduler.scheduleDailySummary()
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
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("meal_reminders", true)) return
        
        val mealName = when (mealType) {
            "breakfast" -> LocaleHelper.getLocalizedString(context, R.string.breakfast)
            "lunch" -> LocaleHelper.getLocalizedString(context, R.string.lunch)
            "dinner" -> LocaleHelper.getLocalizedString(context, R.string.dinner)
            else -> LocaleHelper.getLocalizedString(context, R.string.meal)
        }
        
        val notificationId = when (mealType) {
            "breakfast" -> NOTIFICATION_ID_BREAKFAST
            "lunch" -> NOTIFICATION_ID_LUNCH
            "dinner" -> NOTIFICATION_ID_DINNER
            else -> NOTIFICATION_ID_BREAKFAST
        }
        
        val title = LocaleHelper.getLocalizedString(context, R.string.notification_meal_reminder_title)
        val text = LocaleHelper.getLocalizedString(context, R.string.notification_meal_reminder_text, mealName)
        
        showNotification(context, notificationId, title, text)
    }
    
    private fun showHydrationNotification(context: Context) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("hydration_reminders", true)) return
        
        val title = LocaleHelper.getLocalizedString(context, R.string.notification_hydration_title)
        val text = LocaleHelper.getLocalizedString(context, R.string.notification_hydration_text)
        
        showNotification(context, NOTIFICATION_ID_HYDRATION, title, text)
    }
    
    private fun showDailySummaryNotification(context: Context) {
        val prefs = context.getSharedPreferences(NotificationScheduler.getPreferencesName(context), Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true) || !prefs.getBoolean("daily_summary", true)) return
        
        val title = LocaleHelper.getLocalizedString(context, R.string.notification_summary_title)
        val text = LocaleHelper.getLocalizedString(context, R.string.notification_summary_text)
        
        showNotification(context, NOTIFICATION_ID_SUMMARY, title, text)
    }
    
    private fun showNotification(context: Context, notificationId: Int, title: String, text: String) {
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
        notificationManager.notify(notificationId, notification)
    }
}

