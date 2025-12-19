package com.example.fid

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
import com.example.fid.data.database.FidDatabase
import com.example.fid.data.repository.FidRepository
import com.example.fid.navigation.NavGraph
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.FidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize database with sample data
        val database = FidDatabase.getDatabase(applicationContext)
        val repository = FidRepository(database)
        val seeder = DatabaseSeeder(repository)
        
        CoroutineScope(Dispatchers.IO).launch {
            seeder.seedFoodItems()
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