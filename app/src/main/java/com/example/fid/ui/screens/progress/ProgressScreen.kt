package com.example.fid.ui.screens.progress

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.DailySummary
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.data.repository.PeriodStats
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Enum para los períodos de tiempo disponibles
 */
enum class TimePeriod {
    WEEK, MONTH, YEAR
}

/**
 * Pantalla principal de Progreso
 * Muestra el progreso del usuario en modo detallado o sin números
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    
    var user by remember { mutableStateOf<User?>(null) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    var periodStats by remember { mutableStateOf<PeriodStats?>(null) }
    var dailySummaries by remember { mutableStateOf<List<DailySummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Datos del día de hoy para modo sin números
    var todayFoodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var todayWellness by remember { mutableStateOf<WellnessEntry?>(null) }
    
    // Calcular inicio y fin del día de hoy
    val calendar = remember { Calendar.getInstance() }
    val startOfToday = remember {
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfToday = remember { startOfToday + 24 * 60 * 60 * 1000 }
    
    // Función para calcular rangos de fecha
    fun getDateRange(period: TimePeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endDate = cal.timeInMillis
        
        when (period) {
            TimePeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            TimePeriod.MONTH -> cal.add(Calendar.MONTH, -1)
            TimePeriod.YEAR -> cal.add(Calendar.YEAR, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startDate = cal.timeInMillis
        
        return Pair(startDate, endDate)
    }
    
    // Cargar datos cuando cambia el período
    LaunchedEffect(selectedPeriod) {
        user = repository.getCurrentUser()
        user?.let { u ->
            isLoading = true
            val (startDate, endDate) = getDateRange(selectedPeriod)
            
            // Calcular y guardar resúmenes para días que no tienen
            scope.launch {
                repository.calculateAndSaveDailySummary(u.id, System.currentTimeMillis())
            }
            
            // Obtener estadísticas y resúmenes
            periodStats = repository.getPeriodStats(u.id, startDate, endDate)
            dailySummaries = repository.getDailySummariesByDateRange(u.id, startDate, endDate)
            
            // Cargar datos de hoy para modo sin números
            todayWellness = repository.getTodayWellnessEntry(u.id)
            
            isLoading = false
        }
    }
    
    // Cargar entradas de comida de hoy (Flow separado para no bloquear)
    LaunchedEffect(user) {
        user?.let { u ->
            repository.getFoodEntriesByDateRange(u.id, startOfToday, endOfToday).collect { entries ->
                todayFoodEntries = entries
            }
        }
    }
    
    val isNumberlessMode = user?.numberlessMode ?: false
    
    // Calcular totales del día de hoy
    val todayTotalCalories = todayFoodEntries.sumOf { it.calories.toDouble() }.toFloat()
    val todayTotalProtein = todayFoodEntries.sumOf { it.proteinG.toDouble() }.toFloat()
    val todayTotalFat = todayFoodEntries.sumOf { it.fatG.toDouble() }.toFloat()
    val todayTotalCarbs = todayFoodEntries.sumOf { it.carbG.toDouble() }.toFloat()
    val todayMealsCount = todayFoodEntries.size
    val todayCalorieGoal = user?.tdee ?: 2000f
    val todayProteinGoal = user?.proteinGoalG ?: 150f
    val todayWaterMl = todayWellness?.waterIntakeMl ?: 0f
    val todayWaterGoal = user?.waterGoalMl ?: 2500f
    val todaySleepHours = todayWellness?.sleepHours ?: 0f
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.progress),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            com.example.fid.ui.screens.dashboard.BottomNavigationBar(navController)
        },
        containerColor = DarkBackground
    ) { padding ->
        if (isNumberlessMode) {
            NumberlessProgressView(
                modifier = Modifier.padding(padding),
                calorieProgress = if (todayCalorieGoal > 0) todayTotalCalories / todayCalorieGoal else 0f,
                proteinProgress = if (todayProteinGoal > 0) todayTotalProtein / todayProteinGoal else 0f,
                carbProgress = if ((user?.carbGoalG ?: 0f) > 0) todayTotalCarbs / (user?.carbGoalG ?: 250f) else 0f,
                fatProgress = if ((user?.fatGoalG ?: 0f) > 0) todayTotalFat / (user?.fatGoalG ?: 65f) else 0f,
                waterProgress = if (todayWaterGoal > 0) todayWaterMl / todayWaterGoal else 0f,
                sleepHours = todaySleepHours,
                mealsCount = todayMealsCount,
                periodStats = periodStats,
                dailySummaries = dailySummaries,
                isLoading = isLoading
            )
        } else {
            DetailedProgressView(
                modifier = Modifier.padding(padding),
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it },
                periodStats = periodStats,
                dailySummaries = dailySummaries,
                isLoading = isLoading,
                navController = navController,
                measurementUnit = user?.measurementUnit ?: "metric"
            )
        }
    }
}

