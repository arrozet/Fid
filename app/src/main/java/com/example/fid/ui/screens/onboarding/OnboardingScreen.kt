package com.example.fid.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*

@Composable
fun OnboardingScreen(navController: NavController) {
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
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo or illustration area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Fid",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = stringResource(R.string.discover_new_way),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Features list
                FeatureItem(stringResource(R.string.effortless_ai_registration))
                Spacer(modifier = Modifier.height(16.dp))
                FeatureItem(stringResource(R.string.dynamic_personalized_goals))
                Spacer(modifier = Modifier.height(16.dp))
                FeatureItem(stringResource(R.string.healthy_conscious_relationship))
            }
            
            // Bottom buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Auth.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.start_journey),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { navController.navigate(Screen.Auth.route) }
                ) {
                    Text(
                        text = stringResource(R.string.already_have_account),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PrimaryGreen, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}

