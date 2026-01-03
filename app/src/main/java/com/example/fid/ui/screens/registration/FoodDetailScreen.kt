package com.example.fid.ui.screens.registration

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.FoodItem
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import com.example.fid.utils.UnitConverter
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(navController: NavController, foodId: Long) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var foodItem by remember { mutableStateOf<FoodItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<com.example.fid.data.database.entities.User?>(null) }
    
    // Obtener la preferencia de unidad del usuario
    val measurementUnit = user?.measurementUnit ?: "metric"
    
    // Inicializar unidad y cantidad según el sistema del usuario
    var selectedUnit by remember { mutableStateOf("grams") }
    var amount by remember { mutableStateOf("100") }
    
    // Cargar usuario y ajustar unidad/cantidad inicial
    LaunchedEffect(Unit) {
        user = repository.getCurrentUser()
        val unit = user?.measurementUnit ?: "metric"
        if (unit == "imperial") {
            selectedUnit = "ounces"
            // Convertir 100g a onzas (100g ≈ 3.5 oz)
            amount = "%.1f".format(100f * UnitConverter.G_TO_OZ)
        } else {
            selectedUnit = "grams"
            amount = "100"
        }
    }
    
    // Obtener datos del alimento desde Firestore
    LaunchedEffect(foodId) {
        android.util.Log.d("FoodDetailScreen", "FoodDetailScreen iniciado con foodId: $foodId")
        isLoading = true
        val retrievedItem = repository.getFoodItemById(foodId)
        android.util.Log.d("FoodDetailScreen", "Alimento recuperado de repository: ${retrievedItem != null}")
        foodItem = retrievedItem
        if (foodItem == null) {
            android.util.Log.e("FoodDetailScreen", "ERROR: foodItem es null después de obtenerlo")
        } else {
            foodItem?.let { item ->
                android.util.Log.d("FoodDetailScreen", "Alimento obtenido:")
                android.util.Log.d("FoodDetailScreen", "  - ID: ${item.id}")
                android.util.Log.d("FoodDetailScreen", "  - name: '${item.name}'")
                android.util.Log.d("FoodDetailScreen", "  - nameEs: '${item.nameEs}'")
                android.util.Log.d("FoodDetailScreen", "  - nameEn: '${item.nameEn}'")
                android.util.Log.d("FoodDetailScreen", "  - getLocalizedName: '${item.getLocalizedName(context)}'")
            }
        }
        isLoading = false
    }
    
    // Usar datos del alimento o valores por defecto
    // Obtener el nombre localizado directamente - se recalcula automáticamente cuando foodItem cambia
    val foodName = foodItem?.let { item ->
        val localized = item.getLocalizedName(context)
        when {
            localized.isNotBlank() -> localized
            item.nameEs.isNotBlank() -> item.nameEs
            item.nameEn.isNotBlank() -> item.nameEn
            item.name.isNotBlank() -> item.name
            else -> "Alimento"
        }
    } ?: ""
    val caloriesPer100g = foodItem?.caloriesPer100g ?: 0f
    val proteinPer100g = foodItem?.proteinPer100g ?: 0f
    val fatPer100g = foodItem?.fatPer100g ?: 0f
    val carbPer100g = foodItem?.carbPer100g ?: 0f
    val verificationLevel = foodItem?.verificationLevel ?: "user"
    
    val amountFloat = amount.toFloatOrNull() ?: 100f
    
    // Convertir la cantidad ingresada a gramos para los cálculos
    val amountInGrams = when (selectedUnit) {
        "ounces" -> UnitConverter.convertGramsToMetric(amountFloat, "imperial")
        "grams" -> amountFloat
        else -> amountFloat
    }
    
    val multiplier = amountInGrams / 100f
    
    val totalCalories = caloriesPer100g * multiplier
    val totalProtein = proteinPer100g * multiplier
    val totalFat = fatPer100g * multiplier
    val totalCarbs = carbPer100g * multiplier
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.food_detail),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else if (foodItem == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.food_not_found),
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Food name
                // Log para depuración
                LaunchedEffect(foodName) {
                    android.util.Log.d("FoodDetailScreen", "foodName actualizado: '$foodName'")
                }
                Text(
                    text = foodName.ifBlank { "Alimento" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Verification badge
                VerificationBadge(verificationLevel)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Amount input
                Text(
                    text = stringResource(R.string.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    
                    // Unit selector - muestra gramos u onzas según el sistema del usuario
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, DarkCard, RoundedCornerShape(4.dp))
                                .clickable { expanded = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (selectedUnit == "grams") stringResource(R.string.grams) 
                                       else stringResource(R.string.ounces),
                                color = TextPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            // Siempre permitir cambiar entre gramos y onzas
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.grams)) },
                                onClick = {
                                    // Si estaba en onzas, convertir el valor a gramos
                                    if (selectedUnit == "ounces") {
                                        val currentValue = amount.toFloatOrNull() ?: 0f
                                        val gramsValue = UnitConverter.convertGramsToMetric(currentValue, "imperial")
                                        amount = "%.1f".format(gramsValue)
                                    }
                                    selectedUnit = "grams"
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ounces)) },
                                onClick = {
                                    // Si estaba en gramos, convertir el valor a onzas
                                    if (selectedUnit == "grams") {
                                        val currentValue = amount.toFloatOrNull() ?: 0f
                                        val ouncesValue = UnitConverter.convertGrams(currentValue, "imperial")
                                        amount = "%.1f".format(ouncesValue)
                                    }
                                    selectedUnit = "ounces"
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Nutritional information
                Text(
                    text = stringResource(R.string.nutritional_information),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Total calories card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calories),
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${totalCalories.toInt()} kcal",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Macros
                val measurementUnit = user?.measurementUnit ?: "metric"
                MacroInfoRow(stringResource(R.string.proteins), totalProtein, ProteinColor, measurementUnit)
                Spacer(modifier = Modifier.height(12.dp))
                MacroInfoRow(stringResource(R.string.fats), totalFat, FatColor, measurementUnit)
                Spacer(modifier = Modifier.height(12.dp))
                MacroInfoRow(stringResource(R.string.carbs), totalCarbs, CarbColor, measurementUnit)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Add button
                Button(
                onClick = {
                    scope.launch {
                        try {
                            val user = repository.getCurrentUser()
                            val currentFoodItem = foodItem // Variable local para smart cast
                            if (user != null && currentFoodItem != null) {
                                // Convertir la cantidad a gramos antes de guardar
                                val finalAmountGrams = when (selectedUnit) {
                                    "ounces" -> UnitConverter.convertGramsToMetric(amountFloat, "imperial")
                                    "grams" -> amountFloat
                                    else -> amountFloat
                                }
                                
                                val foodEntry = FoodEntry(
                                    userId = user.id,
                                    foodName = currentFoodItem.nameEs.ifBlank { foodName }, // Fallback a español
                                    foodNameEs = currentFoodItem.nameEs,
                                    foodNameEn = currentFoodItem.nameEn,
                                    amountGrams = finalAmountGrams,
                                    calories = totalCalories,
                                    proteinG = totalProtein,
                                    fatG = totalFat,
                                    carbG = totalCarbs,
                                    mealType = getCurrentMealType(),
                                    registrationMethod = "manual",
                                    verificationLevel = verificationLevel
                                )
                                
                                repository.insertFoodEntry(foodEntry)
                                
                                // Marcar el alimento como usado
                                repository.markFoodAsUsed(foodId)
                                
                                android.util.Log.d("FoodDetailScreen", "Alimento agregado: $foodName")
                                
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.success_food_added),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                navController.popBackStack()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_food_info),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FoodDetailScreen", "Error agregando alimento: ${e.message}", e)
                            Toast.makeText(context, context.getString(R.string.error_saving, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_to_log),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            }
        }
    }
}

@Composable
fun VerificationBadge(level: String) {
    val (text, color) = when (level) {
        "government" -> stringResource(R.string.verified_government) to PrimaryGreen
        "manufacturer" -> stringResource(R.string.verified_manufacturer) to WarningYellow
        "community" -> stringResource(R.string.verified_community) to ProteinColor
        else -> stringResource(R.string.user_submitted) to TextSecondary
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MacroInfoRow(label: String, amount: Float, color: androidx.compose.ui.graphics.Color, unit: String = "metric") {
    val displayAmount = UnitConverter.convertGrams(amount, unit)
    val unitLabel = UnitConverter.getGramsUnitLabel(unit)
    
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            
            Text(
                text = "${displayAmount.toInt()}$unitLabel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Determina el tipo de comida según la hora del día
 */
private fun getCurrentMealType(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..10 -> "breakfast"
        in 12..15 -> "lunch"
        in 17..22 -> "dinner"
        else -> "snack"
    }
}
