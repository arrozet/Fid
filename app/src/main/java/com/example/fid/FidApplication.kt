package com.example.fid

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Clase Application que inicializa Firebase al arrancar la app
 */
class FidApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
    }
}
