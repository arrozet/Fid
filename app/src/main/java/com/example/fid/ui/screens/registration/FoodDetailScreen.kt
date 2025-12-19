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
import com.example.fid.data.database.FidDatabase
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.repository.FidRepository
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(navController: NavController, foodId: Long) {
    val context = LocalContext.current
    val repository = remember { FidRepository(FidDatabase.getDatabase(context)) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var amount by remember { mutableStateOf("100") }
    var selectedUnit by remember { mutableStateOf("grams") }
    
    // Mock food data - in a real app, this would come from the repository
    val foodName = "Pollo a la plancha"
    val caloriesPer100g = 165f
    val proteinPer100g = 31f
    val fatPer100g = 3.6f
    val carbPer100g = 0f
    val verificationLevel = "government"
    
    val amountFloat = amount.toFloatOrNull() ?: 100f
    val multiplier = when (selectedUnit) {
        "grams" -> amountFloat / 100f
        "unit" -> 1f
        else -> amountFloat / 100f
    }
    
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
            // Food name
            Text(
                text = foodName,
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
                
                // Unit selector
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
                                   else stringResource(R.string.unit),
                            color = TextPrimary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.grams)) },
                            onClick = {
                                selectedUnit = "grams"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.unit)) },
                            onClick = {
                                selectedUnit = "unit"
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Nutritional information
            Text(
                text = "Información Nutricional",
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
            MacroInfoRow("Proteínas", totalProtein, ProteinColor)
            Spacer(modifier = Modifier.height(12.dp))
            MacroInfoRow("Grasas", totalFat, FatColor)
            Spacer(modifier = Modifier.height(12.dp))
            MacroInfoRow("Carbohidratos", totalCarbs, CarbColor)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Add button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val user = repository.getCurrentUser()
                            if (user != null) {
                                val foodEntry = FoodEntry(
                                    userId = user.id,
                                    foodName = foodName,
                                    amountGrams = amountFloat,
                                    calories = totalCalories,
                                    proteinG = totalProtein,
                                    fatG = totalFat,
                                    carbG = totalCarbs,
                                    mealType = "snack",
                                    registrationMethod = "manual",
                                    verificationLevel = verificationLevel
                                )
                                
                                repository.insertFoodEntry(foodEntry)
                                
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.success_food_added),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
fun MacroInfoRow(label: String, amount: Float, color: androidx.compose.ui.graphics.Color) {
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
                text = "${amount.toInt()}g",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

