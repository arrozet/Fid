package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.DailySummary
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.data.repository.PeriodStats
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.example.fid.utils.UnitConverter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimePeriod {
    WEEK, MONTH, YEAR
}

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

@Composable
fun NumberlessProgressView(
    modifier: Modifier = Modifier,
    calorieProgress: Float = 0f,
    proteinProgress: Float = 0f,
    carbProgress: Float = 0f,
    fatProgress: Float = 0f,
    waterProgress: Float = 0f,
    sleepHours: Float = 0f,
    mealsCount: Int = 0,
    periodStats: PeriodStats? = null,
    dailySummaries: List<DailySummary> = emptyList(),
    isLoading: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    // Determinar el emoji y mensaje principal basado en datos reales
    val (mainEmoji, mainColor) = when {
        mealsCount == 0 -> "🌱" to PrimaryGreen.copy(alpha = 0.6f)
        calorieProgress >= 0.9f && calorieProgress <= 1.1f -> "🌟" to PrimaryGreen
        calorieProgress >= 0.75f -> "✨" to PrimaryGreen
        calorieProgress >= 0.5f -> "🌳" to PrimaryGreen
        calorieProgress >= 0.25f -> "🌿" to ProteinColor
        calorieProgress > 0f -> "🌱" to ProteinColor
        else -> "🌱" to TextSecondary
    }
    
    // Determinar mensaje de estado basado en datos
    val statusMessage = when {
        mealsCount == 0 -> stringResource(R.string.no_meals_today)
        calorieProgress >= 0.9f && calorieProgress <= 1.1f -> stringResource(R.string.goal_reached)
        calorieProgress >= 0.75f -> stringResource(R.string.almost_there)
        calorieProgress >= 0.5f -> stringResource(R.string.halfway_there)
        calorieProgress > 0f -> stringResource(R.string.keep_going)
        else -> stringResource(R.string.start_logging)
    }
    
    // Determinar estado de la dieta
    val dietStatus = when {
        mealsCount == 0 -> stringResource(R.string.waiting_first_meal)
        proteinProgress >= 0.8f && carbProgress >= 0.5f && fatProgress >= 0.5f -> stringResource(R.string.balanced_diet)
        proteinProgress >= 1.0f -> stringResource(R.string.prioritized_protein)
        carbProgress >= 1.0f -> stringResource(R.string.high_carbs_today)
        fatProgress >= 1.0f -> stringResource(R.string.high_fats_today)
        proteinProgress < 0.5f && mealsCount >= 2 -> stringResource(R.string.need_more_protein)
        else -> stringResource(R.string.on_track)
    }
    
    // Determinar nivel de energía basado en calorías y macros
    val energyMessage = when {
        mealsCount == 0 -> stringResource(R.string.no_energy_data)
        calorieProgress >= 0.7f && proteinProgress >= 0.5f -> stringResource(R.string.high_energy)
        calorieProgress >= 0.5f -> stringResource(R.string.moderate_energy)
        calorieProgress > 0f -> stringResource(R.string.building_energy)
        else -> stringResource(R.string.no_energy_data)
    }
    
    // Determinar consejo basado en datos
    val (tipMessage, tipColor) = when {
        mealsCount == 0 -> stringResource(R.string.tip_start_day) to PrimaryGreen
        waterProgress < 0.5f && mealsCount >= 1 -> stringResource(R.string.tip_drink_water) to WaterColor
        proteinProgress < 0.5f && calorieProgress >= 0.5f -> stringResource(R.string.tip_add_protein) to ProteinColor
        sleepHours > 0f && sleepHours < 6f -> stringResource(R.string.tip_sleep_more) to WarningYellow
        sleepHours > 9f -> stringResource(R.string.tip_sleep_less) to WarningYellow
        fatProgress >= 1.2f -> stringResource(R.string.tip_reduce_fats) to FatColor
        carbProgress >= 1.2f -> stringResource(R.string.tip_reduce_carbs) to CarbColor
        calorieProgress >= 1.1f -> stringResource(R.string.tip_calorie_surplus) to WarningYellow
        else -> stringResource(R.string.tip_keep_going) to PrimaryGreen
    }
    
    // Calcular tendencias del período
    val consistencyTrend = when {
        dailySummaries.isEmpty() -> stringResource(R.string.no_data)
        periodStats?.daysOnTarget ?: 0 >= (periodStats?.totalDays ?: 1) * 0.7 -> stringResource(R.string.excellent)
        periodStats?.daysOnTarget ?: 0 >= (periodStats?.totalDays ?: 1) * 0.5 -> stringResource(R.string.stable)
        else -> stringResource(R.string.improving)
    }
    
    val hydrationTrend = when {
        waterProgress >= 0.8f -> stringResource(R.string.excellent)
        waterProgress >= 0.5f -> stringResource(R.string.stable)
        waterProgress > 0f -> stringResource(R.string.improving)
        else -> stringResource(R.string.no_data)
    }
    
    // Diversidad dietética basada en comidas registradas en el período
    val diversityTrend = when {
        dailySummaries.isEmpty() -> stringResource(R.string.no_data)
        dailySummaries.size >= 5 -> stringResource(R.string.excellent)
        dailySummaries.size >= 3 -> stringResource(R.string.stable)
        else -> stringResource(R.string.improving)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Avatar/Mascot basado en progreso real
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(mainColor.copy(alpha = 0.2f), RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mainEmoji,
                    fontSize = 80.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = statusMessage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = mainColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tarjetas cualitativas basadas en datos reales
            QualitativeCard(
                title = stringResource(R.string.today_status),
                message = dietStatus,
                color = if (mealsCount > 0) PrimaryGreen else TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QualitativeCard(
                title = stringResource(R.string.energy),
                message = energyMessage,
                color = if (mealsCount > 0) ProteinColor else TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QualitativeCard(
                title = stringResource(R.string.gentle_tip),
                message = tipMessage,
                color = tipColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tendencias de bienestar basadas en datos
            Text(
                text = stringResource(R.string.wellness_trends),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SimpleTrendCard(
                stringResource(R.string.hydration_trend), 
                hydrationTrend, 
                when (hydrationTrend) {
                    stringResource(R.string.excellent) -> PrimaryGreen
                    stringResource(R.string.stable) -> ProteinColor
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SimpleTrendCard(
                stringResource(R.string.dietary_diversity), 
                diversityTrend, 
                when (diversityTrend) {
                    stringResource(R.string.excellent) -> PrimaryGreen
                    stringResource(R.string.stable) -> ProteinColor
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SimpleTrendCard(
                stringResource(R.string.consistency), 
                consistencyTrend, 
                when (consistencyTrend) {
                    stringResource(R.string.excellent) -> PrimaryGreen
                    stringResource(R.string.stable) -> ProteinColor
                    else -> TextSecondary
                }
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DetailedProgressView(
    modifier: Modifier = Modifier,
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    periodStats: PeriodStats?,
    dailySummaries: List<DailySummary>,
    isLoading: Boolean,
    navController: NavController,
    measurementUnit: String = "metric"
) {
    val scrollState = rememberScrollState()
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // Period selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodButton(
                text = stringResource(R.string.week),
                selected = selectedPeriod == TimePeriod.WEEK,
                onClick = { onPeriodChange(TimePeriod.WEEK) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = stringResource(R.string.month),
                selected = selectedPeriod == TimePeriod.MONTH,
                onClick = { onPeriodChange(TimePeriod.MONTH) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = stringResource(R.string.year),
                selected = selectedPeriod == TimePeriod.YEAR,
                onClick = { onPeriodChange(TimePeriod.YEAR) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else if (periodStats != null && dailySummaries.isNotEmpty()) {
            // Period overview
            val periodNameWeekly = stringResource(R.string.period_weekly)
            val periodNameMonthly = stringResource(R.string.period_monthly)
            val periodNameYearly = stringResource(R.string.period_yearly)
            val periodName = when (selectedPeriod) {
                TimePeriod.WEEK -> periodNameWeekly
                TimePeriod.MONTH -> periodNameMonthly
                TimePeriod.YEAR -> periodNameYearly
            }
            
            Text(
                text = stringResource(R.string.summary_period, periodName),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    StatRow(
                        stringResource(R.string.daily_average), 
                        "${periodStats.avgCalories.toInt()} kcal", 
                        PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow(
                        stringResource(R.string.days_on_target), 
                        "${periodStats.daysOnTarget} / ${periodStats.totalDays}", 
                        ProteinColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow(
                        stringResource(R.string.average_protein), 
                        "${UnitConverter.convertGrams(periodStats.avgProteinG, measurementUnit).toInt()}$unitLabel", 
                        ProteinColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Macro distribution
            Text(
                text = stringResource(R.string.macro_distribution_period, periodName),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroDistributionCard(
                    stringResource(R.string.proteins), 
                    "${periodStats.proteinPercentage}%", 
                    ProteinColor, 
                    Modifier.weight(1f)
                )
                MacroDistributionCard(
                    stringResource(R.string.fats), 
                    "${periodStats.fatPercentage}%", 
                    FatColor, 
                    Modifier.weight(1f)
                )
                MacroDistributionCard(
                    stringResource(R.string.carbs_short), 
                    "${periodStats.carbPercentage}%", 
                    CarbColor, 
                    Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Daily summaries list
            Text(
                text = stringResource(R.string.daily_history),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            dailySummaries.forEach { summary ->
                DailySummaryCard(
                    summary = summary,
                    measurementUnit = measurementUnit,
                    onClick = {
                        navController.navigate(Screen.DailyDetail.createRoute(summary.date))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        } else {
            // No data
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(DarkCard, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📊",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_data_for_period),
                        color = TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun QualitativeCard(title: String, message: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = color,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun SimpleTrendCard(title: String, trend: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = trend,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun MacroDistributionCard(label: String, percentage: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = percentage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PeriodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryGreen else DarkCard,
            contentColor = if (selected) DarkBackground else TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DailySummaryCard(
    summary: DailySummary,
    measurementUnit: String = "metric",
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(Date(summary.date)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.meals_registered_count, summary.mealsCount),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.totalCalories.toInt()} kcal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    val caloriePercentage = if (summary.calorieGoal > 0) {
                        ((summary.totalCalories / summary.calorieGoal) * 100).toInt()
                    } else 0
                    
                    Text(
                        text = stringResource(R.string.percentage_of_goal, caloriePercentage),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mini progress bars for macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMacroBar(
                    label = "P",
                    current = summary.totalProteinG,
                    goal = summary.proteinGoal,
                    color = ProteinColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "G",
                    current = summary.totalFatG,
                    goal = summary.fatGoal,
                    color = FatColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "C",
                    current = summary.totalCarbG,
                    goal = summary.carbGoal,
                    color = CarbColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Wellness info (sleep and water)
            if (summary.sleepHours > 0f || summary.waterIntakeMl > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (summary.sleepHours > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "😴",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${summary.sleepHours.toInt()}h",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    if (summary.waterIntakeMl > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "💧",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1fL", summary.waterIntakeMl / 1000f),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMacroBar(
    label: String,
    current: Float,
    goal: Float,
    color: androidx.compose.ui.graphics.Color,
    measurementUnit: String = "metric",
    modifier: Modifier = Modifier
) {
    val displayValue = UnitConverter.convertGrams(current, measurementUnit)
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${displayValue.toInt()}$unitLabel",
                fontSize = 10.sp,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = DarkSurface,
        )
    }
}

