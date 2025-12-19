package com.example.fid.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fid.R

/**
 * Helper class for managing notifications
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "fid_notifications"
        private const val CHANNEL_NAME = "Fid Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for meal reminders and progress updates"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a meal reminder notification
     */
    fun showMealReminder(mealType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hora de registrar tu comida")
            .setContentText("No olvides registrar tu $mealType")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(mealType.hashCode(), notification)
    }
    
    /**
     * Show a progress update notification
     */
    fun showProgressUpdate(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Actualización de progreso")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Show a motivational notification
     */
    fun showMotivationalMessage(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Sigue así!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

