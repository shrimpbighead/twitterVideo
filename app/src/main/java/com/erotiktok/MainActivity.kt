package com.erotiktok

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.erotiktok.ui.navigation.EroTikHost
import com.erotiktok.ui.theme.EroTikTokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "=== onCreate called ===")
        enableEdgeToEdge()
        setContent {
            Log.d("MainActivity", "=== setContent called ===")
            EroTikTokTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Log.d("MainActivity", "=== Calling EroTikHost ===")
                    EroTikHost()
                }
            }
        }
    }
}
