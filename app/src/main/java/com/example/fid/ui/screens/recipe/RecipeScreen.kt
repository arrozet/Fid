package com.example.fid.ui.screens.recipe

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.ui.components.SimpleMarkdownText
import com.example.fid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    navController: NavController,
    viewModel: RecipeViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val uiState by viewModel.uiState.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val recipeContent by viewModel.recipeContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val isAddingToLog by viewModel.isAddingToLog.collectAsState()
    val addToLogSuccess by viewModel.addToLogSuccess.collectAsState()
    val nutritionInfo by viewModel.nutritionInfo.collectAsState()
    
    // Mensajes localizados para el ViewModel
    val recipeSavedMessage = stringResource(R.string.recipe_saved)
    val recipeAddedMessage = stringResource(R.string.recipe_added_to_log)
    val atTasteText = stringResource(R.string.at_taste)
    val addAtLeastOneText = stringResource(R.string.add_at_least_one)
    val defaultRecipeNameText = stringResource(R.string.recipe_default_name)
    val couldNotExtractNutritionText = stringResource(R.string.could_not_extract_nutrition)
    val mustLoginToSaveText = stringResource(R.string.must_login_to_save)
    val userNotFoundText = stringResource(R.string.error_user_not_found)
    val errorSavingPattern = stringResource(R.string.error_saving)
    
    // Prompts de IA localizados
    val aiSystemPrompt = stringResource(R.string.ai_system_prompt)
    val aiUserMessageTemplate = stringResource(R.string.ai_user_message_template)
    
    // Placeholders localizados
    val ingredientExample = stringResource(R.string.ingredient_example)
    val quantityExample = stringResource(R.string.quantity_example)
    
    // Mostrar errores como Toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    // Mostrar éxito al guardar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, recipeSavedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    
    // Mostrar éxito al añadir al registro
    LaunchedEffect(addToLogSuccess) {
        if (addToLogSuccess) {
            Toast.makeText(context, recipeAddedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    
    // Auto-scroll cuando se genera contenido
    val scrollState = rememberScrollState()
    LaunchedEffect(recipeContent) {
        if (isGenerating && recipeContent.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chef_ia),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (recipeContent.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.resetRecipe()
                            }
                        ) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.new_recipe),
                                tint = PrimaryGreen
                            )
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            // Sección de ingredientes (oculta cuando hay receta generándose)
            AnimatedVisibility(
                visible = recipeContent.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    // Header de ingredientes
                    Text(
                        text = stringResource(R.string.what_do_you_have),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.add_ingredients_description),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Input de ingredientes
                    IngredientInputSection(
                        ingredientName = uiState.ingredientName,
                        ingredientQuantity = uiState.ingredientQuantity,
                        ingredientExample = ingredientExample,
                        quantityExample = quantityExample,
                        onNameChange = viewModel::updateIngredientName,
                        onQuantityChange = viewModel::updateIngredientQuantity,
                        onAddIngredient = {
                            viewModel.addIngredient(
                                uiState.ingredientName, 
                                uiState.ingredientQuantity,
                                atTasteText
                            )
                            focusManager.clearFocus()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Lista de ingredientes añadidos
                    if (ingredients.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.ingredients_added, ingredients.size),
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(ingredients) { index, ingredient ->
                                IngredientChip(
                                    ingredient = ingredient,
                                    onRemove = { viewModel.removeIngredient(index) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Botones de acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearIngredients() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextSecondary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        listOf(TextSecondary.copy(alpha = 0.5f), TextSecondary.copy(alpha = 0.5f))
                                    )
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.clear))
                            }
                            
                            Button(
                                onClick = { 
                                    viewModel.generateRecipe(
                                        errorNoIngredients = addAtLeastOneText,
                                        defaultRecipeName = defaultRecipeNameText,
                                        systemPrompt = aiSystemPrompt,
                                        userMessageTemplate = aiUserMessageTemplate
                                    ) 
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen,
                                    contentColor = DarkBackground
                                ),
                                enabled = !isGenerating && ingredients.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.generate_recipe),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Indicador de generación
                        if (isGenerating) {
                            Spacer(modifier = Modifier.height(16.dp))
                            StreamingIndicator()
                        }
                    } else {
                        // Estado vacío
                        EmptyIngredientsState()
                    }
                }
            }
            
            // Sección de receta generada
            AnimatedVisibility(
                visible = recipeContent.isNotEmpty() || isGenerating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Indicador de generación
                    if (isGenerating) {
                        StreamingIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Contenido de la receta
                    if (recipeContent.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkCard
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            SelectionContainer {
                                SimpleMarkdownText(
                                    text = recipeContent,
                                    modifier = Modifier.padding(16.dp),
                                    color = TextPrimary,
                                    boldColor = PrimaryGreen,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                            }
                            
                            // Cursor parpadeante mientras genera
                            if (isGenerating) {
                                BlinkingCursor()
                            }
                        }
                        
                        // Botones de acción para la receta
                        if (!isGenerating && nutritionInfo != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            RecipeActionButtons(
                                nutritionInfo = nutritionInfo!!,
                                isSaving = isSaving,
                                saveSuccess = saveSuccess,
                                isAddingToLog = isAddingToLog,
                                addToLogSuccess = addToLogSuccess,
                                onSave = { 
                                    viewModel.saveAsCustomFood(
                                        errorNoNutrition = couldNotExtractNutritionText,
                                        errorNotLoggedIn = mustLoginToSaveText,
                                        errorUserNotFound = userNotFoundText,
                                        errorSaving = errorSavingPattern
                                    ) 
                                },
                                onAddToLog = {
                                    viewModel.addToTodayLog(
                                        errorNoNutrition = couldNotExtractNutritionText,
                                        errorNotLoggedIn = mustLoginToSaveText,
                                        errorUserNotFound = userNotFoundText,
                                        errorSaving = errorSavingPattern
                                    )
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun IngredientInputSection(
    ingredientName: String,
    ingredientQuantity: String,
    ingredientExample: String,
    quantityExample: String,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAddIngredient: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo de nombre
                OutlinedTextField(
                    value = ingredientName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.ingredient)) },
                    placeholder = { Text(ingredientExample) },
                    modifier = Modifier.weight(2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryGreen,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Right) }
                    )
                )
                
                // Campo de cantidad
                OutlinedTextField(
                    value = ingredientQuantity,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.quantity)) },
                    placeholder = { Text(quantityExample) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryGreen,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onAddIngredient()
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botón añadir
            Button(
                onClick = onAddIngredient,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen.copy(alpha = 0.2f),
                    contentColor = PrimaryGreen
                ),
                enabled = ingredientName.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_ingredient))
            }
        }
    }
}

@Composable
private fun IngredientChip(
    ingredient: com.example.fid.data.ai.RecipeIngredient,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = ingredient.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ingredient.quantity,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = ErrorRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyIngredientsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = PrimaryGreen.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_ingredients),
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = stringResource(R.string.add_ingredients_hint),
            color = TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { this.alpha = alpha }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = stringResource(R.string.generating_recipe),
            color = PrimaryGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}

@Composable
private fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    
    Box(
        modifier = Modifier
            .padding(start = 16.dp, bottom = 16.dp)
            .width(8.dp)
            .height(20.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(PrimaryGreen, RoundedCornerShape(2.dp))
    )
}

@Composable
private fun RecipeActionButtons(
    nutritionInfo: RecipeNutritionInfo,
    isSaving: Boolean,
    saveSuccess: Boolean,
    isAddingToLog: Boolean,
    addToLogSuccess: Boolean,
    onSave: () -> Unit,
    onAddToLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Resumen nutricional
            Text(
                text = stringResource(R.string.nutrition_summary),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionChip(
                    label = stringResource(R.string.calories_label),
                    value = "${nutritionInfo.caloriesPerPortion.toInt()}",
                    unit = "kcal",
                    color = PrimaryGreen
                )
                NutritionChip(
                    label = stringResource(R.string.protein_label),
                    value = "${nutritionInfo.proteinPerPortion.toInt()}",
                    unit = "g",
                    color = ProteinColor
                )
                NutritionChip(
                    label = stringResource(R.string.carbs_label),
                    value = "${nutritionInfo.carbsPerPortion.toInt()}",
                    unit = "g",
                    color = CarbColor
                )
                NutritionChip(
                    label = stringResource(R.string.fat_label),
                    value = "${nutritionInfo.fatPerPortion.toInt()}",
                    unit = "g",
                    color = FatColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón de añadir a mi registro de hoy
            Button(
                onClick = onAddToLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (addToLogSuccess) SuccessGreen else WarningYellow,
                    contentColor = DarkBackground,
                    disabledContainerColor = WarningYellow.copy(alpha = 0.5f)
                ),
                enabled = !isAddingToLog && !addToLogSuccess,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isAddingToLog) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else if (addToLogSuccess) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.added_to_today),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.add_to_today),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (!addToLogSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.will_add_to_log, nutritionInfo.name),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botón de guardar en mis comidas
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (saveSuccess) SuccessGreen.copy(alpha = 0.1f) else DarkCard,
                    contentColor = if (saveSuccess) SuccessGreen else PrimaryGreen,
                    disabledContentColor = PrimaryGreen.copy(alpha = 0.5f)
                ),
                enabled = !isSaving && !saveSuccess,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (saveSuccess) SuccessGreen else PrimaryGreen,
                            if (saveSuccess) SuccessGreen else PrimaryGreen
                        )
                    )
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryGreen,
                        strokeWidth = 2.dp
                    )
                } else if (saveSuccess) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.saved),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.save_to_my_foods),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (!saveSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.will_be_saved_as, nutritionInfo.name),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SaveRecipeButton(
    nutritionInfo: RecipeNutritionInfo,
    isSaving: Boolean,
    saveSuccess: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Resumen nutricional
            Text(
                text = stringResource(R.string.nutrition_summary),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionChip(
                    label = stringResource(R.string.calories_label),
                    value = "${nutritionInfo.caloriesPerPortion.toInt()}",
                    unit = "kcal",
                    color = PrimaryGreen
                )
                NutritionChip(
                    label = stringResource(R.string.protein_label),
                    value = "${nutritionInfo.proteinPerPortion.toInt()}",
                    unit = "g",
                    color = ProteinColor
                )
                NutritionChip(
                    label = stringResource(R.string.carbs_label),
                    value = "${nutritionInfo.carbsPerPortion.toInt()}",
                    unit = "g",
                    color = CarbColor
                )
                NutritionChip(
                    label = stringResource(R.string.fat_label),
                    value = "${nutritionInfo.fatPerPortion.toInt()}",
                    unit = "g",
                    color = FatColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón de guardar
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saveSuccess) SuccessGreen else PrimaryGreen,
                    contentColor = DarkBackground,
                    disabledContainerColor = PrimaryGreen.copy(alpha = 0.5f)
                ),
                enabled = !isSaving && !saveSuccess,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else if (saveSuccess) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.saved),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.save_to_my_foods),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (!saveSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.will_be_saved_as, nutritionInfo.name),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NutritionChip(
    label: String,
    value: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = unit,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
