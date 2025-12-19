package com.example.fid.ui.screens.registration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.fid.ui.theme.DarkBackground
import com.example.fid.ui.theme.FidTheme

/**
 * Standalone Activity for Photo Registration with Camera
 * This demonstrates the use of multiple activities and camera permissions
 */
class PhotoRegistrationActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check camera permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                setupContent()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun setupContent() {
        setContent {
            FidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    // Camera preview and capture UI would go here
                    // For now, show a placeholder
                    Toast.makeText(this, "Actividad de cámara iniciada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

