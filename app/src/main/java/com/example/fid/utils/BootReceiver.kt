package com.example.fid.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that reschedules notifications when the device boots up
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling notifications")
            
            try {
                val prefsName = NotificationScheduler.getPreferencesName(context)
                val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("enabled", true)
                
                if (notificationsEnabled) {
                    val scheduler = NotificationScheduler(context)
                    scheduler.scheduleAllNotifications()
                    Log.d("BootReceiver", "Notifications rescheduled successfully")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling notifications: ${e.message}")
            }
        }
    }
}

