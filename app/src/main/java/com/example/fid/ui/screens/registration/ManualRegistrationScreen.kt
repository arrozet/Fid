package com.example.fid.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.example.fid.utils.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var frequentFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var suggestedFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        // Obtener alimentos frecuentes
        repository.getFrequentFoodItems().collect { foods ->
            android.util.Log.d("ManualRegistration", "Alimentos frecuentes obtenidos: ${foods.size}")
            foods.forEach { food ->
                android.util.Log.d("ManualRegistration", "  - ${food.getLocalizedName(context)}, ID: ${food.id}")
            }
            frequentFoods = foods
        }
    }
    
    LaunchedEffect(Unit) {
        // Obtener sugerencias (alimentos aleatorios para mostrar)
        val suggestions = repository.getSuggestedFoods(3)
        android.util.Log.d("ManualRegistration", "Sugerencias obtenidas: ${suggestions.size}")
        suggestions.forEach { food ->
            android.util.Log.d("ManualRegistration", "  - ${food.getLocalizedName(context)}, ID: ${food.id}")
        }
        suggestedFoods = suggestions
    }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            val currentLanguage = LocaleHelper.getCurrentLanguage(context)
            android.util.Log.d("ManualRegistration", "Buscando alimentos con query: '$searchQuery' en idioma: $currentLanguage")
            repository.searchFoodItems(searchQuery, currentLanguage).collect { foods ->
                android.util.Log.d("ManualRegistration", "Resultados de búsqueda obtenidos: ${foods.size}")
                foods.forEach { food ->
                    android.util.Log.d("ManualRegistration", "  - ${food.getLocalizedName(context)}, ID: ${food.id}")
                }
                searchResults = foods
            }
        } else {
            searchResults = emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manual_registration),
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
        ) {
            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_food)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = DarkCard,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            
            // Content
            if (searchQuery.isEmpty()) {
                // Show frequent foods and suggestions
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.frequent_recent),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    item {
                        if (frequentFoods.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(DarkCard, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_frequent_foods),
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(frequentFoods.size) { index ->
                                    val food = frequentFoods[index]
                                    FrequentFoodCard(food) {
                                        android.util.Log.d("ManualRegistration", "Clic en alimento frecuente: ${food.getLocalizedName(context)}, ID: ${food.id}")
                                        if (food.id > 0) {
                                            val route = Screen.FoodDetail.createRoute(food.id)
                                            android.util.Log.d("ManualRegistration", "Navegando a: $route")
                                            navController.navigate(route)
                                        } else {
                                            android.util.Log.e("ManualRegistration", "ERROR: ID inválido para alimento: ${food.getLocalizedName(context)}")
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    item {
                        Text(
                            text = stringResource(R.string.smart_suggestions),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Smart suggestions - alimentos reales de la base de datos
                    items(suggestedFoods.size) { index ->
                        val food = suggestedFoods[index]
                        SuggestionCard(
                            name = food.getLocalizedName(context),
                            calories = food.caloriesPer100g.toInt(),
                            onClick = {
                                android.util.Log.d("ManualRegistration", "Clic en sugerencia: ${food.getLocalizedName(context)}, ID: ${food.id}")
                                if (food.id > 0) {
                                    val route = Screen.FoodDetail.createRoute(food.id)
                                    android.util.Log.d("ManualRegistration", "Navegando a: $route")
                                    navController.navigate(route)
                                } else {
                                    android.util.Log.e("ManualRegistration", "ERROR: ID inválido para alimento: ${food.getLocalizedName(context)}")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Show search results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_results_found),
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(searchResults.size) { index ->
                            val food = searchResults[index]
                            FoodSearchResultCard(food) {
                                android.util.Log.d("ManualRegistration", "Clic en resultado de búsqueda: ${food.getLocalizedName(context)}, ID: ${food.id}")
                                if (food.id > 0) {
                                    val route = Screen.FoodDetail.createRoute(food.id)
                                    android.util.Log.d("ManualRegistration", "Navegando a: $route")
                                    navController.navigate(route)
                                } else {
                                    android.util.Log.e("ManualRegistration", "ERROR: ID inválido para alimento: ${food.getLocalizedName(context)}")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrequentFoodCard(food: FoodItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val localizedName = food.getLocalizedName(context)
    android.util.Log.d("ManualRegistration", "Renderizando FrequentFoodCard: $localizedName, ID: ${food.id}")
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(140.dp)
            .clickable(
                onClick = {
                    android.util.Log.d("ManualRegistration", "CLICK DETECTADO en FrequentFoodCard: $localizedName")
                    onClick()
                }
            )
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = localizedName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${food.caloriesPer100g.toInt()} kcal",
                fontSize = 12.sp,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
fun SuggestionCard(name: String, calories: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.suggested_for_this_time),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Text(
                text = "$calories kcal",
                fontSize = 14.sp,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FoodSearchResultCard(food: FoodItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val localizedName = food.getLocalizedName(context)
    android.util.Log.d("ManualRegistration", "Renderizando FoodSearchResultCard: $localizedName, ID: ${food.id}")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    android.util.Log.d("ManualRegistration", "CLICK DETECTADO en FoodSearchResultCard: $localizedName")
                    onClick()
                }
            )
            .background(DarkCard, RoundedCornerShape(12.dp))
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
                Text(
                    text = stringResource(R.string.calories_per_100g, food.caloriesPer100g.toInt()),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // Verification badge
            Box(
                modifier = Modifier
                    .background(
                        when (food.verificationLevel) {
                            "government" -> PrimaryGreen.copy(alpha = 0.2f)
                            "manufacturer" -> WarningYellow.copy(alpha = 0.2f)
                            else -> DarkSurface
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (food.verificationLevel) {
                        "government" -> "✓"
                        "manufacturer" -> "✓"
                        "community" -> "~"
                        else -> "?"
                    },
                    fontSize = 12.sp,
                    color = when (food.verificationLevel) {
                        "government" -> PrimaryGreen
                        "manufacturer" -> WarningYellow
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}

