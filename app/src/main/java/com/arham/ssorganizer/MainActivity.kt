package com.arham.ssorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arham.ssorganizer.ui.theme.SSOrganizerTheme
import com.arham.ssorganizer.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SSOrganizerTheme {
                AppNavigation()
            }
        }
    }
}
