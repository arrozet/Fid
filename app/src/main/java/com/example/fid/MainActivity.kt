package com.example.fid

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.fid.data.database.DatabaseSeeder
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.navigation.NavGraph
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.FidTheme
import com.example.fid.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize database with sample data (solo la primera vez)
        val repository = FirebaseRepository()
        val seeder = DatabaseSeeder(repository)
        
        val prefs = getSharedPreferences("fid_prefs", MODE_PRIVATE)
        val hasSeeded = prefs.getBoolean("has_seeded_db", false)
        
        // TEMPORAL: Limpiar duplicados en el próximo inicio
        // Después de que se ejecute una vez, puedes comentar este bloque
        CoroutineScope(Dispatchers.IO).launch {
            repository.cleanDuplicateFoodItems()
            android.util.Log.d("MainActivity", "Duplicados limpiados")
        }
        
        if (!hasSeeded) {
            CoroutineScope(Dispatchers.IO).launch {
                seeder.seedFoodItems()
                prefs.edit().putBoolean("has_seeded_db", true).apply()
                android.util.Log.d("MainActivity", "Base de datos inicializada con alimentos de ejemplo")
            }
        }
        
        setContent {
            FidTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }
                
                // Check if user exists to determine start destination
                LaunchedEffect(Unit) {
                    val user = repository.getCurrentUser()
                    startDestination = if (user != null) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }
                }
                
                // Only show navigation when we know the start destination
                startDestination?.let { destination ->
                    NavGraph(
                        navController = navController,
                        startDestination = destination
                    )
                }
            }
        }
    }
}