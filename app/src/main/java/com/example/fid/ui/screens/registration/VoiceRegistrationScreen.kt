package com.example.fid.ui.screens.registration

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import com.example.fid.utils.VoiceRecognitionHelper
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    
    // Voice recognition helper
    val voiceHelper = remember {
        VoiceRecognitionHelper(
            context = context,
            onResult = { text ->
                recognizedText = text
                partialText = ""
                isListening = false
                errorMessage = ""
                Toast.makeText(context, "✓ Reconocido: $text", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                errorMessage = error
                partialText = ""
                isListening = false
                isReady = false
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            },
            onReadyForSpeech = {
                errorMessage = ""
                isReady = true
                Toast.makeText(context, "🎤 Listo, puedes hablar", Toast.LENGTH_SHORT).show()
            },
            onBeginningOfSpeech = {
                errorMessage = ""
                isReady = true
                Toast.makeText(context, "👂 Te estoy escuchando...", Toast.LENGTH_SHORT).show()
            },
            onPartialResult = { partial ->
                partialText = partial
            }
        )
    }
    
    // Solicitar permisos de micrófono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            voiceHelper.startListening()
            isListening = true
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso de micrófono para usar esta función",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Limpiar recursos cuando se desmonta el composable
    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.destroy()
        }
    }
    
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ejemplo: \"200 gramos de pollo a la plancha\"",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                            if (isListening) {
                                // Detener el reconocimiento
                                voiceHelper.stopListening()
                                isListening = false
                            } else {
                                // Iniciar el reconocimiento (solicitar permiso si es necesario)
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                        Column {
                            Text(
                                text = "✓ Reconocido:",
                                color = PrimaryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recognizedText,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else if (partialText.isNotEmpty() && isListening) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "🎤 Escuchando...",
                                color = PrimaryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = partialText,
                                color = TextPrimary.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                } else if (isListening) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isReady) "👂 Hablando..." else "⏳ Iniciando...",
                            color = PrimaryGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Habla claro y cerca del micrófono",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Error message display
                if (errorMessage.isNotEmpty() && !isListening) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "⚠️ Error",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💡 Consejos:\n• Verifica tu conexión a internet\n• Habla claro y cerca del micrófono\n• Reduce el ruido ambiental\n• Asegúrate de tener Google App instalada",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            
            // Confirm button
            if (recognizedText.isNotEmpty()) {
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                val currentUser = Firebase.auth.currentUser
                                if (currentUser == null) {
                                    Toast.makeText(
                                        context,
                                        "Error: Usuario no autenticado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSaving = false
                                    return@launch
                                }
                                
                                // Buscar el usuario en la base de datos para obtener su ID
                                val user = repository.getUserByEmail(currentUser.email ?: "")
                                if (user == null) {
                                    Toast.makeText(
                                        context,
                                        "Error: No se encontró el usuario",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSaving = false
                                    return@launch
                                }
                                
                                // Crear entrada de comida desde el texto reconocido
                                val foodEntry = parseFoodFromText(recognizedText, user.id)
                                
                                // Guardar en Firebase
                                repository.insertFoodEntry(foodEntry)
                                
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.success_food_added),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                isSaving = false
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error al guardar: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isSaving = false
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
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DarkBackground
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.confirm_register),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Función auxiliar para parsear texto reconocido y crear un FoodEntry
 * Intenta extraer información del texto de voz
 */
private fun parseFoodFromText(text: String, userId: Long): FoodEntry {
    val timestamp = System.currentTimeMillis()
    
    // Determinar el tipo de comida basado en la hora del día
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val mealType = when (hour) {
        in 6..10 -> "breakfast"
        in 11..14 -> "lunch"
        in 15..17 -> "snack"
        in 18..22 -> "dinner"
        else -> "snack"
    }
    
    // Intentar extraer cantidades numéricas del texto
    // Ejemplo: "comí 200 gramos de pollo" -> gramos = 200
    val numberRegex = "\\d+".toRegex()
    val numbers = numberRegex.findAll(text).map { it.value.toIntOrNull() ?: 0 }.toList()
    
    val gramos = numbers.firstOrNull() ?: 100 // Por defecto 100g si no se especifica
    
    // Crear el FoodEntry con valores por defecto
    // En una implementación más avanzada, se podría integrar con una API 
    // de nutrición o una base de datos de alimentos
    return FoodEntry(
        id = timestamp,
        userId = userId,
        foodName = text.trim(),
        amountGrams = gramos.toFloat(),
        calories = 0f, // Valores por defecto - se pueden calcular después
        proteinG = 0f,
        fatG = 0f,
        carbG = 0f,
        mealType = mealType,
        registrationMethod = "voice",
        verificationLevel = "user",
        timestamp = timestamp
    )
}
