package com.example.fid.ui.screens.registration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.fid.data.ai.FoodAnalysisResult
import com.example.fid.data.ai.GrokAIService
import com.example.fid.data.database.entities.AIIngredient
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.FoodItem
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

/**
 * Estados posibles de la pantalla de registro por foto
 */
sealed class PhotoScreenState {
    object Camera : PhotoScreenState()
    object Analyzing : PhotoScreenState()
    data class Confirmation(val image: Bitmap, val analysisResult: FoodAnalysisResult?) : PhotoScreenState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val grokService = remember { GrokAIService.getInstance() }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var screenState by remember { mutableStateOf<PhotoScreenState>(PhotoScreenState.Camera) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
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
                            text = stringResource(R.string.camera_permission_required),
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.camera_permission_explanation),
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
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
            }
            screenState is PhotoScreenState.Confirmation -> {
                val confirmationState = screenState as PhotoScreenState.Confirmation
                // Show confirmation screen with the captured image and AI analysis
                FoodConfirmationScreen(
                    image = confirmationState.image,
                    analysisResult = confirmationState.analysisResult,
                    onConfirm = { foodName, foodNameEs, foodNameEn, amount, calories, protein, fat, carbs, mealType ->
                        // Save to database and navigate back
                        scope.launch {
                            saveFoodEntry(
                                context = context,
                                repository = repository,
                                foodName = foodName,
                                foodNameEs = foodNameEs,
                                foodNameEn = foodNameEn,
                                amount = amount,
                                calories = calories,
                                protein = protein,
                                fat = fat,
                                carbs = carbs,
                                mealType = mealType,
                                analysisResult = confirmationState.analysisResult,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.food_registered_success),
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
                        screenState = PhotoScreenState.Camera
                        currentBitmap = null
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            screenState is PhotoScreenState.Analyzing -> {
                // Show analyzing screen
                AIAnalyzingScreen(
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                // Show camera preview
                CameraPreviewScreen(
                    modifier = Modifier.padding(padding),
                    onImageCaptured = { bitmap ->
                        currentBitmap = bitmap
                        // Start AI analysis
                        if (grokService.isConfigured()) {
                            screenState = PhotoScreenState.Analyzing
                            scope.launch {
                                val result = grokService.analyzeFood(bitmap)
                                screenState = PhotoScreenState.Confirmation(bitmap, result)
                            }
                        } else {
                            // If AI is not configured, go directly to confirmation without analysis
                            screenState = PhotoScreenState.Confirmation(bitmap, null)
                            Toast.makeText(
                                context,
                                "IA no configurada. Configura GROK_API_KEY en .env",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
                        text = stringResource(R.string.focus_your_food),
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
                        contentDescription = stringResource(R.string.take_photo),
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

/**
 * Pantalla de análisis con IA - muestra mientras se procesa la imagen
 */
@Composable
fun AIAnalyzingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de IA
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CircularProgressIndicator(
                color = PrimaryGreen,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.analyzing_food_ai),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.analyzing_food_description),
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodConfirmationScreen(
    image: Bitmap,
    analysisResult: FoodAnalysisResult?,
    onConfirm: (String, String, String, Float, Float, Float, Float, Float, String) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit
) {
    // Pre-fill with AI analysis if available
    var foodName by remember { mutableStateOf(analysisResult?.foodName ?: "") }
    var foodNameEs by remember { mutableStateOf(analysisResult?.foodNameEs ?: "") }
    var foodNameEn by remember { mutableStateOf(analysisResult?.foodNameEn ?: "") }
    var amount by remember { mutableStateOf(analysisResult?.totalEstimatedGrams?.takeIf { it > 0 }?.toString() ?: "") }
    var calories by remember { mutableStateOf(analysisResult?.totalCalories?.takeIf { it > 0 }?.toString() ?: "") }
    var protein by remember { mutableStateOf(analysisResult?.totalProteinG?.takeIf { it > 0 }?.toString() ?: "") }
    var fat by remember { mutableStateOf(analysisResult?.totalFatG?.takeIf { it > 0 }?.toString() ?: "") }
    var carbs by remember { mutableStateOf(analysisResult?.totalCarbsG?.takeIf { it > 0 }?.toString() ?: "") }
    var selectedMealType by remember { mutableStateOf(analysisResult?.suggestedMealType ?: "snack") }
    var showMealTypeMenu by remember { mutableStateOf(false) }
    var showIngredientsDialog by remember { mutableStateOf(false) }
    
    val breakfastLabel = stringResource(R.string.meal_breakfast)
    val lunchLabel = stringResource(R.string.meal_lunch)
    val dinnerLabel = stringResource(R.string.meal_dinner)
    val snackLabel = stringResource(R.string.meal_snack)
    
    val mealTypes = listOf(
        "breakfast" to breakfastLabel,
        "lunch" to lunchLabel,
        "dinner" to dinnerLabel,
        "snack" to snackLabel
    )
    
    // Show ingredients dialog if analysis has ingredients
    if (showIngredientsDialog && analysisResult != null && analysisResult.ingredients.isNotEmpty()) {
        IngredientsDialog(
            ingredients = analysisResult.ingredients,
            onDismiss = { showIngredientsDialog = false }
        )
    }
    
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
                contentDescription = stringResource(R.string.captured_photo),
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
            Text(stringResource(R.string.retake_photo))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI Analysis Banner
        if (analysisResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (analysisResult.success) PrimaryGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (analysisResult.success) PrimaryGreen else ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (analysisResult.success) 
                                stringResource(R.string.ai_analysis_complete) 
                            else 
                                stringResource(R.string.ai_analysis_failed),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (analysisResult.success && analysisResult.confidence.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.ai_confidence, analysisResult.confidence),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (!analysisResult.success && analysisResult.errorMessage != null) {
                            Text(
                                text = analysisResult.errorMessage,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    // Show ingredients button if available
                    if (analysisResult.success && analysisResult.ingredients.isNotEmpty()) {
                        TextButton(onClick = { showIngredientsDialog = true }) {
                            Text(
                                text = stringResource(R.string.view_ingredients),
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Info text
        Text(
            text = if (analysisResult?.success == true) 
                stringResource(R.string.review_ai_details) 
            else 
                stringResource(R.string.enter_food_details),
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Food name
        OutlinedTextField(
            value = foodName,
            onValueChange = { 
                foodName = it
                // Also update localized names if user is typing
                if (foodNameEs.isBlank()) foodNameEs = it
                if (foodNameEn.isBlank()) foodNameEn = it
            },
            label = { Text(stringResource(R.string.food_name_label)) },
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
                value = mealTypes.find { it.first == selectedMealType }?.second ?: snackLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.meal_type_label)) },
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
            label = { Text(stringResource(R.string.amount_grams_label)) },
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
            text = stringResource(R.string.macronutrients_label),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Calories
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text(stringResource(R.string.calories_kcal_label)) },
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
                label = { Text(stringResource(R.string.protein_g_label)) },
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
                label = { Text(stringResource(R.string.fat_g_label)) },
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
            label = { Text(stringResource(R.string.carbs_g_label)) },
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
                Text(stringResource(R.string.cancel))
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
                            foodNameEs.ifBlank { foodName },
                            foodNameEn.ifBlank { foodName },
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
                Text(stringResource(R.string.save))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Diálogo que muestra los ingredientes detectados por la IA
 */
@Composable
fun IngredientsDialog(
    ingredients: List<com.example.fid.data.ai.IngredientAnalysis>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ingredients_detected),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ingredients.forEachIndexed { index, ingredient ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = ingredient.name.ifBlank { ingredient.nameEs.ifBlank { ingredient.nameEn } },
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${ingredient.estimatedGrams.toInt()}g",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${ingredient.calories.toInt()} kcal",
                                    color = PrimaryGreen,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text(
                                    text = "P: ${ingredient.proteinG.toInt()}g",
                                    color = ProteinColor,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "F: ${ingredient.fatG.toInt()}g",
                                    color = FatColor,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "C: ${ingredient.carbsG.toInt()}g",
                                    color = CarbColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    if (index < ingredients.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    color = PrimaryGreen
                )
            }
        }
    )
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
                    // Read EXIF data before rotating
                    val exif = ExifInterface(photoFile.absolutePath)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    
                    // Rotate bitmap based on EXIF orientation
                    val rotatedBitmap = rotateBitmapIfNeeded(bitmap, orientation)
                    
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

private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap // No rotation needed
    }
    
    return try {
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        bitmap.recycle() // Free original bitmap memory
        rotatedBitmap
    } catch (e: Exception) {
        bitmap // Return original if rotation fails
    }
}

private suspend fun saveFoodEntry(
    context: Context,
    repository: FirebaseRepository,
    foodName: String,
    foodNameEs: String,
    foodNameEn: String,
    amount: Float,
    calories: Float,
    protein: Float,
    fat: Float,
    carbs: Float,
    mealType: String,
    analysisResult: FoodAnalysisResult?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onError(context.getString(R.string.error_user_not_authenticated))
            return
        }
        
        val user = repository.getUserByEmail(currentUser.email ?: "")
        if (user == null) {
            onError(context.getString(R.string.error_user_not_found))
            return
        }
        
        // Si la IA analizó exitosamente, guardar como comida personalizada
        if (analysisResult?.success == true && amount > 0) {
            // Calcular macros por 100g basándose en la cantidad total
            val caloriesPer100g = (calories / amount) * 100f
            val proteinPer100g = (protein / amount) * 100f
            val fatPer100g = (fat / amount) * 100f
            val carbPer100g = (carbs / amount) * 100f
            
            val customFoodItem = FoodItem(
                nameEs = foodNameEs.ifBlank { foodName },
                nameEn = foodNameEn.ifBlank { foodName },
                name = foodName,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                fatPer100g = fatPer100g,
                carbPer100g = carbPer100g,
                verificationLevel = "ai",
                createdByUserId = user.id, // Marca como comida personalizada del usuario
                isFrequent = false,
                lastUsed = System.currentTimeMillis()
            )
            
            // Guardar la comida personalizada (se reutilizará si ya existe)
            repository.insertFoodItem(customFoodItem)
        }
        
        // Convert AI ingredients to AIIngredient entities
        val aiIngredients = analysisResult?.ingredients?.map { ingredient ->
            AIIngredient(
                name = ingredient.name,
                nameEs = ingredient.nameEs,
                nameEn = ingredient.nameEn,
                estimatedGrams = ingredient.estimatedGrams,
                calories = ingredient.calories,
                proteinG = ingredient.proteinG,
                fatG = ingredient.fatG,
                carbsG = ingredient.carbsG
            )
        } ?: emptyList()
        
        // Guardar el registro de consumo
        val foodEntry = FoodEntry(
            userId = user.id,
            foodName = foodName,
            foodNameEs = foodNameEs,
            foodNameEn = foodNameEn,
            amountGrams = amount,
            calories = calories,
            proteinG = protein,
            fatG = fat,
            carbG = carbs,
            mealType = mealType,
            registrationMethod = if (analysisResult?.success == true) "photo_ai" else "photo",
            verificationLevel = if (analysisResult?.success == true) "ai" else "user",
            timestamp = System.currentTimeMillis(),
            aiIngredients = aiIngredients,
            aiConfidence = analysisResult?.confidence ?: "",
            aiAnalyzed = analysisResult?.success == true
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
