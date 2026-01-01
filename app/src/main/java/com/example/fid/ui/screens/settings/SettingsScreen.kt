package com.example.fid.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.MainActivity
import com.example.fid.R
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.example.fid.utils.LocaleHelper
import com.example.fid.utils.UnitConverter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var user by remember { mutableStateOf<User?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showMeasurementUnitDialog by remember { mutableStateOf(false) }
    var showPersonalDataDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(LocaleHelper.getLanguage(context)) }
    
    LaunchedEffect(Unit) {
        user = repository.getCurrentUser()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
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
        bottomBar = {
            com.example.fid.ui.screens.dashboard.BottomNavigationBar(navController)
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Profile section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(PrimaryGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name?.firstOrNull()?.uppercase() ?: "U"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = user?.name ?: stringResource(R.string.default_user_name),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = user?.email ?: "",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // My Account Section
            SectionTitle(stringResource(R.string.my_account))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(stringResource(R.string.personal_data)) {
                showPersonalDataDialog = true
            }
            SettingsItem(stringResource(R.string.my_goals)) {
                navController.navigate(Screen.GoalSetup.route)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Preferences Section
            SectionTitle(stringResource(R.string.app_preferences))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(stringResource(R.string.my_foods)) {
                navController.navigate(Screen.CustomFoods.route)
            }
            
            SettingsToggleItem(
                title = stringResource(R.string.numberless_mode_toggle),
                description = stringResource(R.string.numberless_mode_description),
                checked = user?.numberlessMode ?: false,
                onCheckedChange = { enabled ->
                    scope.launch {
                        user?.let { u ->
                            val updatedUser = u.copy(numberlessMode = enabled)
                            repository.updateUser(updatedUser)
                            user = updatedUser
                            Toast.makeText(
                                context,
                                if (enabled) context.getString(R.string.numberless_mode_enabled) else context.getString(R.string.numberless_mode_disabled),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            
            // Language selector
            SettingsItemWithValue(
                title = stringResource(R.string.language),
                value = LocaleHelper.getLanguageDisplayName(currentLanguage),
                onClick = { showLanguageDialog = true }
            )
            
            // Measurement units selector
            val measurementUnitDisplay = when (user?.measurementUnit) {
                "imperial" -> "Imperial (lb, ft)"
                else -> "Métrico (kg, cm)"
            }
            SettingsItemWithValue(
                title = stringResource(R.string.measurement_units),
                value = measurementUnitDisplay,
                onClick = { showMeasurementUnitDialog = true }
            )
            SettingsItem(stringResource(R.string.notifications)) {
                navController.navigate(Screen.NotificationSettings.route)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Support Section
            SectionTitle(stringResource(R.string.support_legal))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(stringResource(R.string.faq)) {
                showFaqDialog = true
            }
            SettingsItem(stringResource(R.string.contact)) {
                showContactDialog = true
            }
            SettingsItem(stringResource(R.string.privacy_policy)) {
                showPrivacyDialog = true
            }
            SettingsItem(stringResource(R.string.terms_of_service)) {
                showTermsDialog = true
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sign out button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.2f),
                    contentColor = ErrorRed
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.sign_out),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Sign out confirmation dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.sign_out),
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.sign_out_confirmation),
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            scope.launch {
                                try {
                                    // Cerrar sesión en Firebase Auth
                                    Firebase.auth.signOut()
                                    
                                    // Cerrar sesión en Google Sign-In
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInClient.signOut()
                                    
                                    // Navegar a onboarding
                                    navController.navigate(Screen.Onboarding.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.error_sign_out, e.message), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.sign_out),
                            color = ErrorRed
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = PrimaryGreen
                        )
                    }
                },
                containerColor = DarkCard
            )
        }
        
        // Language selection dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.select_language),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        LocaleHelper.getSupportedLanguages().forEach { (code, displayName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (code != currentLanguage) {
                                            LocaleHelper.setLanguage(context, code)
                                            currentLanguage = code
                                            showLanguageDialog = false
                                            
                                            // Show toast and restart activity
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.language_changed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            
                                            // Restart the activity to apply the new locale
                                            val activity = context as? Activity
                                            activity?.let {
                                                val intent = Intent(it, MainActivity::class.java)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                it.startActivity(intent)
                                                it.finish()
                                            }
                                        } else {
                                            showLanguageDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = code == currentLanguage,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryGreen,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = displayName,
                                    fontSize = 16.sp,
                                    color = if (code == currentLanguage) PrimaryGreen else TextPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = PrimaryGreen
                        )
                    }
                },
                containerColor = DarkCard
            )
        }
        
        // Measurement Unit selection dialog
        if (showMeasurementUnitDialog) {
            AlertDialog(
                onDismissRequest = { showMeasurementUnitDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.select_measurement_unit),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        listOf(
                            "metric" to R.string.metric_system,
                            "imperial" to R.string.imperial_system
                        ).forEach { (unitCode, stringRes) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            user?.let { u ->
                                                val updatedUser = u.copy(measurementUnit = unitCode)
                                                repository.updateUser(updatedUser)
                                                user = updatedUser
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.measurement_unit_changed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        showMeasurementUnitDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = user?.measurementUnit == unitCode,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryGreen,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(stringRes),
                                    fontSize = 16.sp,
                                    color = if (user?.measurementUnit == unitCode) PrimaryGreen else TextPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMeasurementUnitDialog = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = PrimaryGreen
                        )
                    }
                },
                containerColor = DarkCard
            )
        }
        
        // Personal Data Dialog
        if (showPersonalDataDialog) {
            PersonalDataDialog(
                user = user,
                onDismiss = { showPersonalDataDialog = false },
                onSave = { name, age, height, weight ->
                    scope.launch {
                        user?.let { u ->
                            val updatedUser = u.copy(
                                name = name,
                                age = age,
                                heightCm = height,
                                currentWeightKg = weight
                            )
                            repository.updateUser(updatedUser)
                            user = updatedUser
                            Toast.makeText(context, context.getString(R.string.personal_data_saved), Toast.LENGTH_SHORT).show()
                        }
                    }
                    showPersonalDataDialog = false
                }
            )
        }
        
        // FAQ Dialog
        if (showFaqDialog) {
            FaqDialog(onDismiss = { showFaqDialog = false })
        }
        
        // Contact Dialog
        if (showContactDialog) {
            ContactDialog(
                onDismiss = { showContactDialog = false },
                onSendEmail = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:soporte@fid-app.com")
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.contact_subject))
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                    showContactDialog = false
                }
            )
        }
        
        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }
        
        // Terms of Service Dialog
        if (showTermsDialog) {
            TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary
    )
}

@Composable
fun SettingsItem(title: String, onClick: () -> Unit) {
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
                fontSize = 16.sp,
                color = TextPrimary
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String? = null,
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
                    color = TextPrimary
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SettingsItemWithValue(title: String, value: String, onClick: () -> Unit) {
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
                fontSize = 16.sp,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PersonalDataDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int, height: Float, weight: Float) -> Unit
) {
    val measurementUnit = user?.measurementUnit ?: "metric"
    
    // Convert initial values to user's measurement unit
    val initialWeight = user?.currentWeightKg?.let { 
        UnitConverter.convertWeight(it, measurementUnit) 
    } ?: 0f
    
    // For height in imperial, calculate feet and inches separately
    val initialHeightCm = user?.heightCm ?: 0f
    val initialFeet: Int
    val initialInches: Int
    if (measurementUnit == "imperial" && initialHeightCm > 0) {
        val totalInches = initialHeightCm * UnitConverter.CM_TO_INCHES
        initialFeet = (totalInches / 12).toInt()
        initialInches = (totalInches % 12).toInt()
    } else {
        initialFeet = 0
        initialInches = 0
    }
    
    var name by remember { mutableStateOf(user?.name ?: "") }
    var age by remember { mutableStateOf(user?.age?.toString() ?: "") }
    // For metric: single height field in cm
    var heightCm by remember { mutableStateOf(if (measurementUnit == "metric" && initialHeightCm > 0) initialHeightCm.toInt().toString() else "") }
    // For imperial: feet and inches fields
    var heightFeet by remember { mutableStateOf(if (initialFeet > 0) initialFeet.toString() else "") }
    var heightInches by remember { mutableStateOf(if (initialFeet > 0 || initialInches > 0) initialInches.toString() else "") }
    var weight by remember { mutableStateOf(if (initialWeight > 0) "%.1f".format(initialWeight) else "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_personal_data),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
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
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.age)) },
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
                
                // Height input - different for metric vs imperial
                if (measurementUnit == "imperial") {
                    // Imperial: Two fields for feet and inches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = heightFeet,
                            onValueChange = { heightFeet = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.height_ft)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = PrimaryGreen,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        OutlinedTextField(
                            value = heightInches,
                            onValueChange = { heightInches = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.height_in)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = PrimaryGreen,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                    }
                } else {
                    // Metric: Single field for cm
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.height_cm)) },
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
                }
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { 
                        val weightLabel = if (measurementUnit == "imperial") stringResource(R.string.weight_lb) else stringResource(R.string.current_weight_kg)
                        Text(weightLabel) 
                    },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ageInt = age.toIntOrNull() ?: 25
                    val weightInput = weight.toFloatOrNull() ?: if (measurementUnit == "imperial") 154f else 70f
                    
                    // Calculate height in cm
                    val finalHeightCm = if (measurementUnit == "imperial") {
                        // Convert feet + inches to cm
                        val feet = heightFeet.toIntOrNull() ?: 5
                        val inches = heightInches.toIntOrNull() ?: 7
                        val totalInches = (feet * 12) + inches
                        totalInches / UnitConverter.CM_TO_INCHES
                    } else {
                        heightCm.toFloatOrNull() ?: 170f
                    }
                    
                    // Convert weight back to metric for saving
                    val weightKg = UnitConverter.convertWeightToMetric(weightInput, measurementUnit)
                    
                    onSave(name, ageInt, finalHeightCm, weightKg)
                }
            ) {
                Text(stringResource(R.string.save), color = PrimaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.faq_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FaqItem(
                    question = stringResource(R.string.faq_q1),
                    answer = stringResource(R.string.faq_a1)
                )
                FaqItem(
                    question = stringResource(R.string.faq_q2),
                    answer = stringResource(R.string.faq_a2)
                )
                FaqItem(
                    question = stringResource(R.string.faq_q3),
                    answer = stringResource(R.string.faq_a3)
                )
                FaqItem(
                    question = stringResource(R.string.faq_q4),
                    answer = stringResource(R.string.faq_a4)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = PrimaryGreen)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun FaqItem(question: String, answer: String) {
    Column {
        Text(
            text = question,
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ContactDialog(
    onDismiss: () -> Unit,
    onSendEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.contact_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.contact_description),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.contact_email),
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSendEmail) {
                Text(stringResource(R.string.send_email), color = PrimaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.privacy_policy_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy_content),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = PrimaryGreen)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.terms_of_service_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.terms_of_service_content),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = PrimaryGreen)
            }
        },
        containerColor = DarkCard
    )
}
