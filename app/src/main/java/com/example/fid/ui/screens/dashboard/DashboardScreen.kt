package com.example.fid.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
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
    var showAddMenu by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis
    val endOfDay = startOfDay + 24 * 60 * 60 * 1000
    
    LaunchedEffect(Unit) {
        user = repository.getCurrentUser()
        user?.let { u ->
            // Calcular y guardar resumen diario actual
            scope.launch {
                repository.calculateAndSaveDailySummary(u.id, System.currentTimeMillis())
            }
            
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
                    text = "Macronutrientes",
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
                    text = "Índice de Bienestar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WellnessCard(
                        title = stringResource(R.string.hydration),
                        value = "2.5 L",
                        modifier = Modifier.weight(1f)
                    )
                    
                    WellnessCard(
                        title = stringResource(R.string.sleep),
                        value = "7.5 h",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Recent meals
                if (foodEntries.isNotEmpty()) {
                    Text(
                        text = "Comidas de Hoy",
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(20.dp))
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
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
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
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
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
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
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
            icon = { Text("🏠", fontSize = 24.sp) },
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
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = DarkCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        
        NavigationBarItem(
            icon = { Text("📊", fontSize = 24.sp) },
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
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = DarkCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        
        NavigationBarItem(
            icon = { Text("⚙️", fontSize = 24.sp) },
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
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = DarkCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

