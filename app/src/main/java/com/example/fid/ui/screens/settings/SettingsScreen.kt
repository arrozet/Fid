package com.example.fid.ui.screens.settings

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
import com.example.fid.R
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
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
                            text = user?.name ?: "Usuario",
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
                Toast.makeText(context, "Personal Data", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.my_goals)) {
                navController.navigate(Screen.GoalSetup.route)
            }
            SettingsItem(stringResource(R.string.change_password)) {
                Toast.makeText(context, "Change Password", Toast.LENGTH_SHORT).show()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Preferences Section
            SectionTitle(stringResource(R.string.app_preferences))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsToggleItem(
                title = stringResource(R.string.numberless_mode_toggle),
                description = "Oculta los números de calorías",
                checked = user?.numberlessMode ?: false,
                onCheckedChange = { enabled ->
                    scope.launch {
                        user?.let { u ->
                            val updatedUser = u.copy(numberlessMode = enabled)
                            repository.updateUser(updatedUser)
                            user = updatedUser
                            Toast.makeText(
                                context,
                                if (enabled) "Modo sin números activado" else "Modo sin números desactivado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            
            SettingsItem(stringResource(R.string.measurement_units)) {
                Toast.makeText(context, "Measurement Units", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.notifications)) {
                Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Connectivity Section
            SectionTitle(stringResource(R.string.connectivity))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(stringResource(R.string.integrate_google_fit)) {
                Toast.makeText(context, "Google Fit Integration", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.integrate_apple_health)) {
                Toast.makeText(context, "Apple Health Integration (iOS)", Toast.LENGTH_SHORT).show()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Support Section
            SectionTitle(stringResource(R.string.support_legal))
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(stringResource(R.string.faq)) {
                Toast.makeText(context, "FAQ", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.contact)) {
                Toast.makeText(context, "Contact", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.privacy_policy)) {
                Toast.makeText(context, "Privacy Policy", Toast.LENGTH_SHORT).show()
            }
            SettingsItem(stringResource(R.string.terms_of_service)) {
                Toast.makeText(context, "Terms of Service", Toast.LENGTH_SHORT).show()
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
                        text = "¿Estás seguro de que quieres cerrar sesión?",
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
                                    Toast.makeText(context, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
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
