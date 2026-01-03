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
        
        // IMPORTANTE: Si actualizaste el modelo FoodItem con soporte multiidioma,
        // necesitas re-sembrar la base de datos. Para hacer esto:
        // 1. Descomenta las siguientes líneas
        // 2. Ejecuta la app una vez
        // 3. Vuelve a comentar las líneas
        
        // DESCOMENTA ESTO PARA ACTUALIZAR LOS ALIMENTOS CON SOPORTE MULTIIDIOMA:
        // ⚠️ EJECUTA LA APP UNA VEZ CON ESTO DESCOMENTADO, LUEGO VUELVE A COMENTAR
        // YA SE EJECUTÓ - Los alimentos tienen nameEs y nameEn correctamente
        /*
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("MainActivity", "Limpiando y re-sembrando base de datos...")
            repository.cleanAllFoodItems()
            seeder.seedFoodItems()
            prefs.edit().putBoolean("has_seeded_db", true).apply()
            android.util.Log.d("MainActivity", "Base de datos re-inicializada con alimentos multiidioma")
        }
        */
        
        // DESCOMENTA ESTO PARA ACTUALIZAR EL CAMPO 'name' CON EL VALOR DE 'nameEs':
        // ⚠️ EJECUTA LA APP UNA VEZ CON ESTO DESCOMENTADO, LUEGO VUELVE A COMENTAR
        /*
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("MainActivity", "Actualizando campo 'name' desde 'nameEs'...")
            repository.updateFoodItemsNameFromNameEs()
            android.util.Log.d("MainActivity", "Campo 'name' actualizado correctamente")
        }
        */
        
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
                
                // Check if user exists first, then check if initial setup is needed
                LaunchedEffect(Unit) {
                    val user = repository.getCurrentUser()
                    
                    startDestination = if (user == null) {
                        // No hay usuario: siempre mostrar setup inicial para que configure idioma y unidades
                        Screen.InitialSetup.route
                    } else {
                        // Hay usuario: verificar si tiene preferencias guardadas
                        val initialSetupDone = prefs.getBoolean("initial_setup_done", false)
                        val measurementUnit = prefs.getString("measurement_unit", null)
                        
                        if (!initialSetupDone || measurementUnit == null) {
                            // Usuario existe pero no tiene preferencias guardadas: mostrar setup
                            Screen.InitialSetup.route
                        } else {
                            // Usuario existe y tiene preferencias: ir al dashboard
                            Screen.Dashboard.route
                        }
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