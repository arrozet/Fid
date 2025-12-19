package com.example.fid.ui.screens.registration

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.example.fid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    var isAnalyzing by remember { mutableStateOf(false) }
    
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                if (isAnalyzing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.analyzing_food),
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.focus_your_food),
                        color = TextSecondary,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }
            
            // Capture button
            if (!isAnalyzing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(40.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            isAnalyzing = true
                            // Simulate AI processing
                            Toast.makeText(
                                context,
                                "Tomando foto...",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // In a real app, this would process the image with AI
                            // For now, just navigate back
                        },
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Text(
                            text = "📷",
                            fontSize = 36.sp
                        )
                    }
                }
            }
        }
    }
}
