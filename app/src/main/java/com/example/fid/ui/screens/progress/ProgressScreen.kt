package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.FidDatabase
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FidRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FidRepository(FidDatabase.getDatabase(context)) }
    val scrollState = rememberScrollState()
    
    var user by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(Unit) {
        user = repository.getCurrentUser()
    }
    
    val isNumberlessMode = user?.numberlessMode ?: false
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.progress),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            com.example.fid.ui.screens.dashboard.BottomNavigationBar(navController)
        },
        containerColor = DarkBackground
    ) { padding ->
        if (isNumberlessMode) {
            NumberlessProgressView(Modifier.padding(padding))
        } else {
            DetailedProgressView(Modifier.padding(padding))
        }
    }
}

@Composable
fun NumberlessProgressView(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Avatar/Mascot
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(PrimaryGreen.copy(alpha = 0.2f), RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌱",
                fontSize = 80.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "¡Te sientes radiante!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Qualitative messages
        QualitativeCard(
            title = "Estado de hoy",
            message = stringResource(R.string.balanced_diet),
            color = PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QualitativeCard(
            title = "Energía",
            message = stringResource(R.string.high_energy),
            color = ProteinColor
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QualitativeCard(
            title = "Consejo suave",
            message = "Hoy has consumido mucho azúcar natural de la fruta. Para mantener tus niveles de energía, considera un snack rico en proteínas.",
            color = WarningYellow
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Wellness trends (without numbers)
        Text(
            text = "Tendencias de Bienestar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SimpleTrendCard("Hidratación", "Mejorando ↗", PrimaryGreen)
        Spacer(modifier = Modifier.height(12.dp))
        SimpleTrendCard("Diversidad Dietética", "Estable →", ProteinColor)
        Spacer(modifier = Modifier.height(12.dp))
        SimpleTrendCard("Consistencia", "Excelente ✓", PrimaryGreen)
    }
}

@Composable
fun DetailedProgressView(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // Weekly overview
        Text(
            text = "Resumen Semanal",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                StatRow("Promedio diario", "1,950 kcal", PrimaryGreen)
                Spacer(modifier = Modifier.height(12.dp))
                StatRow("Días en objetivo", "5 / 7", ProteinColor)
                Spacer(modifier = Modifier.height(12.dp))
                StatRow("Proteína promedio", "145g", ProteinColor)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Weight trend
        Text(
            text = "Tendencia de Peso",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkCard, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Gráfico de tendencia de peso",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Macro distribution
        Text(
            text = "Distribución de Macros (Última Semana)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MacroDistributionCard("Proteínas", "30%", ProteinColor, Modifier.weight(1f))
            MacroDistributionCard("Grasas", "30%", FatColor, Modifier.weight(1f))
            MacroDistributionCard("Carbos", "40%", CarbColor, Modifier.weight(1f))
        }
    }
}

@Composable
fun QualitativeCard(title: String, message: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = color,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun SimpleTrendCard(title: String, trend: String, color: androidx.compose.ui.graphics.Color) {
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
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = trend,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun MacroDistributionCard(label: String, percentage: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = percentage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

