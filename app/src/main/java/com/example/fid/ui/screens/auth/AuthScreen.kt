package com.example.fid.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            } else {
                                android.util.Log.d("AuthScreen", "Usuario encontrado: ${existingUser.name}, navegando a Dashboard")
                                
                                // Usuario existente con objetivos configurados, ir al dashboard
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthScreen", "Error en Google Sign-In: ${e.message}", e)
                        errorMessage = "Error con Google Sign-In: ${e.message}"
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
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Fid",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.welcome_to_fid),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.discover_new_way),
                fontSize = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Google Sign-In button
            OutlinedButton(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(stringResource(R.string.continue_with_google), fontSize = 14.sp)
            }
        }
    }
}

