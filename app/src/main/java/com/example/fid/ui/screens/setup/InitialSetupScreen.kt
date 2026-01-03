package com.example.fid.ui.screens.setup

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.example.fid.utils.LocaleHelper

@Composable
fun InitialSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.LANGUAGE_SPANISH) }
    var selectedUnit by remember { mutableStateOf("metric") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fid",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH) 
                        "Bienvenido" else "Welcome",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Configuremos tu experiencia" else "Let's set up your experience",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Language selection
                Text(
                    text = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Selecciona tu idioma" else "Select your language",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Spanish option
                SelectionCard(
                    title = "Español",
                    isSelected = selectedLanguage == LocaleHelper.LANGUAGE_SPANISH,
                    onClick = { selectedLanguage = LocaleHelper.LANGUAGE_SPANISH }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // English option
                SelectionCard(
                    title = "English",
                    isSelected = selectedLanguage == LocaleHelper.LANGUAGE_ENGLISH,
                    onClick = { selectedLanguage = LocaleHelper.LANGUAGE_ENGLISH }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Units selection
                Text(
                    text = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Unidades de medida" else "Measurement units",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Metric option
                SelectionCard(
                    title = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Métrico (kg, cm, g)" else "Metric (kg, cm, g)",
                    isSelected = selectedUnit == "metric",
                    onClick = { selectedUnit = "metric" }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Imperial option
                SelectionCard(
                    title = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Imperial (lb, ft, oz)" else "Imperial (lb, ft, oz)",
                    isSelected = selectedUnit == "imperial",
                    onClick = { selectedUnit = "imperial" }
                )
            }
            
            // Continue button
            Button(
                onClick = {
                    // Guardar las preferencias
                    val prefs = context.getSharedPreferences("fid_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("initial_setup_done", true)
                        .putString("measurement_unit", selectedUnit)
                        .apply()
                    
                    // Guardar idioma
                    LocaleHelper.setLanguage(context, selectedLanguage)
                    
                    // Navegar directamente a Auth y eliminar InitialSetup del stack
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.InitialSetup.route) { inclusive = true }
                    }
                    
                    // Reiniciar la actividad después de navegar para aplicar el idioma
                    activity?.recreate()
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
                    text = if (selectedLanguage == LocaleHelper.LANGUAGE_SPANISH)
                        "Continuar" else "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else DarkCard,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) PrimaryGreen else DarkCard,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
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
                color = if (isSelected) PrimaryGreen else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

