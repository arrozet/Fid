package com.example.fid.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import com.example.fid.utils.NotificationScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val notificationScheduler = remember { NotificationScheduler(context) }
    
    // Notification preferences - específicas por usuario
    val prefs = remember { 
        context.getSharedPreferences(
            NotificationScheduler.getPreferencesName(context), 
            android.content.Context.MODE_PRIVATE
        ) 
    }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("enabled", true)) }
    var mealRemindersEnabled by remember { mutableStateOf(prefs.getBoolean("meal_reminders", true)) }
    var hydrationRemindersEnabled by remember { mutableStateOf(prefs.getBoolean("hydration_reminders", true)) }
    var dailySummaryEnabled by remember { mutableStateOf(prefs.getBoolean("daily_summary", true)) }
    
    // Times
    var breakfastTime by remember { mutableStateOf(prefs.getString("breakfast_time", "08:00") ?: "08:00") }
    var lunchTime by remember { mutableStateOf(prefs.getString("lunch_time", "13:00") ?: "13:00") }
    var dinnerTime by remember { mutableStateOf(prefs.getString("dinner_time", "20:00") ?: "20:00") }
    var dailySummaryTime by remember { mutableStateOf(prefs.getString("daily_summary_time", "21:00") ?: "21:00") }
    
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var currentTimePickerType by remember { mutableStateOf("") }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            prefs.edit().putBoolean("enabled", true).apply()
            notificationScheduler.scheduleAllNotifications()
        }
    }
    
    // Check permission on launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission && notificationsEnabled) {
                notificationsEnabled = false
            }
        }
    }
    
    fun savePreference(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    fun savePreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Master Toggle
            NotificationToggleItem(
                title = stringResource(R.string.enable_notifications),
                description = stringResource(R.string.enable_notifications_desc),
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Check permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                notificationsEnabled = true
                                savePreference("enabled", true)
                                scope.launch {
                                    android.util.Log.d("NotificationSettings", "Activando todas las notificaciones...")
                                    notificationScheduler.scheduleAllNotifications()
                                    android.util.Log.d("NotificationSettings", "Notificaciones activadas")
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            notificationsEnabled = true
                            savePreference("enabled", true)
                            scope.launch {
                                android.util.Log.d("NotificationSettings", "Activando todas las notificaciones...")
                                notificationScheduler.scheduleAllNotifications()
                                android.util.Log.d("NotificationSettings", "Notificaciones activadas")
                            }
                        }
                    } else {
                        notificationsEnabled = false
                        savePreference("enabled", false)
                        scope.launch {
                            android.util.Log.d("NotificationSettings", "Desactivando todas las notificaciones...")
                            notificationScheduler.cancelAllNotifications()
                        }
                    }
                }
            )
            
            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Meal Reminders Section
                SectionTitle(stringResource(R.string.meal_reminders))
                Spacer(modifier = Modifier.height(12.dp))
                
                NotificationToggleItem(
                    title = stringResource(R.string.meal_reminders),
                    description = stringResource(R.string.meal_reminders_desc),
                    checked = mealRemindersEnabled,
                    onCheckedChange = { enabled ->
                        mealRemindersEnabled = enabled
                        savePreference("meal_reminders", enabled)
                        scope.launch {
                            if (enabled) {
                                android.util.Log.d("NotificationSettings", "Programando recordatorios de comidas...")
                                notificationScheduler.scheduleMealReminders()
                                android.util.Log.d("NotificationSettings", "Recordatorios de comidas programados")
                            } else {
                                android.util.Log.d("NotificationSettings", "Cancelando recordatorios de comidas...")
                                notificationScheduler.cancelMealReminders()
                            }
                        }
                    }
                )
                
                if (mealRemindersEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TimePickerItem(
                        title = stringResource(R.string.breakfast_time),
                        time = breakfastTime,
                        onClick = {
                            currentTimePickerType = "breakfast"
                            showTimePickerDialog = true
                        }
                    )
                    
                    TimePickerItem(
                        title = stringResource(R.string.lunch_time),
                        time = lunchTime,
                        onClick = {
                            currentTimePickerType = "lunch"
                            showTimePickerDialog = true
                        }
                    )
                    
                    TimePickerItem(
                        title = stringResource(R.string.dinner_time),
                        time = dinnerTime,
                        onClick = {
                            currentTimePickerType = "dinner"
                            showTimePickerDialog = true
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Hydration Reminders
                SectionTitle(stringResource(R.string.hydration))
                Spacer(modifier = Modifier.height(12.dp))
                
                NotificationToggleItem(
                    title = stringResource(R.string.hydration_reminders),
                    description = stringResource(R.string.hydration_reminders_desc),
                    checked = hydrationRemindersEnabled,
                    onCheckedChange = { enabled ->
                        hydrationRemindersEnabled = enabled
                        savePreference("hydration_reminders", enabled)
                        if (enabled) {
                            notificationScheduler.scheduleHydrationReminders()
                        } else {
                            notificationScheduler.cancelHydrationReminders()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Daily Summary
                SectionTitle(stringResource(R.string.daily_summary))
                Spacer(modifier = Modifier.height(12.dp))
                
                NotificationToggleItem(
                    title = stringResource(R.string.daily_summary),
                    description = stringResource(R.string.daily_summary_desc),
                    checked = dailySummaryEnabled,
                    onCheckedChange = { enabled ->
                        dailySummaryEnabled = enabled
                        savePreference("daily_summary", enabled)
                        if (enabled) {
                            notificationScheduler.scheduleDailySummary()
                        } else {
                            notificationScheduler.cancelDailySummary()
                        }
                    }
                )
                
                if (dailySummaryEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TimePickerItem(
                        title = stringResource(R.string.summary_time),
                        time = dailySummaryTime,
                        onClick = {
                            currentTimePickerType = "summary"
                            showTimePickerDialog = true
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test Notification Button
            if (notificationsEnabled) {
                Button(
                    onClick = {
                        scope.launch {
                            android.util.Log.d("NotificationSettings", "Probando notificación...")
                            val notificationHelper = com.example.fid.utils.NotificationHelper(context)
                            notificationHelper.showMealReminder("breakfast")
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.test_notification_sent),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.test_notification),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Time Picker Dialog
    if (showTimePickerDialog) {
        TimePickerDialog(
            currentTime = when (currentTimePickerType) {
                "breakfast" -> breakfastTime
                "lunch" -> lunchTime
                "dinner" -> dinnerTime
                "summary" -> dailySummaryTime
                else -> "12:00"
            },
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selectedTime ->
                when (currentTimePickerType) {
                    "breakfast" -> {
                        breakfastTime = selectedTime
                        savePreference("breakfast_time", selectedTime)
                        notificationScheduler.scheduleMealReminders()
                    }
                    "lunch" -> {
                        lunchTime = selectedTime
                        savePreference("lunch_time", selectedTime)
                        notificationScheduler.scheduleMealReminders()
                    }
                    "dinner" -> {
                        dinnerTime = selectedTime
                        savePreference("dinner_time", selectedTime)
                        notificationScheduler.scheduleMealReminders()
                    }
                    "summary" -> {
                        dailySummaryTime = selectedTime
                        savePreference("daily_summary_time", selectedTime)
                        notificationScheduler.scheduleDailySummary()
                    }
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryGreen,
                    checkedTrackColor = PrimaryGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun TimePickerItem(
    title: String,
    time: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = time,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val timeParts = currentTime.split(":")
    val initialHour = timeParts[0].toIntOrNull() ?: 12
    val initialMinute = timeParts[1].toIntOrNull() ?: 0
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = stringResource(R.string.select_time),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = DarkBackground,
                    selectorColor = PrimaryGreen,
                    containerColor = DarkCard,
                    periodSelectorBorderColor = PrimaryGreen,
                    clockDialSelectedContentColor = DarkBackground,
                    clockDialUnselectedContentColor = TextSecondary,
                    periodSelectorSelectedContainerColor = PrimaryGreen,
                    periodSelectorUnselectedContainerColor = DarkBackground,
                    periodSelectorSelectedContentColor = DarkBackground,
                    periodSelectorUnselectedContentColor = TextPrimary,
                    timeSelectorSelectedContainerColor = PrimaryGreen,
                    timeSelectorUnselectedContainerColor = DarkBackground,
                    timeSelectorSelectedContentColor = DarkBackground,
                    timeSelectorUnselectedContentColor = TextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = String.format("%02d", timePickerState.hour)
                    val minute = String.format("%02d", timePickerState.minute)
                    onConfirm("$hour:$minute")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = DarkBackground
                )
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

