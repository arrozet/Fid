package com.example.fid.ui.screens.auth

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
import com.example.fid.data.database.FidDatabase
import com.example.fid.data.repository.FidRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FidRepository(FidDatabase.getDatabase(context)) }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
                                    errorMessage = "User already exists"
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
                    text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement Google sign in */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Google", fontSize = 14.sp)
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Implement Apple sign in */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Apple", fontSize = 14.sp)
                }
            }
        }
    }
}

