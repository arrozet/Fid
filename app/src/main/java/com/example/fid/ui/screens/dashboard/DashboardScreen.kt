package com.example.fid.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var user by remember { mutableStateOf<User?>(null) }
    var foodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var wellnessEntry by remember { mutableStateOf<WellnessEntry?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis
    val endOfDay = startOfDay + 24 * 60 * 60 * 1000
    
    // Función para recargar wellness
    fun refreshWellness(userId: Long) {
        scope.launch {
            wellnessEntry = repository.getTodayWellnessEntry(userId)
        }
    }
    
    LaunchedEffect(Unit) {
        user = repository.getCurrentUser()
        user?.let { u ->
            // Calcular y guardar resumen diario actual
            scope.launch {
                repository.calculateAndSaveDailySummary(u.id, System.currentTimeMillis())
            }
            
            // Cargar wellness entry del día
            wellnessEntry = repository.getTodayWellnessEntry(u.id)
            
            repository.getFoodEntriesByDateRange(u.id, startOfDay, endOfDay).collect { entries ->
                foodEntries = entries
            }
        }
    }
    
    val totalCalories = foodEntries.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = foodEntries.sumOf { it.proteinG.toDouble() }.toFloat()
    val totalFat = foodEntries.sumOf { it.fatG.toDouble() }.toFloat()
    val totalCarbs = foodEntries.sumOf { it.carbG.toDouble() }.toFloat()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.hello, user?.name ?: "User"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = PrimaryGreen,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
            }
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calories Ring
                CaloriesRing(
                    consumed = totalCalories,
                    goal = user?.tdee ?: 2000f
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Macronutrients
                Text(
                    text = stringResource(R.string.macronutrients),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                MacroProgressBar(
                    label = stringResource(R.string.proteins),
                    current = totalProtein,
                    goal = user?.proteinGoalG ?: 150f,
                    color = ProteinColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MacroProgressBar(
                    label = stringResource(R.string.fats),
                    current = totalFat,
                    goal = user?.fatGoalG ?: 65f,
                    color = FatColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MacroProgressBar(
                    label = stringResource(R.string.carbs),
                    current = totalCarbs,
                    goal = user?.carbGoalG ?: 250f,
                    color = CarbColor
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Wellness Index
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
                    // Tarjeta de Hidratación (clickeable)
                    val waterLiters = (wellnessEntry?.waterIntakeMl ?: 0f) / 1000f
                    val waterGoalLiters = (user?.waterGoalMl ?: 2500f) / 1000f
                    HydrationCard(
                        currentLiters = waterLiters,
                        goalLiters = waterGoalLiters,
                        onClick = { showWaterDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Tarjeta de Sueño (clickeable)
                    SleepCard(
                        sleepHours = wellnessEntry?.sleepHours ?: 0f,
                        onClick = { showSleepDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Recent meals
                if (foodEntries.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.todays_meals),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    foodEntries.forEach { entry ->
                        FoodEntryCard(entry)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            // Add Menu Modal
            if (showAddMenu) {
                AddFoodMenu(
                    onDismiss = { showAddMenu = false },
                    onPhotoClick = {
                        showAddMenu = false
                        navController.navigate(Screen.PhotoRegistration.route)
                    },
                    onVoiceClick = {
                        showAddMenu = false
                        navController.navigate(Screen.VoiceRegistration.route)
                    },
                    onManualClick = {
                        showAddMenu = false
                        navController.navigate(Screen.ManualRegistration.route)
                    }
                )
            }
            
            // Dialog de Hidratación
            if (showWaterDialog) {
                WaterIntakeDialog(
                    currentMl = wellnessEntry?.waterIntakeMl ?: 0f,
                    goalMl = user?.waterGoalMl ?: 2500f,
                    onDismiss = { showWaterDialog = false },
                    onAddWater = { amountMl ->
                        user?.let { u ->
                            scope.launch {
                                repository.addWaterIntake(u.id, amountMl)
                                refreshWellness(u.id)
                                Toast.makeText(context, context.getString(R.string.water_added), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showWaterDialog = false
                    },
                    onReset = {
                        user?.let { u ->
                            scope.launch {
                                repository.resetWaterIntake(u.id)
                                refreshWellness(u.id)
                                Toast.makeText(context, context.getString(R.string.water_reset), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showWaterDialog = false
                    },
                    onChangeGoal = { newGoalMl ->
                        user?.let { u ->
                            scope.launch {
                                val updatedUser = u.copy(waterGoalMl = newGoalMl)
                                repository.updateUser(updatedUser)
                                user = updatedUser
                                Toast.makeText(context, context.getString(R.string.goal_updated), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            
            // Dialog de Sueño
            if (showSleepDialog) {
                SleepLogDialog(
                    currentHours = wellnessEntry?.sleepHours ?: 0f,
                    onDismiss = { showSleepDialog = false },
                    onSaveSleep = { hours ->
                        user?.let { u ->
                            scope.launch {
                                repository.setSleepHours(u.id, hours)
                                refreshWellness(u.id)
                                Toast.makeText(context, context.getString(R.string.sleep_logged), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showSleepDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun CaloriesRing(consumed: Float, goal: Float) {
    val progress = (consumed / goal).coerceIn(0f, 1f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(DarkCard, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
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
                    text = "${consumed.toInt()}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                Text(
                    text = "/ ${goal.toInt()} ${stringResource(R.string.calories)}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.calories_remaining),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${(goal - consumed).coerceAtLeast(0f).toInt()} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun MacroProgressBar(label: String, current: Float, goal: Float, color: androidx.compose.ui.graphics.Color) {
    val progress = (current / goal).coerceIn(0f, 1f)
    
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
                text = "${current.toInt()}g / ${goal.toInt()}g",
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
fun HydrationCard(
    currentLiters: Float,
    goalLiters: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentLiters / goalLiters).coerceIn(0f, 1f)
    val displayValue = if (currentLiters > 0) {
        String.format(Locale.getDefault(), "%.1f L", currentLiters)
    } else {
        stringResource(R.string.tap_to_add)
    }
    
    Box(
        modifier = modifier
            .height(120.dp)
            .background(DarkCard, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hydration),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "💧",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayValue,
                fontSize = if (currentLiters > 0) 22.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentLiters > 0) PrimaryGreen else TextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            // Barra de progreso con objetivo al final
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryGreen,
                    trackColor = DarkSurface,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%.1fL", goalLiters),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SleepCard(
    sleepHours: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayValue = if (sleepHours > 0) {
        String.format(Locale.getDefault(), "%.1f h", sleepHours)
    } else {
        stringResource(R.string.tap_to_add)
    }
    
    // Color basado en calidad de sueño
    val sleepColor = when {
        sleepHours == 0f -> TextSecondary
        sleepHours < 6f -> ErrorRed       // Rojo - muy poco sueño
        sleepHours < 7f -> FatColor       // Amarillo - casi óptimo
        sleepHours > 9f -> FatColor       // Amarillo - mucho sueño
        else -> PrimaryGreen              // Verde - óptimo (7-9h)
    }
    
    // Mensaje y emoji según horas de sueño
    val (sleepMessage, sleepEmoji) = when {
        sleepHours == 0f -> "" to ""
        sleepHours < 6f -> stringResource(R.string.sleep_improve) to "⚠️"
        sleepHours < 7f -> stringResource(R.string.sleep_almost) to "💪"
        sleepHours > 9f -> stringResource(R.string.sleep_too_much) to "💤"
        else -> stringResource(R.string.sleep_excellent) to "✓"
    }
    
    Box(
        modifier = modifier
            .height(120.dp)
            .background(DarkCard, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sleep),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "😴",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayValue,
                fontSize = if (sleepHours > 0) 22.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = sleepColor
            )
            if (sleepHours > 0) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$sleepEmoji $sleepMessage",
                    fontSize = 11.sp,
                    color = sleepColor
                )
            }
        }
    }
}

@Composable
fun WaterIntakeDialog(
    currentMl: Float,
    goalMl: Float,
    onDismiss: () -> Unit,
    onAddWater: (Float) -> Unit,
    onReset: () -> Unit,
    onChangeGoal: (Float) -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var showGoalInput by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf(goalMl.toInt().toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = stringResource(R.string.add_water),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Mostrar consumo actual y objetivo
                Text(
                    text = stringResource(R.string.water_intake_today),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f", currentMl),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = String.format(Locale.getDefault(), " / %.0f ml", goalMl),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                when {
                    showGoalInput -> {
                        // Input para cambiar objetivo
                        Text(
                            text = stringResource(R.string.water_goal_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = goalAmount,
                            onValueChange = { goalAmount = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.enter_water_goal)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            suffix = { Text("ml", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = PrimaryGreen,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Chips de selección rápida para objetivo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2000, 2500, 3000).forEach { ml ->
                                FilterChip(
                                    selected = goalAmount == ml.toString(),
                                    onClick = { goalAmount = ml.toString() },
                                    label = { Text("${ml/1000f}L", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen,
                                        selectedLabelColor = DarkBackground,
                                        containerColor = DarkCard,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGoalInput = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            
                            Button(
                                onClick = {
                                    goalAmount.toFloatOrNull()?.let { onChangeGoal(it) }
                                    showGoalInput = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen,
                                    contentColor = DarkBackground
                                ),
                                enabled = goalAmount.isNotEmpty()
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                    showCustomInput -> {
                        // Input personalizado
                        OutlinedTextField(
                            value = customAmount,
                            onValueChange = { customAmount = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.enter_water_ml)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = PrimaryGreen,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomInput = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            
                            Button(
                                onClick = {
                                    customAmount.toFloatOrNull()?.let { onAddWater(it) }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen,
                                    contentColor = DarkBackground
                                ),
                                enabled = customAmount.isNotEmpty()
                            ) {
                                Text(stringResource(R.string.add_water))
                            }
                        }
                    }
                    else -> {
                        // Botones de añadir rápido
                        Button(
                            onClick = { onAddWater(250f) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.add_glass), fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onAddWater(500f) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.add_bottle), fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { showCustomInput = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.custom_amount))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Botón para cambiar objetivo
                        OutlinedButton(
                            onClick = { showGoalInput = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.change_goal))
                        }
                        
                        // Botón de reiniciar si hay agua registrada
                        if (currentMl > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = onReset,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.reset),
                                    color = FatColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!showCustomInput && !showGoalInput) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun SleepLogDialog(
    currentHours: Float,
    onDismiss: () -> Unit,
    onSaveSleep: (Float) -> Unit
) {
    var sleepInput by remember { mutableStateOf(if (currentHours > 0) currentHours.toString() else "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = stringResource(R.string.log_sleep),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.sleep_last_night),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = sleepInput,
                    onValueChange = { 
                        // Permitir números y punto decimal
                        val filtered = it.filter { c -> c.isDigit() || c == '.' }
                        // Solo un punto decimal
                        if (filtered.count { c -> c == '.' } <= 1) {
                            sleepInput = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.enter_sleep_hours)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("h", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de selección rápida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(6f, 7f, 8f, 9f).forEach { hours ->
                        FilterChip(
                            selected = sleepInput == hours.toString(),
                            onClick = { sleepInput = hours.toString() },
                            label = { Text("${hours.toInt()}h") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkCard,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    sleepInput.toFloatOrNull()?.let { onSaveSleep(it) }
                },
                enabled = sleepInput.isNotEmpty() && sleepInput.toFloatOrNull() != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = DarkBackground
                )
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}

// Mantener WellnessCard por retrocompatibilidad (puede eliminarse después)
@Composable
fun WellnessCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
fun FoodEntryCard(entry: FoodEntry) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.foodName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${entry.amountGrams.toInt()}g • ${entry.calories.toInt()} kcal",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AddFoodMenu(
    onDismiss: () -> Unit,
    onPhotoClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onManualClick: () -> Unit
) {
    // InteractionSources para detectar el estado pressed de cada botón
    val photoInteractionSource = remember { MutableInteractionSource() }
    val voiceInteractionSource = remember { MutableInteractionSource() }
    val manualInteractionSource = remember { MutableInteractionSource() }
    
    val isPhotoPressed by photoInteractionSource.collectIsPressedAsState()
    val isVoicePressed by voiceInteractionSource.collectIsPressedAsState()
    val isManualPressed by manualInteractionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(20.dp))
                .clickable(enabled = false, onClick = {}) // Intercepta clics para que no se propaguen al fondo
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.register_food),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onPhotoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = if (isPhotoPressed) 0.96f else 1f
                        scaleY = if (isPhotoPressed) 0.96f else 1f
                    },
                interactionSource = photoInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPhotoPressed) PrimaryGreenDark else PrimaryGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.snap_it),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = if (isVoicePressed) 0.96f else 1f
                        scaleY = if (isVoicePressed) 0.96f else 1f
                    },
                interactionSource = voiceInteractionSource,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isVoicePressed) PrimaryGreenLight else PrimaryGreen,
                    containerColor = if (isVoicePressed) PrimaryGreen.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.voice_registration),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onManualClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = if (isManualPressed) 0.96f else 1f
                        scaleY = if (isManualPressed) 0.96f else 1f
                    },
                interactionSource = manualInteractionSource,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isManualPressed) PrimaryGreenLight else PrimaryGreen,
                    containerColor = if (isManualPressed) PrimaryGreen.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.manual_registration),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.home), fontSize = 10.sp) },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { 
                if (currentRoute != Screen.Dashboard.route) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreenLight,
                selectedTextColor = PrimaryGreenLight,
                indicatorColor = DarkCard,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            )
        )
        
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.progress), fontSize = 10.sp) },
            selected = currentRoute == Screen.Progress.route,
            onClick = { 
                if (currentRoute != Screen.Progress.route) {
                    navController.navigate(Screen.Progress.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreenLight,
                selectedTextColor = PrimaryGreenLight,
                indicatorColor = DarkCard,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            )
        )
        
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.settings), fontSize = 10.sp) },
            selected = currentRoute == Screen.Settings.route,
            onClick = { 
                if (currentRoute != Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreenLight,
                selectedTextColor = PrimaryGreenLight,
                indicatorColor = DarkCard,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            )
        )
    }
}

