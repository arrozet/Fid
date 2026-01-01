package com.example.fid.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.FoodItem
import com.example.fid.data.repository.FirebaseRepository
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    
    var currentUser by remember { mutableStateOf<com.example.fid.data.database.entities.User?>(null) }
    var customFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var foodToEdit by remember { mutableStateOf<FoodItem?>(null) }
    
    // Obtener usuario actual
    LaunchedEffect(Unit) {
        currentUser = repository.getCurrentUser()
    }
    
    // Obtener comidas personalizadas
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            repository.getCustomFoodItems(user.id).collect { foods ->
                customFoods = foods
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.my_foods),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_custom_food), tint = PrimaryGreen)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customFoods.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_custom_foods),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(customFoods) { food ->
                    CustomFoodCard(
                        food = food,
                        onEdit = {
                            foodToEdit = food
                            showEditDialog = true
                        },
                        onDelete = {
                            foodToDelete = food
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Diálogo para agregar comida personalizada
    if (showAddDialog) {
        AddCustomFoodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { foodItem ->
                scope.launch {
                    currentUser?.let { user ->
                        try {
                            repository.insertCustomFoodItem(foodItem, user.id)
                            android.util.Log.d("CustomFoods", "Comida personalizada creada exitosamente")
                        } catch (e: Exception) {
                            android.util.Log.e("CustomFoods", "Error creando comida personalizada: ${e.message}")
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }
    
    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que quieres eliminar ${foodToDelete!!.getLocalizedName(context)}?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            currentUser?.let { user ->
                                repository.deleteCustomFoodItem(foodToDelete!!.id, user.id)
                            }
                        }
                        showDeleteDialog = false
                        foodToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    foodToDelete = null
                }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
    
    // Diálogo para editar comida personalizada
    if (showEditDialog && foodToEdit != null) {
        EditCustomFoodDialog(
            food = foodToEdit!!,
            onDismiss = {
                showEditDialog = false
                foodToEdit = null
            },
            onConfirm = { updatedFoodItem ->
                scope.launch {
                    currentUser?.let { user ->
                        try {
                            repository.updateCustomFoodItem(updatedFoodItem, user.id)
                            android.util.Log.d("CustomFoods", "Comida personalizada actualizada exitosamente")
                        } catch (e: Exception) {
                            android.util.Log.e("CustomFoods", "Error actualizando comida personalizada: ${e.message}")
                        }
                    }
                }
                showEditDialog = false
                foodToEdit = null
            }
        )
    }
}

@Composable
fun CustomFoodCard(food: FoodItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val localizedName = food.getLocalizedName(context)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${food.caloriesPer100g.toInt()} kcal | ${food.proteinPer100g}g P | ${food.carbPer100g}g C | ${food.fatPer100g}g G",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = PrimaryGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomFoodDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (FoodItem) -> Unit
) {
    var nameEs by remember { mutableStateOf(food.nameEs) }
    var nameEn by remember { mutableStateOf(food.nameEn) }
    var calories by remember { mutableStateOf(food.caloriesPer100g.toString()) }
    var protein by remember { mutableStateOf(food.proteinPer100g.toString()) }
    var fat by remember { mutableStateOf(food.fatPer100g.toString()) }
    var carbs by remember { mutableStateOf(food.carbPer100g.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_custom_food),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nameEs,
                        onValueChange = { nameEs = it },
                        label = { Text(stringResource(R.string.food_name_spanish)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text(stringResource(R.string.food_name_english)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.calories_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.protein_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.fat_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.carbs_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameEs.isNotBlank() && calories.isNotBlank()) {
                        val updatedFoodItem = food.copy(
                            nameEs = nameEs,
                            nameEn = nameEn.ifBlank { nameEs },
                            caloriesPer100g = calories.toFloatOrNull() ?: 0f,
                            proteinPer100g = protein.toFloatOrNull() ?: 0f,
                            fatPer100g = fat.toFloatOrNull() ?: 0f,
                            carbPer100g = carbs.toFloatOrNull() ?: 0f
                        )
                        onConfirm(updatedFoodItem)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(stringResource(R.string.save), color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (FoodItem) -> Unit
) {
    var nameEs by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_custom_food),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nameEs,
                        onValueChange = { nameEs = it },
                        label = { Text(stringResource(R.string.food_name_spanish)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text(stringResource(R.string.food_name_english)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.calories_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.protein_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.fat_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.carbs_per_100g_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = DarkCard,
                            focusedLabelColor = PrimaryGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGreen
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameEs.isNotBlank() && calories.isNotBlank()) {
                        val foodItem = FoodItem(
                            nameEs = nameEs,
                            nameEn = nameEn.ifBlank { nameEs },
                            caloriesPer100g = calories.toFloatOrNull() ?: 0f,
                            proteinPer100g = protein.toFloatOrNull() ?: 0f,
                            fatPer100g = fat.toFloatOrNull() ?: 0f,
                            carbPer100g = carbs.toFloatOrNull() ?: 0f,
                            verificationLevel = "user"
                        )
                        onConfirm(foodItem)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(stringResource(R.string.add), color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

