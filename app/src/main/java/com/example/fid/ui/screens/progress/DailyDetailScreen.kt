package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import com.example.fid.utils.UnitConverter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(navController: NavController, date: Long) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var summary by remember { mutableStateOf<DailySummary?>(null) }
    var foodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(date) {
        user = repository.getCurrentUser()
        user?.let { u ->
            // Calcular inicio y fin del día
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000
            
            // Obtener resumen
            summary = repository.getDailySummary(u.id, startOfDay)
            
            // Si no existe resumen, calcularlo
            if (summary == null) {
                repository.calculateAndSaveDailySummary(u.id, startOfDay)
                summary = repository.getDailySummary(u.id, startOfDay)
            }
            
            // Obtener entradas de comida
            repository.getFoodEntriesByDateRange(u.id, startOfDay, endOfDay).collect { entries ->
                foodEntries = entries.sortedBy { it.timestamp }
                isLoading = false
            }
        }
    }
    
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.day_detail),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = dateFormatter.format(Date(date)),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary card
                summary?.let { s ->
                    // Calories ring
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            val progress = if (s.calorieGoal > 0) {
                                (s.totalCalories / s.calorieGoal).coerceIn(0f, 1f)
                            } else 0f
                            
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(180.dp),
                                color = PrimaryGreen,
                                strokeWidth = 16.dp,
                                trackColor = DarkSurface,
                                strokeCap = StrokeCap.Round
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${s.totalCalories.toInt()}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "/ ${s.calorieGoal.toInt()} kcal",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Macros
                    Text(
                        text = stringResource(R.string.macronutrients),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val measurementUnit = user?.measurementUnit ?: "metric"
                    
                    MacroProgressBar(
                        label = stringResource(R.string.proteins),
                        current = s.totalProteinG,
                        goal = s.proteinGoal,
                        color = ProteinColor,
                        measurementUnit = measurementUnit
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MacroProgressBar(
                        label = stringResource(R.string.fats),
                        current = s.totalFatG,
                        goal = s.fatGoal,
                        color = FatColor,
                        measurementUnit = measurementUnit
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MacroProgressBar(
                        label = stringResource(R.string.carbs),
                        current = s.totalCarbG,
                        goal = s.carbGoal,
                        color = CarbColor,
                        measurementUnit = measurementUnit
                    )
                    
                    // Wellness info (sleep and water)
                    if (s.sleepHours > 0f || s.waterIntakeMl > 0f) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.wellness_index),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (s.sleepHours > 0f) {
                                WellnessInfoCard(
                                    emoji = "😴",
                                    label = stringResource(R.string.sleep),
                                    value = String.format(Locale.getDefault(), "%.1f h", s.sleepHours),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (s.waterIntakeMl > 0f) {
                                WellnessInfoCard(
                                    emoji = "💧",
                                    label = stringResource(R.string.hydration),
                                    value = String.format(Locale.getDefault(), "%.1f L", s.waterIntakeMl / 1000f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Food entries
                Text(
                    text = stringResource(R.string.meals_count, foodEntries.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (foodEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(DarkCard, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🍽️",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_meals_registered),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    val measurementUnit = user?.measurementUnit ?: "metric"
                    foodEntries.forEach { entry ->
                        FoodEntryDetailCard(entry, timeFormatter, measurementUnit)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    current: Float,
    goal: Float,
    color: androidx.compose.ui.graphics.Color,
    measurementUnit: String = "metric"
) {
    val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
    val displayCurrent = UnitConverter.convertGrams(current, measurementUnit)
    val displayGoal = UnitConverter.convertGrams(goal, measurementUnit)
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = "${displayCurrent.toInt()}$unitLabel / ${displayGoal.toInt()}$unitLabel",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = DarkCard,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun FoodEntryDetailCard(entry: FoodEntry, timeFormatter: SimpleDateFormat, unit: String = "metric") {
    val context = LocalContext.current
    val unitLabel = UnitConverter.getGramsUnitLabel(unit)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getMealEmoji(entry.mealType),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = entry.getLocalizedFoodName(context),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = getMealTypeNameLocalized(entry.mealType),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NutrientChip("${entry.calories.toInt()} kcal", PrimaryGreen)
                    NutrientChip("P: ${UnitConverter.convertGrams(entry.proteinG, unit).toInt()}$unitLabel", ProteinColor)
                    NutrientChip("G: ${UnitConverter.convertGrams(entry.fatG, unit).toInt()}$unitLabel", FatColor)
                    NutrientChip("C: ${UnitConverter.convertGrams(entry.carbG, unit).toInt()}$unitLabel", CarbColor)
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeFormatter.format(Date(entry.timestamp)),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${UnitConverter.convertGrams(entry.amountGrams, unit).toInt()}$unitLabel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun NutrientChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun getMealEmoji(mealType: String): String {
    return when (mealType) {
        "breakfast" -> "🌅"
        "lunch" -> "☀️"
        "dinner" -> "🌙"
        "snack" -> "🍎"
        else -> "🍽️"
    }
}

@Composable
fun getMealTypeNameLocalized(mealType: String): String {
    return when (mealType) {
        "breakfast" -> stringResource(R.string.meal_breakfast)
        "lunch" -> stringResource(R.string.meal_lunch)
        "dinner" -> stringResource(R.string.meal_dinner)
        "snack" -> stringResource(R.string.meal_snack)
        else -> stringResource(R.string.meal_food)
    }
}

@Composable
fun WellnessInfoCard(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
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

