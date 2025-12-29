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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.data.repository.PeriodStats
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
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
    
    // Función para calcular rangos de fecha
    fun getDateRange(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.timeInMillis
        
        when (period) {
            TimePeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            TimePeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
            TimePeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.timeInMillis
        
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
            isLoading = false
        }
    }
    
    val isNumberlessMode = user?.numberlessMode ?: false
    
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
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
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
            NumberlessProgressView(Modifier.padding(padding))
        } else {
            DetailedProgressView(
                modifier = Modifier.padding(padding),
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it },
                periodStats = periodStats,
                dailySummaries = dailySummaries,
                isLoading = isLoading,
                navController = navController
            )
        }
    }
}

@Composable
fun NumberlessProgressView(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Avatar/Mascot
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(PrimaryGreen.copy(alpha = 0.2f), RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌱",
                fontSize = 80.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "¡Te sientes radiante!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Qualitative messages
        QualitativeCard(
            title = "Estado de hoy",
            message = stringResource(R.string.balanced_diet),
            color = PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QualitativeCard(
            title = "Energía",
            message = stringResource(R.string.high_energy),
            color = ProteinColor
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QualitativeCard(
            title = "Consejo suave",
            message = "Hoy has consumido mucho azúcar natural de la fruta. Para mantener tus niveles de energía, considera un snack rico en proteínas.",
            color = WarningYellow
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Wellness trends (without numbers)
        Text(
            text = "Tendencias de Bienestar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SimpleTrendCard("Hidratación", "Mejorando ↗", PrimaryGreen)
        Spacer(modifier = Modifier.height(12.dp))
        SimpleTrendCard("Diversidad Dietética", "Estable →", ProteinColor)
        Spacer(modifier = Modifier.height(12.dp))
        SimpleTrendCard("Consistencia", "Excelente ✓", PrimaryGreen)
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
    navController: NavController
) {
    val scrollState = rememberScrollState()
    
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
                text = "Semana",
                selected = selectedPeriod == TimePeriod.WEEK,
                onClick = { onPeriodChange(TimePeriod.WEEK) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = "Mes",
                selected = selectedPeriod == TimePeriod.MONTH,
                onClick = { onPeriodChange(TimePeriod.MONTH) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = "Año",
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
            val periodName = when (selectedPeriod) {
                TimePeriod.WEEK -> "Semanal"
                TimePeriod.MONTH -> "Mensual"
                TimePeriod.YEAR -> "Anual"
            }
            
            Text(
                text = "Resumen $periodName",
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
                        "Promedio diario", 
                        "${periodStats.avgCalories.toInt()} kcal", 
                        PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow(
                        "Días en objetivo", 
                        "${periodStats.daysOnTarget} / ${periodStats.totalDays}", 
                        ProteinColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow(
                        "Proteína promedio", 
                        "${periodStats.avgProteinG.toInt()}g", 
                        ProteinColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Macro distribution
            Text(
                text = "Distribución de Macros ($periodName)",
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
                    "Proteínas", 
                    "${periodStats.proteinPercentage}%", 
                    ProteinColor, 
                    Modifier.weight(1f)
                )
                MacroDistributionCard(
                    "Grasas", 
                    "${periodStats.fatPercentage}%", 
                    FatColor, 
                    Modifier.weight(1f)
                )
                MacroDistributionCard(
                    "Carbos", 
                    "${periodStats.carbPercentage}%", 
                    CarbColor, 
                    Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Daily summaries list
            Text(
                text = "Historial Diario",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            dailySummaries.forEach { summary ->
                DailySummaryCard(
                    summary = summary,
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
                        text = "No hay datos para este período",
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
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    
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
                        text = "${summary.mealsCount} comidas registradas",
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
                        text = "$caloriePercentage% del objetivo",
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
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "G",
                    current = summary.totalFatG,
                    goal = summary.fatGoal,
                    color = FatColor,
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "C",
                    current = summary.totalCarbG,
                    goal = summary.carbGoal,
                    color = CarbColor,
                    modifier = Modifier.weight(1f)
                )
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
    modifier: Modifier = Modifier
) {
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
                text = "${current.toInt()}g",
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

