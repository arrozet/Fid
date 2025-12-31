package com.example.fid.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
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
                text = if (isSignUp) stringResource(R.string.create_account) 
                       else stringResource(R.string.sign_in),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = PrimaryGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = PrimaryGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!isSignUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { /* TODO: Implement forgot password */ }) {
                        Text(
                            text = stringResource(R.string.forgot_password),
                            color = PrimaryGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main action button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = context.getString(R.string.error_empty_field)
                        return@Button
                    }
                    
                    if (password.length < 6) {
                        errorMessage = context.getString(R.string.error_invalid_password)
                        return@Button
                    }
                    
                    scope.launch {
                        try {
                            val existingUser = repository.getUserByEmail(email)
                            if (isSignUp) {
                                if (existingUser != null) {
                                    errorMessage = context.getString(R.string.error_user_already_exists)
                                } else {
                                    navController.navigate(Screen.GoalSetup.route)
                                }
                            } else {
                                if (existingUser != null) {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                } else {
                                    errorMessage = context.getString(R.string.error_login_failed)
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isSignUp) stringResource(R.string.create_account) 
                           else stringResource(R.string.sign_in),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle between sign in and sign up
            Row {
                Text(
                    text = if (isSignUp) stringResource(R.string.already_have_account_question) + " " 
                           else stringResource(R.string.dont_have_account) + " ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = { isSignUp = !isSignUp },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isSignUp) stringResource(R.string.sign_in) 
                               else stringResource(R.string.create_account),
                        color = PrimaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Social login options
            Text(
                text = stringResource(R.string.or_continue_with),
                color = TextSecondary,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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

