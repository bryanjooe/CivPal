package com.example.civic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.example.civic.ui.navigation.CivicNavHost
import com.example.civic.ui.theme.CivicNavy
import com.example.civic.ui.theme.CivicSurface
import com.example.civic.ui.theme.CivicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CivicNavy.toArgb()),
            navigationBarStyle = SystemBarStyle.light(CivicSurface.toArgb(), CivicSurface.toArgb())
        )
        setContent {
            CivicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CivicNavHost()
                }
            }
        }
    }
}
