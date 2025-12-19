package com.example.fid.ui.screens.registration

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_animation"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.voice_registration),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = stringResource(R.string.say_what_you_ate),
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Microphone button
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            if (isListening) PrimaryGreen.copy(alpha = 0.2f) else DarkCard,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            isListening = !isListening
                            if (isListening) {
                                Toast.makeText(
                                    context,
                                    "Escuchando...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(120.dp)
                            .scale(if (isListening) scale else 1f)
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = 60.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Recognized text display
                if (recognizedText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = recognizedText,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                } else if (isListening) {
                    Text(
                        text = "Hablando...",
                        color = PrimaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Confirm button
            if (recognizedText.isNotEmpty()) {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.success_food_added),
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = stringResource(R.string.confirm_register),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
