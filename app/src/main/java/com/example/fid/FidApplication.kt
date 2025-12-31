package com.example.fid

import android.app.Application
import android.content.Context
import com.example.fid.utils.LocaleHelper
import com.example.fid.utils.NotificationHelper
import com.example.fid.utils.NotificationScheduler
import com.google.firebase.FirebaseApp

/**
 * Clase Application que inicializa Firebase y el idioma al arrancar la app
 */
class FidApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Inicializar canal de notificaciones
        NotificationHelper(this)
        
        // Programar notificaciones si están habilitadas
        val prefs = getSharedPreferences("fid_notifications", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("enabled", true)
        if (notificationsEnabled) {
            val scheduler = NotificationScheduler(this)
            scheduler.scheduleAllNotifications()
        }
    }
}
