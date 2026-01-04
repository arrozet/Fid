package com.example.fid.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.fid.R
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Animación del logo
    var animationStarted by remember { mutableStateOf(false) }
    val titleOffset by animateFloatAsState(
        targetValue = if (animationStarted) -12f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 550, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoOffset by animateFloatAsState(
        targetValue = if (animationStarted) 0f else 20f,
        animationSpec = tween(durationMillis = 550, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "logoOffset"
    )
    
    LaunchedEffect(Unit) {
        animationStarted = true
    }
    
    // Manejar botón atrás para volver a InitialSetup
    BackHandler {
        navController.navigate(Screen.InitialSetup.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
        }
    }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                
                scope.launch {
                    isLoading = true
                    try {
                        val authResult = auth.signInWithCredential(credential).await()
                        val firebaseUser = authResult.user
                        
                        if (firebaseUser != null) {
                            val userEmail = firebaseUser.email ?: ""
                            val userName = firebaseUser.displayName ?: "Usuario"
                            
                            android.util.Log.d("AuthScreen", "Google Sign-In exitoso: $userName ($userEmail)")
                            
                            // Buscar usuario existente en Firestore por email
                            val existingUser = repository.getUserByEmail(userEmail)
                            
                            if (existingUser == null) {
                                android.util.Log.d("AuthScreen", "Usuario NO encontrado en Firestore, creando nuevo")
                                
                                // Nuevo usuario de Google, crear usuario en Firestore
                                val newUser = User(
                                    id = System.currentTimeMillis(),
                                    email = userEmail,
                                    name = userName,
                                    age = 25,
                                    gender = "male",
                                    heightCm = 170f,
                                    currentWeightKg = 70f,
                                    targetWeightKg = null,
                                    activityLevel = "moderate",
                                    goal = "maintain_weight",
                                    tdee = 2000f,
                                    proteinGoalG = 150f,
                                    fatGoalG = 65f,
                                    carbGoalG = 250f,
                                    numberlessMode = false,
                                    measurementUnit = "metric"
                                )
                                repository.insertUser(newUser)
                                
                                // Ir a configuración de objetivos para personalizar
                                navController.navigate(Screen.GoalSetup.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            } else {
                                android.util.Log.d("AuthScreen", "Usuario encontrado: ${existingUser.name}, navegando a Dashboard")
                                
                                // Usuario existente con objetivos configurados, ir al dashboard
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthScreen", "Error en Google Sign-In: ${e.message}", e)
                        errorMessage = "Error con Google Sign-In: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                errorMessage = context.getString(R.string.error_google_signin_failed, e.message)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Header con logo y título animados
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Fid",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        modifier = Modifier.offset(x = titleOffset.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Image(
                        painter = painterResource(id = R.drawable.logo_fid),
                        contentDescription = "Fid Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .offset(x = logoOffset.dp)
                            .alpha(logoAlpha)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.discover_new_way),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            
            // Features section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.CameraAlt,
                    text = stringResource(R.string.effortless_ai_registration)
                )
                FeatureCard(
                    icon = Icons.Default.Psychology,
                    text = stringResource(R.string.dynamic_personalized_goals)
                )
                FeatureCard(
                    icon = Icons.Default.Favorite,
                    text = stringResource(R.string.healthy_conscious_relationship)
                )
            }
            
            // Bottom section con botón y error
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Google Sign-In button mejorado
                Button(
                    onClick = {
                        errorMessage = null
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DarkBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.continue_with_google),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

