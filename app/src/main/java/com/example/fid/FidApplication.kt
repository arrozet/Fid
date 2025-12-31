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
        
        // Programar notificaciones si están habilitadas (solo si hay usuario logueado)
        // Nota: En el arranque puede que aún no haya usuario, así que esto se manejará cuando el usuario inicie sesión
        try {
            val prefsName = NotificationScheduler.getPreferencesName(this)
            val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean("enabled", true)
            if (notificationsEnabled) {
                val scheduler = NotificationScheduler(this)
                scheduler.scheduleAllNotifications()
            }
        } catch (e: Exception) {
            android.util.Log.e("FidApplication", "Error programando notificaciones al arrancar: ${e.message}")
        }
    }
}
