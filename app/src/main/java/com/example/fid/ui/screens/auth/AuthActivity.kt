package com.example.fid.ui.screens.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fid.MainActivity
import com.example.fid.ui.theme.DarkBackground
import com.example.fid.ui.theme.FidTheme

/**
 * Standalone Activity for Authentication
 * This demonstrates the use of multiple activities as required
 */
class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    // You can use the AuthScreen composable here
                    // or create a navigation controller for this activity
                    
                    // For now, redirect to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

