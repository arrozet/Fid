package com.example.fid.ui.screens.registration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.snap_it),
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
        when {
            !cameraPermissionState.status.isGranted -> {
                // Permission denied screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Permiso de cámara requerido",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para tomar fotos de tus comidas, necesitamos acceso a la cámara",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            )
                        ) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }
            capturedImage != null -> {
                // Show confirmation screen with the captured image
                FoodConfirmationScreen(
                    image = capturedImage!!,
                    onConfirm = { foodName, amount, calories, protein, fat, carbs, mealType ->
                        // Save to database and navigate back
                        scope.launch {
                            saveFoodEntry(
                                context = context,
                                repository = repository,
                                foodName = foodName,
                                amount = amount,
                                calories = calories,
                                protein = protein,
                                fat = fat,
                                carbs = carbs,
                                mealType = mealType,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Comida registrada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    Toast.makeText(
                                        context,
                                        "Error: $error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    },
                    onRetake = {
                        capturedImage = null
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            else -> {
                // Show camera preview
                CameraPreviewScreen(
                    modifier = Modifier.padding(padding),
                    onImageCaptured = { bitmap ->
                        capturedImage = bitmap
                    },
                    onError = { error ->
                        Toast.makeText(
                            context,
                            "Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPreviewScreen(
    modifier: Modifier = Modifier,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    
    // Setup camera when composable launches
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = getCameraProvider(context)
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay instructions
        if (!isCapturing) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = DarkCard.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Enfoca tu comida",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
        
        // Capture button
        if (!isCapturing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(40.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        isCapturing = true
                        captureImage(
                            context = context,
                            imageCapture = imageCapture,
                            onImageCaptured = { bitmap ->
                                isCapturing = false
                                onImageCaptured(bitmap)
                            },
                            onError = { error ->
                                isCapturing = false
                                onError(error)
                            }
                        )
                    },
                    containerColor = PrimaryGreen,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Tomar foto",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else {
            // Show capturing indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(DarkCard.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                CircularProgressIndicator(
                    color = PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodConfirmationScreen(
    image: Bitmap,
    onConfirm: (String, Float, Float, Float, Float, Float, String) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("snack") }
    var showMealTypeMenu by remember { mutableStateOf(false) }
    
    val mealTypes = listOf(
        "breakfast" to "Desayuno",
        "lunch" to "Almuerzo",
        "dinner" to "Cena",
        "snack" to "Snack"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Captured image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Foto capturada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Retake button
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryGreen
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tomar otra foto")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info text
        Text(
            text = "Ingresa los detalles de tu comida:",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Food name
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Nombre de la comida") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Meal type dropdown
        ExposedDropdownMenuBox(
            expanded = showMealTypeMenu,
            onExpandedChange = { showMealTypeMenu = it }
        ) {
            OutlinedTextField(
                value = mealTypes.find { it.first == selectedMealType }?.second ?: "Snack",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de comida") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMealTypeMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            ExposedDropdownMenu(
                expanded = showMealTypeMenu,
                onDismissRequest = { showMealTypeMenu = false }
            ) {
                mealTypes.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedMealType = value
                            showMealTypeMenu = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Amount
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Cantidad (gramos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Macronutrientes:",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Calories
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text("Calorías (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Protein
            OutlinedTextField(
                value = protein,
                onValueChange = { protein = it },
                label = { Text("Proteína (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            // Fat
            OutlinedTextField(
                value = fat,
                onValueChange = { fat = it },
                label = { Text("Grasa (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Carbs
        OutlinedTextField(
            value = carbs,
            onValueChange = { carbs = it },
            label = { Text("Carbohidratos (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancelar")
            }
            
            Button(
                onClick = {
                    val amountFloat = amount.toFloatOrNull() ?: 0f
                    val caloriesFloat = calories.toFloatOrNull() ?: 0f
                    val proteinFloat = protein.toFloatOrNull() ?: 0f
                    val fatFloat = fat.toFloatOrNull() ?: 0f
                    val carbsFloat = carbs.toFloatOrNull() ?: 0f
                    
                    if (foodName.isNotEmpty() && amountFloat > 0) {
                        onConfirm(
                            foodName,
                            amountFloat,
                            caloriesFloat,
                            proteinFloat,
                            fatFloat,
                            carbsFloat,
                            selectedMealType
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                enabled = foodName.isNotEmpty() && amount.toFloatOrNull() != null && amount.toFloatOrNull()!! > 0
            ) {
                Text("Guardar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture?,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(Exception("ImageCapture not initialized"))
        return
    }
    
    val photoFile = File(
        context.cacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )
    
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    
                    // Rotate bitmap if needed (camera might capture in different orientation)
                    val rotatedBitmap = rotateBitmapIfNeeded(bitmap)
                    
                    onImageCaptured(rotatedBitmap)
                    photoFile.delete() // Clean up
                } catch (e: Exception) {
                    onError(e)
                }
            }
            
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap): Bitmap {
    // Most cameras capture in landscape, so we might need to rotate
    // For simplicity, we'll just return the bitmap as is
    // In a production app, you'd want to check EXIF data
    return bitmap
}

private suspend fun saveFoodEntry(
    context: Context,
    repository: FirebaseRepository,
    foodName: String,
    amount: Float,
    calories: Float,
    protein: Float,
    fat: Float,
    carbs: Float,
    mealType: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onError("Usuario no autenticado")
            return
        }
        
        val user = repository.getUserByEmail(currentUser.email ?: "")
        if (user == null) {
            onError("No se encontró el usuario")
            return
        }
        
        val foodEntry = FoodEntry(
            userId = user.id,
            foodName = foodName,
            amountGrams = amount,
            calories = calories,
            proteinG = protein,
            fatG = fat,
            carbG = carbs,
            mealType = mealType,
            registrationMethod = "photo",
            verificationLevel = "user",
            timestamp = System.currentTimeMillis()
        )
        
        repository.insertFoodEntry(foodEntry)
        onSuccess()
        
    } catch (e: Exception) {
        onError(e.message ?: "Error desconocido")
    }
}

// Helper function to get CameraProvider using coroutines
private suspend fun getCameraProvider(context: Context): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            continuation.resume(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(context))
    }
}
